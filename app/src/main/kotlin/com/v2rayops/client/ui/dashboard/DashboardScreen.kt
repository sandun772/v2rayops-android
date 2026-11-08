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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.v2rayops.client.network.DiagnosticStatus
import com.v2rayops.client.ui.dashboard.components.ActiveProfileCard
import com.v2rayops.client.ui.dashboard.components.ClientUsageWidget
import com.v2rayops.client.ui.dashboard.components.CyberBottomNavBar
import com.v2rayops.client.ui.dashboard.components.CyberConnectButtonArea
import com.v2rayops.client.ui.dashboard.components.CyberTopBar

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
    val configs by viewModel.configs.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val diagnosticResult by viewModel.diagnosticResult.collectAsState()

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
    // Only flag unstable if connected and latency is genuinely excessive (>450ms) or test outright failed
    val isUnstable = isConnected && (diagnosticResult?.status == DiagnosticStatus.FAILED || ((diagnosticResult?.pingMs ?: 0L) > 450L))
    val pingMs = if (isConnected) (diagnosticResult?.pingMs?.coerceAtLeast(0L) ?: 0L) else 0L

    Scaffold(
        topBar = {
            CyberTopBar(
                connectionState = connectionState,
                onNavigateToSettings = onNavigateToSettings
            )
        },
        bottomBar = {
            CyberBottomNavBar(
                connectionState = connectionState,
                onNavigateToProfiles = onNavigateToServers,
                onNavigateToHome = { /* Already on Home */ },
                onNavigateToLogs = onNavigateToLogs
            )
        },
        containerColor = Color(0xFF04060A) // 100% Solid Deep Cosmic Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Active Profile Selector Card (01 // ACTIVE PROFILE)
            ActiveProfileCard(
                selectedConfig = selectedConfig,
                configs = configs,
                onSelectConfig = { config ->
                    viewModel.selectConfig(config)
                }
            )

            // 2. Centerpiece: Circular Dot Matrix Aura + Precision Cyber Connect Orb
            CyberConnectButtonArea(
                connectionState = connectionState,
                isUnstable = isUnstable,
                onClick = handleConnectClick
            )

            // 3. Client Usage & Telemetry Widget (02 // USAGE & TELEMETRY)
            ClientUsageWidget(
                connectionState = connectionState,
                trafficStats = trafficStats,
                selectedConfig = selectedConfig,
                userProfile = userProfile,
                pingMs = pingMs
            )

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
