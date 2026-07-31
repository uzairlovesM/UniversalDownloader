package com.waheed.universaldownloader.ads

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

private const val TAG = "BannerAdView"

/**
 * Drop this at the bottom of a screen (e.g. HomeScreen, LibraryScreen) to show a banner ad.
 * Uses an adaptive anchored banner (Google's current recommendation over the legacy fixed
 * AdSize.BANNER) which sizes itself to the screen width for a more modern, less "boxed-in" look.
 * Renders nothing if remote config has ads disabled.
 */
@Composable
fun BannerAdView(adManager: AdManager, modifier: Modifier = Modifier) {
    if (!adManager.areAdsEnabled()) return

    val context = LocalContext.current
    val density = LocalDensity.current

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            val screenWidthDp = (ctx.resources.displayMetrics.widthPixels / density.density).toInt()
            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, screenWidthDp)

            AdView(ctx).apply {
                setAdSize(adSize)
                adUnitId = AdUnitIds.BANNER
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d(TAG, "Banner ad loaded successfully")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(TAG, "Banner ad failed to load: ${error.message} (code ${error.code}, domain ${error.domain})")
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
