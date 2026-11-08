package com.v2rayops.client.ui.dashboard

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.v2rayops.client.domain.model.ConnectionState
import com.v2rayops.client.ui.dashboard.components.ActiveNodeSummaryCard
import com.v2rayops.client.ui.dashboard.components.ClientBottomNavBar
import com.v2rayops.client.ui.dashboard.components.ClientNavTab
import com.v2rayops.client.ui.dashboard.components.ConcentricRadarDial
import com.v2rayops.client.ui.dashboard.components.DigitalTimerText
import com.v2rayops.client.ui.dashboard.components.ProxyModeSelector
import com.v2rayops.client.ui.dashboard.components.SpeedDividedSection
import com.v2rayops.client.ui.dashboard.components.SpeedOptimizationMode
import com.v2rayops.client.ui.dashboard.components.StatusPill
import com.v2rayops.client.ui.dashboard.components.UsageCockpitDialog
import com.v2rayops.client.ui.dashboard.components.V2RayOpsTopBar

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

    var speedMode by remember { mutableStateOf(SpeedOptimizationMode.BALANCED) }
    var showUsageCockpit by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

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

    val isConnected = connectionState.isConnected
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

                // 1. Status Capsule Pill (• DISCONNECTED / • HANDSHAKING / • CONNECTED)
                StatusPill(connectionState = connectionState)

                // 2. Big Digital Clock (00:00:00 or 01:24:37)
                DigitalTimerText(connectionState = connectionState)

                // 3. Center Concentric Dot Radar Dial
                ConcentricRadarDial(
                    connectionState = connectionState,
                    onClick = handleConnectClick
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
                    selectedMode = speedMode,
                    onModeSelected = { speedMode = it }
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
    }
}
