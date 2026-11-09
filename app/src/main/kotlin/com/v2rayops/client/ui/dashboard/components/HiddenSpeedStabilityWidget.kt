package com.v2rayops.client.ui.dashboard.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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

// App Signature Connect Button Purples
private val PurplePrimary = Color(0xFF6366F1) // Electric Indigo matching radar connect dial border
private val PurpleLavender = Color(0xFFA5B4FC) // Luminous periwinkle/lavender matching dial icon & text
private val CardBgDark = Color(0xFF0B0F1E)
private val CardBorderDark = Color(0xFF151E34)

/**
 * Pull-Down Charging Pill:
 * Smoothly translates down with finger swipe, charging over 3 seconds.
 * Styled with the exact Connect Button Electric Indigo (#6366F1) & White.
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
                    .background(CardBgDark)
                    .border(1.dp, PurplePrimary.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Purple Charging Circular Arc matching Connect Button #6366F1
                Box(
                    modifier = Modifier.size(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(16.dp)) {
                        drawArc(
                            color = CardBorderDark,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = PurplePrimary,
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
                    color = if (chargeProgress >= 1f) PurpleLavender else Color.White,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

/**
 * 3-Card Speed & Stability HUD:
 * Displays DOWNLOAD, UPLOAD, and STABILITY cards side-by-side.
 * Compact, low height (~58dp), Connect Button Electric Indigo (#6366F1) & White theme.
 * Skeleton shimmer loading applies smoothly across the card widgets at start while
 * both Download and Upload start at 0.0 with download increasing first and then upload.
 */
@Composable
fun HiddenSpeedStabilityWidget(
    state: QuickAuditState,
    onDismiss: () -> Unit,
    onRetest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "widgetShimmer")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -250f,
        targetValue = 550f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    // Skeleton shimmer brush across the card surfaces during audit
    val cardBackground = if (state.isRunning) {
        Brush.linearGradient(
            colors = listOf(
                CardBgDark,
                Color(0xFF171A36), // Subtle Electric Indigo ambient shimmer
                CardBgDark
            ),
            start = Offset(shimmerTranslate, 0f),
            end = Offset(shimmerTranslate + 180f, 80f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(CardBgDark, CardBgDark)
        )
    }

    val cardBorder = if (state.isRunning) {
        PurplePrimary.copy(alpha = 0.5f)
    } else {
        CardBorderDark
    }

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
                color = PurpleLavender
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.isCompleted) {
                    Text(
                        text = "RETEST",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = PurpleLavender,
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
                    .background(cardBackground)
                    .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
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
                        color = PurpleLavender
                    )

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "%.1f".format(Locale.US, state.downloadMbps),
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

            // Card 2: UPLOAD
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(cardBackground)
                    .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
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
                        color = PurpleLavender
                    )

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "%.1f".format(Locale.US, state.uploadMbps),
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

            // Card 3: STABILITY (PING)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(cardBackground)
                    .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
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
                        color = PurpleLavender
                    )

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
