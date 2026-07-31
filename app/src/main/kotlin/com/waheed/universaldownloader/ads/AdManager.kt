package com.waheed.universaldownloader.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.waheed.universaldownloader.remoteconfig.RemoteConfigManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Test ad unit IDs. Banner and Interstitial below are real, obtained from the AdMob console.
 * Rewarded uses Google's official test ID until a real rewarded ad unit is created in AdMob console
 * (App > Ad units > Add ad unit > Rewarded) — swap AdUnitIds.REWARDED before production release.
 */
object AdUnitIds {
    const val BANNER = "ca-app-pub-5880560632795483/5848334415"
    const val INTERSTITIAL = "ca-app-pub-5880560632795483/6439095425"
    const val REWARDED = "ca-app-pub-3940256099942544/5224354917" // Google test ID — replace with real one
}

private const val TAG = "AdManager"

@Singleton
class AdManager @Inject constructor(
    private val remoteConfigManager: RemoteConfigManager
) {
    private var isInitialized = false

    /**
     * Initializes the Mobile Ads SDK unconditionally. The ads_enabled remote-config flag
     * gates whether individual ad surfaces (banner/interstitial/rewarded) actually LOAD and
     * SHOW — it must not gate SDK initialization itself, since that flag's value may not be
     * reliably available yet at cold start (Remote Config fetch/defaults are async).
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        MobileAds.initialize(context) { status ->
            isInitialized = true
            Log.d(TAG, "MobileAds initialized: ${status.adapterStatusMap.keys}")
        }
    }

    fun areAdsEnabled(): Boolean = remoteConfigManager.areAdsEnabled()

    // ---------- Interstitial ----------

    private var interstitialAd: InterstitialAd? = null
    private var isLoadingInterstitial = false

    fun preloadInterstitial(context: Context) {
        if (!areAdsEnabled() || interstitialAd != null || isLoadingInterstitial) return
        isLoadingInterstitial = true
        InterstitialAd.load(
            context,
            AdUnitIds.INTERSTITIAL,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingInterstitial = false
                    Log.d(TAG, "Interstitial loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoadingInterstitial = false
                    Log.e(TAG, "Interstitial failed to load: ${error.message} (code ${error.code})")
                }
            }
        )
    }

    fun showInterstitialIfReady(activity: Activity) {
        val ad = interstitialAd
        if (ad == null) {
            preloadInterstitial(activity)
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                preloadInterstitial(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Interstitial failed to show: ${error.message}")
                interstitialAd = null
                preloadInterstitial(activity)
            }
        }
        ad.show(activity)
    }

    // ---------- Rewarded ----------

    private var rewardedAd: RewardedAd? = null
    private var isLoadingRewarded = false

    /** Pre-loads a rewarded ad — call this ahead of time (e.g. when entering a screen that offers a reward). */
    fun preloadRewarded(context: Context) {
        if (!areAdsEnabled() || rewardedAd != null || isLoadingRewarded) return
        isLoadingRewarded = true
        RewardedAd.load(
            context,
            AdUnitIds.REWARDED,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoadingRewarded = false
                    Log.d(TAG, "Rewarded ad loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoadingRewarded = false
                    Log.e(TAG, "Rewarded ad failed to load: ${error.message} (code ${error.code})")
                }
            }
        )
    }

    fun isRewardedReady(): Boolean = rewardedAd != null

    /**
     * Shows the preloaded rewarded ad. onUserEarnedReward fires only if the user watches to
     * completion — wire this to unlock whatever perk you're offering (e.g. an ad-free download slot).
     */
    fun showRewardedIfReady(activity: Activity, onUserEarnedReward: (RewardItem) -> Unit, onNotReady: () -> Unit = {}) {
        val ad = rewardedAd
        if (ad == null) {
            onNotReady()
            preloadRewarded(activity)
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                preloadRewarded(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Rewarded ad failed to show: ${error.message}")
                rewardedAd = null
                preloadRewarded(activity)
            }
        }
        ad.show(activity) { rewardItem -> onUserEarnedReward(rewardItem) }
    }
}
