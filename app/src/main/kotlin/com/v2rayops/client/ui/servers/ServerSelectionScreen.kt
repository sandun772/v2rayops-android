package com.v2rayops.client.ui.servers

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.v2rayops.client.domain.model.VpnConfig
import com.v2rayops.client.ui.dashboard.components.ClientBottomNavBar
import com.v2rayops.client.ui.dashboard.components.ClientNavTab
import com.v2rayops.client.ui.dashboard.components.deriveConfigPackageDetails
import com.v2rayops.client.ui.servers.components.OneClickFineTuneCard

@Composable
fun ServerSelectionScreen(
    onBackClick: () -> Unit,
    onNavigateToHome: () -> Unit = onBackClick,
    onNavigateToRouting: () -> Unit = {},
    viewModel: ServerSelectionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val configs by viewModel.configs.collectAsState()
    val selectedConfig by viewModel.selectedConfig.collectAsState()
    val pingMap by viewModel.pingMap.collectAsState()
    val isTestingAll by viewModel.isTestingAll.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val fineTuneState by viewModel.fineTuneState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

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

    val filteredConfigs = remember(configs, searchQuery) {
        if (searchQuery.isBlank()) configs
        else {
            val q = searchQuery.lowercase()
            configs.filter {
                it.displayName.lowercase().contains(q) ||
                it.serverCountry.lowercase().contains(q) ||
                it.protocol.lowercase().contains(q) ||
                it.host.lowercase().contains(q)
            }
        }
    }

    Scaffold(
        bottomBar = {
            ClientBottomNavBar(
                selectedTab = ClientNavTab.FINETUNE,
                connectionState = connectionState,
                onTabSelected = { tab ->
                    when (tab) {
                        ClientNavTab.CONNECT -> onNavigateToHome()
                        ClientNavTab.FINETUNE -> { /* Already on Finetune */ }
                        ClientNavTab.TOOLS -> onNavigateToRouting()
                        ClientNavTab.USAGE -> { /* Handled on Dashboard */ }
                        ClientNavTab.SETTINGS -> onBackClick()
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
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // 1. Top Header Row: "Active Configs" Title + Functional "Test all" Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Active Configs",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // ⏱ Functional Live Edge TCP Socket Test All Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0C101E))
                        .border(1.dp, Color(0xFF1E2842), RoundedCornerShape(8.dp))
                        .clickable(enabled = !isTestingAll) {
                            viewModel.testAllNodes()
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isTestingAll) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(13.dp),
                            strokeWidth = 1.5.dp,
                            color = Color(0xFF818CF8)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Testing...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif,
                            color = Color(0xFF818CF8)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Speed,
                            contentDescription = "Test all",
                            tint = Color(0xFFA5B4FC),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Test all",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif,
                            color = Color(0xFFA5B4FC)
                        )
                    }
                }
            }

            // 2. Scrollable Body: One-Click Auto Fine Tune Card + Search + Assigned Configurations
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Hero: One-Click Full Tuning Auto Card
                item {
                    OneClickFineTuneCard(
                        state = fineTuneState,
                        selectedConfig = selectedConfig,
                        onStartFineTune = handleStartFineTune,
                        onStopFineTune = { viewModel.stopOneClickFineTune() },
                        onDismissReport = { viewModel.dismissFineTuneReport() }
                    )
                }

                // Search Box [🔍 Search by name, country, protocol]
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0C101E))
                            .border(1.dp, Color(0xFF151E34), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.SansSerif
                            ),
                            cursorBrush = SolidColor(Color(0xFF818CF8)),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search by name, country, protocol",
                                        color = Color(0xFF64748B),
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                // Node List Section: Assigned Configurations Header
                if (filteredConfigs.isNotEmpty()) {
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Assigned Configurations",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Live Network",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "V2RAYOPS NETWORK • ${filteredConfigs.size} PROFILES",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    items(filteredConfigs, key = { it.id }) { config ->
                        val isSelected = config.id == selectedConfig?.id
                        val realPing = pingMap[config.id]
                        NodeCardItem(
                            config = config,
                            isSelected = isSelected,
                            pingMs = realPing,
                            onClick = {
                                viewModel.selectConfig(config)
                                onBackClick()
                            }
                        )
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No active configurations found",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NodeCardItem(
    config: VpnConfig,
    isSelected: Boolean,
    pingMs: Long?,
    onClick: () -> Unit
) {
    val meta = remember(config) { deriveConfigPackageDetails(config) }
    val subtitle = "${meta.isp} • ${meta.limit}"

    val (pingText, pingColor) = when {
        pingMs == null -> "-- ms" to Color(0xFF64748B)
        pingMs <= 0L -> "timeout" to Color(0xFFF43F5E)
        pingMs < 100L -> "$pingMs ms" to Color(0xFF22C55E)
        pingMs < 200L -> "$pingMs ms" to Color(0xFFF59E0B)
        else -> "$pingMs ms" to Color(0xFFF97316)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF12162B) else Color(0xFF0B0F1C))
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFF5856D6) else Color(0xFF141C30),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Authentic Brand Package Icon (Real Zoom, TikTok, YouTube, Gaming, etc.)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF141C30))
                    .border(1.dp, Color(0xFF222F4C), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = meta.drawableRes),
                    contentDescription = meta.isp,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Center details: Title + ISP & Limit Subtitle
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = config.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Real measured Latency Text
            Text(
                text = pingText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = pingColor
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Radio Indicator (Solid Purple Checkmark if Selected, Empty Ring if Not)
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF5856D6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color(0xFF26334F), CircleShape)
                )
            }
        }
    }
}
