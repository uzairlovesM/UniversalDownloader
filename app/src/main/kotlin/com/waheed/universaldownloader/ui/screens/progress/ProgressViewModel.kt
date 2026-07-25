package com.waheed.universaldownloader.ui.screens.progress

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.universaldownloader.data.local.DownloadEntity
import com.waheed.universaldownloader.data.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class DownloadProgressState {
    data class InProgress(val percent: Float, val etaSeconds: Long) : DownloadProgressState()
    data class Completed(val entity: DownloadEntity) : DownloadProgressState()
    data class Failed(val message: String) : DownloadProgressState()
}

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val repository: DownloadRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<DownloadProgressState>(DownloadProgressState.InProgress(0f, 0))
    val state: StateFlow<DownloadProgressState> = _state

    fun startDownload(
        url: String,
        title: String,
        thumbnailUrl: String?,
        siteName: String,
        isAudioOnly: Boolean,
        formatSelector: String
    ) {
        viewModelScope.launch {
            val outputDir = File(context.getExternalFilesDir(null), "Downloads").apply { mkdirs() }

            val result = repository.downloadAndSave(
                url = url,
                title = title,
                thumbnailUrl = thumbnailUrl,
                siteName = siteName,
                outputDir = outputDir.absolutePath,
                isAudioOnly = isAudioOnly,
                formatSelector = formatSelector,
                onProgress = { percent, eta ->
                    _state.value = DownloadProgressState.InProgress(percent, eta)
                }
            )

            result.fold(
                onSuccess = { entity -> _state.value = DownloadProgressState.Completed(entity) },
                onFailure = { e -> _state.value = DownloadProgressState.Failed(e.message ?: "Download failed") }
            )
        }
    }
}
