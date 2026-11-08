package com.v2rayops.client.ui.dashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2rayops.client.designsystem.theme.CyberAccent
import com.v2rayops.client.designsystem.theme.CyberBorder
import com.v2rayops.client.designsystem.theme.CyberDanger
import com.v2rayops.client.designsystem.theme.CyberPrimary
import com.v2rayops.client.designsystem.theme.CyberSuccess
import com.v2rayops.client.designsystem.theme.CyberTextMuted
import com.v2rayops.client.designsystem.theme.CyberTextPrimary
import com.v2rayops.client.designsystem.theme.CyberWarning
import com.v2rayops.client.domain.model.ConnectionState

enum class PowerButtonVisualState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    UNSTABLE,
    RETRYING
}

@Composable
fun GlassyPowerButton(
    connectionState: ConnectionState,
    isUnstable: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visualState = when {
        connectionState is ConnectionState.Error -> PowerButtonVisualState.RETRYING
        connectionState.isConnecting || connectionState is ConnectionState.Disconnecting -> PowerButtonVisualState.CONNECTING
        isUnstable && connectionState.isConnected -> PowerButtonVisualState.UNSTABLE
        connectionState.isConnected -> PowerButtonVisualState.CONNECTED
        else -> PowerButtonVisualState.DISCONNECTED
    }

    val stateColor by animateColorAsState(
        targetValue = when (visualState) {
            PowerButtonVisualState.CONNECTED -> CyberSuccess
            PowerButtonVisualState.CONNECTING -> CyberAccent
            PowerButtonVisualState.UNSTABLE -> CyberWarning
            PowerButtonVisualState.RETRYING -> CyberDanger
            PowerButtonVisualState.DISCONNECTED -> Color(0xFF64748B)
        },
        animationSpec = tween(350),
        label = "stateColor"
    )

    // Tactile press depression animation
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "pressScale"
    )

    // Pulse animations for connecting and ambient glow
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )

    val isBusy = visualState == PowerButtonVisualState.CONNECTING

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(230.dp)
                .scale(pressScale),
            contentAlignment = Alignment.Center
        ) {
            // 1. Multi-Tiered Ambient Glow Halo
            val glowAlpha = when (visualState) {
                PowerButtonVisualState.CONNECTED -> 0.42f
                PowerButtonVisualState.CONNECTING -> 0.55f * pulseGlow
                PowerButtonVisualState.UNSTABLE -> 0.40f * pulseGlow
                PowerButtonVisualState.RETRYING -> 0.48f * pulseGlow
                PowerButtonVisualState.DISCONNECTED -> 0.14f
            }

            Box(
                modifier = Modifier
                    .size(230.dp)
                    .graphicsLayer { alpha = glowAlpha }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                stateColor.copy(alpha = 0.75f),
                                stateColor.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // 2. Rotating Kinetic Particle Ring (while Connecting)
            if (isBusy) {
                Box(
                    modifier = Modifier
                        .size(196.dp)
                        .rotate(rotationAngle)
                        .border(
                            BorderStroke(
                                width = 2.dp,
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        CyberPrimary,
                                        CyberAccent,
                                        Color.Transparent
                                    )
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }

            // 3. Main Glassy Orb Lens (180dp diameter)
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                    // Bezel rim with specular gradient
                    .border(
                        BorderStroke(
                            width = 2.dp,
                            brush = if (isBusy) {
                                Brush.sweepGradient(
                                    colors = listOf(CyberPrimary, CyberAccent, CyberPrimary)
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        stateColor.copy(alpha = 0.8f),
                                        Color(0x33FFFFFF),
                                        stateColor.copy(alpha = 0.4f),
                                        Color(0xFF0F172A)
                                    )
                                )
                            }
                        ),
                        shape = CircleShape
                    )
                    // Glass body background with convex specular reflection
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0x3DFFFFFF), // Top specular reflection
                                Color(0xFF141D30), // Mid glass refraction
                                Color(0xFF0A0F1D)  // Deep obsidian base
                            ),
                            center = Offset(130f, 60f),
                            radius = 280f
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Secondary inner specular crescent arc
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                        .clip(CircleShape)
                        .border(
                            BorderStroke(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x4DFFFFFF),
                                        Color.Transparent
                                    )
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // 4. Center Power Icon (58dp)
                Icon(
                    imageVector = Icons.Filled.PowerSettingsNew,
                    contentDescription = "VPN Power Toggle",
                    tint = stateColor,
                    modifier = Modifier
                        .size(58.dp)
                        .graphicsLayer {
                            shadowElevation = if (visualState == PowerButtonVisualState.CONNECTED) 16f else 0f
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 5. Dynamic Status Capsule
        StatusCapsule(visualState = visualState, stateColor = stateColor)
    }
}

@Composable
private fun StatusCapsule(
    visualState: PowerButtonVisualState,
    stateColor: Color
) {
    val statusText = when (visualState) {
        PowerButtonVisualState.CONNECTED -> "CONNECTED • SECURE"
        PowerButtonVisualState.CONNECTING -> "ESTABLISHING TUNNEL..."
        PowerButtonVisualState.UNSTABLE -> "UNSTABLE LATENCY"
        PowerButtonVisualState.RETRYING -> "RETRYING CONNECTION"
        PowerButtonVisualState.DISCONNECTED -> "TAP TO CONNECT"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xD90C101B))
            .border(1.dp, stateColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Pulse Dot
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(stateColor)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = statusText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = if (visualState == PowerButtonVisualState.DISCONNECTED) CyberTextMuted else CyberTextPrimary
            )
        }
    }
}
