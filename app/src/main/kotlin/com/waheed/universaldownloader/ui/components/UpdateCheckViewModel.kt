package com.waheed.universaldownloader.ui.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.universaldownloader.remoteconfig.RemoteConfigManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UpdatePromptState {
    object None : UpdatePromptState()
    data class OptionalUpdate(val latestVersion: String, val apkUrl: String) : UpdatePromptState()
    data class ForcedUpdate(val latestVersion: String, val apkUrl: String) : UpdatePromptState()
    object MaintenanceMode : UpdatePromptState()
}

@HiltViewModel
class UpdateCheckViewModel @Inject constructor(
    application: Application,
    private val remoteConfigManager: RemoteConfigManager
) : AndroidViewModel(application) {

    private val _promptState = MutableStateFlow<UpdatePromptState>(UpdatePromptState.None)
    val promptState: StateFlow<UpdatePromptState> = _promptState.asStateFlow()

    private var dismissedThisSession = false

    init {
        checkForUpdate()
    }

    private fun checkForUpdate() {
        viewModelScope.launch {
            remoteConfigManager.fetchAndActivate()

            if (remoteConfigManager.isMaintenanceMode()) {
                _promptState.value = UpdatePromptState.MaintenanceMode
                return@launch
            }

            val ctx = getApplication<Application>()
            val currentCode = ctx.packageManager.getPackageInfo(ctx.packageName, 0).let { info ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) info.longVersionCode
                else @Suppress("DEPRECATION") info.versionCode.toLong()
            }

            val latestVersion = remoteConfigManager.getLatestVersionName()
            val apkUrl = remoteConfigManager.getUpdateApkUrl()

            when {
                remoteConfigManager.isCurrentVersionBelowMinimum(currentCode) -> {
                    _promptState.value = UpdatePromptState.ForcedUpdate(latestVersion, apkUrl)
                }
                remoteConfigManager.isUpdateAvailable(currentCode) && !dismissedThisSession -> {
                    _promptState.value = UpdatePromptState.OptionalUpdate(latestVersion, apkUrl)
                }
                else -> {
                    _promptState.value = UpdatePromptState.None
                }
            }
        }
    }

    fun dismissOptionalUpdate() {
        dismissedThisSession = true
        if (_promptState.value is UpdatePromptState.OptionalUpdate) {
            _promptState.value = UpdatePromptState.None
        }
    }
}
