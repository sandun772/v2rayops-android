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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tv
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2rayops.client.domain.model.ConnectionState
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun ConcentricRadarDial(
    connectionState: ConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionState.isConnected
    val isConnecting = connectionState.isConnecting || connectionState is ConnectionState.Disconnecting

    // Theme color mapping based on exact mockups
    val stateColor by animateColorAsState(
        targetValue = when {
            isConnecting -> Color(0xFFF59E0B) // Amber Handshaking
            isConnected -> Color(0xFF6366F1)  // Electric Indigo/Cyan Connected
            else -> Color(0xFF1E2D48)         // Slate-blue Disconnected
        },
        animationSpec = tween(400)
    )

    val iconColor by animateColorAsState(
        targetValue = when {
            isConnecting -> Color(0xFFF59E0B)
            isConnected -> Color(0xFF818CF8)
            else -> Color(0xFF64748B)
        },
        animationSpec = tween(400)
    )

    val actionText = when {
        isConnecting -> "CANCEL"
        isConnected -> "STOP"
        else -> "CONNECT"
    }

    // Dynamic wave ripple transitions for connected animation
    val infiniteTransition = rememberInfiniteTransition(label = "RadarMotion")

    // Continuous radial ripple wave expanding outward from button (0 to 1)
    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RippleProgress"
    )

    // Secondary pulsing glow on connected button
    val buttonGlowScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isConnected) 1.08f else if (isConnecting) 1.04f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ButtonGlowScale"
    )

    val density = LocalDensity.current
    // 146dp button -> radius ~73dp. Orbit track is placed at 124dp.
    val radarRadiusPx = with(density) { 124.dp.toPx() }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 300dp Box containing Concentric Dot Rings, Orbital Track (when connected) & 146dp Center Power Button
        Box(
            modifier = Modifier
                .size(300.dp)
                .drawBehind {
                    val cx = size.width / 2f
                    val cy = size.height / 2f

                    // 5 Concentric Rings of Dots wrapping around the 146dp button
                    val ringRadii = listOf(
                        size.minDimension * 0.28f, // ~84dp
                        size.minDimension * 0.33f, // ~99dp
                        size.minDimension * 0.38f, // ~114dp
                        size.minDimension * 0.43f, // ~129dp
                        size.minDimension * 0.48f  // ~144dp
                    )
                    val dotCounts = listOf(28, 36, 44, 52, 60)

                    for (i in ringRadii.indices) {
                        val radius = ringRadii[i]
                        val count = dotCounts[i]
                        val angleStep = (2 * Math.PI) / count
                        val normalizedRing = i.toFloat() / (ringRadii.size - 1).toFloat()

                        // Calculate dynamic ripple wave boost when connected
                        val waveDelta = rippleProgress - normalizedRing
                        // Gaussian peak centered at the ripple front
                        val waveBoost = if (isConnected) {
                            exp(-((waveDelta * 4.2f) * (waveDelta * 4.2f)))
                        } else {
                            0f
                        }

                        val baseDotColor = when {
                            isConnecting -> Color(0xFFF59E0B)
                            isConnected -> {
                                // Shimmer between neon cyan (#00E5FF) and electric violet (#818CF8)
                                if (waveBoost > 0.4f) Color(0xFF00E5FF) else Color(0xFF38BDF8)
                            }
                            else -> Color(0xFF2563EB)
                        }

                        // Alpha calculation: when connected, wave boost makes dots blaze brightly (up to 0.95f)
                        val dotAlpha = when {
                            isConnected -> (0.18f + waveBoost * 0.77f).coerceIn(0f, 1f)
                            isConnecting -> 0.45f
                            else -> (0.16f + 0.12f * (1f - normalizedRing)).coerceIn(0f, 1f)
                        }

                        // Radius calculation: passing wave expands dots dynamically
                        val dotRadius = with(density) {
                            if (isConnected) {
                                (1.3.dp + (1.2.dp * waveBoost)).toPx()
                            } else {
                                1.3.dp.toPx()
                            }
                        }

                        for (j in 0 until count) {
                            val angle = j * angleStep
                            val x = cx + radius * cos(angle).toFloat()
                            val y = cy + radius * sin(angle).toFloat()

                            drawCircle(
                                color = baseDotColor.copy(alpha = dotAlpha),
                                radius = dotRadius,
                                center = Offset(x, y)
                            )
                        }
                    }

                    // Orbital Guide Track line: ONLY shown when device is connected
                    if (isConnected) {
                        drawCircle(
                            color = Color(0xFF1E2D48).copy(alpha = 0.65f),
                            radius = radarRadiusPx,
                            style = Stroke(width = with(density) { 1.dp.toPx() })
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Satellite Device Nodes: ONLY shown when device is connected
            if (isConnected) {
                SatelliteNode(
                    icon = Icons.Filled.Laptop,
                    angleDeg = 45.0,
                    orbitRadiusPx = radarRadiusPx
                )
                SatelliteNode(
                    icon = Icons.Filled.Smartphone,
                    angleDeg = 200.0,
                    orbitRadiusPx = radarRadiusPx
                )
                SatelliteNode(
                    icon = Icons.Filled.Tv,
                    angleDeg = 290.0,
                    orbitRadiusPx = radarRadiusPx
                )
            }

            // Glowing halo behind the button when connected
            if (isConnected) {
                Box(
                    modifier = Modifier
                        .size(158.dp)
                        .scale(buttonGlowScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF6366F1).copy(alpha = 0.28f),
                                    Color(0xFF06B6D4).copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // Center Power Button — Resized to prominent 146dp
            Box(
                modifier = Modifier
                    .size(146.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF090D1A))
                    .border(
                        width = 2.5.dp,
                        color = stateColor,
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
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = actionText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.2.sp,
                        color = iconColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Device Count Subtitle: ONLY shown when connected
        if (isConnected) {
            Text(
                text = "3 OTHER DEVICES ON THIS TUNNEL",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp,
                color = Color(0xFF64748B)
            )
        } else {
            // Invisible placeholder spacer to maintain smooth layout height stability
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun SatelliteNode(
    icon: ImageVector,
    angleDeg: Double,
    orbitRadiusPx: Float
) {
    val angleRad = Math.toRadians(angleDeg)
    val offsetX = (orbitRadiusPx * cos(angleRad)).roundToInt()
    val offsetY = (orbitRadiusPx * sin(angleRad)).roundToInt()

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX, offsetY) }
            .size(28.dp)
            .clip(CircleShape)
            .background(Color(0xFF0C1222))
            .border(1.dp, Color(0xFF1E2D48), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(14.dp)
        )

        // Top-right tiny active green status indicator dot
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(Color(0xFF22C55E))
        )
    }
}
