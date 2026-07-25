package com.waheed.universaldownloader.data.repository

import com.waheed.universaldownloader.data.local.DownloadDao
import com.waheed.universaldownloader.data.local.DownloadEntity
import com.waheed.universaldownloader.engine.YtDlpEngine
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class DownloadResult {
    data class Progress(val percent: Float, val etaSeconds: Long) : DownloadResult()
    data class Success(val entity: DownloadEntity) : DownloadResult()
    data class Failure(val message: String) : DownloadResult()
}

@Singleton
class DownloadRepository @Inject constructor(
    private val engine: YtDlpEngine,
    private val dao: DownloadDao
) {
    fun getRecentDownloads(limit: Int = 5): Flow<List<DownloadEntity>> = dao.getRecentDownloads(limit)
    fun getAllDownloads(): Flow<List<DownloadEntity>> = dao.getAllDownloads()

    suspend fun downloadAndSave(
        url: String,
        title: String,
        thumbnailUrl: String?,
        siteName: String,
        outputDir: String,
        isAudioOnly: Boolean,
        formatSelector: String,
        onProgress: (Float, Long) -> Unit
    ): Result<DownloadEntity> {
        val engineReady = engine.initialize()
        if (!engineReady) {
            return Result.failure(Exception("Download engine failed to initialize"))
        }

        val downloadResult = if (isAudioOnly) {
            engine.downloadAudioOnly(url, outputDir) { percent, eta, _ -> onProgress(percent, eta) }
        } else {
            engine.download(url, outputDir, formatSelector) { percent, eta, _ -> onProgress(percent, eta) }
        }

        return downloadResult.fold(
            onSuccess = {
                val downloadedFile = File(outputDir).listFiles()
                    ?.maxByOrNull { it.lastModified() }
                val filePath = downloadedFile?.absolutePath ?: ""
                val fileSize = downloadedFile?.length() ?: 0L

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
                Result.success(entity)
            },
            onFailure = { Result.failure(it) }
        )
    }
}
