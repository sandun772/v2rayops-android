package com.v2rayops.client.ui.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
 * Skeleton shimmer component for loading states in the 3 cards.
 * Uses the app's purple & dark theme palette (#151E34 to #8B5CF6).
 */
@Composable
fun SkeletonShimmer(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerColors = listOf(
        Color(0xFF151E34),
        Color(0xFF8B5CF6).copy(alpha = 0.28f),
        Color(0xFF151E34)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = shimmerTranslate, y = shimmerTranslate)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(brush)
    )
}

/**
 * Pull-Down Charging Pill:
 * Smoothly translates down with finger swipe, charging over 3 seconds.
 * Strictly adheres to the app's purple & white theme.
 */
@Composable
fun PullDownChargingPill(
    pullOffset: Float,
    chargeProgress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(pullOffset.dp)
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        if (pullOffset > 10f) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0B0F1E))
                    .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Purple Charging Circular Arc
                Box(
                    modifier = Modifier.size(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(16.dp)) {
                        drawArc(
                            color = Color(0xFF151E34),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = Color(0xFF8B5CF6),
                            startAngle = -90f,
                            sweepAngle = 360f * chargeProgress.coerceIn(0f, 1f),
                            useCenter = false,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                val remainingSec = (3.0f * (1.0f - chargeProgress)).coerceAtLeast(0f)
                Text(
                    text = if (chargeProgress >= 1f) "READY" else "HOLD 3S FOR AUDIT (${"%.1f".format(Locale.US, remainingSec)}s)",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (chargeProgress >= 1f) Color(0xFFA78BFA) else Color.White,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

/**
 * 3-Card Speed & Stability HUD:
 * Displays DOWNLOAD, UPLOAD, and STABILITY cards side-by-side.
 * Compact, low height (~58dp), strict purple & white theme, skeleton loading.
 */
@Composable
fun HiddenSpeedStabilityWidget(
    state: QuickAuditState,
    onDismiss: () -> Unit,
    onRetest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Minimalist Header: LIVE AUDIT + Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LIVE AUDIT",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = Color(0xFFA78BFA)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.isCompleted) {
                    Text(
                        text = "RETEST",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA78BFA),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onRetest)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = "✕",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF64748B),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 3 Cards: DOWNLOAD | UPLOAD | STABILITY
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: DOWNLOAD
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0B0F1E))
                    .border(1.dp, Color(0xFF151E34), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "↓ DL",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA78BFA)
                    )

                    if (state.isRunning && state.downloadMbps <= 0.0) {
                        SkeletonShimmer(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(16.dp)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = if (state.downloadMbps > 0) "%.1f".format(Locale.US, state.downloadMbps) else "--",
                                fontSize = 17.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Mbps",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(bottom = 1.dp)
                            )
                        }
                    }
                }
            }

            // Card 2: UPLOAD
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0B0F1E))
                    .border(1.dp, Color(0xFF151E34), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "↑ UL",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA78BFA)
                    )

                    if (state.isRunning && state.uploadMbps <= 0.0) {
                        SkeletonShimmer(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(16.dp)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = if (state.uploadMbps > 0) "%.1f".format(Locale.US, state.uploadMbps) else "--",
                                fontSize = 17.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Mbps",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(bottom = 1.dp)
                            )
                        }
                    }
                }
            }

            // Card 3: STABILITY (PING)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0B0F1E))
                    .border(1.dp, Color(0xFF151E34), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "◈ PING",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA78BFA)
                    )

                    if (state.isRunning && state.pingMs <= 0) {
                        SkeletonShimmer(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(16.dp)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = if (state.pingMs > 0) "${state.pingMs}" else "--",
                                fontSize = 17.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "ms",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(bottom = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
