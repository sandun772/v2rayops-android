package com.v2rayops.client.ui.dashboard

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.v2rayops.client.designsystem.components.CyberCard
import com.v2rayops.client.designsystem.components.CyberTopAppBar
import com.v2rayops.client.designsystem.components.StatusBadge
import com.v2rayops.client.designsystem.components.StatusIndicatorType
import com.v2rayops.client.designsystem.theme.CyberAccent
import com.v2rayops.client.designsystem.theme.CyberBackground
import com.v2rayops.client.designsystem.theme.CyberBorder
import com.v2rayops.client.designsystem.theme.CyberCard
import com.v2rayops.client.designsystem.theme.CyberDanger
import com.v2rayops.client.designsystem.theme.CyberTextMuted
import com.v2rayops.client.designsystem.theme.CyberTextPrimary
import com.v2rayops.client.designsystem.theme.CyberWarning
import com.v2rayops.client.domain.model.ConnectionState
import com.v2rayops.client.ui.dashboard.components.ActiveServerCard
import com.v2rayops.client.ui.dashboard.components.ConnectionButton
import com.v2rayops.client.ui.dashboard.components.ConnectionDiagnosticsCard
import com.v2rayops.client.ui.dashboard.components.StatsCard

@Composable
fun DashboardScreen(
    onNavigateToServers: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()
    val trafficStats by viewModel.trafficStats.collectAsState()
    val selectedConfig by viewModel.selectedConfig.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val diagnosticResult by viewModel.diagnosticResult.collectAsState()
    val isTestingConnection by viewModel.isTestingConnection.collectAsState()

    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onConnectToggle()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top App Bar with Official Site Logo
            CyberTopAppBar(
                title = "V2RAYOPS",
                logoRes = com.v2rayops.client.R.drawable.v2rayops_logo,
                actions = {
                    IconButton(onClick = { viewModel.refreshDashboard() }, enabled = !isRefreshing) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = CyberTextPrimary
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = CyberTextPrimary
                        )
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Connection Status Pill
                StatusBadge(
                    text = when (connectionState) {
                        is ConnectionState.Connected -> "TUNNEL ACTIVE"
                        is ConnectionState.Connecting -> "ESTABLISHING"
                        is ConnectionState.Disconnecting -> "CLOSING"
                        is ConnectionState.Error -> "FAILED"
                        is ConnectionState.Unavailable -> "ENGINE PENDING"
                        else -> "DISCONNECTED"
                    },
                    status = when (connectionState) {
                        is ConnectionState.Connected -> StatusIndicatorType.CONNECTED
                        is ConnectionState.Connecting -> StatusIndicatorType.CONNECTING
                        is ConnectionState.Error -> StatusIndicatorType.ERROR
                        is ConnectionState.Unavailable -> StatusIndicatorType.WARNING
                        else -> StatusIndicatorType.DISCONNECTED
                    }
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Main Connection Button
                ConnectionButton(
                    connectionState = connectionState,
                    onClick = {
                        val state = connectionState
                        if (state.isConnected || state.isConnecting) {
                            viewModel.onConnectToggle()
                        } else {
                            val prepareIntent = VpnService.prepare(context)
                            if (prepareIntent != null) {
                                vpnPrepareLauncher.launch(prepareIntent)
                            } else {
                                viewModel.onConnectToggle()
                            }
                        }
                    }
                )

                // Connection Error Banner
                if (connectionState is ConnectionState.Error) {
                    val errorMessage = (connectionState as ConnectionState.Error).message
                    Spacer(modifier = Modifier.height(20.dp))
                    CyberCard(
                        backgroundColor = CyberCard,
                        borderColor = CyberDanger.copy(alpha = 0.6f),
                        padding = 14.dp
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Error",
                                    tint = CyberDanger,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                                Text(
                                    text = "Connection Failed",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberDanger
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errorMessage,
                                fontSize = 12.sp,
                                color = CyberTextPrimary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Honest Architectural Banner if Engine is pending
                if (connectionState is ConnectionState.Unavailable) {
                    val reason = (connectionState as ConnectionState.Unavailable).reason
                    Spacer(modifier = Modifier.height(20.dp))
                    CyberCard(
                        backgroundColor = CyberCard,
                        borderColor = CyberWarning.copy(alpha = 0.5f),
                        padding = 12.dp
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Notice",
                                tint = CyberWarning,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                            Text(
                                text = reason,
                                fontSize = 12.sp,
                                color = CyberTextPrimary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Active Client Config Card
                ActiveServerCard(
                    config = selectedConfig,
                    onClick = onNavigateToServers
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Connection Diagnostics & Real IP Verification Card
                ConnectionDiagnosticsCard(
                    result = diagnosticResult,
                    isTesting = isTestingConnection,
                    isVpnConnected = connectionState.isConnected,
                    onRunTest = { viewModel.runConnectionTest() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Live Stats & Balance Card
                StatsCard(
                    stats = trafficStats,
                    profile = profile
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
