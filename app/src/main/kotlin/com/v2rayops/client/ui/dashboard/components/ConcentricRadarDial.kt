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
import androidx.compose.ui.geometry.Offset
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

    val subtitle = when {
        isConnected -> "3 OTHER DEVICES ON THIS TUNNEL"
        else -> "3 OTHER DEVICES ON THIS ACCOUNT"
    }

    // Gentle pulse animation for handshaking or connected
    val infiniteTransition = rememberInfiniteTransition(label = "RadarMotion")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = if (isConnecting || isConnected) 0.95f else 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val density = LocalDensity.current
    val radarRadiusPx = with(density) { 110.dp.toPx() }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 280dp Box containing Concentric Dot Rings, Orbital Track, Satellites & Center Power Button
        Box(
            modifier = Modifier
                .size(280.dp)
                .drawBehind {
                    val cx = size.width / 2f
                    val cy = size.height / 2f

                    // 1. Concentric Dot Rings Radiating Outward (Exact match to mockup)
                    val ringRadii = listOf(
                        size.minDimension * 0.27f,
                        size.minDimension * 0.33f,
                        size.minDimension * 0.39f,
                        size.minDimension * 0.44f,
                        size.minDimension * 0.48f
                    )
                    val dotCounts = listOf(24, 32, 40, 48, 56)

                    for (i in ringRadii.indices) {
                        val radius = ringRadii[i]
                        val count = dotCounts[i]
                        val angleStep = (2 * Math.PI) / count
                        val normalizedRing = (i + 1f) / ringRadii.size

                        val baseDotColor = when {
                            isConnecting -> Color(0xFFF59E0B)
                            isConnected -> Color(0xFF38BDF8)
                            else -> Color(0xFF2563EB)
                        }

                        val dotAlpha = (0.12f + 0.25f * (1f - normalizedRing * 0.5f)) * pulseAlpha

                        for (j in 0 until count) {
                            val angle = j * angleStep
                            val x = cx + radius * cos(angle).toFloat()
                            val y = cy + radius * sin(angle).toFloat()

                            drawCircle(
                                color = baseDotColor.copy(alpha = dotAlpha.coerceIn(0f, 1f)),
                                radius = with(density) { 1.25.dp.toPx() },
                                center = Offset(x, y)
                            )
                        }
                    }

                    // 2. Orbital Guide Track (Thin circular line)
                    drawCircle(
                        color = Color(0xFF141C30),
                        radius = radarRadiusPx,
                        style = Stroke(width = with(density) { 1.dp.toPx() })
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // 3 Satellite Device Nodes positioned along the orbit
            SatelliteNode(
                icon = Icons.Filled.Laptop,
                angleDeg = 45.0,
                orbitRadiusPx = radarRadiusPx,
                isConnected = isConnected
            )
            SatelliteNode(
                icon = Icons.Filled.Smartphone,
                angleDeg = 200.0,
                orbitRadiusPx = radarRadiusPx,
                isConnected = isConnected
            )
            SatelliteNode(
                icon = Icons.Filled.Tv,
                angleDeg = 290.0,
                orbitRadiusPx = radarRadiusPx,
                isConnected = isConnected
            )

            // Center Power Button (120dp exact circle)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF090D1A))
                    .border(
                        width = 2.dp,
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
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = actionText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        color = iconColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Device Count Subtitle directly below radar
        Text(
            text = subtitle,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.5.sp,
            color = Color(0xFF64748B)
        )
    }
}

@Composable
private fun SatelliteNode(
    icon: ImageVector,
    angleDeg: Double,
    orbitRadiusPx: Float,
    isConnected: Boolean
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
            .border(1.dp, Color(0xFF1A2640), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF64748B),
            modifier = Modifier.size(14.dp)
        )

        // Top-right tiny status indicator dot
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(4.dp)
                .clip(CircleShape)
                .background(if (isConnected) Color(0xFF22C55E) else Color(0xFF64748B))
        )
    }
}
