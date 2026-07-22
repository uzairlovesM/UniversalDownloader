package com.waheed.universaldownloader.remoteconfig

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.google.firebase.Firebase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central manager for Firebase Remote Config.
 * Fetches remote values that control app behavior without needing a new release:
 * - min_supported_version: force-update threshold
 * - latest_version_code / latest_version_name: auto-update system
 * - update_apk_url: direct GitHub Releases link for the updater
 * - maintenance_mode: kill-switch for the app
 * - ads_enabled: remote AdMob toggle
 */
@Singleton
class RemoteConfigManager @Inject constructor() {

    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600 // 1 hour in production
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(
            mapOf(
                "min_supported_version" to 1L,
                "latest_version_code" to 1L,
                "latest_version_name" to "1.0.0",
                "update_apk_url" to "",
                "maintenance_mode" to false,
                "ads_enabled" to true
            )
        )
    }

    /** Fetches and activates the latest values from the Firebase console. Call once at app startup. */
    suspend fun fetchAndActivate(): Boolean {
        return try {
            remoteConfig.fetchAndActivate().await()
        } catch (e: Exception) {
            false // network failure — cached/default values remain in effect
        }
    }

    fun getMinSupportedVersion(): Long = remoteConfig.getLong("min_supported_version")
    fun getLatestVersionCode(): Long = remoteConfig.getLong("latest_version_code")
    fun getLatestVersionName(): String = remoteConfig.getString("latest_version_name")
    fun getUpdateApkUrl(): String = remoteConfig.getString("update_apk_url")
    fun isMaintenanceMode(): Boolean = remoteConfig.getBoolean("maintenance_mode")
    fun areAdsEnabled(): Boolean = remoteConfig.getBoolean("ads_enabled")

    /** True if the app's current versionCode is below the server's minimum — should force an update screen. */
    fun isCurrentVersionBelowMinimum(currentVersionCode: Long): Boolean {
        return currentVersionCode < getMinSupportedVersion()
    }

    /** True if a newer version exists than what's installed — should show an "update available" prompt. */
    fun isUpdateAvailable(currentVersionCode: Long): Boolean {
        return currentVersionCode < getLatestVersionCode()
    }
}
