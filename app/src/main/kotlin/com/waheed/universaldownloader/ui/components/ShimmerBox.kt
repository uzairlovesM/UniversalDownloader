package com.waheed.universaldownloader.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.waheed.universaldownloader.ui.theme.SurfaceElevated
import com.waheed.universaldownloader.ui.theme.SurfaceGlass

/** A shimmering placeholder box — use anywhere content is still loading (thumbnails, list rows). */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier, cornerRadius: Int = 16) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -500f,
        targetValue = 500f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val brush = Brush.linearGradient(
        colors = listOf(SurfaceElevated, SurfaceGlass, SurfaceElevated),
        start = Offset(translateAnim - 300f, 0f),
        end = Offset(translateAnim, 300f)
    )

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush, RoundedCornerShape(cornerRadius.dp))
    )
}
