package com.waheed.universaldownloader.ui.screens.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.waheed.universaldownloader.data.local.DownloadDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PlayerUiState {
    object Loading : PlayerUiState()
    data class Ready(
        val title: String,
        val isAudio: Boolean,
        val isPlaying: Boolean,
        val currentPositionMs: Long,
        val durationMs: Long,
        val bufferedPercentage: Int,
        val playbackSpeed: Float,
        val isMuted: Boolean
    ) : PlayerUiState()
    data class Error(val message: String) : PlayerUiState()
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    application: Application,
    private val downloadDao: DownloadDao,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val fileId: Long = checkNotNull(savedStateHandle["fileId"])

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var previousVolume = 1f
    private var loadedTitle: String = ""
    private var loadedIsAudio: Boolean = false

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(application).build().apply {
        addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateReadyState { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val current = _uiState.value
                    if (current is PlayerUiState.Loading) {
                        _uiState.value = PlayerUiState.Ready(
                            title = loadedTitle,
                            isAudio = loadedIsAudio,
                            isPlaying = isPlaying,
                            currentPositionMs = currentPosition,
                            durationMs = duration.coerceAtLeast(0),
                            bufferedPercentage = bufferedPercentage,
                            playbackSpeed = playbackParameters.speed,
                            isMuted = volume == 0f
                        )
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _uiState.value = PlayerUiState.Error(error.message ?: "Playback failed")
            }
        })
    }

    init {
        loadMedia()
    }

    private fun loadMedia() {
        viewModelScope.launch {
            val download = downloadDao.getById(fileId)
            if (download == null) {
                _uiState.value = PlayerUiState.Error("File not found")
                return@launch
            }
            loadedTitle = download.title
            loadedIsAudio = download.isAudio

            val mediaItem = MediaItem.Builder()
                .setUri(download.filePath)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(download.title)
                        .build()
                )
                .build()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    private inline fun updateReadyState(transform: (PlayerUiState.Ready) -> PlayerUiState.Ready) {
        val current = _uiState.value
        if (current is PlayerUiState.Ready) {
            _uiState.value = transform(current)
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        updateReadyState { it.copy(currentPositionMs = positionMs) }
    }

    fun seekForward(millis: Long = 10_000) {
        seekTo((exoPlayer.currentPosition + millis).coerceAtMost(exoPlayer.duration.coerceAtLeast(0)))
    }

    fun seekBackward(millis: Long = 10_000) {
        seekTo((exoPlayer.currentPosition - millis).coerceAtLeast(0))
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed)
        updateReadyState { it.copy(playbackSpeed = speed) }
    }

    fun toggleMute() {
        if (exoPlayer.volume > 0f) {
            previousVolume = exoPlayer.volume
            exoPlayer.volume = 0f
            updateReadyState { it.copy(isMuted = true) }
        } else {
            exoPlayer.volume = previousVolume
            updateReadyState { it.copy(isMuted = false) }
        }
    }

    fun refreshPosition() {
        updateReadyState {
            it.copy(
                currentPositionMs = exoPlayer.currentPosition,
                bufferedPercentage = exoPlayer.bufferedPercentage
            )
        }
    }

    override fun onCleared() {
        exoPlayer.release()
        super.onCleared()
    }
}
