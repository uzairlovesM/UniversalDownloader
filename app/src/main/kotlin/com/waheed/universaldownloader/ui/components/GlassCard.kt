package com.waheed.universaldownloader.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.waheed.universaldownloader.ui.theme.AmberPrimary
import com.waheed.universaldownloader.ui.theme.BorderGlass
import com.waheed.universaldownloader.ui.theme.SurfaceElevated
import com.waheed.universaldownloader.ui.theme.SurfaceGlass

/**
 * Layered glassmorphism card: soft ambient shadow, diagonal glass gradient, subtle gradient
 * border (amber-tinted at the top edge, fading to neutral), and optional press-depth animation
 * when wrapped around a clickable modifier upstream.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 20,
    elevation: Int = 8,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) {
    val isPressed by (interactionSource?.collectIsPressedAsState() ?: remember { androidx.compose.runtime.mutableStateOf(false) })
    val animatedElevation by animateDpAsState(
        targetValue = if (isPressed) (elevation / 2).dp else elevation.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardElevation"
    )

    val shape = RoundedCornerShape(cornerRadius.dp)

    Box(
        modifier = modifier
            .shadow(
                elevation = animatedElevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(SurfaceElevated, SurfaceGlass, SurfaceElevated.copy(alpha = 0.9f))
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AmberPrimary.copy(alpha = 0.35f),
                        BorderGlass.copy(alpha = 0.15f),
                        BorderGlass.copy(alpha = 0.05f)
                    )
                ),
                shape = shape
            )
    ) {
        content()
    }
}

/** A flatter, lower-emphasis variant for dense lists (e.g. Library grid items) where a full
 *  shadowed GlassCard on every item would look heavy. */
@Composable
fun GlassCardFlat(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 16,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(SurfaceGlass.copy(alpha = 0.6f))
            .border(0.8.dp, BorderGlass.copy(alpha = 0.12f), shape)
    ) {
        content()
    }
}
