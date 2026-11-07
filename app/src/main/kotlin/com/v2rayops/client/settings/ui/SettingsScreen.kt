package com.v2rayops.client.settings.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.draw.clip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.v2rayops.client.designsystem.components.CyberButton
import com.v2rayops.client.designsystem.components.CyberButtonVariant
import com.v2rayops.client.designsystem.components.CyberCard
import com.v2rayops.client.designsystem.components.CyberTopAppBar
import com.v2rayops.client.designsystem.theme.CyberAccent
import com.v2rayops.client.designsystem.theme.CyberBackground
import com.v2rayops.client.designsystem.theme.CyberBorder
import com.v2rayops.client.designsystem.theme.CyberCard
import com.v2rayops.client.designsystem.theme.CyberDanger
import com.v2rayops.client.designsystem.theme.CyberPrimary
import com.v2rayops.client.designsystem.theme.CyberTextMuted
import com.v2rayops.client.designsystem.theme.CyberTextPrimary
import com.v2rayops.client.designsystem.theme.CyberTextSecondary
import com.v2rayops.client.domain.model.RoutingMode
import com.v2rayops.client.settings.SettingsViewModel

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onLogoutSuccess: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToOptimizer: () -> Unit,
    onNavigateToRouteFinder: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CyberTopAppBar(
                title = "SYSTEM CONFIGURATION",
                onBackClick = onBackClick
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                SectionHeader(title = "SECURITY & TUNNELING")

                CyberCard(padding = 16.dp) {
                    SettingToggleItem(
                        icon = Icons.Default.Shield,
                        title = "Kill Switch",
                        description = "Block unencrypted network traffic if the VPN tunnel drops unexpectedly",
                        checked = settings.killSwitchEnabled,
                        onCheckedChange = { viewModel.toggleKillSwitch(it) }
                    )

                    HorizontalDivider(
                        color = CyberBorder,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    SettingToggleItem(
                        icon = Icons.Default.AltRoute,
                        title = "Bypass Sri Lanka Traffic",
                        description = "Route local Sri Lankan domains and private LAN IPs outside the VPN tunnel",
                        checked = settings.routingMode == RoutingMode.BYPASS_LAN_AND_SRI_LANKA,
                        onCheckedChange = { isChecked ->
                            viewModel.setRoutingMode(
                                if (isChecked) RoutingMode.BYPASS_LAN_AND_SRI_LANKA else RoutingMode.GLOBAL
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                SectionHeader(title = "SMART AUTO CONNECT")

                CyberCard(padding = 16.dp) {
                    SettingToggleItem(
                        icon = Icons.Default.NetworkWifi,
                        title = "Auto-Connect on Wi-Fi",
                        description = "Automatically establish encrypted tunnel upon joining Wi-Fi networks",
                        checked = settings.autoConnectOnWifi,
                        onCheckedChange = { viewModel.toggleAutoConnectWifi(it) }
                    )

                    HorizontalDivider(
                        color = CyberBorder,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    SettingToggleItem(
                        icon = Icons.Default.Security,
                        title = "Auto-Connect on Cellular",
                        description = "Automatically reconnect when mobile cellular data becomes active",
                        checked = settings.autoConnectOnCellular,
                        onCheckedChange = { viewModel.toggleAutoConnectCellular(it) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                SectionHeader(title = "SMART TRAFFIC & ADAPTIVE QOS")

                CyberCard(padding = 16.dp) {
                    SettingToggleItem(
                        icon = Icons.Default.Speed,
                        title = "Smart QoS (Anti-Bufferbloat)",
                        description = "Smooths heavy background downloads to protect foreground apps and eliminate connection timeouts",
                        checked = settings.smartQoSEnabled,
                        onCheckedChange = { viewModel.toggleSmartQoS(it) }
                    )

                    HorizontalDivider(
                        color = CyberBorder,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    SettingToggleItem(
                        icon = Icons.Default.NetworkCheck,
                        title = "Cellular MTU Optimization (1380)",
                        description = "Prevents LTE mobile network packet fragmentation and retransmission lag",
                        checked = settings.cellularMtuOptimized,
                        onCheckedChange = { viewModel.toggleCellularMtuOptimized(it) }
                    )

                    HorizontalDivider(
                        color = CyberBorder,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    SettingToggleItem(
                        icon = Icons.Default.Shield,
                        title = "Anti-DPI Delay & Carrier Jitter Fix",
                        description = "Splits TLS ClientHello into micro-fragments (100-200B) and bypasses Nagle delay to defeat carrier DPI stalls",
                        checked = settings.dpiDelayFixEnabled,
                        onCheckedChange = { viewModel.toggleDpiDelayFix(it) }
                    )

                    HorizontalDivider(
                        color = CyberBorder,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Download Speed Headroom Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Download Bandwidth Headroom",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyberTextPrimary
                            )
                            Text(
                                text = "Leaves dedicated headroom for responsive browsing and messaging",
                                fontSize = 12.sp,
                                color = CyberTextMuted
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = listOf(0 to "AUTO", 5 to "5 MBPS", 10 to "10 MBPS", 20 to "20 MBPS")
                        options.forEach { (mbps, label) ->
                            val isSelected = settings.downloadSpeedLimitMbps == mbps
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) CyberPrimary else CyberCard)
                                    .border(
                                        1.dp,
                                        if (isSelected) CyberAccent else CyberBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { viewModel.setDownloadSpeedLimit(mbps) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSelected) Color.White else CyberTextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                SectionHeader(title = "ENGINE ARCHITECTURE")

                CyberCard(padding = 16.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "VPN Engine Core",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyberTextPrimary
                            )
                            Text(
                                text = "Modular Xray Core abstraction (VLESS Reality)",
                                fontSize = 12.sp,
                                color = CyberTextMuted
                            )
                        }
                        Text(
                            text = "Phase 1 Foundation",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Network Optimizer Entry Point
                CyberCard(
                    padding = 16.dp,
                    borderColor = CyberAccent.copy(alpha = 0.7f),
                    onClick = onNavigateToOptimizer
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Network Optimizer",
                                tint = CyberAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Network Tuning & Diagnostic Studio",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CyberTextPrimary
                                )
                                Text(
                                    text = "Analyze carrier, jitter, PMTU, and auto-tune V2Ray",
                                    fontSize = 12.sp,
                                    color = CyberTextMuted
                                )
                            }
                        }
                        Text(
                            text = "TUNE →",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Best SNI & Fastest Route Finder Entry Point
                CyberCard(
                    padding = 16.dp,
                    borderColor = CyberAccent.copy(alpha = 0.7f),
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
                                contentDescription = "Route Finder",
                                tint = CyberAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "SNI & Fastest Route Finder",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CyberTextPrimary
                                )
                                Text(
                                    text = "Benchmark zero-rated bug hosts & TLS handshake routes",
                                    fontSize = 12.sp,
                                    color = CyberTextMuted
                                )
                            }
                        }
                        Text(
                            text = "SCAN →",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Developer Logs Entry Point
                CyberCard(
                    padding = 16.dp,
                    borderColor = CyberAccent.copy(alpha = 0.5f),
                    onClick = onNavigateToLogs
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Dev Logs",
                                tint = CyberAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Developer Logs & Diagnostics",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CyberTextPrimary
                                )
                                Text(
                                    text = "View live runtime logs & copy debug dump",
                                    fontSize = 12.sp,
                                    color = CyberTextMuted
                                )
                            }
                        }
                        Text(
                            text = "OPEN →",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Logout Button
                CyberButton(
                    onClick = { viewModel.logout(onLogoutSuccess) },
                    variant = CyberButtonVariant.OUTLINED
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Log Out",
                        tint = CyberDanger,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DISCONNECT & UNBIND DEVICE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberDanger
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp,
        color = CyberTextMuted,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun SettingToggleItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = CyberAccent,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyberTextPrimary
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = CyberTextMuted,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CyberPrimary,
                uncheckedThumbColor = CyberTextMuted,
                uncheckedTrackColor = CyberCard,
                uncheckedBorderColor = CyberBorder
            )
        )
    }
}
