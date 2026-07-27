package com.waheed.universaldownloader.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Drop this at the bottom of a screen (e.g. HomeScreen, LibraryScreen) to show a
 * banner ad. Renders nothing if remote config has ads disabled.
 */
@Composable
fun BannerAdView(adManager: AdManager, modifier: Modifier = Modifier) {
    if (!adManager.areAdsEnabled()) return

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdUnitIds.BANNER_TEST
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
