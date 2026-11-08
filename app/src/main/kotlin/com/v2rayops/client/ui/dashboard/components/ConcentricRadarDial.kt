package com.v2rayops.client.ui.dashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2rayops.client.domain.model.ConnectionState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ConcentricRadarDial(
    connectionState: ConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionState.isConnected
    val isConnecting = connectionState.isConnecting || connectionState is ConnectionState.Disconnecting

    // Blue to Dark Purple palette matching the theme
    val blueColor = Color(0xFF3B82F6)        // Vibrant Royal Blue
    val purpleColor = Color(0xFF6366F1)      // Electric Indigo
    val darkPurpleColor = Color(0xFF312E81)  // Deep Midnight Purple

    val actionText = when {
        isConnecting -> "CANCEL"
        isConnected -> "STOP"
        else -> "CONNECT"
    }

    // Refined, subtle breathing animation for glow & dots
    val infiniteTransition = rememberInfiniteTransition(label = "RadarThemeMotion")

    val breathPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathPhase"
    )

    // Smooth pulse scale on glow ring
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isConnected) 1.05f else if (isConnecting) 1.03f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowScale"
    )

    val density = LocalDensity.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 280dp Box containing Concentric Dot Rings (orbit circle & satellites removed)
        Box(
            modifier = Modifier
                .size(280.dp)
                .drawBehind {
                    val cx = size.width / 2f
                    val cy = size.height / 2f

                    // 5 Concentric Rings of Dots wrapping around the 146dp button
                    val ringRadii = listOf(
                        size.minDimension * 0.29f, // ~81dp
                        size.minDimension * 0.34f, // ~95dp
                        size.minDimension * 0.39f, // ~109dp
                        size.minDimension * 0.44f, // ~123dp
                        size.minDimension * 0.48f  // ~134dp
                    )
                    val dotCounts = listOf(28, 36, 44, 52, 60)

                    for (i in ringRadii.indices) {
                        val radius = ringRadii[i]
                        val count = dotCounts[i]
                        val angleStep = (2 * Math.PI) / count
                        val normalizedRing = i.toFloat() / (ringRadii.size - 1).toFloat()

                        // Color selection: Blue to Dark Purple gradient
                        val baseDotColor = when {
                            isConnecting -> Color(0xFFF59E0B)
                            isConnected -> {
                                // Inner rings blue (#3B82F6), middle indigo (#6366F1), outer dark purple (#312E81)
                                when (i) {
                                    0, 1 -> blueColor
                                    2, 3 -> purpleColor
                                    else -> darkPurpleColor
                                }
                            }
                            else -> Color(0xFF1E2D48)
                        }

                        // Subtle, refined alpha: gentle breath without chaotic flashing
                        val dotAlpha = when {
                            isConnected -> (0.22f + 0.22f * breathPhase * (1f - normalizedRing * 0.4f)).coerceIn(0f, 1f)
                            isConnecting -> (0.30f + 0.20f * breathPhase).coerceIn(0f, 1f)
                            else -> (0.12f + 0.10f * (1f - normalizedRing)).coerceIn(0f, 1f)
                        }

                        val dotRadiusPx = with(density) { 1.25.dp.toPx() }

                        for (j in 0 until count) {
                            val angle = j * angleStep
                            val x = cx + radius * cos(angle).toFloat()
                            val y = cy + radius * sin(angle).toFloat()

                            drawCircle(
                                color = baseDotColor.copy(alpha = dotAlpha),
                                radius = dotRadiusPx,
                                center = Offset(x, y)
                            )
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Blue to Dark Purple Ambient Glow Ring behind the button (when connected)
            if (isConnected) {
                Box(
                    modifier = Modifier
                        .size(162.dp)
                        .scale(glowScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    blueColor.copy(alpha = 0.22f),
                                    purpleColor.copy(alpha = 0.16f),
                                    darkPurpleColor.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            } else if (isConnecting) {
                Box(
                    modifier = Modifier
                        .size(156.dp)
                        .scale(glowScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFF59E0B).copy(alpha = 0.18f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // Center Power Button — 146dp circular disc with Blue to Dark Purple gradient border
            val borderBrush = when {
                isConnecting -> Brush.verticalGradient(
                    colors = listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                )
                isConnected -> Brush.verticalGradient(
                    colors = listOf(
                        blueColor,        // Royal blue top highlight
                        purpleColor,      // Electric indigo midtone
                        darkPurpleColor   // Deep purple base
                    )
                )
                else -> Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E2D48), Color(0xFF121B2D))
                )
            }

            val iconColor = when {
                isConnecting -> Color(0xFFF59E0B)
                isConnected -> Color(0xFF818CF8) // Glowing purple/indigo icon
                else -> Color(0xFF64748B)
            }

            Box(
                modifier = Modifier
                    .size(146.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF090D1A))
                    .border(
                        width = 2.5.dp,
                        brush = borderBrush,
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.PowerSettingsNew,
                        contentDescription = "Power",
                        tint = iconColor,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = actionText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.2.sp,
                        color = iconColor
                    )
                }
            }
        }
    }
}
