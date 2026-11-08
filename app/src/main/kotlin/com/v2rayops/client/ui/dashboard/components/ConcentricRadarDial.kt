package com.v2rayops.client.ui.dashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2rayops.client.domain.model.ConnectionState
import kotlin.math.cos
import kotlin.math.sin

/**
 * Concentric Radar Dial matching the exact mockups:
 * - 7 Concentric rings of circular dots with smooth radial brightness falloff
 * - 2 High-tech dashed circular orbital tracks (NO device icons, clean orbital radar lines)
 * - Matte flat obsidian center power button (140dp) with razor-sharp solid color border
 * - Luminous power icon and clean monospace action text (CONNECT / CANCEL / STOP)
 */
@Composable
fun ConcentricRadarDial(
    connectionState: ConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionState.isConnected
    val isConnecting = connectionState.isConnecting || connectionState is ConnectionState.Disconnecting

    // Colors matching reference mockups
    val connectedBorder = Color(0xFF6366F1)     // Electric Indigo
    val connectedIcon = Color(0xFF9D9BFE)       // Luminous Periwinkle / Lavender
    val connectedDot = Color(0xFF6366F1)        // Indigo dots

    val connectingBorder = Color(0xFFF59E0B)    // Amber Gold
    val connectingIcon = Color(0xFFF59E0B)
    val connectingDot = Color(0xFFF59E0B)

    val disconnectedBorder = Color(0xFF232E44)  // Steel Slate Navy
    val disconnectedIcon = Color(0xFF6B7A99)    // Muted Steel Slate
    val disconnectedDot = Color(0xFF1E283D)

    // Smoothly animated transition between states (no sudden popping)
    val targetBorder = when {
        isConnected -> connectedBorder
        isConnecting -> connectingBorder
        else -> disconnectedBorder
    }
    val animatedBorderColor by animateColorAsState(
        targetValue = targetBorder,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "BorderColor"
    )

    val targetIcon = when {
        isConnected -> connectedIcon
        isConnecting -> connectingIcon
        else -> disconnectedIcon
    }
    val animatedIconColor by animateColorAsState(
        targetValue = targetIcon,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "IconColor"
    )

    val targetDot = when {
        isConnected -> connectedDot
        isConnecting -> connectingDot
        else -> disconnectedDot
    }
    val animatedDotColor by animateColorAsState(
        targetValue = targetDot,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "DotColor"
    )

    val actionText = when {
        isConnecting -> "CANCEL"
        isConnected -> "STOP"
        else -> "CONNECT"
    }

    // State transition progress (0f disconnected -> 0.6f connecting -> 1f connected)
    val stateProgress by animateFloatAsState(
        targetValue = when {
            isConnected -> 1.0f
            isConnecting -> 0.65f
            else -> 0.0f
        },
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "StateProgress"
    )

    // Gentle ambient breathing (calm, subtle, professional)
    val infiniteTransition = rememberInfiniteTransition(label = "RadarMotion")

    val breathMultiplier by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(3400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathMultiplier"
    )

    // Subtle, continuous slow rotation of the orbital dashed lines (60s cycle)
    val orbitRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitRotation"
    )

    val density = LocalDensity.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 284dp Radar Field Container
        Box(
            modifier = Modifier
                .size(284.dp)
                .drawBehind {
                    val cx = size.width / 2f
                    val cy = size.height / 2f

                    // 7 Concentric Rings of Dots wrapping outwards from the 140dp button
                    val dotRadii = listOf(
                        size.minDimension * 0.260f, // Ring 0: ~78dp
                        size.minDimension * 0.293f, // Ring 1: ~88dp
                        size.minDimension * 0.326f, // Ring 2: ~98dp
                        size.minDimension * 0.360f, // Ring 3: ~108dp
                        size.minDimension * 0.393f, // Ring 4: ~118dp
                        size.minDimension * 0.426f, // Ring 5: ~128dp
                        size.minDimension * 0.460f  // Ring 6: ~138dp
                    )
                    val dotCounts = listOf(26, 32, 38, 44, 52, 60, 68)

                    // 2 Concentric Orbital Track Radii (Dashed lines, NO device satellites)
                    val orbitInnerRadius = size.minDimension * 0.400f // ~120dp (between ring 4 & 5)
                    val orbitOuterRadius = size.minDimension * 0.486f // ~146dp (just outside ring 6)

                    // --- 1. Draw 2 Dashed Orbital Track Circles ---
                    val orbitColor = when {
                        isConnected -> Color(0xFF2B3356).copy(alpha = 0.55f)
                        isConnecting -> Color(0xFF3E3722).copy(alpha = 0.55f)
                        else -> Color(0xFF172033).copy(alpha = 0.40f)
                    }

                    val dashEffect = PathEffect.dashPathEffect(
                        floatArrayOf(5.dp.toPx(), 8.dp.toPx()),
                        0f
                    )

                    // Inner orbit circle with subtle slow rotation
                    rotate(degrees = orbitRotation, pivot = Offset(cx, cy)) {
                        drawCircle(
                            color = orbitColor,
                            radius = orbitInnerRadius,
                            center = Offset(cx, cy),
                            style = Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = dashEffect
                            )
                        )
                    }

                    // Outer orbit circle (counter-rotating for futuristic sci-fi depth)
                    rotate(degrees = -orbitRotation * 0.65f, pivot = Offset(cx, cy)) {
                        drawCircle(
                            color = orbitColor.copy(alpha = orbitColor.alpha * 0.85f),
                            radius = orbitOuterRadius,
                            center = Offset(cx, cy),
                            style = Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = dashEffect
                            )
                        )
                    }

                    // --- 2. Draw 7 Concentric Dot Rings ---
                    val dotRadiusPx = with(density) { 1.25.dp.toPx() }

                    for (i in dotRadii.indices) {
                        val radius = dotRadii[i]
                        val count = dotCounts[i]
                        val angleStep = (2 * Math.PI) / count
                        val normalizedRing = i.toFloat() / (dotRadii.size - 1).toFloat()

                        // Natural radial falloff: inner rings are brighter, outer rings fade smoothly
                        val baseFalloff = (1f - normalizedRing * 0.72f).coerceIn(0.12f, 1f)

                        // Staggered cascade wave for state transition
                        val ringCascade = ((stateProgress - (i * 0.08f)) / 0.50f).coerceIn(0f, 1f)

                        val dotAlpha = when {
                            isConnected -> {
                                val activeAlpha = 0.72f * baseFalloff * breathMultiplier
                                val idleAlpha = 0.28f * baseFalloff
                                (idleAlpha + (activeAlpha - idleAlpha) * ringCascade).coerceIn(0.06f, 0.95f)
                            }
                            isConnecting -> {
                                val activeAlpha = 0.70f * baseFalloff * breathMultiplier
                                val idleAlpha = 0.28f * baseFalloff
                                (idleAlpha + (activeAlpha - idleAlpha) * ringCascade).coerceIn(0.06f, 0.95f)
                            }
                            else -> {
                                (0.32f * baseFalloff).coerceIn(0.06f, 0.40f)
                            }
                        }

                        for (j in 0 until count) {
                            val angle = j * angleStep
                            val x = cx + radius * cos(angle).toFloat()
                            val y = cy + radius * sin(angle).toFloat()

                            drawCircle(
                                color = animatedDotColor.copy(alpha = dotAlpha),
                                radius = dotRadiusPx,
                                center = Offset(x, y)
                            )
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Center Power Button — Flat Matte Obsidian disc (136dp) with razor-sharp 2dp border
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF090C19)) // Clean matte obsidian background
                    .border(
                        width = 2.dp,
                        color = animatedBorderColor,
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
                        tint = animatedIconColor,
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(
                        text = actionText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        color = animatedIconColor
                    )
                }
            }
        }
    }
}
