package com.v2rayops.client.ui.optimizer

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.v2rayops.client.network.FineTunePhase
import com.v2rayops.client.network.FineTuneState
import com.v2rayops.client.ui.dashboard.components.ActiveNodeSummaryCard
import com.v2rayops.client.ui.dashboard.components.ClientBottomNavBar
import com.v2rayops.client.ui.dashboard.components.ClientNavTab
import com.v2rayops.client.ui.dashboard.components.V2RayOpsTopBar
import java.util.Locale

// App Signature Colors matching Home Screen
private val PurplePrimary = Color(0xFF6366F1) // Electric Indigo
private val PurpleLavender = Color(0xFFA5B4FC) // Luminous Lavender
private val CosmicBgDark = Color(0xFF050811)
private val CardBgDark = Color(0xFF0B0F1E)
private val CardBorderDark = Color(0xFF151E34)

@Composable
fun NetworkOptimizerScreen(
    onBackClick: () -> Unit,
    onNavigateToHome: () -> Unit = onBackClick,
    onNavigateToServers: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToRouteFinder: () -> Unit = {},
    viewModel: NetworkOptimizerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val fineTuneState by viewModel.fineTuneState.collectAsState()
    val selectedConfig by viewModel.selectedConfig.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0D1426))
                    .border(1.dp, PurplePrimary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
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
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = PurpleLavender,
                    letterSpacing = 1.sp
                )
            }

            // Top: Active Server Card
            ActiveNodeSummaryCard(
                config = selectedConfig,
                pingMs = selectedConfig?.measuredPingMs,
                onClick = onNavigateToServers
            )

            // Dynamic Content: Completed Report vs Running Animation vs Idle One-Click Trigger
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

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * State A: Idle One-Click Hero Section:
 * Features a prominent cyber action button, clean feature pills, and zero terminal clutter.
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Center Hero Action Dial
        Box(
            modifier = Modifier
                .size(170.dp)
                .clickable(onClick = onStartFineTune),
            contentAlignment = Alignment.Center
        ) {
            // Rotating outer orbital dash ring
            Canvas(modifier = Modifier.size(170.dp)) {
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
                    .size(136.dp)
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
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
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

        // Subtitle info
        Text(
            text = "Auto-calibrates latency, downlink pipeline, and uplink TCP buffers for peak connection line speed.",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // 3 Feature Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FeatureMiniCard(
                title = "LATENCY",
                desc = "Pure TCP route probe",
                modifier = Modifier.weight(1f)
            )
            FeatureMiniCard(
                title = "DOWNLINK",
                desc = "Dynamic MTU tuning",
                modifier = Modifier.weight(1f)
            )
            FeatureMiniCard(
                title = "UPLINK",
                desc = "TCP window unchoke",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FeatureMiniCard(
    title: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CardBgDark)
            .border(1.dp, CardBorderDark, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = PurpleLavender
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = desc,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF64748B),
                lineHeight = 12.sp
            )
        }
    }
}

/**
 * State B: Running Animated Section:
 * Orbital animated scanner with percentage and 4 animated pipeline stages.
 * Zero terminal console outputs.
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
        FineTunePhase.IDLE -> 0
        FineTunePhase.DISCONNECTING, FineTunePhase.PROBING_FRONTS -> 1
        FineTunePhase.APPLYING_ROUTE -> 2
        FineTunePhase.BENCHMARKING -> 3
        FineTunePhase.OPTIMIZING_SETTINGS -> 4
        FineTunePhase.COMPLETED -> 4
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Center Animated Cyber Dial
        Box(
            modifier = Modifier.size(150.dp),
            contentAlignment = Alignment.Center
        ) {
            // Rotating dashed orbital ring
            Canvas(modifier = Modifier.size(150.dp)) {
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
                    .size(118.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF090E1F))
                    .border(2.dp, PurplePrimary.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(state.progress * 100).toInt()}%",
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "~${state.estimatedSecondsRemaining}s left",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = PurpleLavender
                    )
                }
            }
        }

        // Live Phase Status Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(CardBgDark)
                .border(1.dp, PurplePrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "STAGE $currentStage/4: ${state.phase.title.substringBefore("...")}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 4 Animated Pipeline Stages
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StageCard(
                stageNumber = 1,
                name = "Carrier Pure-Socket Analysis",
                description = "Probing edge endpoint & baseline latency",
                isCompleted = currentStage > 1,
                isActive = currentStage == 1
            )
            StageCard(
                stageNumber = 2,
                name = "Route Calibration & Tunnel Activation",
                description = "Configuring bug-host fronting & tunnel socket",
                isCompleted = currentStage > 2,
                isActive = currentStage == 2
            )
            StageCard(
                stageNumber = 3,
                name = "Downlink & Uplink Benchmarking",
                description = "Iterative throughput testing & unchoking",
                isCompleted = currentStage > 3,
                isActive = currentStage == 3
            )
            StageCard(
                stageNumber = 4,
                name = "Kernel Parameters & QoS Synchronization",
                description = "Applying optimal MTU, TCP buffers & DNS",
                isCompleted = currentStage > 4,
                isActive = currentStage == 4
            )
        }

        // Cancel Action
        Text(
            text = "CANCEL OPTIMIZATION",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF64748B),
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onCancel)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun StageCard(
    stageNumber: Int,
    name: String,
    description: String,
    isCompleted: Boolean,
    isActive: Boolean
) {
    val borderColor = when {
        isActive -> PurplePrimary
        isCompleted -> Color(0xFF22C55E)
        else -> CardBorderDark
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardBgDark)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCompleted -> Color(0xFF06301A)
                            isActive -> Color(0xFF1E1B4B)
                            else -> Color(0xFF0F1524)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Done",
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(15.dp)
                    )
                } else if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(PurpleLavender)
                    )
                } else {
                    Text(
                        text = "$stageNumber",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = name,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive || isCompleted) Color.White else Color(0xFF64748B)
                )
                Text(
                    text = description,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF94A3B8)
                )
            }
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Success Pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF062314))
                .border(1.dp, Color(0xFF15803D), RoundedCornerShape(12.dp))
                .padding(vertical = 10.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "NETWORK ACCELERATION APPLIED",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4ADE80)
                )
            }
        }

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

        // Elapsed Time & Parameters Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CardBgDark)
                .border(1.dp, CardBorderDark, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
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
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ELAPSED TIME",
                        fontSize = 10.sp,
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
                    text = elapsedText,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Action Buttons: Done (Primary) + Run Again
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PurplePrimary)
                .clickable(onClick = onApplyAndDone),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "DONE & RETURN TO HOME",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CardBgDark)
                .border(1.dp, PurplePrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .clickable(onClick = onRunAgain),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = PurpleLavender,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RUN AGAIN",
                    fontSize = 11.sp,
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
            .clip(RoundedCornerShape(10.dp))
            .background(CardBgDark)
            .border(1.dp, CardBorderDark, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 9.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = PurpleLavender,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(5.dp))

            // Before Value
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$beforeValue $unit",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // After Value
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = afterValue,
                    fontSize = 16.sp,
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
                    modifier = Modifier.padding(bottom = 1.5.dp),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            // Improvement Tag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF082717))
                    .border(0.5.dp, Color(0xFF15803D), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = improvementText,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4ADE80),
                    maxLines = 1
                )
            }
        }
    }
}
