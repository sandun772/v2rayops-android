package com.v2rayops.client.ui.dashboard

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.v2rayops.client.R
import com.v2rayops.client.designsystem.components.CyberDotMatrixBackground
import com.v2rayops.client.designsystem.theme.CyberAccent
import com.v2rayops.client.designsystem.theme.CyberBorder
import com.v2rayops.client.designsystem.theme.CyberPrimary
import com.v2rayops.client.designsystem.theme.CyberTextMuted
import com.v2rayops.client.designsystem.theme.CyberTextPrimary
import com.v2rayops.client.network.DiagnosticStatus
import com.v2rayops.client.ui.dashboard.components.ActiveConfigButton
import com.v2rayops.client.ui.dashboard.components.GlassyPowerButton
import com.v2rayops.client.ui.dashboard.components.TopStatsRibbon

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

    val isUnstable = diagnosticResult?.status == DiagnosticStatus.FAILED || ((diagnosticResult?.pingMs ?: 0L) > 300L)
    val pingMs = diagnosticResult?.pingMs ?: 0L

    CyberDotMatrixBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Sleek Top Header (Brand Logo + Title + Settings Icon)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.v2rayops_logo_trimmed),
                        contentDescription = "V2RayOps",
                        modifier = Modifier.size(36.dp, 22.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Ray",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = CyberTextPrimary
                    )
                    Text(
                        text = "OPS",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = CyberPrimary
                    )
                }

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0C101B))
                        .border(1.dp, CyberBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = CyberTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 2. Top Little Metric Ribbon
            TopStatsRibbon(
                connectionState = connectionState,
                trafficStats = trafficStats,
                pingMs = pingMs
            )

            // Flexible space centering the power button
            Spacer(modifier = Modifier.weight(1f))

            // 3. Centerpiece: Big Glassy Power Button with multi-state glow
            GlassyPowerButton(
                connectionState = connectionState,
                isUnstable = isUnstable,
                onClick = handleConnectClick
            )

            Spacer(modifier = Modifier.weight(1.2f))

            // 4. Active Config Selector Button (ISP Name + Package Icon + Switch sheet)
            ActiveConfigButton(
                selectedConfig = selectedConfig,
                configs = configs,
                onSelectConfig = { config ->
                    viewModel.selectConfig(config)
                },
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}
