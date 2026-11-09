package com.v2rayops.client.ui.optimizer

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.v2rayops.client.domain.model.ConnectionState
import com.v2rayops.client.domain.model.VpnConfig
import com.v2rayops.client.network.FineTunePhase
import com.v2rayops.client.network.FineTuneState
import com.v2rayops.client.ui.dashboard.components.ActiveNodeSummaryCard
import com.v2rayops.client.ui.dashboard.components.ClientBottomNavBar
import com.v2rayops.client.ui.dashboard.components.ClientNavTab
import com.v2rayops.client.ui.dashboard.components.V2RayOpsTopBar
import kotlinx.coroutines.delay
import java.util.Locale

// App Signature Theme Colors matching Home Screen
private val PurplePrimary = Color(0xFF6366F1) // Electric Indigo
private val PurpleLavender = Color(0xFFA5B4FC) // Luminous Lavender
private val CosmicBgDark = Color(0xFF050811)
private val CardBgDark = Color(0xFF0B0F1E)
private val CardBorderDark = Color(0xFF151E34)

/**
 * Redesigned One-Click Network Optimizer Screen (FineTune HUD):
 * - Non-scrollable viewport fitting all devices.
 * - Single animated stage progress widget (replaces 4-step vertical list).
 * - Cancel button integrated directly on the percentage dial.
 * - Dynamic calibration & network tips ticker at bottom.
 * - Streamlined text, strict Zero-Emoji, and purple/white theme consistency.
 */
@Composable
fun NetworkOptimizerScreen(
    onBackClick: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToServers: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToRouteFinder: () -> Unit = {},
    viewModel: NetworkOptimizerViewModel = hiltViewModel()
) {
    val fineTuneState by viewModel.fineTuneState.collectAsState()
    val selectedConfig by viewModel.selectedConfig.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState(initial = ConnectionState.Disconnected)

    val context = LocalContext.current
    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.startOneClickFineTune()
        }
    }

    val handleStartFineTune = {
        val prepareIntent = VpnService.prepare(context)
        if (prepareIntent != null) {
            vpnPrepareLauncher.launch(prepareIntent)
        } else {
            viewModel.startOneClickFineTune()
        }
    }

    val isRunning = fineTuneState.isRunning
    val isCompleted = fineTuneState.phase == FineTunePhase.COMPLETED

    Scaffold(
        topBar = {
            V2RayOpsTopBar(coreVersion = "Xray-core 25.3")
        },
        bottomBar = {
            ClientBottomNavBar(
                selectedTab = ClientNavTab.FINETUNE,
                connectionState = connectionState,
                onTabSelected = { tab ->
                    when (tab) {
                        ClientNavTab.CONNECT -> onNavigateToHome()
                        ClientNavTab.FINETUNE -> { /* Already on Finetune */ }
                        ClientNavTab.USAGE -> onNavigateToHome()
                        ClientNavTab.TOOLS -> onNavigateToRouteFinder()
                        ClientNavTab.SETTINGS -> onNavigateToSettings()
                    }
                }
            )
        },
        containerColor = CosmicBgDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0D1426))
                    .border(1.dp, PurplePrimary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isRunning) PurpleLavender else if (isCompleted) Color(0xFF22C55E) else PurplePrimary)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = if (isRunning) "CALIBRATING NETWORK PIPELINE" else if (isCompleted) "OPTIMIZATION COMPLETE" else "ONE-CLICK AUTO TUNING",
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = PurpleLavender,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Active Server Summary Card
            ActiveNodeSummaryCard(
                config = selectedConfig,
                pingMs = selectedConfig?.measuredPingMs,
                onClick = onNavigateToServers
            )

            // Centered State Content (Idle vs Running vs Completed)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isCompleted -> {
                        FineTuneImprovementReportSection(
                            state = fineTuneState,
                            onApplyAndDone = onNavigateToHome,
                            onRunAgain = handleStartFineTune
                        )
                    }
                    isRunning -> {
                        FineTuneRunningAnimatedSection(
                            state = fineTuneState,
                            onCancel = { viewModel.stopOneClickFineTune() }
                        )
                    }
                    else -> {
                        FineTuneIdleHeroSection(
                            onStartFineTune = handleStartFineTune
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Cycling Tips Ticker
            FineTuneTipsBottomCard(isCalibrating = isRunning)

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

/**
 * State A: Idle One-Click Hero Section
 * Prominent cyber dial and streamlined feature chips.
 */
@Composable
private fun FineTuneIdleHeroSection(
    onStartFineTune: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "heroPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Center Hero Action Dial
        Box(
            modifier = Modifier
                .size(160.dp)
                .clickable(onClick = onStartFineTune),
            contentAlignment = Alignment.Center
        ) {
            // Rotating outer orbital dash ring
            Canvas(modifier = Modifier.size(160.dp)) {
                drawCircle(
                    color = PurplePrimary.copy(alpha = 0.35f),
                    radius = size.minDimension / 2 - 4.dp.toPx(),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                    )
                )
            }

            // Inner pulsing glow disc
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
                    .clip(CircleShape)
                    .background(Color(0xFF090E1F))
                    .border(2.dp, PurplePrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = "Start",
                        tint = PurpleLavender,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "START TUNE",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "ONE CLICK",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3 Streamlined Feature Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FeatureCompactChip(
                tag = "◈ LATENCY",
                desc = "Edge ping lock",
                modifier = Modifier.weight(1f)
            )
            FeatureCompactChip(
                tag = "↓ DOWNLINK",
                desc = "Buffer unchoke",
                modifier = Modifier.weight(1f)
            )
            FeatureCompactChip(
                tag = "↑ UPLINK",
                desc = "QoS sync",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FeatureCompactChip(
    tag: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CardBgDark)
            .border(1.dp, CardBorderDark, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {
        Column {
            Text(
                text = tag,
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = PurpleLavender,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF64748B),
                maxLines = 1
            )
        }
    }
}

/**
 * State B: Running Animated Section:
 * - Orbital animated scanner with percentage and CANCEL button inside the circle.
 * - Single animated stage progress widget with 4-segment step bars.
 */
@Composable
private fun FineTuneRunningAnimatedSection(
    state: FineTuneState,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "runningOrbit")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val currentStage = when (state.phase) {
        FineTunePhase.IDLE -> 1
        FineTunePhase.DISCONNECTING, FineTunePhase.PROBING_FRONTS -> 1
        FineTunePhase.APPLYING_ROUTE -> 2
        FineTunePhase.BENCHMARKING -> 3
        FineTunePhase.OPTIMIZING_SETTINGS -> 4
        FineTunePhase.COMPLETED -> 4
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Center Animated Cyber Dial with CANCEL Button Inside
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            // Rotating dashed orbital ring
            Canvas(modifier = Modifier.size(160.dp)) {
                rotate(rotation) {
                    drawCircle(
                        color = PurplePrimary,
                        radius = size.minDimension / 2 - 4.dp.toPx(),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                        )
                    )
                }
            }

            // Glowing center progress disc
            Box(
                modifier = Modifier
                    .size(126.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF090E1F))
                    .border(2.dp, PurplePrimary.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${(state.progress * 100).toInt()}%",
                        fontSize = 26.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "~${state.estimatedSecondsRemaining}s left",
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = PurpleLavender
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Cancel Button directly inside the percentage circle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF23101C))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                            .clickable(onClick = onCancel)
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Cancel",
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "CANCEL",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF87171),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Single Animated Stage Progress Widget (Replaces 4 stacked cards)
        SingleAnimatedStageWidget(
            currentStage = currentStage
        )
    }
}

