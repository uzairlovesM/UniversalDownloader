package com.waheed.universaldownloader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import com.waheed.universaldownloader.ui.theme.BorderGlass
import com.waheed.universaldownloader.ui.theme.SurfaceElevated
import com.waheed.universaldownloader.ui.theme.SurfaceGlass

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 20,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(listOf(SurfaceElevated, SurfaceGlass)),
                shape = RoundedCornerShape(cornerRadius)
            )
            .border(1.dp, BorderGlass, RoundedCornerShape(cornerRadius))
    ) {
        content()
    }
}
