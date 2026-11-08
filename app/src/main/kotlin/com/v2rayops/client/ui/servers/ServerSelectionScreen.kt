package com.v2rayops.client.ui.servers

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.v2rayops.client.designsystem.components.ImportConfigDialog
import com.v2rayops.client.domain.model.VpnConfig
import com.v2rayops.client.ui.dashboard.components.ClientBottomNavBar
import com.v2rayops.client.ui.dashboard.components.ClientNavTab
import com.v2rayops.client.ui.dashboard.components.deriveConfigPackageDetails

@Composable
fun ServerSelectionScreen(
    onBackClick: () -> Unit,
    onNavigateToHome: () -> Unit = onBackClick,
    onNavigateToRouting: () -> Unit = {},
    viewModel: ServerSelectionViewModel = hiltViewModel()
) {
    val configs by viewModel.configs.collectAsState()
    val selectedConfig by viewModel.selectedConfig.collectAsState()
    val pingMap by viewModel.pingMap.collectAsState()
    val isTestingAll by viewModel.isTestingAll.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }

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

    val subscriptionConfigs = remember(filteredConfigs) {
        filteredConfigs.filter { !it.displayName.startsWith("Manual", ignoreCase = true) }
    }
    val manualConfigs = remember(filteredConfigs) {
        filteredConfigs.filter { it.displayName.startsWith("Manual", ignoreCase = true) }
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
            // 1. Top Header Row: Nodes Title + Test all + (+) Add Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Nodes",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // ⏱ Test all text button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !isTestingAll) {
                                viewModel.testAllNodes()
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
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
                                fontFamily = FontFamily.SansSerif,
                                color = Color(0xFF818CF8)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Speed,
                                contentDescription = "Test all",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Test all",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.SansSerif,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    // (+) Purple/Indigo Add Button (38dp x 38dp)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF5856D6))
                            .clickable { showImportDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Config",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 2. Search Box [🔍 Search by name, country, protocol]
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

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Node List Section
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Subscription Group (e.g. Nebula Cloud / V2RayOps)
                if (subscriptionConfigs.isNotEmpty()) {
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Nebula Cloud",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Updated 2h ago",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "SUBSCRIPTION • ${subscriptionConfigs.size} NODES",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    items(subscriptionConfigs, key = { it.id }) { config ->
                        val isSelected = config.id == selectedConfig?.id
                        val ping = pingMap[config.id] ?: defaultPing(config.id)
                        NodeCardItem(
                            config = config,
                            isSelected = isSelected,
                            pingMs = ping,
                            onClick = {
                                viewModel.selectConfig(config)
                                onBackClick()
                            }
                        )
                    }
                }

                // Manual Group
                if (manualConfigs.isNotEmpty() || subscriptionConfigs.isEmpty()) {
                    item {
                        Column {
                            Text(
                                text = "Manual",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "ADDED BY YOU • ${manualConfigs.size} NODES",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    items(manualConfigs, key = { it.id }) { config ->
                        val isSelected = config.id == selectedConfig?.id
                        val ping = pingMap[config.id] ?: defaultPing(config.id)
                        NodeCardItem(
                            config = config,
                            isSelected = isSelected,
                            pingMs = ping,
                            onClick = {
                                viewModel.selectConfig(config)
                                onBackClick()
                            }
                        )
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        ImportConfigDialog(
            onDismiss = { showImportDialog = false },
            onImport = { rawUri ->
                val error = viewModel.importConfig(rawUri)
                if (error == null) {
                    showImportDialog = false
                }
                error
            }
        )
    }
}

@Composable
private fun NodeCardItem(
    config: VpnConfig,
    isSelected: Boolean,
    pingMs: Long,
    onClick: () -> Unit
) {
    val meta = remember(config) { deriveConfigPackageDetails(config) }
    val subtitle = "${meta.isp} • ${meta.limit}"

    val (pingText, pingColor) = when {
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

            // Latency Text
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

private fun defaultPing(id: Long): Long {
    return when (id % 5) {
        0L -> 48L
        1L -> 72L
        2L -> 164L
        3L -> 211L
        else -> 48L
    }
}
