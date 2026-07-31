package com.waheed.universaldownloader.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.waheed.universaldownloader.data.repository.DownloadRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private const val CHANNEL_ID = "download_progress_channel"
private const val NOTIFICATION_ID = 1001

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: DownloadRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_URL = "url"
        const val KEY_TITLE = "title"
        const val KEY_THUMBNAIL = "thumbnail"
        const val KEY_SITE = "site"
        const val KEY_OUTPUT_DIR = "output_dir"
        const val KEY_IS_AUDIO = "is_audio"
        const val KEY_FORMAT = "format"
        const val KEY_ERROR_MESSAGE = "error_message"
        const val KEY_RESULT_ENTITY_ID = "result_entity_id"
    }

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: "Download"
        val thumbnail = inputData.getString(KEY_THUMBNAIL)
        val site = inputData.getString(KEY_SITE) ?: "unknown"
        val outputDir = inputData.getString(KEY_OUTPUT_DIR) ?: return Result.failure()
        val isAudio = inputData.getBoolean(KEY_IS_AUDIO, false)
        val format = inputData.getString(KEY_FORMAT) ?: "best"

        createNotificationChannel()
        setForeground(buildForegroundInfo(title, 0))

        val result = repository.downloadAndSave(
            url = url,
            title = title,
            thumbnailUrl = thumbnail,
            siteName = site,
            outputDir = outputDir,
            isAudioOnly = isAudio,
            formatSelector = format,
            onProgress = { progress ->
                setProgressAsync(
                    workDataOf(
                        "progress" to progress.percent,
                        "speed" to (progress.speed ?: ""),
                        "eta" to progress.etaSeconds,
                        "attempt" to progress.attempt
                    )
                )
                updateNotification(title, progress.percent.toInt(), progress.speed)
            }
        )

        return result.fold(
            onSuccess = { entity ->
                Result.success(workDataOf(KEY_RESULT_ENTITY_ID to entity.id))
            },
            onFailure = { error ->
                Result.failure(workDataOf(KEY_ERROR_MESSAGE to (error.message ?: "Download failed")))
            }
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Download progress",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundInfo(title: String, progress: Int, speed: String? = null): ForegroundInfo {
        val speedText = if (speed != null) " • $speed" else ""
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Downloading… $progress%$speedText")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(title: String, progress: Int, speed: String? = null) {
        val speedText = if (speed != null) " • $speed" else ""
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Downloading… $progress%$speedText")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
    }
}
