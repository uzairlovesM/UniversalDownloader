package com.waheed.universaldownloader.ads

import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.waheed.universaldownloader.remoteconfig.RemoteConfigManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Test ad unit IDs (Google's official ones) — safe to ship in debug builds.
 * IMPORTANT: replace with real ad unit IDs from the AdMob console before any
 * production/Play Store release, or Google can suspend the AdMob account for
 * accidentally serving test ads in production.
 */
object AdUnitIds {
    const val BANNER_TEST = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL_TEST = "ca-app-pub-3940256099942544/1033173712"
}

@Singleton
class AdManager @Inject constructor(
    private val remoteConfigManager: RemoteConfigManager
) {
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        if (!remoteConfigManager.areAdsEnabled()) return
        MobileAds.initialize(context) {
            isInitialized = true
        }
    }

    fun areAdsEnabled(): Boolean = remoteConfigManager.areAdsEnabled()
}
