package com.v2rayops.client.ui.dashboard

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.v2rayops.client.domain.model.ConnectionState
import com.v2rayops.client.ui.dashboard.components.ActiveNodeSummaryCard
import com.v2rayops.client.ui.dashboard.components.ClientBottomNavBar
import com.v2rayops.client.ui.dashboard.components.ClientNavTab
import com.v2rayops.client.ui.dashboard.components.ConcentricRadarDial
import com.v2rayops.client.ui.dashboard.components.DigitalTimerText
import com.v2rayops.client.ui.dashboard.components.HiddenSpeedStabilityWidget
import com.v2rayops.client.ui.dashboard.components.IpHuntCockpitDialog
import com.v2rayops.client.ui.dashboard.components.ProxyModeSelector
import com.v2rayops.client.ui.dashboard.components.PullDownChargingPill
import com.v2rayops.client.ui.dashboard.components.SpeedDividedSection
import com.v2rayops.client.ui.dashboard.components.SpeedOptimizationMode
import com.v2rayops.client.ui.dashboard.components.StatusPill
import com.v2rayops.client.ui.dashboard.components.UsageCockpitDialog
import com.v2rayops.client.ui.dashboard.components.V2RayOpsTopBar
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    onNavigateToServers: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToOptimizer: () -> Unit,
    onNavigateToRouteFinder: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()
    val trafficStats by viewModel.trafficStats.collectAsState()
    val selectedConfig by viewModel.selectedConfig.collectAsState()
    val diagnosticResult by viewModel.diagnosticResult.collectAsState()
    val livePingMs by viewModel.livePingMs.collectAsState()
    val quickAuditState by viewModel.quickAuditState.collectAsState()
    val haptic = LocalHapticFeedback.current

    val currentMode = remember(selectedConfig) {
        when (selectedConfig?.tuningMode?.uppercase()) {
            "SPEED" -> SpeedOptimizationMode.SPEED
            "STABILITY" -> SpeedOptimizationMode.STABILITY
            else -> SpeedOptimizationMode.BALANCED
        }
    }
    var showUsageCockpit by remember { mutableStateOf(false) }
    val ipHuntState by viewModel.ipHuntState.collectAsState()
    var showIpHuntDialog by remember { mutableStateOf(false) }

    var pullDistance by remember { mutableFloatStateOf(0f) }
    var isCharging by remember { mutableStateOf(false) }
    var chargeProgress by remember { mutableFloatStateOf(0f) }
    var isHiddenWidgetVisible by remember { mutableStateOf(false) }
    var isRetestMode by remember { mutableStateOf(false) }
    var isTriggerLockedUntilRelease by remember { mutableStateOf(false) }

    val animatedPullOffset by animateFloatAsState(
        targetValue = pullDistance,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pullSpring"
    )

    val scrollState = rememberScrollState()

    // Auto-hide the speed & stability HUD whenever user scrolls down
    LaunchedEffect(scrollState.value) {
        if (scrollState.value > 15 && isHiddenWidgetVisible) {
            isHiddenWidgetVisible = false
        }
    }

    // Auto-hide the speed & stability HUD after 15s cooldown once audit completes
    LaunchedEffect(quickAuditState.isCompleted, isHiddenWidgetVisible) {
        if (quickAuditState.isCompleted && isHiddenWidgetVisible) {
            delay(15000L) // 15-second cooldown
            if (isHiddenWidgetVisible) {
                isHiddenWidgetVisible = false
            }
        }
    }

    // Hold countdown effect: 3s for initial trigger, 1s for retest
    LaunchedEffect(isCharging) {
        if (isCharging) {
            val durationMs = if (isRetestMode) 1000L else 3000L
            val startTime = System.currentTimeMillis()
            while (isCharging) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                chargeProgress = progress
                if (progress >= 1f) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isHiddenWidgetVisible = true
                    isCharging = false
                    pullDistance = 0f
                    chargeProgress = 0f
                    isTriggerLockedUntilRelease = true
                    viewModel.startQuickAudit()
                    break
                }
                delay(16)
            }
        } else {
            chargeProgress = 0f
        }
    }

    // Show top gradient ONLY when scrolled, smoothly ramping up without instant jump on 1px
    val topGradientAlpha by remember {
        derivedStateOf {
            val startThreshold = 20f
            val fullThreshold = 85f
            ((scrollState.value.toFloat() - startThreshold) / (fullThreshold - startThreshold)).coerceIn(0f, 1f)
        }
    }
    val animatedTopGradientAlpha by animateFloatAsState(
        targetValue = topGradientAlpha,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "TopGradientAlpha"
    )

    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onConnectToggle()
        }
    }

    val handleConnectClick = {
        val prepareIntent = VpnService.prepare(context)
        if (prepareIntent != null) {
            vpnPrepareLauncher.launch(prepareIntent)
        } else {
            viewModel.onConnectToggle()
        }
    }

    val realPingMs = livePingMs ?: diagnosticResult?.pingMs?.takeIf { it > 0 }

    Scaffold(
        topBar = {
            V2RayOpsTopBar(coreVersion = "Xray-core 25.3")
        },
        bottomBar = {
            ClientBottomNavBar(
                selectedTab = ClientNavTab.CONNECT,
                connectionState = connectionState,
                onTabSelected = { tab ->
                    when (tab) {
                        ClientNavTab.CONNECT -> {
                            // User is already on home: pressing Connect again connects or disconnects VPN!
                            handleConnectClick()
                        }
                        ClientNavTab.FINETUNE -> onNavigateToOptimizer()
                        ClientNavTab.USAGE -> showUsageCockpit = true
                        ClientNavTab.TOOLS -> onNavigateToRouteFinder()
                        ClientNavTab.SETTINGS -> onNavigateToSettings()
                    }
                }
            )
        },
        containerColor = Color(0xFF050811) // Exact Deep Cosmic Background from mockups
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(quickAuditState.isRunning) {
                    if (quickAuditState.isRunning) return@pointerInput

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        // Disallow pull if page is scrolled down
                        if (scrollState.value > 8) return@awaitEachGesture

                        isRetestMode = isHiddenWidgetVisible
                        val startY = down.position.y
                        var armed = false

                        try {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val pointer = event.changes.find { it.id == down.id }
                                if (pointer == null || !pointer.pressed) {
                                    break
                                }

                                // If test was already fired during this hold, wait for finger to be lifted!
                                if (isTriggerLockedUntilRelease) {
                                    pullDistance = 0f
                                    continue
                                }

                                val deltaY = pointer.position.y - startY
                                if (deltaY > 0 && scrollState.value <= 8) {
                                    // Smooth responsive pull damping (natural 20px swipe activates)
                                    val damped = (deltaY * 0.5f).coerceIn(0f, 60f)
                                    pullDistance = damped
                                    if (damped >= 10f && !armed) {
                                        armed = true
                                        isCharging = true
                                    } else if (damped < 4f && armed) {
                                        armed = false
                                        isCharging = false
                                    }
                                } else if (deltaY < -30f) {
                                    // Intentional upward scroll
                                    break
                                }
                            }
                        } finally {
                            armed = false
                            isCharging = false
                            isTriggerLockedUntilRelease = false
                            pullDistance = 0f
                            chargeProgress = 0f
                        }
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(modifier = Modifier.height(2.dp))

                // Pull-Down Charging Pill (Initial trigger: 3s hold, Retest: 1s hold)
                PullDownChargingPill(
                    pullOffset = animatedPullOffset,
                    chargeProgress = chargeProgress,
                    isRetest = isRetestMode
                )

                // 3-Card Speed & Stability HUD (DOWNLOAD | UPLOAD | STABILITY)
                AnimatedVisibility(
                    visible = isHiddenWidgetVisible,
                    enter = expandVertically(tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(180)),
                    exit = shrinkVertically(tween(200, easing = FastOutSlowInEasing)) + fadeOut(tween(150))
                ) {
                    HiddenSpeedStabilityWidget(
                        state = quickAuditState,
                        onDismiss = {
                            isHiddenWidgetVisible = false
                            viewModel.dismissQuickAudit()
                        }
                    )
                }

                // 1. Status Capsule Pill (• DISCONNECTED / • HANDSHAKING / • CONNECTED)
                StatusPill(connectionState = connectionState)

                // 2. Big Digital Clock (00:00:00 or 01:24:37)
                DigitalTimerText(connectionState = connectionState)

                // 3. Center Concentric Dot Radar Dial (Single tap: normal connect, Hold: IP Hunter)
                ConcentricRadarDial(
                    connectionState = connectionState,
                    onClick = handleConnectClick,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showIpHuntDialog = true
                    }
                )

                // 4. Download / Upload Speed Section with Dividing Lines
                SpeedDividedSection(
                    connectionState = connectionState,
                    trafficStats = trafficStats
                )

                // 5. Active Node Summary Card (Real brand icons & ISP / Limit display)
                ActiveNodeSummaryCard(
                    config = selectedConfig,
                    pingMs = realPingMs,
                    onClick = onNavigateToServers
                )

                // 6. SPEED & OPTIMIZATION MODE Segmented Control (Balanced / Speed / Stability)
                ProxyModeSelector(
                    selectedMode = currentMode,
                    onModeSelected = { mode ->
                        viewModel.setSpeedOptimizationMode(mode)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Top Gradient Scrim: Appears smoothly only when scrolled past threshold (no instant flash on 1px)
            if (animatedTopGradientAlpha > 0.005f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .align(Alignment.TopCenter)
                        .graphicsLayer { alpha = animatedTopGradientAlpha }
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF050811),
                                    Color(0xFA050811),
                                    Color(0xCC050811),
                                    Color(0x77050811),
                                    Color(0x22050811),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // Bottom Gradient Scrim: Blends into the bottom bar smoothly
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xAA050811),
                                Color(0xFF050811)
                            )
                        )
                    )
            )
        }

        // Usage & Cockpit Telemetry Modal (matching web dashboard widgets)
        if (showUsageCockpit) {
            UsageCockpitDialog(
                connectionState = connectionState,
                trafficStats = trafficStats,
                selectedConfig = selectedConfig,
                pingMs = realPingMs,
                onDismiss = { showUsageCockpit = false }
            )
        }

        // IP Hunt Mode Cockpit (Triggered on Press & Hold of Center Connect Dial)
        if (showIpHuntDialog) {
            IpHuntCockpitDialog(
                state = ipHuntState,
                onStartHunt = { target ->
                    viewModel.startIpHunt(target) { matchedIp ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showIpHuntDialog = false
                        handleConnectClick()
                    }
                },
                onStopHunt = { viewModel.stopIpHunt() },
                onSetTargetPrefix = { viewModel.setIpHuntTargetPrefix(it) },
                onOpenAccessibilitySettings = { viewModel.openAccessibilitySettings(context) },
                onOpenAppPermissionSettings = { viewModel.openAppPermissionSettings(context) },
                onOpenAirplaneSettings = { viewModel.openAirplaneSettings(context) },
                onRefreshCapabilities = { viewModel.refreshIpHuntCapabilities() },
                onConnectNow = {
                    showIpHuntDialog = false
                    handleConnectClick()
                },
                onDismiss = {
                    viewModel.stopIpHunt()
                    showIpHuntDialog = false
                }
            )
        }
    }
}
