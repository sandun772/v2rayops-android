package com.v2rayops.client.settings.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.v2rayops.client.designsystem.components.FutureReleaseCard
import com.v2rayops.client.settings.SettingsViewModel
import com.v2rayops.client.ui.dashboard.components.ClientBottomNavBar
import com.v2rayops.client.ui.dashboard.components.ClientNavTab
import com.v2rayops.client.ui.dashboard.components.V2RayOpsTopBar

/**
 * Settings & Core Rules Screen
 * Displays a blurred wireframe scratch of upcoming fine-grained tuning & routing options in the background,
 * with an informative "Future Updates" note card, "Check For Updates" button, and Sign Out action.
 */
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {},
    onLogoutSuccess: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToLogs: () -> Unit = {},
    onNavigateToOptimizer: () -> Unit = {},
    onNavigateToRouteFinder: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            V2RayOpsTopBar(coreVersion = "Xray-core 25.3")
        },
        bottomBar = {
            ClientBottomNavBar(
                selectedTab = ClientNavTab.SETTINGS,
                onTabSelected = { tab ->
                    when (tab) {
                        ClientNavTab.CONNECT -> onNavigateToHome()
                        ClientNavTab.FINETUNE -> onNavigateToOptimizer()
                        ClientNavTab.TOOLS -> onNavigateToRouteFinder()
                        ClientNavTab.SETTINGS -> { /* Already here */ }
                        ClientNavTab.USAGE -> onNavigateToHome()
                    }
                }
            )
        },
        containerColor = Color(0xFF050811)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Background: Blurred Wireframe Scratch of Settings Controls
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(16.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsScratchWireframe()
            }

            // 2. Dark Obsidian Scrim Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF050811).copy(alpha = 0.82f))
            )

            // 3. Foreground: Settings Content
            val remoteEnabled by viewModel.remoteManagementEnabled.collectAsState()
            val remoteVersion by viewModel.remoteSettingsVersion.collectAsState()
            val lastSyncStatus by viewModel.lastRemoteSyncStatus.collectAsState()
            val syncState by viewModel.remoteSyncState.collectAsState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cloud Remote Management & Diagnostics Card (v1.0.4)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0C101B))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "CLOUD MANAGEMENT",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6366F1),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Remote VLESS & Tunnel Controls",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                            Switch(
                                checked = remoteEnabled,
                                onCheckedChange = { viewModel.toggleRemoteManagement(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF6366F1),
                                    uncheckedThumbColor = Color(0xFF94A3B8),
                                    uncheckedTrackColor = Color(0xFF1E293B)
                                )
                            )
                        }

                        Text(
                            text = if (remoteEnabled)
                                "Allows the V2RayOps Web Dashboard to remotely update VLESS endpoints and fine-tune latency parameters. Failure diagnostics are sent only after 3 consecutive errors."
                            else
                                "Remote management is paused. Incoming cloud settings and VLESS URL overrides are ignored.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 16.sp
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF060913))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Revision: v$remoteVersion • $lastSyncStatus",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF38BDF8)
                            )
                            Button(
                                onClick = { viewModel.syncWithCloud() },
                                enabled = syncState !is com.v2rayops.client.network.RemoteSyncState.Syncing,
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                if (syncState is com.v2rayops.client.network.RemoteSyncState.Syncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 1.5.dp,
                                        color = Color(0xFF6366F1)
                                    )
                                } else {
                                    Text(text = "Sync Now", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                FutureReleaseCard(
                    badgeText = "CORE SETTINGS IN REFINEMENT",
                    title = "CORE CONFIGURATION & RULES",
                    subtitle = "Advanced Routing & Protocol Overrides",
                    description = "Granular per-app routing rules, custom upstream DNS resolvers, kill switch controls, and kernel-level network tuning will be unlocked in future updates.",
                    icon = Icons.Default.Settings,
                    versionTag = "TARGET: V1.1.0 SETTINGS UPDATE",
                    additionalAction = {
                        Button(
                            onClick = { viewModel.logout(onLogoutSuccess) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E0B12),
                                contentColor = Color(0xFFFB7185)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF4C1D28)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign Out",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFFB7185)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SIGN OUT OF ACCOUNT",
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFB7185)
                            )
                        }
                    }
                )
            }
        }
    }
}

/**
 * Scratch wireframe sketch of the settings options
 */
@Composable
private fun SettingsScratchWireframe() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0B1020))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(width = 130.dp, height = 12.dp).background(Color(0xFF334155), RoundedCornerShape(2.dp)))
                        Box(modifier = Modifier.size(width = 80.dp, height = 8.dp).background(Color(0xFF1E293B), RoundedCornerShape(2.dp)))
                    }
                    Box(modifier = Modifier.size(width = 40.dp, height = 22.dp).background(Color(0xFF1E293B), RoundedCornerShape(11.dp)))
                }
            }
        }
    }
}
