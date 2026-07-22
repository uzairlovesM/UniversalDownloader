package com.waheed.universaldownloader.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps the bundled yt-dlp standalone binary.
 * On first run, copies the binary from assets into the app's private
 * files directory (where it can be marked executable) and invokes it
 * via ProcessBuilder for every subsequent operation.
 */
@Singleton
class YtDlpEngine @Inject constructor(
    private val context: Context
) {
    private val binaryName = "yt-dlp"
    private val binaryFile: File by lazy { File(context.filesDir, binaryName) }

    /** Copies the binary out of assets and chmod +x's it. Call once at app startup. */
    suspend fun ensureBinaryReady(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!binaryFile.exists() || binaryFile.length() == 0L) {
                context.assets.open("bin/$binaryName").use { input ->
                    binaryFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                binaryFile.setExecutable(true, false)
            }
            binaryFile.exists() && binaryFile.canExecute()
        } catch (e: Exception) {
            false
        }
    }

    /** Fetches metadata (title, formats, thumbnail) for a URL without downloading it. */
    suspend fun fetchInfo(url: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val process = ProcessBuilder(
                binaryFile.absolutePath,
                "--dump-json",
                "--no-playlist",
                url
            ).redirectErrorStream(false).start()

            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                throw RuntimeException("yt-dlp failed (code $exitCode): $error")
            }
            output
        }
    }

    /** Downloads a URL at the given format/quality into the target directory. */
    suspend fun download(
        url: String,
        outputDir: String,
        formatSelector: String = "best",
        onProgress: (String) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val process = ProcessBuilder(
                binaryFile.absolutePath,
                "-f", formatSelector,
                "-o", "$outputDir/%(title)s.%(ext)s",
                "--newline",
                url
            ).redirectErrorStream(true).start()

            val outputLines = StringBuilder()
            process.inputStream.bufferedReader().forEachLine { line ->
                onProgress(line)
                outputLines.appendLine(line)
            }
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw RuntimeException("Download failed (code $exitCode)")
            }
            outputLines.toString()
        }
    }

    fun getEngineVersion(): String? {
        return try {
            val process = ProcessBuilder(binaryFile.absolutePath, "--version").start()
            process.inputStream.bufferedReader().readText().trim().also { process.waitFor() }
        } catch (e: Exception) {
            null
        }
    }
}
