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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2rayops.client.designsystem.theme.CyberAccent
import com.v2rayops.client.designsystem.theme.CyberBorder
import com.v2rayops.client.designsystem.theme.CyberBorderSubtle
import com.v2rayops.client.designsystem.theme.CyberDanger
import com.v2rayops.client.designsystem.theme.CyberPrimary
import com.v2rayops.client.designsystem.theme.CyberSuccess
import com.v2rayops.client.designsystem.theme.CyberTextMuted
import com.v2rayops.client.designsystem.theme.CyberTextPrimary
import com.v2rayops.client.designsystem.theme.CyberWarning
import com.v2rayops.client.domain.model.ConnectionState
import kotlin.math.hypot
import kotlin.math.pow

@Composable
fun CyberConnectButtonArea(
    connectionState: ConnectionState,
    isUnstable: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionState.isConnected
    val isConnecting = connectionState.isConnecting || connectionState is ConnectionState.Disconnecting
    val isRetrying = connectionState is ConnectionState.Error

    val activeStateColor by animateColorAsState(
        targetValue = when {
            isRetrying -> CyberDanger
            isConnecting -> CyberAccent
            isUnstable && isConnected -> CyberWarning
            isConnected -> CyberSuccess
            else -> CyberPrimary
        },
        animationSpec = tween(400)
    )

    // Pulse animation for connected or connecting
    val infiniteTransition = rememberInfiniteTransition(label = "CyberConnectAura")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isConnected || isConnecting) 1.06f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpinAngle"
    )

    val density = LocalDensity.current
    val gridSpacingPx = with(density) { 20.dp.toPx() }
    val dotRadiusPx = with(density) { 1.25.dp.toPx() }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 270dp Box containing the Circular Dot-Matrix Aura + Connect Orb
        Box(
            modifier = Modifier
                .size(260.dp)
                .drawBehind {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val maxRadius = size.minDimension / 2f

                    // Draw circular gradient dots aura strictly within maxRadius
                    var x = gridSpacingPx / 2f
                    while (x < size.width) {
                        var y = gridSpacingPx / 2f
                        while (y < size.height) {
                            val dist = hypot(x - cx, y - cy)
                            if (dist <= maxRadius) {
                                val norm = dist / maxRadius
                                val alpha = 0.22f * (1f - norm).pow(1.3f)
                                drawCircle(
                                    color = Color(0xFF94A3B8).copy(alpha = alpha.coerceIn(0f, 1f)),
                                    radius = dotRadiusPx,
                                    center = Offset(x, y)
                                )
                            }
                            y += gridSpacingPx
                        }
                        x += gridSpacingPx
                    }

                    // Draw subtle concentric cyber radar rings
                    drawCircle(
                        color = activeStateColor.copy(alpha = if (isConnected) 0.12f else 0.05f),
                        radius = maxRadius * 0.95f,
                        style = Stroke(width = 1f)
                    )
                    drawCircle(
                        color = activeStateColor.copy(alpha = if (isConnected) 0.18f else 0.08f),
                        radius = maxRadius * 0.72f,
                        style = Stroke(width = 1f)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Rotating kinetic orbital ring during connecting
            if (isConnecting) {
                Box(
                    modifier = Modifier
                        .size(176.dp)
                        .rotate(spinAngle)
                        .drawBehind {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(
                                        Color.Transparent,
                                        activeStateColor.copy(alpha = 0.1f),
                                        activeStateColor
                                    )
                                ),
                                startAngle = 0f,
                                sweepAngle = 280f,
                                useCenter = false,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                )
            }

            // Outer subtle glow halo
            Box(
                modifier = Modifier
                    .size(152.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                activeStateColor.copy(alpha = if (isConnected) 0.22f else 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Precision Cyber Connect Button (144dp)
            Box(
                modifier = Modifier
                    .size(144.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0F172A),
                                Color(0xFF0A0F1E)
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                activeStateColor,
                                activeStateColor.copy(alpha = 0.6f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Inner concentric tech boundary
                Box(
                    modifier = Modifier
                        .size(122.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.dp,
                            color = activeStateColor.copy(alpha = 0.25f),
                            shape = CircleShape
                        )
                )

                // Neon Power Symbol
                Icon(
                    imageVector = Icons.Filled.PowerSettingsNew,
                    contentDescription = "VPN Start/Stop",
                    tint = activeStateColor,
                    modifier = Modifier.size(54.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Cyber Status Capsule
        val statusText = when {
            isRetrying -> "RETRYING CONNECTION"
            isConnecting -> "ESTABLISHING TUNNEL..."
            isUnstable && isConnected -> "UNSTABLE LATENCY"
            isConnected -> "CONNECTED • SECURE"
            else -> "TAP TO CONNECT"
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0B101E))
                .border(1.dp, activeStateColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(activeStateColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                color = activeStateColor
            )
        }
    }
}
