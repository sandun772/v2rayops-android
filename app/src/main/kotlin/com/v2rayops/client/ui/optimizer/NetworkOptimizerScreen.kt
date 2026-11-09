package com.v2rayops.client.ui.optimizer

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.v2rayops.client.ui.dashboard.components.ClientBottomNavBar
import com.v2rayops.client.ui.dashboard.components.ClientNavTab
import com.v2rayops.client.ui.servers.components.OneClickFineTuneCard
import com.v2rayops.client.designsystem.components.CyberCard
import com.v2rayops.client.designsystem.components.CyberTopAppBar
import com.v2rayops.client.designsystem.theme.CyberAccent
import com.v2rayops.client.designsystem.theme.CyberBackground
import com.v2rayops.client.designsystem.theme.CyberBorder
import com.v2rayops.client.designsystem.theme.CyberBorderSubtle
import com.v2rayops.client.designsystem.theme.CyberCard
import com.v2rayops.client.designsystem.theme.CyberCardElevated
import com.v2rayops.client.designsystem.theme.CyberDanger
import com.v2rayops.client.designsystem.theme.CyberPrimary
import com.v2rayops.client.designsystem.theme.CyberSuccess
import com.v2rayops.client.designsystem.theme.CyberTextMuted
import com.v2rayops.client.designsystem.theme.CyberTextPrimary
import com.v2rayops.client.designsystem.theme.CyberTextSecondary
import com.v2rayops.client.designsystem.theme.CyberWarning
import com.v2rayops.client.network.BeastOptimizationReport
import com.v2rayops.client.network.BeastProgressState
import com.v2rayops.client.network.BeastTestPhase
import com.v2rayops.client.network.BeastTestProfile
import com.v2rayops.client.network.CongestionStressProgress
import com.v2rayops.client.network.CongestionStressResult
import com.v2rayops.client.network.Download100MbProgress
import com.v2rayops.client.network.Download100MbResult
import com.v2rayops.client.network.DownloadBenchmarkProgress
import com.v2rayops.client.network.DownloadBenchmarkResult
import com.v2rayops.client.network.InAppBenchmarkReport
import com.v2rayops.client.network.NetworkOptimizationReport
import com.v2rayops.client.network.SpeedTestPhase
import com.v2rayops.client.network.SpeedTestProgress
import com.v2rayops.client.network.SpeedTestResult
import com.v2rayops.client.network.StabilityProgress
import com.v2rayops.client.network.StreamingBenchmarkResult
import com.v2rayops.client.network.StreamingQuality
import com.v2rayops.client.network.TwoMinuteStabilityResult
import java.util.Locale

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

    val report by viewModel.report.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val isApplying by viewModel.isApplying.collectAsState()
    val applySuccess by viewModel.applySuccess.collectAsState()

    val isTestingDl by viewModel.isTestingDownload.collectAsState()
    val streamResult by viewModel.streamingResult.collectAsState()
    val isTestingStream by viewModel.isTestingStreaming.collectAsState()

    val beastState by viewModel.beastProgressState.collectAsState()

    // 2-Minute Stability, 100MB Download, Congestion Stress, Speedtest states
    val stabProgress by viewModel.stabilityProgress.collectAsState()
    val stabResult by viewModel.stabilityResult.collectAsState()
    val dl100Progress by viewModel.download100MbProgress.collectAsState()
    val dl100Result by viewModel.download100MbResult.collectAsState()
    val congestionProgress by viewModel.congestionProgress.collectAsState()
    val congestionResult by viewModel.congestionResult.collectAsState()
    val speedProgress by viewModel.speedTestProgress.collectAsState()
    val speedResult by viewModel.speedTestResult.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Carrier Audit, 1: Speedtest, 2: Stress & 100MB, 3: Beast Auto-Tune

    Scaffold(
        topBar = {
            CyberTopAppBar(
                title = "FINETUNE ENGINE",
                onBackClick = onBackClick,
                actions = {
                    val anyActive = isAnalyzing || isTestingDl || isTestingStream || beastState.isRunning ||
                            stabProgress.isRunning || dl100Progress.isRunning || congestionProgress.isRunning || speedProgress.isRunning
                    IconButton(
                        onClick = {
                            when (selectedTab) {
                                0 -> viewModel.runAnalysis()
                                1 -> viewModel.startFullSpeedTest()
                                2 -> viewModel.startTwoMinuteStabilityTest(120)
                                3 -> viewModel.startBeastOptimization(BeastTestProfile.EXHAUSTIVE_15M)
                            }
                        },
                        enabled = !anyActive
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Re-analyze",
                            tint = if (anyActive) CyberTextMuted else CyberAccent
                        )
                    }
                }
            )
        },
        bottomBar = {
            ClientBottomNavBar(
                selectedTab = ClientNavTab.FINETUNE,
                connectionState = connectionState,
                onTabSelected = { tab ->
                    when (tab) {
                        ClientNavTab.CONNECT -> onNavigateToHome()
                        ClientNavTab.FINETUNE -> { /* Already on Finetune */ }
                        ClientNavTab.TOOLS -> onNavigateToRouteFinder()
                        ClientNavTab.USAGE -> onNavigateToHome()
                        ClientNavTab.SETTINGS -> onNavigateToSettings()
                    }
                }
            )
        },
        containerColor = Color(0xFF050811)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            // --- 0. HERO: ONE-CLICK FULL TUNING AUTO ENGINE ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OneClickFineTuneCard(
                    state = fineTuneState,
                    selectedConfig = selectedConfig,
                    onStartFineTune = handleStartFineTune,
                    onStopFineTune = { viewModel.stopOneClickFineTune() },
                    onDismissReport = { viewModel.dismissFineTuneReport() },
                    onSelectMode = { viewModel.setFineTuneTargetMode(it) },
                    onSelectPreset = { viewModel.selectPresetConfig(it) }
                )
            }

            // --- 1. Section Header for Granular Controls ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "MANUAL DEV CALIBRATION & AUDIT",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
            }

            // 4-Way Tab Selector: [ CARRIER & PMTU | SPEEDTEST | STRESS & 100MB | BEAST AUTO-TUNE ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TabSelectorPill(
                    label = "CARRIER & PMTU",
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                TabSelectorPill(
                    label = "SPEEDTEST",
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                TabSelectorPill(
                    label = "STRESS & 100MB",
                    isSelected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
                TabSelectorPill(
                    label = "BEAST AUTO-TUNE",
                    isSelected = selectedTab == 3,
                    onClick = { selectedTab = 3 }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // TAB 0: CARRIER & PMTU AUDIT
                        AnimatedVisibility(
                            visible = isAnalyzing,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            CyberCard(
                                backgroundColor = CyberCardElevated,
                                borderColor = CyberAccent,
                                padding = 16.dp
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = CyberAccent
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = "ANALYZING CLIENT NETWORK...",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = CyberAccent
                                        )
                                        Text(
                                            text = "Testing PMTU, latency jitter, and DNS resolver race",
                                            fontSize = 11.sp,
                                            color = CyberTextMuted
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        if (report != null) {
                            val rep = report!!

                            HeroRatingCard(rep)
                            Spacer(modifier = Modifier.height(14.dp))

                            LatencyMetricsCard(rep)
                            Spacer(modifier = Modifier.height(14.dp))

                            DpiJitterInspectionCard(rep)
                            Spacer(modifier = Modifier.height(14.dp))

                            DnsRaceCard(rep)
                            Spacer(modifier = Modifier.height(14.dp))

                            RecommendedTuningCard(rep)
                            Spacer(modifier = Modifier.height(20.dp))

                            if (applySuccess || rep.isAlreadyOptimized) {
                                CyberCard(
                                    backgroundColor = CyberCard,
                                    borderColor = CyberSuccess.copy(alpha = 0.6f),
                                    padding = 12.dp
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Applied",
                                            tint = CyberSuccess,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "CONFIGURATION IS FULLY OPTIMIZED FOR THIS NETWORK",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = CyberSuccess
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            Button(
                                onClick = { viewModel.applyOptimization() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isApplying && !isAnalyzing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (rep.isAlreadyOptimized) CyberCardElevated else CyberPrimary
                                )
                            ) {
                                if (isApplying) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "APPLYING CONFIGURATION...", fontSize = 12.sp, color = Color.White)
                                } else {
                                    Icon(
                                        imageVector = if (rep.isAlreadyOptimized) Icons.Default.Check else Icons.Default.Tune,
                                        contentDescription = "Apply",
                                        tint = if (rep.isAlreadyOptimized) CyberSuccess else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (rep.isAlreadyOptimized) "APPLIED & ACTIVE (CONFIGURED)" else "APPLY OPTIMAL CONFIGURATION",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (rep.isAlreadyOptimized) CyberSuccess else Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            CyberCard(
                                modifier = Modifier.fillMaxWidth(),
                                padding = 14.dp,
                                borderColor = CyberAccent,
                                backgroundColor = CyberCardElevated,
                                onClick = onNavigateToRouteFinder
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AltRoute,
                                            contentDescription = null,
                                            tint = CyberAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "FIND FASTEST SNI & CARRIER ROUTE",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = CyberTextPrimary,
                                                letterSpacing = 0.5.sp
                                            )
                                            Text(
                                                text = "Audit zero-rated bug hosts & TLS handshake latencies",
                                                fontSize = 11.sp,
                                                color = CyberTextMuted
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedButton(
                                onClick = { viewModel.runAnalysis() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isAnalyzing,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorderSubtle)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Re-run",
                                    tint = CyberTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "RE-RUN NETWORK BENCHMARK",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberTextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(30.dp))
                        }
                    }
                    1 -> {
                        // TAB 1: FULL 3-PHASE SPEEDTEST (PING / JITTER, DOWNLOAD, UPLOAD)
                        SpeedTestSection(
                            progress = speedProgress,
                            result = speedResult,
                            onStart = { viewModel.startFullSpeedTest() },
                            onStop = { viewModel.stopFullSpeedTest() }
                        )
                    }
                    2 -> {
                        // TAB 2: STRESS & STABILITY SUITE (2-MIN AUDIT, 100MB DOWNLOAD, CONGESTION STRESS)
                        StressAnd100MbSection(
                            stabProgress = stabProgress,
                            stabResult = stabResult,
                            onStartStability = { viewModel.startTwoMinuteStabilityTest(120) },
                            onStopStability = { viewModel.stopTwoMinuteStabilityTest() },
                            dl100Progress = dl100Progress,
                            dl100Result = dl100Result,
                            onStartDl100 = { viewModel.start100MbDownloadTest() },
                            onStopDl100 = { viewModel.stop100MbDownloadTest() },
                            congestionProgress = congestionProgress,
                            congestionResult = congestionResult,
                            onStartCongestion = { viewModel.startCongestionStressTest(30) },
                            onStopCongestion = { viewModel.stopCongestionStressTest() },
                            streamingResult = streamResult,
                            isTestingStreaming = isTestingStream,
                            onRunStreaming = { viewModel.runStreamingTest() }
                        )
                    }
                    3 -> {
                        // TAB 3: ALL-IN-ONE BEAST NETWORK OPTIMIZATION SUITE
                        BeastOptimizationSection(
                            state = beastState,
                            onStart = { profile -> viewModel.startBeastOptimization(profile) },
                            onStop = { viewModel.stopBeastOptimization() },
                            onApplyBest = { viewModel.applyCurrentBestBeast() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabSelectorPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) CyberPrimary else CyberCard)
            .border(
                width = 1.dp,
                color = if (isSelected) CyberAccent else CyberBorder,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (isSelected) Color.White else CyberTextSecondary
        )
    }
}

@Composable
private fun BeastOptimizationSection(
    state: BeastProgressState,
    onStart: (BeastTestProfile) -> Unit,
    onStop: () -> Unit,
    onApplyBest: () -> Unit
) {
    var selectedProfile by remember { mutableStateOf(BeastTestProfile.EXHAUSTIVE_15M) }

    Column {
        // Hero Notice Card
        CyberCard(
            modifier = Modifier.fillMaxWidth(),
            padding = 16.dp,
            borderColor = CyberAccent,
            backgroundColor = CyberCardElevated
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = CyberAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ALL-IN-ONE BEAST AUTO-OPTIMIZATION",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextPrimary,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "This is an exhaustive, deep network auto-tuning engine. Unlike an instant guess, it stress-tests bug hosts over sustained intervals (15m - 30m), measures carrier packet loss & jitter under continuous bursts, calibrates DPI packet fragmentation, and auto-applies the optimal V2Ray settings for maximum stability.",
                    fontSize = 11.sp,
                    color = CyberTextSecondary,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Profile Duration Selector Pills
        if (!state.isRunning) {
            Text(
                text = "SELECT CALIBRATION DEPTH & DURATION:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = CyberTextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BeastTestProfile.values().forEach { prof ->
                    val isSelected = selectedProfile == prof
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) CyberPrimary else CyberCard)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) CyberAccent else CyberBorderSubtle,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { selectedProfile = prof }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${prof.durationMinutes} MINS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSelected) Color.White else CyberTextPrimary
                            )
                            Text(
                                text = prof.displayName.uppercase(),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else CyberTextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Live Execution Card
        if (state.isRunning) {
            val remMin = state.estimatedRemainingSeconds / 60
            val remSec = state.estimatedRemainingSeconds % 60
            val timeFormatted = String.format("%02d:%02d", remMin, remSec)

            val elapMin = state.elapsedSeconds / 60
            val elapSec = state.elapsedSeconds % 60
            val elapsedFormatted = String.format("%02d:%02d", elapMin, elapSec)

            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 16.dp,
                borderColor = CyberAccent,
                backgroundColor = CyberCard
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ESTIMATED REMAINING TIME",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextMuted
                            )
                            Text(
                                text = timeFormatted,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyberAccent
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "ELAPSED: $elapsedFormatted",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextSecondary
                            )
                            Text(
                                text = "${(state.overallProgressPercent * 100).toInt()}% COMPLETED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyberSuccess
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { state.overallProgressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = CyberAccent,
                        trackColor = CyberBorder
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Phase Badge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberCardElevated)
                            .border(1.dp, CyberAccent.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = CyberAccent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.currentPhase.title.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live stress metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MiniMetric(
                            label = "PACKET LOSS",
                            value = "${"%.1f".format(state.measuredLossPercent)}%",
                            modifier = Modifier.weight(1f)
                        )
                        MiniMetric(
                            label = "CARRIER JITTER",
                            value = "±${state.measuredJitterMs}ms",
                            modifier = Modifier.weight(1f)
                        )
                        MiniMetric(
                            label = "WINNING ROUTE",
                            value = state.winningRoute?.domain?.substringBefore(".") ?: "--",
                            modifier = Modifier.weight(1f)
                        )
                        MiniMetric(
                            label = "OPTIMAL MTU",
                            value = "${state.winningMtu ?: 1380}B",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Console Terminal Output
                    Text(
                        text = "LIVE CALIBRATION STREAM:",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberBackground)
                            .border(1.dp, CyberBorderSubtle, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            state.statusLog.takeLast(6).forEach { line ->
                                Text(
                                    text = "> $line",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (line.contains("Winning", ignoreCase = true) || line.contains("SUCCESS", ignoreCase = true)) CyberSuccess else CyberTextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onApplyBest,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberAccent)
                        ) {
                            Text(
                                text = "APPLY CURRENT BEST",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }

                        OutlinedButton(
                            onClick = onStop,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberDanger)
                        ) {
                            Text(
                                text = "ABORT TEST",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyberDanger
                            )
                        }
                    }
                }
            }
        } else if (state.beastReport != null) {
            // Completed Beast Scorecard
            val rep = state.beastReport
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 16.dp,
                borderColor = CyberSuccess,
                backgroundColor = CyberCard
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = CyberSuccess,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BEAST CALIBRATION ACTIVE & CONFIGURED",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberSuccess
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        TuningRow(
                            label = "Winning Route & Strategy",
                            value = "[${rep.winningRoute.strategy.shortTag}] ${rep.winningRoute.domain}",
                            detail = if (rep.winningRoute.strategy == com.v2rayops.client.network.RouteStrategy.ADDR_SNI) "Addr Fronting (Zero Timeout Isolation)" else "Direct SNI Mode"
                        )
                        HorizontalDivider(color = CyberBorderSubtle, thickness = 1.dp)

                        TuningRow(
                            label = "Calibrated Cellular MTU",
                            value = "${rep.winningMtu} Bytes",
                            detail = "Zero-fragmentation LTE boundary"
                        )
                        HorizontalDivider(color = CyberBorderSubtle, thickness = 1.dp)

                        TuningRow(
                            label = "Fastest DNS Resolver",
                            value = "${rep.winningDns} (${rep.winningDnsProvider})",
                            detail = "Lowest query RTT"
                        )
                        HorizontalDivider(color = CyberBorderSubtle, thickness = 1.dp)

                        TuningRow(
                            label = "DPI Evasion & Jitter Fix",
                            value = "ACTIVE (100-200B / 10-20ms)",
                            detail = "Middlebox TLS fragmentation & TCP NoDelay"
                        )
                        HorizontalDivider(color = CyberBorderSubtle, thickness = 1.dp)

                        TuningRow(
                            label = "Sustained Packet Loss Rate",
                            value = "${"%.1f".format(rep.packetLossPercent)}%",
                            detail = "Measured across continuous stress bursts"
                        )
                        HorizontalDivider(color = CyberBorderSubtle, thickness = 1.dp)

                        TuningRow(
                            label = "Sustained Carrier Jitter",
                            value = "±${rep.avgSustainedJitterMs}ms",
                            detail = "Stabilized with adaptive token-bucket queue"
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { onStart(selectedProfile) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary)
                    ) {
                        Text(
                            text = "RE-CALIBRATE BEAST OPTIMIZATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            // Idle State: Start Calibration Button
            Button(
                onClick = { onStart(selectedProfile) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "START ${selectedProfile.durationMinutes}-MINUTE BEAST CALIBRATION",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // What this suite tunes card
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 14.dp,
                backgroundColor = CyberCard
            ) {
                Column {
                    Text(
                        text = "WHAT THIS MULTI-PHASE TEST WILL CONFIGURE:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val features = listOf(
                        "Multi-round validation of all bug hosts (Standard SNI & Addr-Fronting)",
                        "Carrier DPI middlebox packet fragmentation calibration (100-200B)",
                        "Precise cellular PMTU boundary discovery (1360 - 1420 Bytes)",
                        "Sustained packet loss & timing jitter stress testing across rounds",
                        "TCP_NODELAY Nagle bypass to eliminate carrier delayed-ACKs",
                        "TCP Sliding window buffer & anti-bufferbloat headroom calculation"
                    )

                    features.forEach { feat ->
                        Row(modifier = Modifier.padding(vertical = 3.dp)) {
                            Text(
                                text = "• ",
                                fontSize = 11.sp,
                                color = CyberAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = feat,
                                fontSize = 11.sp,
                                color = CyberTextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun SpeedTestSection(
    progress: SpeedTestProgress,
    result: SpeedTestResult?,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CyberCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = CyberCardElevated,
            borderColor = if (progress.isRunning) CyberAccent else CyberBorder,
            padding = 16.dp
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = CyberAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SPEEDTEST (DL / UL / LATENCY)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberTextPrimary,
                            letterSpacing = 1.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (progress.isRunning) CyberAccent.copy(alpha = 0.2f) else CyberBorderSubtle)
                            .border(1.dp, if (progress.isRunning) CyberAccent else CyberBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = progress.phase.label.uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (progress.isRunning) CyberAccent else CyberTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Throughput Readouts Grid (Download & Upload)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Download Speed Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberBackground)
                            .border(1.dp, if (progress.phase == SpeedTestPhase.DOWNLOAD) CyberAccent else CyberBorderSubtle, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = CyberSuccess, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "DOWNLOAD",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberTextMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val displayDl = if (progress.phase == SpeedTestPhase.DOWNLOAD && progress.currentSpeedMbps > 0) {
                                progress.currentSpeedMbps
                            } else {
                                result?.downloadSpeedMbps ?: progress.downloadSpeedMbps
                            }
                            Text(
                                text = "%.2f".format(Locale.US, displayDl),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextPrimary
                            )
                            Text(
                                text = "Mbps" + (if (result != null) " (Peak: ${"%.1f".format(Locale.US, result.peakDownloadMbps)})" else ""),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextMuted
                            )
                        }
                    }

                    // Upload Speed Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberBackground)
                            .border(1.dp, if (progress.phase == SpeedTestPhase.UPLOAD) CyberAccent else CyberBorderSubtle, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = CyberAccent, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "UPLOAD",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberTextMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val displayUl = if (progress.phase == SpeedTestPhase.UPLOAD && progress.currentSpeedMbps > 0) {
                                progress.currentSpeedMbps
                            } else {
                                result?.uploadSpeedMbps ?: progress.uploadSpeedMbps
                            }
                            Text(
                                text = "%.2f".format(Locale.US, displayUl),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextPrimary
                            )
                            Text(
                                text = "Mbps" + (if (result != null) " (Peak: ${"%.1f".format(Locale.US, result.peakUploadMbps)})" else ""),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Latency & Jitter Box Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricMiniBox(
                        label = "PING (LATENCY)",
                        value = "${result?.pingMs ?: progress.pingMs} ms",
                        color = CyberPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricMiniBox(
                        label = "JITTER VARIANCE",
                        value = "±${result?.jitterMs ?: progress.jitterMs} ms",
                        color = CyberWarning,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (progress.isRunning) {
                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { progress.progressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = CyberAccent,
                        trackColor = CyberBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = progress.statusText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextMuted
                    )
                }

                if (result != null && !progress.isRunning) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberSuccess.copy(alpha = 0.12f))
                            .border(1.dp, CyberSuccess.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberSuccess, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = result.rating,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyberSuccess
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Button
                if (progress.isRunning) {
                    Button(
                        onClick = onStop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberDanger),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "STOP SPEEDTEST",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }
                } else {
                    Button(
                        onClick = onStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (result != null) "RE-TEST CONNECTION SPEED" else "START FULL SPEEDTEST",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StressAnd100MbSection(
    stabProgress: StabilityProgress,
    stabResult: TwoMinuteStabilityResult?,
    onStartStability: () -> Unit,
    onStopStability: () -> Unit,
    dl100Progress: Download100MbProgress,
    dl100Result: Download100MbResult?,
    onStartDl100: () -> Unit,
    onStopDl100: () -> Unit,
    congestionProgress: CongestionStressProgress,
    congestionResult: CongestionStressResult?,
    onStartCongestion: () -> Unit,
    onStopCongestion: () -> Unit,
    streamingResult: StreamingBenchmarkResult?,
    isTestingStreaming: Boolean,
    onRunStreaming: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Two-Minute Deep Stability Audit Card
        TwoMinuteStabilityCard(
            progress = stabProgress,
            result = stabResult,
            onStart = onStartStability,
            onStop = onStopStability
        )

        // 2. 100MB Download & Automatic Dev Report Card
        Download100MbCard(
            progress = dl100Progress,
            result = dl100Result,
            onStart = onStartDl100,
            onStop = onStopDl100
        )

        // 3. Congestion & Bufferbloat Stress Card (Concurrent DL + UL)
        CongestionStressCard(
            progress = congestionProgress,
            result = congestionResult,
            onStart = onStartCongestion,
            onStop = onStopCongestion
        )

        // 4. Video Streaming Buffer Simulation Card
        StreamingSimulationCard(
            result = streamingResult,
            isTesting = isTestingStreaming,
            onTestClick = onRunStreaming
        )

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun TwoMinuteStabilityCard(
    progress: StabilityProgress,
    result: TwoMinuteStabilityResult?,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val remainingSec = (progress.totalSeconds - progress.elapsedSeconds).coerceAtLeast(0)
    val timeFormatted = String.format(Locale.US, "%02d:%02d", remainingSec / 60, remainingSec % 60)

    val currentGrade = result?.stabilityGrade ?: when {
        progress.stabilityScorePercent >= 90 && progress.packetLossPercent <= 2.0 -> "ROCK SOLID (A+)"
        progress.stabilityScorePercent >= 78 -> "STABLE (A)"
        progress.stabilityScorePercent >= 62 -> "MODERATE (B)"
        progress.stabilityScorePercent >= 45 -> "JITTER PRONE (C)"
        progress.stabilityScorePercent > 0 -> "DEGRADED (D)"
        else -> "IDLE"
    }

    val gradeColor = when {
        currentGrade.contains("A") -> CyberSuccess
        currentGrade.contains("B") -> CyberAccent
        currentGrade.contains("C") -> CyberWarning
        else -> CyberDanger
    }

    CyberCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = CyberCardElevated,
        borderColor = if (progress.isRunning) CyberAccent else CyberBorder,
        padding = 16.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NetworkCheck,
                        contentDescription = null,
                        tint = CyberAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "2-MIN DEEP STABILITY AUDIT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextPrimary,
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(gradeColor.copy(alpha = 0.15f))
                        .border(1.dp, gradeColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = currentGrade,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = gradeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Timer & Stability Score Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "REMAINING TIME",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextMuted
                    )
                    Text(
                        text = timeFormatted,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (progress.isRunning) CyberAccent else CyberTextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "STABILITY SCORE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextMuted
                    )
                    Text(
                        text = "${progress.stabilityScorePercent}%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = gradeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { progress.progressPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = CyberAccent,
                trackColor = CyberBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Latency & Loss Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetricMiniBox(
                    label = "AVG PING",
                    value = "${progress.avgLatencyMs} ms",
                    color = CyberTextPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricMiniBox(
                    label = "JITTER",
                    value = "±${progress.jitterMs} ms",
                    color = CyberWarning,
                    modifier = Modifier.weight(1f)
                )
                MetricMiniBox(
                    label = "LOSS",
                    value = "${"%.1f".format(Locale.US, progress.packetLossPercent)}%",
                    color = if (progress.packetLossPercent > 5.0) CyberDanger else CyberSuccess,
                    modifier = Modifier.weight(1f)
                )
                MetricMiniBox(
                    label = "PROBES",
                    value = "${progress.packetsReceived}/${progress.packetsSent}",
                    color = CyberAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            // Recent Ping Sparkline Bars
            if (progress.recentPings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "RECENT SAMPLES (LAST 20 PINGS)",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = CyberTextMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyberBackground)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    progress.recentPings.forEach { ping ->
                        val barHeight = ((ping.toFloat() / 350f).coerceIn(0.12f, 1f) * 26).dp
                        val barColor = if (ping < 120) CyberSuccess else if (ping < 280) CyberWarning else CyberDanger
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(barHeight)
                                .clip(RoundedCornerShape(1.dp))
                                .background(barColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = progress.statusSummary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = CyberTextMuted
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (progress.isRunning) {
                Button(
                    onClick = onStop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberDanger),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "STOP 2-MIN AUDIT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            } else {
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (result != null) "RE-RUN 2-MIN STABILITY AUDIT" else "START 2-MIN STABILITY AUDIT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun Download100MbCard(
    progress: Download100MbProgress,
    result: Download100MbResult?,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val mbReceived = progress.bytesReceived / (1024.0 * 1024.0)

    CyberCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = CyberCardElevated,
        borderColor = if (progress.isRunning) CyberAccent else CyberBorder,
        padding = 16.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = CyberAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "100MB DOWNLOAD & DEV REPORT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextPrimary,
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyberAccent.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "ANYCAST CDN",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Payload Progress Meter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TRANSFERRED",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextMuted
                    )
                    Text(
                        text = "${"%.1f".format(Locale.US, mbReceived)} MB / 100 MB",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "CURRENT SPEED",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextMuted
                    )
                    Text(
                        text = "${"%.2f".format(Locale.US, progress.currentMbps)} Mbps",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberSuccess
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress.progressPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = CyberSuccess,
                trackColor = CyberBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Speed stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricMiniBox(
                    label = "AVERAGE",
                    value = "${"%.2f".format(Locale.US, result?.avgSpeedMbps ?: progress.avgMbps)} Mbps",
                    color = CyberTextPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricMiniBox(
                    label = "PEAK SPEED",
                    value = "${"%.2f".format(Locale.US, result?.peakSpeedMbps ?: progress.peakMbps)} Mbps",
                    color = CyberAccent,
                    modifier = Modifier.weight(1f)
                )
                MetricMiniBox(
                    label = "TIME / ETA",
                    value = "${progress.elapsedSeconds}s" + (if (progress.estimatedRemainingSeconds > 0) " (ETA ${progress.estimatedRemainingSeconds}s)" else ""),
                    color = CyberWarning,
                    modifier = Modifier.weight(1f)
                )
            }

            // Dev Report VPS Sync Banner
            if (result?.devReportUploaded == true) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberSuccess.copy(alpha = 0.12f))
                        .border(1.dp, CyberSuccess.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = CyberSuccess, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "DEV REPORT #${result.devReportId} UPLOADED TO VPS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyberSuccess
                            )
                            Text(
                                text = "Telemetry & 100MB benchmark metrics synchronized to VPS for analysis",
                                fontSize = 8.sp,
                                color = CyberTextMuted
                            )
                        }
                    }
                }
            } else if (result?.devReportUploadError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "VPS Report Upload Note: ${result.devReportUploadError}",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberWarning
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (progress.isRunning) {
                Button(
                    onClick = onStop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberDanger),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "STOP 100MB DOWNLOAD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            } else {
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (result != null) "RE-RUN 100MB BENCHMARK" else "START 100MB DOWNLOAD & DEV DUMP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun CongestionStressCard(
    progress: CongestionStressProgress,
    result: CongestionStressResult?,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val grade = result?.bufferbloatGrade ?: when {
        progress.bufferbloatDeltaMs <= 15 -> "GRADE A+ (Zero Inflation)"
        progress.bufferbloatDeltaMs <= 35 -> "GRADE A (Minimal Delay)"
        progress.bufferbloatDeltaMs <= 70 -> "GRADE B (Mild Bloat)"
        progress.bufferbloatDeltaMs <= 150 -> "GRADE C (Noticeable Delay)"
        progress.bufferbloatDeltaMs <= 300 -> "GRADE D (Heavy Queuing)"
        progress.bufferbloatDeltaMs > 0 -> "GRADE F (Bufferbloat Saturated)"
        else -> "IDLE"
    }

    val gradeColor = when {
        grade.contains("A") -> CyberSuccess
        grade.contains("B") -> CyberAccent
        grade.contains("C") -> CyberWarning
        else -> CyberDanger
    }

    CyberCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = CyberCardElevated,
        borderColor = if (progress.isRunning) CyberAccent else CyberBorder,
        padding = 16.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = CyberAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CONGESTION & BUFFERBLOAT STRESS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextPrimary,
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(gradeColor.copy(alpha = 0.15f))
                        .border(1.dp, gradeColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = grade,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = gradeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Simultaneously downloads & uploads data streams while measuring latency inflation under active load",
                fontSize = 9.sp,
                color = CyberTextMuted
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Ping Delta Comparison Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricMiniBox(
                    label = "BASELINE IDLE PING",
                    value = "${progress.baselineIdlePingMs} ms",
                    color = CyberTextMuted,
                    modifier = Modifier.weight(1f)
                )
                MetricMiniBox(
                    label = "LOADED ACTIVE PING",
                    value = "${progress.currentPingMs} ms",
                    color = CyberTextPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricMiniBox(
                    label = "BUFFERBLOAT DELTA",
                    value = "+${progress.bufferbloatDeltaMs} ms",
                    color = gradeColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Concurrent Throughput Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricMiniBox(
                    label = "ACTIVE DOWNLOAD",
                    value = "${"%.1f".format(Locale.US, progress.currentDownloadMbps)} Mbps",
                    color = CyberSuccess,
                    modifier = Modifier.weight(1f)
                )
                MetricMiniBox(
                    label = "ACTIVE UPLOAD",
                    value = "${"%.1f".format(Locale.US, progress.currentUploadMbps)} Mbps",
                    color = CyberAccent,
                    modifier = Modifier.weight(1f)
                )
                MetricMiniBox(
                    label = "PACKET LOSS",
                    value = "${"%.1f".format(Locale.US, progress.packetLossPercent)}%",
                    color = if (progress.packetLossPercent > 3.0) CyberDanger else CyberTextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            if (progress.isRunning) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress.progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = CyberAccent,
                    trackColor = CyberBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = progress.statusMessage,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberTextMuted
                )
            }

            if (result?.recommendation != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberBackground)
                        .border(1.dp, CyberBorderSubtle, RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = result.recommendation,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (progress.isRunning) {
                Button(
                    onClick = onStop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberDanger),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "STOP STRESS TEST",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            } else {
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (result != null) "RE-RUN CONGESTION TEST" else "RUN CONGESTION STRESS TEST (30s)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricMiniBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CyberBackground)
            .border(1.dp, CyberBorderSubtle, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column {
            Text(
                text = label,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = CyberTextMuted
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = color
            )
        }
    }
}

@Composable
private fun ThroughputBenchmarkSection(
    progress: DownloadBenchmarkProgress,
    downloadResult: DownloadBenchmarkResult?,
    streamingResult: StreamingBenchmarkResult?,
    isTestingDownload: Boolean,
    isTestingStreaming: Boolean,
    benchmarkReport: InAppBenchmarkReport?,
    isApplying: Boolean,
    applySuccess: Boolean,
    onRunDownload: () -> Unit,
    onRunStreaming: () -> Unit,
    onRunFullTest: () -> Unit,
    onApplyOptimizations: () -> Unit
) {
    Column {
        // Hero Action Button
        Button(
            onClick = onRunFullTest,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !isTestingDownload && !isTestingStreaming,
            colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isTestingDownload || isTestingStreaming) "RUNNING BENCHMARK IN APP..." else "BENCHMARK DOWNLOAD & STREAMING",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 1. Download Benchmark Card
        DownloadBenchmarkCard(
            progress = progress,
            result = downloadResult,
            isTesting = isTestingDownload,
            onTestClick = onRunDownload
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Video Streaming Simulation Card
        StreamingSimulationCard(
            result = streamingResult,
            isTesting = isTestingStreaming,
            onTestClick = onRunStreaming
        )

        // 3. Throughput-Driven Tuning Recommendation
        if (benchmarkReport != null) {
            Spacer(modifier = Modifier.height(16.dp))
            ThroughputOptimizationCard(
                report = benchmarkReport,
                isApplying = isApplying,
                applySuccess = applySuccess,
                onApply = onApplyOptimizations
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun DownloadBenchmarkCard(
    progress: DownloadBenchmarkProgress,
    result: DownloadBenchmarkResult?,
    isTesting: Boolean,
    onTestClick: () -> Unit
) {
    CyberCard(
        modifier = Modifier.fillMaxWidth(),
        padding = 16.dp,
        borderColor = if (isTesting) CyberAccent else CyberBorder
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = CyberAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MICRO-PAYLOAD DOWNLOAD TEST",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextPrimary
                    )
                }

                if (!isTesting) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CyberCardElevated)
                            .border(1.dp, CyberBorderSubtle, RoundedCornerShape(4.dp))
                            .clickable(onClick = onTestClick)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "TEST",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val displaySpeed = if (isTesting) progress.currentSpeedMbps else result?.avgSpeedMbps ?: 0.0
            val peakSpeed = result?.peakSpeedMbps ?: progress.currentSpeedMbps

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "MEASURED THROUGHPUT",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextMuted
                    )
                    Text(
                        text = if (displaySpeed > 0.0) "${"%.2f".format(displaySpeed)} Mbps" else "-- Mbps",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (displaySpeed > 10.0) CyberSuccess else if (displaySpeed > 3.0) CyberAccent else CyberWarning
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "PEAK: ${"%.2f".format(peakSpeed)} Mbps",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextSecondary
                    )
                    if (result != null && result.bufferbloatPingIncreaseMs > 0) {
                        Text(
                            text = "Bufferbloat Delta: +${result.bufferbloatPingIncreaseMs}ms",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (result.bufferbloatPingIncreaseMs < 30) CyberSuccess else CyberWarning
                        )
                    }
                }
            }

            if (isTesting) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress.progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = CyberAccent,
                    trackColor = CyberBorder
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Streaming test chunks: ${(progress.bytesReceived / 1024)} KB read...",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberTextMuted
                )
            } else if (result != null && result.isSuccessful) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Successfully transferred ${(result.totalBytes / 1024)} KB in ${result.durationMs}ms",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberSuccess
                )
            }
        }
    }
}

@Composable
private fun StreamingSimulationCard(
    result: StreamingBenchmarkResult?,
    isTesting: Boolean,
    onTestClick: () -> Unit
) {
    CyberCard(
        modifier = Modifier.fillMaxWidth(),
        padding = 16.dp,
        borderColor = if (isTesting) CyberAccent else CyberBorder
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = CyberAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VIDEO STREAMING SIMULATION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextPrimary
                    )
                }

                if (!isTesting) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CyberCardElevated)
                            .border(1.dp, CyberBorderSubtle, RoundedCornerShape(4.dp))
                            .clickable(onClick = onTestClick)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "TEST",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isTesting) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = CyberAccent
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Simulating consecutive video segments & TTFB...",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberAccent
                    )
                }
            } else if (result != null && result.isSuccessful) {
                val qualityColor = when (result.quality) {
                    StreamingQuality.UHD_4K -> CyberSuccess
                    StreamingQuality.FHD_1080P -> CyberAccent
                    StreamingQuality.HD_720P -> CyberWarning
                    StreamingQuality.SD_480P -> CyberDanger
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "STREAMING READINESS",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberTextMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(qualityColor.copy(alpha = 0.2f))
                                .border(1.dp, qualityColor, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = result.quality.label.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = qualityColor
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "TTFB: ${result.timeToFirstByteMs}ms",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberTextPrimary
                        )
                        Text(
                            text = "Bitrate: ${"%.2f".format(result.sustainedBitrateMbps)} Mbps",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberAccent
                        )
                        Text(
                            text = "Buffer: ${result.bufferStabilityPercent}% Stable",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberSuccess
                        )
                    }
                }
            } else {
                Text(
                    text = "Simulates HLS video chunk loading, calculates Time-to-First-Frame (TTFB) and buffer underrun safety for 1080p/4K video.",
                    fontSize = 11.sp,
                    color = CyberTextMuted,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun ThroughputOptimizationCard(
    report: InAppBenchmarkReport,
    isApplying: Boolean,
    applySuccess: Boolean,
    onApply: () -> Unit
) {
    CyberCard(
        modifier = Modifier.fillMaxWidth(),
        padding = 16.dp,
        borderColor = CyberAccent,
        backgroundColor = CyberCardElevated
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = CyberAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TUNING SYNTHESIS FROM TEST RESULTS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyberTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = report.optimizationSummary,
                fontSize = 11.sp,
                color = CyberTextSecondary,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniMetric(
                    label = "TCP BUFFER",
                    value = "${report.recommendedTcpBufferKb} KB",
                    modifier = Modifier.weight(1f)
                )
                MiniMetric(
                    label = "QOS HEADROOM",
                    value = if (report.recommendedHeadroomMbps > 0) "${report.recommendedHeadroomMbps} MBPS" else "AUTO",
                    modifier = Modifier.weight(1f)
                )
                MiniMetric(
                    label = "ANTI-BUFFERBLOAT",
                    value = "ENABLED",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (applySuccess) {
                CyberCard(
                    backgroundColor = CyberCard,
                    borderColor = CyberSuccess.copy(alpha = 0.6f),
                    padding = 10.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = CyberSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STREAMING OPTIMIZATIONS APPLIED TO ENGINE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberSuccess
                        )
                    }
                }
            } else {
                Button(
                    onClick = onApply,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(6.dp),
                    enabled = !isApplying,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberAccent)
                ) {
                    if (isApplying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "APPLY STREAMING OPTIMIZATIONS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CyberBackground)
            .border(1.dp, CyberBorderSubtle, RoundedCornerShape(6.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = CyberAccent
            )
            Text(
                text = label,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                color = CyberTextMuted
            )
        }
    }
}

@Composable
private fun HeroRatingCard(report: NetworkOptimizationReport) {
    val gradeColor = when (report.stabilityGrade) {
        "A+", "A" -> CyberSuccess
        "B" -> CyberWarning
        else -> CyberDanger
    }

    CyberCard(
        backgroundColor = CyberCard,
        borderColor = gradeColor.copy(alpha = 0.5f),
        padding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CellTower,
                        contentDescription = "Carrier",
                        tint = CyberAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${report.carrierName} • ${report.networkType}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = report.stabilityDescription,
                    fontSize = 12.sp,
                    color = CyberTextPrimary,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(gradeColor.copy(alpha = 0.15f))
                    .border(2.dp, gradeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = report.stabilityGrade,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = gradeColor
                )
            }
        }
    }
}

@Composable
private fun LatencyMetricsCard(report: NetworkOptimizationReport) {
    CyberCard(
        backgroundColor = CyberCard,
        borderColor = CyberBorderSubtle,
        padding = 16.dp
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Latency",
                    tint = CyberAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CARRIER LATENCY & JITTER AUDIT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyberTextPrimary,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricItem(
                    label = "MIN PING",
                    value = "${report.minPingMs}ms",
                    color = CyberSuccess,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "AVG PING",
                    value = "${report.avgPingMs}ms",
                    color = if (report.avgPingMs < 200) CyberSuccess else CyberWarning,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "MAX PING",
                    value = "${report.maxPingMs}ms",
                    color = if (report.maxPingMs < 350) CyberWarning else CyberDanger,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "JITTER",
                    value = "${report.jitterMs}ms",
                    color = if (report.jitterMs < 40) CyberSuccess else CyberDanger,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CyberBackground)
            .border(1.dp, CyberBorderSubtle, RoundedCornerShape(6.dp))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = CyberTextMuted
            )
        }
    }
}

@Composable
private fun DpiJitterInspectionCard(report: NetworkOptimizationReport) {
    val dpiColor = if (report.isDpiDetected) CyberWarning else CyberSuccess

    CyberCard(
        backgroundColor = CyberCard,
        borderColor = if (report.isDpiDetected) CyberWarning.copy(alpha = 0.5f) else CyberBorderSubtle,
        padding = 16.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "DPI Audit",
                        tint = CyberAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DPI DELAY & JITTER AUDIT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextPrimary,
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(dpiColor.copy(alpha = 0.15f))
                        .border(1.dp, dpiColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (report.isDpiDetected) "DPI ACTIVE" else "DPI BYPASSED",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = dpiColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (report.isDpiDetected) {
                    "Carrier middlebox is intercepting initial TLS ClientHello packets, introducing +${report.dpiInspectionDelayMs}ms handshake stall. TLS packet fragmentation and TCP NoDelay bypass DPI inspection."
                } else {
                    "Minimal middlebox inspection delay observed (+${report.dpiInspectionDelayMs}ms). Packets pass with low carrier jitter."
                },
                fontSize = 11.sp,
                color = CyberTextSecondary,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricItem(
                    label = "DPI STALL",
                    value = "+${report.dpiInspectionDelayMs}ms",
                    color = if (report.dpiInspectionDelayMs < 60) CyberSuccess else CyberWarning,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "JITTER",
                    value = "±${report.jitterMs}ms",
                    color = if (report.jitterMs < 40) CyberSuccess else CyberDanger,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "ANTI-DPI TLS",
                    value = "100-200B",
                    color = CyberAccent,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "TCP NODELAY",
                    value = "ACTIVE",
                    color = CyberSuccess,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DnsRaceCard(report: NetworkOptimizationReport) {
    CyberCard(
        backgroundColor = CyberCard,
        borderColor = CyberBorderSubtle,
        padding = 16.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = "DNS",
                        tint = CyberAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DNS RESOLVER SPEED RACE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextPrimary,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "WINNER: ${report.optimalDnsProvider}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyberSuccess
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                report.dnsResults.forEach { dns ->
                    val isFastest = dns.serverIp == report.optimalDns
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isFastest) CyberCardElevated else CyberBackground)
                            .border(
                                1.dp,
                                if (isFastest) CyberSuccess.copy(alpha = 0.5f) else CyberBorderSubtle,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = dns.providerName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFastest) CyberTextPrimary else CyberTextSecondary
                            )
                            Text(
                                text = dns.serverIp,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextMuted
                            )
                        }

                        Text(
                            text = if (dns.isReachable) "${dns.latencyMs}ms" else "UNREACHABLE",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (!dns.isReachable) CyberDanger else if (isFastest) CyberSuccess else CyberTextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendedTuningCard(report: NetworkOptimizationReport) {
    CyberCard(
        backgroundColor = CyberCard,
        borderColor = CyberBorderSubtle,
        padding = 16.dp
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = "Tuning",
                    tint = CyberAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RECOMMENDED V2RAY PROFILE PARAMETERS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyberTextPrimary,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TuningRow(
                    label = "Optimal MTU (Anti-Fragmentation)",
                    value = "${report.optimalMtu} Bytes",
                    detail = if (report.optimalMtu <= 1400) "Clamped for LTE carrier encapsulation" else "Standard Ethernet MTU"
                )
                HorizontalDivider(color = CyberBorderSubtle, thickness = 1.dp)

                TuningRow(
                    label = "TCP Window Buffer (Anti-Bufferbloat)",
                    value = "${report.optimalBufferSizeKb} KB",
                    detail = "Optimized to prevent bufferbloat packet drops"
                )
                HorizontalDivider(color = CyberBorderSubtle, thickness = 1.dp)

                TuningRow(
                    label = "DNS Resolver Forwarding",
                    value = "${report.optimalDns} (${report.optimalDnsProvider})",
                    detail = "Fastest query resolution on ${report.carrierName}"
                )
                HorizontalDivider(color = CyberBorderSubtle, thickness = 1.dp)

                TuningRow(
                    label = "Smart Adaptive QoS Engine",
                    value = "ENABLED",
                    detail = "Dynamic token-bucket rate smoothing"
                )
                HorizontalDivider(color = CyberBorderSubtle, thickness = 1.dp)

                TuningRow(
                    label = "Anti-DPI Packet Fragmentation",
                    value = "ENABLED (100-200B / 10-20ms)",
                    detail = "Splits TLS ClientHello across frames to defeat DPI middlebox stalls"
                )
                HorizontalDivider(color = CyberBorderSubtle, thickness = 1.dp)

                TuningRow(
                    label = "TCP NoDelay (Nagle Bypass)",
                    value = "ENABLED",
                    detail = "Disables delayed-ACK buffering to eliminate carrier jitter"
                )
            }
        }
    }
}

@Composable
private fun TuningRow(
    label: String,
    value: String,
    detail: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = CyberTextPrimary
            )
            Text(
                text = detail,
                fontSize = 10.sp,
                color = CyberTextMuted
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = CyberAccent
        )
    }
}
