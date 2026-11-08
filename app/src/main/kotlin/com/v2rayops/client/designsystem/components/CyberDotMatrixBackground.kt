package com.v2rayops.client.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.v2rayops.client.designsystem.theme.CyberBackground

@Composable
fun CyberDotMatrixBackground(
    modifier: Modifier = Modifier,
    gridSpacing: Dp = 28.dp,
    dotRadius: Dp = 1.25.dp,
    dotColor: Color = Color(0xFF94A3B8).copy(alpha = 0.12f),
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val spacingPx = with(density) { gridSpacing.toPx() }
    val radiusPx = with(density) { dotRadius.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0A0F1F), // Atmospheric deep midnight indigo core
                        Color(0xFF050812), // Seamless subtle transition
                        CyberBackground    // #04060A deep cosmic edge
                    )
                )
            )
            .drawBehind {
                val width = size.width
                val height = size.height

                var x = spacingPx / 2f
                while (x < width) {
                    var y = spacingPx / 2f
                    while (y < height) {
                        drawCircle(
                            color = dotColor,
                            radius = radiusPx,
                            center = Offset(x, y)
                        )
                        y += spacingPx
                    }
                    x += spacingPx
                }
            }
    ) {
        content()
    }
}
