package com.waheed.universaldownloader.engine

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo
import com.yausername.ffmpeg.FFmpeg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps youtubedl-android (GPL-3.0), which bundles yt-dlp + a Python runtime
 * and an ffmpeg binary internally. This class is our single point of contact
 * with the download engine — the rest of the app never talks to YoutubeDL directly.
 */
@Singleton
class YtDlpEngine @Inject constructor(
    private val context: Context
) {
    private var isInitialized = false

    /** Must be called once (e.g. in UDApplication.onCreate) before any other method. */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext true
        try {
            YoutubeDL.getInstance().init(context)
            FFmpeg.getInstance().init(context)
            isInitialized = true
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Fetches metadata (title, thumbnail, available formats) without downloading. */
    suspend fun fetchInfo(url: String): Result<VideoInfo> = withContext(Dispatchers.IO) {
        runCatching {
            YoutubeDL.getInstance().getInfo(url)
        }
    }

    /**
     * Downloads a URL at the given format/quality into outputDir.
     * formatSelector examples: "best", "bestaudio", "bestvideo[height<=720]+bestaudio"
     */
    suspend fun download(
        url: String,
        outputDir: String,
        formatSelector: String = "best",
        onProgress: (progressPercent: Float, etaSeconds: Long, line: String) -> Unit = { _, _, _ -> }
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val request = YoutubeDLRequest(url).apply {
                addOption("-f", formatSelector)
                addOption("-o", "$outputDir/%(title)s.%(ext)s")
                addOption("--no-mtime")
            }
            val response = YoutubeDL.getInstance().execute(request) { progress, eta, line ->
                onProgress(progress, eta, line)
            }
            response.out
        }
    }

    /** Extracts audio-only (MP3) from a URL — used for the "Audio" quality option. */
    suspend fun downloadAudioOnly(
        url: String,
        outputDir: String,
        onProgress: (progressPercent: Float, etaSeconds: Long, line: String) -> Unit = { _, _, _ -> }
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val request = YoutubeDLRequest(url).apply {
                addOption("-x")
                addOption("--audio-format", "mp3")
                addOption("-o", "$outputDir/%(title)s.%(ext)s")
                addOption("--no-mtime")
            }
            val response = YoutubeDL.getInstance().execute(request) { progress, eta, line ->
                onProgress(progress, eta, line)
            }
            response.out
        }
    }

    fun getEngineVersion(): String {
        return try {
            YoutubeDL.getInstance().version(context) ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
}