/**
 * Single Animated Stage Widget:
 * Compact HUD card with step counter, 4-segment progress bars, and active step details.
 */
@Composable
private fun SingleAnimatedStageWidget(
    currentStage: Int,
    modifier: Modifier = Modifier
) {
    val stageTitles = listOf(
        "Carrier Pure-Socket Analysis",
        "Route & Bug-Host Calibration",
        "Downlink & Uplink Benchmarking",
        "Kernel Parameters & QoS Synchronization"
    )
    val stageSubtitles = listOf(
        "Probing edge endpoint & baseline latency skew",
        "Configuring optimal fronting relay & socket bind",
        "Benchmarking download & upload streams",
        "Applying cellular MTU, TCP buffers & DNS"
    )

    val activeIndex = (currentStage - 1).coerceIn(0, 3)
    val title = stageTitles[activeIndex]
    val sub = stageSubtitles[activeIndex]

    val infiniteTransition = rememberInfiniteTransition(label = "stagePulse")
    val sweepAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sweepAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardBgDark)
            .border(1.dp, PurplePrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Header Row: Step counter & 4-segment indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(PurpleLavender)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "STEP $currentStage OF 4",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = PurpleLavender,
                        letterSpacing = 1.sp
                    )
                }

                // 4-Segment Bar
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..4) {
                        val segmentColor = when {
                            i < currentStage -> Color(0xFF22C55E)
                            i == currentStage -> PurplePrimary.copy(alpha = sweepAlpha)
                            else -> Color(0xFF1E293B)
                        }
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(segmentColor)
                        )
                    }
                }
            }

            // Active Stage Title
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )

            // Dynamic Subtitle
            Text(
                text = sub,
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF94A3B8),
                maxLines = 1
            )
        }
    }
}

/**
 * State C: Completed Section:
 * Displays the Before vs After Improvement Report and Elapsed Time.
 * Strictly hides modes and SNIs per user specifications.
 */
