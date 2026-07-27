package com.waheed.universaldownloader.data.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class DefaultQuality(val label: String) {
    BEST("Best available"),
    HIGH_1080P("1080p"),
    MEDIUM_720P("720p"),
    LOW_480P("480p"),
    AUDIO_ONLY("Audio only")
}

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_QUALITY = stringPreferencesKey("default_quality")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only_downloads")
        val NOTIFY_ON_COMPLETE = booleanPreferencesKey("notify_on_complete")
        val AUTO_DELETE_AFTER_SHARE = booleanPreferencesKey("auto_delete_after_share")
        val DOWNLOAD_FOLDER_NAME = stringPreferencesKey("download_folder_name")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        ThemeMode.entries.find { it.name == prefs[Keys.THEME_MODE] } ?: ThemeMode.SYSTEM
    }

    val defaultQuality: Flow<DefaultQuality> = context.dataStore.data.map { prefs ->
        DefaultQuality.entries.find { it.name == prefs[Keys.DEFAULT_QUALITY] } ?: DefaultQuality.BEST
    }

    val wifiOnlyDownloads: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.WIFI_ONLY] ?: false
    }

    val notifyOnComplete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.NOTIFY_ON_COMPLETE] ?: true
    }

    val autoDeleteAfterShare: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_DELETE_AFTER_SHARE] ?: false
    }

    val downloadFolderName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.DOWNLOAD_FOLDER_NAME] ?: "UniversalDownloader"
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDefaultQuality(quality: DefaultQuality) {
        context.dataStore.edit { it[Keys.DEFAULT_QUALITY] = quality.name }
    }

    suspend fun setWifiOnlyDownloads(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WIFI_ONLY] = enabled }
    }

    suspend fun setNotifyOnComplete(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFY_ON_COMPLETE] = enabled }
    }

    suspend fun setAutoDeleteAfterShare(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_DELETE_AFTER_SHARE] = enabled }
    }

    suspend fun setDownloadFolderName(name: String) {
        context.dataStore.edit { it[Keys.DOWNLOAD_FOLDER_NAME] = name }
    }
}
