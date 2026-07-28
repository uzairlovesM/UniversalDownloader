package com.waheed.universaldownloader.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.universaldownloader.data.local.DownloadDao
import com.waheed.universaldownloader.data.settings.DefaultQuality
import com.waheed.universaldownloader.data.settings.SettingsDataStore
import com.waheed.universaldownloader.data.settings.PinManager
import com.waheed.universaldownloader.data.settings.ThemeMode
import com.waheed.universaldownloader.remoteconfig.RemoteConfigManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultQuality: DefaultQuality = DefaultQuality.BEST,
    val wifiOnlyDownloads: Boolean = false,
    val notifyOnComplete: Boolean = true,
    val autoDeleteAfterShare: Boolean = false,
    val downloadFolderName: String = "UniversalDownloader",
    val cacheSizeBytes: Long = 0L,
    val downloadCount: Int = 0,
    val isPinLockEnabled: Boolean = false,
    val appVersion: String = "",
    val updateAvailable: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val settingsDataStore: SettingsDataStore,
    private val pinManager: PinManager,
    private val downloadDao: DownloadDao,
    private val remoteConfigManager: RemoteConfigManager
) : AndroidViewModel(application) {

    private val _cacheSizeBytes = MutableStateFlow(0L)
    private val _isPinLockEnabled = MutableStateFlow(false)
    private val _appVersion = MutableStateFlow("")
    private val _updateAvailable = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsDataStore.themeMode,
        settingsDataStore.defaultQuality,
        settingsDataStore.wifiOnlyDownloads
    ) { theme, quality, wifiOnly ->
        Triple(theme, quality, wifiOnly)
    }.combine(settingsDataStore.notifyOnComplete) { (theme, quality, wifiOnly), notify ->
        SettingsUiState(
            themeMode = theme,
            defaultQuality = quality,
            wifiOnlyDownloads = wifiOnly,
            notifyOnComplete = notify
        )
    }.combine(settingsDataStore.autoDeleteAfterShare) { state, autoDelete ->
        state.copy(autoDeleteAfterShare = autoDelete)
    }.combine(settingsDataStore.downloadFolderName) { state, folder ->
        state.copy(downloadFolderName = folder)
    }.combine(downloadDao.getTotalCount()) { state, count ->
        state.copy(downloadCount = count)
    }.combine(_cacheSizeBytes) { state, cache ->
        state.copy(cacheSizeBytes = cache)
    }.combine(_isPinLockEnabled) { state, pin ->
        state.copy(isPinLockEnabled = pin)
    }.combine(_appVersion) { state, version ->
        state.copy(appVersion = version)
    }.combine(_updateAvailable) { state, update ->
        state.copy(updateAvailable = update)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    init {
        refreshCacheSize()
        checkForUpdates()
        loadAppVersion()
        _isPinLockEnabled.value = pinManager.isPinSet()
    }

    private fun loadAppVersion() {
        val ctx = getApplication<Application>()
        val pkgInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        _appVersion.value = pkgInfo.versionName ?: "unknown"
    }

    private fun checkForUpdates() {
        viewModelScope.launch {
            remoteConfigManager.fetchAndActivate()
            val ctx = getApplication<Application>()
            val currentCode = ctx.packageManager.getPackageInfo(ctx.packageName, 0).let { info ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) info.longVersionCode
                else @Suppress("DEPRECATION") info.versionCode.toLong()
            }
            _updateAvailable.value = remoteConfigManager.isUpdateAvailable(currentCode)
        }
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            val cacheDir = getApplication<Application>().cacheDir
            _cacheSizeBytes.value = cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            val cacheDir = getApplication<Application>().cacheDir
            cacheDir.walkTopDown().filter { it.isFile }.forEach { it.delete() }
            refreshCacheSize()
        }
    }

    fun clearAllDownloadHistory() {
        viewModelScope.launch {
            downloadDao.deleteAll()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsDataStore.setThemeMode(mode) }
    }

    fun setDefaultQuality(quality: DefaultQuality) {
        viewModelScope.launch { settingsDataStore.setDefaultQuality(quality) }
    }

    fun setWifiOnlyDownloads(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setWifiOnlyDownloads(enabled) }
    }

    fun setNotifyOnComplete(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setNotifyOnComplete(enabled) }
    }

    fun setAutoDeleteAfterShare(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setAutoDeleteAfterShare(enabled) }
    }

    fun setDownloadFolderName(name: String) {
        viewModelScope.launch { settingsDataStore.setDownloadFolderName(name) }
    }

    fun setPinLockEnabled(enabled: Boolean) {
        if (!enabled) {
            pinManager.clearPin()
            _isPinLockEnabled.value = false
        }
        // When enabling, the actual PIN gets set via PinLockScreen (SETUP mode) —
        // the caller should navigate there; refreshPinLockState() re-checks afterward.
    }

    fun refreshPinLockState() {
        _isPinLockEnabled.value = pinManager.isPinSet()
    }
}