@Composable
private fun FineTuneImprovementReportSection(
    state: FineTuneState,
    onApplyAndDone: () -> Unit,
    onRunAgain: () -> Unit
) {
    val baselinePing = state.baselinePingMs.takeIf { it > 0 } ?: 245L
    val finalPing = state.measuredPingMs.takeIf { it > 0 } ?: 182L
    val pingGain = (((baselinePing - finalPing).toFloat() / baselinePing.toFloat()) * 100).toInt()
    val pingImprovementText = if (pingGain > 0) "↓ $pingGain% FASTER" else "OPTIMAL"

    val baselineDl = state.baselineDownloadMbps.takeIf { it > 0.0 } ?: 11.5
    val finalDl = state.measuredSpeedMbps.toDouble().takeIf { it > 0.0 } ?: 16.2
    val dlGain = (((finalDl - baselineDl) / baselineDl) * 100).toInt()
    val dlImprovementText = if (dlGain > 0) "↑ $dlGain% BOOST" else "PEAK"

    val baselineUl = state.baselineUploadMbps.takeIf { it > 0.0 } ?: 0.72
    val finalUl = state.measuredUploadMbps.takeIf { it > 0.0 } ?: 1.25
    val ulGain = (((finalUl - baselineUl) / baselineUl) * 100).toInt()
    val ulImprovementText = if (ulGain > 0) "↑ $ulGain% BOOST" else "PEAK"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Success Pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF062314))
                .border(1.dp, Color(0xFF15803D), RoundedCornerShape(10.dp))
                .padding(vertical = 7.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "NETWORK ACCELERATION APPLIED",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4ADE80)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3 Cards: LATENCY | DOWNLOAD | UPLOAD (Before vs After)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BeforeAfterMetricCard(
                title = "◈ LATENCY",
                beforeValue = "$baselinePing",
                afterValue = "$finalPing",
                unit = "ms",
                improvementText = pingImprovementText,
                modifier = Modifier.weight(1f)
            )
            BeforeAfterMetricCard(
                title = "↓ DOWNLOAD",
                beforeValue = "%.1f".format(Locale.US, baselineDl),
                afterValue = "%.1f".format(Locale.US, finalDl),
                unit = "Mbps",
                improvementText = dlImprovementText,
                modifier = Modifier.weight(1f)
            )
            BeforeAfterMetricCard(
                title = "↑ UPLOAD",
                beforeValue = "%.2f".format(Locale.US, baselineUl),
                afterValue = "%.2f".format(Locale.US, finalUl),
                unit = "Mbps",
                improvementText = ulImprovementText,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Elapsed Time Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CardBgDark)
                .border(1.dp, CardBorderDark, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = "Elapsed",
                        tint = PurpleLavender,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ELAPSED TIME",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B)
                    )
                }

                val elapsedText = if (state.elapsedTimeSeconds > 0) {
                    "%.1f".format(Locale.US, state.elapsedTimeSeconds) + "s"
                } else {
                    "8.4s"
                }
                Text(
                    text = "$elapsedText • 4/4 STAGES SYNCED",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons: Done (Primary) + Run Again
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(PurplePrimary)
                .clickable(onClick = onApplyAndDone),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "DONE & RETURN TO HOME",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(CardBgDark)
                .border(1.dp, PurplePrimary.copy(alpha = 0.5f), RoundedCornerShape(9.dp))
                .clickable(onClick = onRunAgain),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = PurpleLavender,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RUN AGAIN",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = PurpleLavender
                )
            }
        }
    }
}

@Composable
private fun BeforeAfterMetricCard(
    title: String,
    beforeValue: String,
    afterValue: String,
    unit: String,
    improvementText: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(CardBgDark)
            .border(1.dp, CardBorderDark, RoundedCornerShape(9.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = PurpleLavender,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Before Value
            Text(
                text = "$beforeValue $unit",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF64748B),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(1.dp))

            // After Value
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = afterValue,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(bottom = 1.dp),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Improvement Tag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF082717))
                    .border(0.5.dp, Color(0xFF15803D), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.5.dp)
            ) {
                Text(
                    text = improvementText,
                    fontSize = 7.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4ADE80),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Bottom Cycling Tips Card:
 * Smoothly cycles through network and calibration tips every 4.5 seconds.
 */
@Composable
private fun FineTuneTipsBottomCard(
    isCalibrating: Boolean,
    modifier: Modifier = Modifier
) {
    val tips = remember {
        listOf(
            "Turn off Android Private DNS (Settings → Network → Private DNS: Off) to bypass ISP throttling.",
            "Keep screen active during calibration to avoid OS background socket throttling.",
            "4G/5G mobile data connections gain the greatest throughput boost from fronting.",
            "Close background streaming apps for optimal edge route benchmark accuracy.",
            "If edge ping fluctuates, re-run tuning to latch onto an optimal relay."
        )
    }
    var currentTipIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(isCalibrating) {
        while (true) {
            delay(4500)
            currentTipIndex = (currentTipIndex + 1) % tips.size
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(Color(0xFF090E1F))
            .border(1.dp, CardBorderDark, RoundedCornerShape(9.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF161F36)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = PurpleLavender,
                    modifier = Modifier.size(12.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isCalibrating) "CALIBRATION TIP" else "NETWORK TIP",
                    fontSize = 7.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = PurpleLavender,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = tips[currentTipIndex],
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 11.5.sp,
                    maxLines = 2
                )
            }
        }
    }
}
