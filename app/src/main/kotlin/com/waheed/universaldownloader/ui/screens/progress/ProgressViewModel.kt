package com.waheed.universaldownloader.ui.screens.progress

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.waheed.universaldownloader.data.local.DownloadDao
import com.waheed.universaldownloader.data.local.DownloadEntity
import com.waheed.universaldownloader.worker.DownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

sealed class DownloadProgressState {
    data class InProgress(val percent: Float, val etaSeconds: Long) : DownloadProgressState()
    data class Completed(val entity: DownloadEntity) : DownloadProgressState()
    data class Failed(val message: String) : DownloadProgressState()
}

@HiltViewModel
class ProgressViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao
) : ViewModel() {

    private val _state = MutableStateFlow<DownloadProgressState>(DownloadProgressState.InProgress(0f, 0))
    val state: StateFlow<DownloadProgressState> = _state

    private val workManager = WorkManager.getInstance(context)
    private var activeWorkId: UUID? = null

    fun startDownload(
        url: String,
        title: String,
        thumbnailUrl: String?,
        siteName: String,
        isAudioOnly: Boolean,
        formatSelector: String
    ) {
        val outputDir = File(context.getExternalFilesDir(null), "Downloads").apply { mkdirs() }

        val inputData = workDataOf(
            DownloadWorker.KEY_URL to url,
            DownloadWorker.KEY_TITLE to title,
            DownloadWorker.KEY_THUMBNAIL to (thumbnailUrl ?: ""),
            DownloadWorker.KEY_SITE to siteName,
            DownloadWorker.KEY_OUTPUT_DIR to outputDir.absolutePath,
            DownloadWorker.KEY_IS_AUDIO to isAudioOnly,
            DownloadWorker.KEY_FORMAT to formatSelector
        )

        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .build()

        activeWorkId = workRequest.id
        workManager.enqueue(workRequest)
        observeWork(workRequest.id)
    }

    private fun observeWork(workId: UUID) {
        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(workId).collect { workInfo ->
                if (workInfo == null) return@collect

                when (workInfo.state) {
                    WorkInfo.State.RUNNING -> {
                        val progress = workInfo.progress.getFloat("progress", 0f)
                        _state.value = DownloadProgressState.InProgress(progress, 0)
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val entityId = workInfo.outputData.getLong(DownloadWorker.KEY_RESULT_ENTITY_ID, -1L)
                        if (entityId != -1L) {
                            val entity = downloadDao.getById(entityId)
                            if (entity != null) {
                                _state.value = DownloadProgressState.Completed(entity)
                            } else {
                                _state.value = DownloadProgressState.Failed("Download saved but could not be loaded")
                            }
                        } else {
                            _state.value = DownloadProgressState.Failed("Download completed with no result")
                        }
                    }
                    WorkInfo.State.FAILED -> {
                        val message = workInfo.outputData.getString(DownloadWorker.KEY_ERROR_MESSAGE)
                            ?: "Download failed"
                        _state.value = DownloadProgressState.Failed(message)
                    }
                    WorkInfo.State.CANCELLED -> {
                        _state.value = DownloadProgressState.Failed("Download cancelled")
                    }
                    else -> { /* ENQUEUED, BLOCKED — keep showing current progress state */ }
                }
            }
        }
    }

    fun cancelDownload() {
        activeWorkId?.let { workManager.cancelWorkById(it) }
    }
}
