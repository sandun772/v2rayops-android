package com.v2rayops.client.ui.dashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import com.v2rayops.client.domain.model.SmartHuntProgress
import com.v2rayops.client.domain.model.SmartHuntStage
import kotlin.math.cos
import kotlin.math.sin

/**
 * Concentric Radar Dial matching reference design:
 * - 5 Concentric rings of dots clustered as a halo around the 130dp center button (ending at ~106dp radius)
 * - Clean dark space between the dots and orbit circles (ZERO overlap!)
 * - 2 High-tech dashed circular orbital tracks positioned outside the dot field (inner at ~132dp, outer at ~156dp)
 * - Flat matte obsidian center power button (130dp) with razor-sharp 2dp border
 * - Luminous power icon and monospace action text (CONNECT / CANCEL / STOP)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConcentricRadarDial(
    connectionState: ConnectionState,
    isSmartHunting: Boolean = false,
    isCyclingAirplane: Boolean = false,
    smartHuntProgress: SmartHuntProgress = SmartHuntProgress(),
    isNetworkWorkingFine: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isAirplanePhase = isCyclingAirplane ||
            smartHuntProgress.stage == SmartHuntStage.WAITING_AIRPLANE_ON ||
            smartHuntProgress.stage == SmartHuntStage.AIRPLANE_ON ||
            smartHuntProgress.stage == SmartHuntStage.DEAD_IP_RETRY ||
            smartHuntProgress.stage == SmartHuntStage.IP_MISMATCH

    val isAcquiringPhase = smartHuntProgress.stage == SmartHuntStage.ACQUIRING_IP ||
            smartHuntProgress.stage == SmartHuntStage.VERIFYING_TRAFFIC
    val isStabilizingPhase = isAcquiringPhase

    val isConnected = connectionState.isConnected && !smartHuntProgress.isActive && !isSmartHunting
    val isConnecting = smartHuntProgress.isActive || isSmartHunting || connectionState.isConnecting || connectionState is ConnectionState.Disconnecting
    val isHealthyWorking = isConnected && isNetworkWorkingFine

    // Colors matching reference mockups
    val connectedBorder = Color(0xFF6366F1)     // Electric Indigo
    val connectedIcon = Color(0xFF9D9BFE)       // Luminous Periwinkle / Lavender
    val connectedDot = Color(0xFF6366F1)        // Indigo dots

    val connectingBorder = Color(0xFFF59E0B)    // Amber Gold
    val connectingIcon = Color(0xFFF59E0B)
    val connectingDot = Color(0xFFF59E0B)

    val airplaneBorder = Color(0xFFFB7185)      // Radiant Coral Rose
    val airplaneIcon = Color(0xFFFB7185)

    val stabilizingBorder = Color(0xFF06B6D4)   // Cyber Cyan
    val stabilizingIcon = Color(0xFF06B6D4)

    val disconnectedBorder = Color(0xFF232E44)  // Steel Slate Navy
    val disconnectedIcon = Color(0xFF6B7A99)    // Muted Steel Slate
    val disconnectedDot = Color(0xFF1E283D)

    // Smoothly animated transition between states
    val targetBorder = when {
        isAirplanePhase -> airplaneBorder
        isAcquiringPhase -> stabilizingBorder
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
        isAirplanePhase -> airplaneIcon
        isAcquiringPhase -> stabilizingIcon
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
        isSmartHunting -> if (isConnecting) "CONNECTING" else "CANCEL"
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

    // Transitions for radar animations
    val infiniteTransition = rememberInfiniteTransition(label = "RadarMotion")

    // 1. Dual-peak ECG Heartbeat Wave (lub-dub rhythm) active during Airplane Mode Phase
    val heartbeatProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1100 // 1.1s physiological heartbeat cycle
                0.0f at 0 with FastOutSlowInEasing
                1.0f at 160 with FastOutSlowInEasing     // Systolic thump (LUB)
                0.15f at 300 with FastOutSlowInEasing    // Contraction
                0.75f at 430 with FastOutSlowInEasing    // Diastolic thump (DUB)
                0.0f at 600 with FastOutSlowInEasing     // Return to rest
                0.0f at 1100 with LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "HeartbeatPulse"
    )

    // 2. Cellular Carrier Signal Sonar Wave active during 5s Network Stabilization Phase
    val sonarProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SonarProgress"
    )

    // 3. Network Health Telemetry Wave active when Connected & Networking works fine
    val networkPulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "NetworkPulseProgress"
    )

    val breathMultiplier by infiniteTransition.animateFloat(
        initialValue = 0.90f,
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
        // 320dp Radar Field Container
        Box(
            modifier = Modifier
                .size(320.dp)
                .drawBehind {
                    val cx = size.width / 2f
                    val cy = size.height / 2f

                    // Center button radius = 65dp (130dp diameter)
                    val buttonRadius = 65.dp.toPx()

                    // 5 Concentric Rings of Dots clustered tightly around the button (from 72dp to 106dp)
                    // Exactly matching reference: R0 * 1.11, 1.27, 1.41, 1.54, 1.63
                    val dotRadii = listOf(
                        buttonRadius * 1.12f, // Ring 0: ~73dp
                        buttonRadius * 1.26f, // Ring 1: ~82dp
                        buttonRadius * 1.39f, // Ring 2: ~90dp
                        buttonRadius * 1.51f, // Ring 3: ~98dp
                        buttonRadius * 1.63f  // Ring 4: ~106dp
                    )
                    val dotCounts = listOf(24, 30, 36, 42, 48)

                    // 2 Concentric Orbital Track Radii (Dashed lines, well outside the dots with clean empty space!)
                    // Exactly matching reference: R0 * 2.03 and R0 * 2.40
                    val orbitInnerRadius = buttonRadius * 2.03f // ~132dp (clean gap of ~26dp from outer dots)
                    val orbitOuterRadius = buttonRadius * 2.40f // ~156dp (outer track)

                    // --- 1. Draw 2 Dashed Orbital Track Circles in Clean Dark Space ---
                    val orbitColor = when {
                        isAirplanePhase -> Color(0xFF4C1D28).copy(alpha = 0.60f)
                        isStabilizingPhase -> Color(0xFF0C4648).copy(alpha = 0.60f)
                        isConnected -> Color(0xFF2B3356).copy(alpha = 0.55f)
                        isConnecting -> Color(0xFF3E3722).copy(alpha = 0.55f)
                        else -> Color(0xFF172033).copy(alpha = 0.40f)
                    }

                    val dashEffect = PathEffect.dashPathEffect(
                        floatArrayOf(4.dp.toPx(), 7.dp.toPx()),
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

                    // Outer orbit circle (counter-rotating for sci-fi radar depth)
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

                    // --- 2. Draw 5 Concentric Dot Rings (Gradient Halo fading into dark space) ---
                    val dotRadiusPx = with(density) { 1.25.dp.toPx() }

                    for (i in dotRadii.indices) {
                        val radius = dotRadii[i]
                        val count = dotCounts[i]
                        val angleStep = (2 * Math.PI) / count
                        val normalizedRing = i.toFloat() / (dotRadii.size - 1).toFloat()

                        // Natural radial falloff: inner rings are bright, outer rings softly fade to dark
                        val baseFalloff = (1f - normalizedRing * 0.78f).coerceIn(0.12f, 1f)

                        // Staggered cascade wave for state transition
                        val ringCascade = ((stateProgress - (i * 0.09f)) / 0.50f).coerceIn(0f, 1f)

                        // 1. Outward cascading heartbeat pulse across concentric rings (Airplane ON / Hold)
                        val ringHeartbeat = if (isAirplanePhase) {
                            val delayFraction = i * 0.07f
                            ((heartbeatProgress - delayFraction) / (1f - delayFraction)).coerceIn(0f, 1f)
                        } else 0f

                        // 2. Outward cascading cellular signal wave (Network Stabilizing 5s)
                        val ringSonar = if (isStabilizingPhase) {
                            val waveFraction = ((sonarProgress - (i * 0.16f)).mod(1f))
                            (1f - waveFraction).coerceIn(0f, 1f)
                        } else 0f

                        // 3. Outward cascading telemetry pulse (Connected & Networking works fine)
                        val ringPulse = if (isHealthyWorking) {
                            val waveFraction = ((networkPulseProgress - (i * 0.16f)).mod(1f))
                            val dist = kotlin.math.abs(waveFraction - 0.20f)
                            (1f - (dist / 0.20f)).coerceIn(0f, 1f)
                        } else 0f

                        // Scaling ring animation: subtle outward ring radius scale (~3.8%) when healthy pulse passes
                        val currentRingRadius = if (isHealthyWorking) {
                            radius * (1f + ringPulse * 0.038f)
                        } else {
                            radius
                        }

                        // Dynamic dot radius: expands with heartbeat, sonar, or healthy telemetry pulse
                        val currentDotRadius = when {
                            isAirplanePhase -> dotRadiusPx * (1f + ringHeartbeat * 0.90f)
                            isStabilizingPhase -> dotRadiusPx * (1f + ringSonar * 0.65f)
                            isHealthyWorking -> dotRadiusPx * (1f + ringPulse * 0.55f)
                            else -> dotRadiusPx
                        }

                        val dotAlpha = when {
                            isAirplanePhase -> {
                                val base = 0.35f * baseFalloff
                                val surge = ringHeartbeat * 0.65f
                                (base + surge).coerceIn(0.16f, 0.98f)
                            }
                            isStabilizingPhase -> {
                                val base = 0.32f * baseFalloff
                                val surge = ringSonar * 0.68f
                                (base + surge).coerceIn(0.18f, 0.98f)
                            }
                            isConnected -> {
                                val activeAlpha = 0.75f * baseFalloff * breathMultiplier
                                val idleAlpha = 0.28f * baseFalloff
                                val surge = if (isHealthyWorking) ringPulse * 0.42f else 0f
                                ((idleAlpha + (activeAlpha - idleAlpha) * ringCascade) + surge).coerceIn(0.06f, 0.98f)
                            }
                            isConnecting -> {
                                val activeAlpha = 0.72f * baseFalloff * breathMultiplier
                                val idleAlpha = 0.28f * baseFalloff
                                (idleAlpha + (activeAlpha - idleAlpha) * ringCascade).coerceIn(0.06f, 0.95f)
                            }
                            else -> {
                                (0.34f * baseFalloff).coerceIn(0.06f, 0.40f)
                            }
                        }

                        // Radiant dot gradients per event:
                        // 1. Airplane Phase: Coral Rose (#FB7185) -> Cyber Amber (#F59E0B)
                        // 2. Stabilizing Phase: Cyber Cyan (#06B6D4) -> Emerald Mint (#10B981)
                        // 3. Connected (Healthy): High-tech radial gradient (Lavender -> Electric Indigo -> Deep Space Indigo)
                        // 4. Connecting / Normal: animatedDotColor
                        val ringDotColor = when {
                            isAirplanePhase -> {
                                val t = normalizedRing
                                val r = (0xFB * (1 - t) + 0xF5 * t) / 255f
                                val g = (0x71 * (1 - t) + 0x9E * t) / 255f
                                val b = (0x85 * (1 - t) + 0x0B * t) / 255f
                                Color(r, g, b, 1f)
                            }
                            isAcquiringPhase -> {
                                val t = normalizedRing
                                val r = (0x06 * (1 - t) + 0x10 * t) / 255f
                                val g = (0xB6 * (1 - t) + 0xB9 * t) / 255f
                                val b = (0xD4 * (1 - t) + 0x81 * t) / 255f
                                Color(r, g, b, 1f)
                            }
                            isConnected -> {
                                val t = normalizedRing
                                // High-tech radial gradient: Luminous Periwinkle Lavender (#9D9BFE) -> Electric Indigo (#6366F1) -> Deep Cosmic Indigo (#3730A3)
                                val r = if (t < 0.5f) {
                                    val f = t * 2f
                                    (0x9D * (1 - f) + 0x63 * f) / 255f
                                } else {
                                    val f = (t - 0.5f) * 2f
                                    (0x63 * (1 - f) + 0x37 * f) / 255f
                                }
                                val g = if (t < 0.5f) {
                                    val f = t * 2f
                                    (0x9B * (1 - f) + 0x66 * f) / 255f
                                } else {
                                    val f = (t - 0.5f) * 2f
                                    (0x66 * (1 - f) + 0x30 * f) / 255f
                                }
                                val b = if (t < 0.5f) {
                                    val f = t * 2f
                                    (0xFE * (1 - f) + 0xF1 * f) / 255f
                                } else {
                                    val f = (t - 0.5f) * 2f
                                    (0xF1 * (1 - f) + 0xA3 * f) / 255f
                                }

                                if (ringPulse > 0.05f) {
                                    val highlight = ringPulse * 0.35f
                                    val cr = (r * (1 - highlight) + 0.38f * highlight)
                                    val cg = (g * (1 - highlight) + 0.82f * highlight)
                                    val cb = (b * (1 - highlight) + 1.00f * highlight)
                                    Color(cr, cg, cb, 1f)
                                } else {
                                    Color(r, g, b, 1f)
                                }
                            }
                            else -> animatedDotColor
                        }

                        for (j in 0 until count) {
                            val angle = j * angleStep
                            val x = cx + currentRingRadius * cos(angle).toFloat()
                            val y = cy + currentRingRadius * sin(angle).toFloat()

                            drawCircle(
                                color = ringDotColor.copy(alpha = dotAlpha),
                                radius = currentDotRadius,
                                center = Offset(x, y)
                            )
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Center Power Button — Flat Matte Obsidian disc (130dp) with razor-sharp 2dp border
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF090C19)) // Clean matte obsidian background
                    .border(
                        width = 2.dp,
                        color = animatedBorderColor,
                        shape = CircleShape
                    )
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                        onLongClick = onLongClick
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
                        modifier = Modifier.size(36.dp)
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
