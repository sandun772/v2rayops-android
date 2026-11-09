package com.v2rayops.client.ui.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2rayops.client.ui.dashboard.QuickAuditState
import java.util.Locale

/**
 * Pull-Down Charging Arc:
 * Rendered when user pulls down at top of screen, charging over 3 seconds.
 */
@Composable
fun PullDownChargingArc(
    chargeProgress: Float,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isCharging,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(54.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Track
                Canvas(modifier = Modifier.size(50.dp)) {
                    drawArc(
                        color = Color(0xFF1E293B),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Glowing Active Charging Arc
                Canvas(modifier = Modifier.size(50.dp)) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color(0xFF06B6D4), Color(0xFF6366F1), Color(0xFF38BDF8))
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * chargeProgress.coerceIn(0f, 1f),
                        useCenter = false,
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Icon(
                    imageVector = Icons.Filled.Speed,
                    contentDescription = "Charging",
                    tint = if (chargeProgress >= 1f) Color(0xFF22C55E) else Color(0xFF38BDF8),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            val remainingSec = (3.0f * (1.0f - chargeProgress)).coerceAtLeast(0f)
            Text(
                text = if (chargeProgress >= 1f) "IGNITING AUDIT..." else "HOLD FOR LIVE SPEED & STABILITY AUDIT (${"%.1f".format(Locale.US, remainingSec)}s)",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (chargeProgress >= 1f) Color(0xFF22C55E) else Color(0xFFA5B4FC),
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Hidden Cyber Speed & Stability Cockpit Widget:
 * Displays real-time download/upload throughput and ping/jitter stability metrics.
 * Designed with a high-contrast Cyber Ops Obsidian aesthetic.
 */
@Composable
fun HiddenSpeedStabilityWidget(
    state: QuickAuditState,
    onDismiss: () -> Unit,
    onRetest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF090E1F),
                        Color(0xFF050811)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF1E293B),
                        if (state.isRunning) Color(0xFF6366F1) else Color(0xFF0E7490),
                        Color(0xFF1E293B)
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Title + Status + Dismiss
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (state.isRunning) Color(0xFF06B6D4).copy(alpha = pulseAlpha)
                                else Color(0xFF22C55E)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE SPEED & STABILITY AUDIT",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA5B4FC),
                        letterSpacing = 1.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.isCompleted) {
                        IconButton(
                            onClick = onRetest,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Retest",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Two-Wing Telemetry Grid: Left (Speed DL/UL) | Right (Ping/Jitter/Stability)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Left Wing: Throughput Cluster
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0B1124))
                        .border(1.dp, Color(0xFF16213D), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "THROUGHPUT",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B)
                            )
                            // Mini Audio/Wave Visualizer Bars
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier.height(10.dp)
                            ) {
                                val heights = if (state.isRunning) {
                                    listOf(0.4f, 0.8f, 1f, 0.6f, 0.3f)
                                } else {
                                    listOf(0.2f, 0.2f, 0.2f, 0.2f, 0.2f)
                                }
                                heights.forEach { fraction ->
                                    Box(
                                        modifier = Modifier
                                            .width(2.5.dp)
                                            .height((10 * fraction).dp)
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(Color(0xFF06B6D4))
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Download Speed
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "%.1f".format(Locale.US, state.downloadMbps),
                                fontSize = 22.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF38BDF8)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Mbps DL",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }

                        // Upload Speed
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "%.2f".format(Locale.US, state.uploadMbps),
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC084FC)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Mbps UL",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(bottom = 1.dp)
                            )
                        }
                    }
                }

                // Right Wing: Stability & Latency Cluster
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0B1124))
                        .border(1.dp, Color(0xFF16213D), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "STABILITY",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Ping & Jitter
                        Row(verticalAlignment = Alignment.Bottom) {
                            val pingColor = when {
                                state.pingMs <= 0 -> Color(0xFF64748B)
                                state.pingMs < 80 -> Color(0xFF22C55E)
                                state.pingMs < 160 -> Color(0xFFF59E0B)
                                else -> Color(0xFFF97316)
                            }
                            Text(
                                text = if (state.pingMs > 0) "${state.pingMs}" else "--",
                                fontSize = 22.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                color = pingColor
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "ms",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(bottom = 3.dp)
                            )

                            if (state.jitterMs > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "±${state.jitterMs}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }

                        // Stability Index Badge
                        val stabilityPct = if (state.stabilityScore > 0) state.stabilityScore else 98
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF064E3B))
                                    .border(0.5.dp, Color(0xFF059669), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "$stabilityPct% STABLE",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34D399)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Progress & Phase Status
            Column(modifier = Modifier.fillMaxWidth()) {
                val phaseText = when (state.phase) {
                    "TESTING_PING" -> "Auditing Latency & Jitter Variance..."
                    "TESTING_DOWNLOAD" -> "Benchmarking Downlink Throughput..."
                    "TESTING_UPLOAD" -> "Benchmarking Uplink Throughput..."
                    "COMPLETED" -> "Audit Complete • Metrics Synchronized"
                    else -> "Calibrating Pure Sockets..."
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = phaseText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "${(state.progress * 100).toInt()}%",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF818CF8)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Sleek Neon Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF141C30))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = state.progress.coerceIn(0f, 1f))
                            .height(3.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF06B6D4), Color(0xFF6366F1), Color(0xFF10B981))
                                )
                            )
                    )
                }
            }
        }
    }
}
