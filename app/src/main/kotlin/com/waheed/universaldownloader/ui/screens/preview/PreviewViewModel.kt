package com.waheed.universaldownloader.ui.screens.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.universaldownloader.engine.YtDlpEngine
import com.yausername.youtubedl_android.mapper.VideoInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PreviewUiState {
    object Loading : PreviewUiState()
    data class Success(val info: VideoInfo) : PreviewUiState()
    data class Error(val message: String) : PreviewUiState()
}

@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val engine: YtDlpEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow<PreviewUiState>(PreviewUiState.Loading)
    val uiState: StateFlow<PreviewUiState> = _uiState

    fun fetchInfo(url: String) {
        viewModelScope.launch {
            _uiState.value = PreviewUiState.Loading

            val engineReady = engine.initialize()
            if (!engineReady) {
                _uiState.value = PreviewUiState.Error("Download engine failed to start. Please try again.")
                return@launch
            }

            engine.fetchInfo(url).fold(
                onSuccess = { info -> _uiState.value = PreviewUiState.Success(info) },
                onFailure = { e ->
                    _uiState.value = PreviewUiState.Error(
                        e.message ?: "Could not fetch info for this link. Check the URL and try again."
                    )
                }
            )
        }
    }
}
