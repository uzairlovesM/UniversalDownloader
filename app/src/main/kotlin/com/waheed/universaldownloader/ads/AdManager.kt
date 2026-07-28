package com.waheed.universaldownloader.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
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
    const val BANNER_TEST = "ca-app-pub-5880560632795483/5848334415"
    const val INTERSTITIAL_TEST = "ca-app-pub-5880560632795483/6439095425"
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

    private var interstitialAd: InterstitialAd? = null
    private var isLoadingInterstitial = false

    /** Pre-loads an interstitial so it's ready to show instantly when needed. */
    fun preloadInterstitial(context: Context) {
        if (!areAdsEnabled() || interstitialAd != null || isLoadingInterstitial) return
        isLoadingInterstitial = true
        InterstitialAd.load(
            context,
            AdUnitIds.INTERSTITIAL_TEST,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingInterstitial = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoadingInterstitial = false
                }
            }
        )
    }

    /** Shows the preloaded interstitial if available, then preloads the next one. */
    fun showInterstitialIfReady(activity: Activity) {
        val ad = interstitialAd
        if (ad == null) {
            preloadInterstitial(activity)
            return
        }
        ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                preloadInterstitial(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                interstitialAd = null
                preloadInterstitial(activity)
            }
        }
        ad.show(activity)
    }
}
