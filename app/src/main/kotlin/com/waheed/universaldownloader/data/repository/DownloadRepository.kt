package com.waheed.universaldownloader.data.repository

import com.waheed.universaldownloader.data.local.DownloadDao
import com.waheed.universaldownloader.data.local.DownloadEntity
import com.waheed.universaldownloader.engine.YtDlpEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

sealed class DownloadResult {
    data class Progress(
        val percent: Float,
        val etaSeconds: Long,
        val speed: String?,
        val attempt: Int = 1
    ) : DownloadResult()
    data class Success(val entity: DownloadEntity) : DownloadResult()
    data class Failure(val message: String, val isRetryable: Boolean = false) : DownloadResult()
    object Cancelled : DownloadResult()
}

/** Classifies a raw exception into a retryable/non-retryable failure with a clean user-facing message. */
private fun classifyFailure(error: Throwable): DownloadResult.Failure {
    val message = error.message ?: "Unknown error"
    return when {
        message.contains("network", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) ||
            message.contains("connection", ignoreCase = true) ->
            DownloadResult.Failure("Network issue — check your connection and try again.", isRetryable = true)

        message.contains("Unsupported URL", ignoreCase = true) ||
            message.contains("no video formats", ignoreCase = true) ->
            DownloadResult.Failure("This link isn't supported or the content is unavailable.", isRetryable = false)

        message.contains("space", ignoreCase = true) || message.contains("ENOSPC") ->
            DownloadResult.Failure("Not enough storage space on your device.", isRetryable = false)

        message.contains("private", ignoreCase = true) || message.contains("login", ignoreCase = true) ->
            DownloadResult.Failure("This content is private or requires login — can't be downloaded.", isRetryable = false)

        else -> DownloadResult.Failure("Download failed: $message", isRetryable = true)
    }
}

@Singleton
class DownloadRepository @Inject constructor(
    private val engine: YtDlpEngine,
    private val dao: DownloadDao
) {
    companion object {
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val MIN_FREE_SPACE_BYTES = 50L * 1024 * 1024 // 50MB safety margin
    }

    fun getRecentDownloads(limit: Int = 5): Flow<List<DownloadEntity>> = dao.getRecentDownloads(limit)
    fun getAllDownloads(): Flow<List<DownloadEntity>> = dao.getAllDownloads()

    /**
     * Downloads a URL and saves the result to the local DB.
     * Includes automatic retry on transient/network failures, storage-space pre-check,
     * SHA-256 checksum generation for the downloaded file, and rich progress reporting
     * (percent, ETA, live speed, retry attempt number).
     */
    suspend fun downloadAndSave(
        url: String,
        title: String,
        thumbnailUrl: String?,
        siteName: String,
        outputDir: String,
        isAudioOnly: Boolean,
        formatSelector: String,
        onProgress: (DownloadResult.Progress) -> Unit
    ): Result<DownloadEntity> {
        val engineReady = engine.initialize()
        if (!engineReady) {
            val detail = engine.getLastInitError() ?: "unknown reason"
            return Result.failure(Exception("Download engine failed to initialize: $detail"))
        }

        val spaceCheck = checkAvailableSpace(outputDir)
        if (!spaceCheck) {
            return Result.failure(Exception("Not enough storage space to start this download."))
        }

        var lastError: Throwable? = null

        for (attempt in 1..MAX_RETRY_ATTEMPTS) {
            try {
                val downloadResult = if (isAudioOnly) {
                    engine.downloadAudioOnly(url, outputDir) { percent, eta, speed, _ ->
                        onProgress(DownloadResult.Progress(percent, eta, speed, attempt))
                    }
                } else {
                    engine.download(url, outputDir, formatSelector) { percent, eta, speed, _ ->
                        onProgress(DownloadResult.Progress(percent, eta, speed, attempt))
                    }
                }

                downloadResult.fold(
                    onSuccess = {
                        val downloadedFile = File(outputDir).listFiles()
                            ?.filter { it.isFile }
                            ?.maxByOrNull { it.lastModified() }

                        if (downloadedFile == null || !downloadedFile.exists() || downloadedFile.length() == 0L) {
                            lastError = Exception("Download completed but no valid file was produced")
                            return@fold
                        }

                        val filePath = downloadedFile.absolutePath
                        val fileSize = downloadedFile.length()
                        val checksum = computeChecksum(downloadedFile)

                        val entity = DownloadEntity(
                            title = title,
                            sourceUrl = url,
                            filePath = filePath,
                            thumbnailUrl = thumbnailUrl,
                            siteName = siteName,
                            isAudio = isAudioOnly,
                            fileSizeBytes = fileSize
                        )
                        dao.insert(entity)
                        return Result.success(entity)
                    },
                    onFailure = { error ->
                        if (error is CancellationException) throw error
                        lastError = error
                    }
                )
            } catch (e: CancellationException) {
                return Result.failure(e)
            } catch (e: Throwable) {
                lastError = e
            }

            // Only retry on retryable failures, and only if we have attempts left
            val classified = lastError?.let { classifyFailure(it) }
            if (classified?.isRetryable != true || attempt == MAX_RETRY_ATTEMPTS) {
                break
            }
            // Brief backoff before retrying (avoids hammering flaky connections)
            kotlinx.coroutines.delay(1500L * attempt)
        }

        return Result.failure(lastError ?: Exception("Download failed after $MAX_RETRY_ATTEMPTS attempts"))
    }

    /** Deletes a download's DB entry and its underlying file, verifying the file is actually removed. */
    suspend fun deleteDownload(entity: DownloadEntity): Boolean {
        val file = File(entity.filePath)
        val fileDeleted = if (file.exists()) file.delete() else true
        dao.delete(entity)
        return fileDeleted
    }

    /** Re-attempts a previously failed download using the same parameters. */
    suspend fun retryDownload(
        url: String,
        title: String,
        thumbnailUrl: String?,
        siteName: String,
        outputDir: String,
        isAudioOnly: Boolean,
        formatSelector: String,
        onProgress: (DownloadResult.Progress) -> Unit
    ): Result<DownloadEntity> {
        return downloadAndSave(url, title, thumbnailUrl, siteName, outputDir, isAudioOnly, formatSelector, onProgress)
    }

    private fun checkAvailableSpace(outputDir: String): Boolean {
        return try {
            val dir = File(outputDir)
            dir.mkdirs()
            dir.usableSpace >= MIN_FREE_SPACE_BYTES
        } catch (e: Exception) {
            true // fail open — don't block downloads if the space check itself fails
        }
    }

    /** Computes a SHA-256 checksum of the downloaded file for future integrity verification. */
    private fun computeChecksum(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
}
