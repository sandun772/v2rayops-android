package com.v2rayops.client.ui.dashboard

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    var speedMode by remember { mutableStateOf(SpeedOptimizationMode.BALANCED) }
    var showUsageCockpit by remember { mutableStateOf(false) }

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
    val pingMs = if (isConnected) (diagnosticResult?.pingMs?.coerceAtLeast(0L) ?: 48L) else 48L

    Scaffold(
        topBar = {
            V2RayOpsTopBar(coreVersion = "Xray-core 25.3")
        },
        bottomBar = {
            ClientBottomNavBar(
                selectedTab = ClientNavTab.CONNECT,
                onTabSelected = { tab ->
                    when (tab) {
                        ClientNavTab.CONNECT -> { /* Already on Connect */ }
                        ClientNavTab.FINETUNE -> onNavigateToServers()
                        ClientNavTab.USAGE -> showUsageCockpit = true
                        ClientNavTab.TOOLS -> onNavigateToRouteFinder()
                        ClientNavTab.SETTINGS -> onNavigateToSettings()
                    }
                }
            )
        },
        containerColor = Color(0xFF050811) // Exact Deep Cosmic Background from mockups
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Status Capsule Pill (• DISCONNECTED / • HANDSHAKING / • CONNECTED)
            StatusPill(connectionState = connectionState)

            // 2. Big Digital Clock (00:00:00 or 01:24:37)
            DigitalTimerText(connectionState = connectionState)

            // 3. Center Concentric Dot Radar Dial (Smooth progressive wave entrance, subtle glassy power button)
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
                pingMs = pingMs,
                onClick = onNavigateToServers
            )

            // 6. SPEED & OPTIMIZATION MODE Segmented Control (Balanced / Speed / Stability)
            ProxyModeSelector(
                selectedMode = speedMode,
                onModeSelected = { speedMode = it }
            )

            Spacer(modifier = Modifier.height(6.dp))
        }

        // Usage & Cockpit Telemetry Modal (matching web dashboard widgets)
        if (showUsageCockpit) {
            UsageCockpitDialog(
                connectionState = connectionState,
                trafficStats = trafficStats,
                selectedConfig = selectedConfig,
                pingMs = pingMs,
                onDismiss = { showUsageCockpit = false }
            )
        }
    }
}
