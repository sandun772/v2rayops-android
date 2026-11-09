package com.v2rayops.client.ui.routefinder

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.v2rayops.client.designsystem.components.FutureReleaseCard
import com.v2rayops.client.ui.dashboard.components.ClientBottomNavBar
import com.v2rayops.client.ui.dashboard.components.ClientNavTab
import com.v2rayops.client.ui.dashboard.components.V2RayOpsTopBar

/**
 * Tools & Local Network Utilities Screen
 * Displays a blurred wireframe scratch of upcoming local power tools in the background:
 * - Multi-Thread Accelerated Parallel Downloader
 * - P2P Local Wi-Fi File Sharing (AirDrop/Mesh style)
 * - LAN & Subnet Diagnostics Suite
 * With an informative "Future Updates" note card and "Check For Updates" button.
 */
@Composable
fun RouteFinderScreen(
    onBackClick: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToFinetune: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            V2RayOpsTopBar(coreVersion = "Xray-core 25.3")
        },
        bottomBar = {
            ClientBottomNavBar(
                selectedTab = ClientNavTab.TOOLS,
                onTabSelected = { tab ->
                    when (tab) {
                        ClientNavTab.CONNECT -> onNavigateToHome()
                        ClientNavTab.FINETUNE -> onNavigateToFinetune()
                        ClientNavTab.SETTINGS -> onNavigateToSettings()
                        ClientNavTab.TOOLS -> { /* Already here */ }
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
            // 1. Background: Blurred Scratch Wireframe of Planned Network Tools
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(16.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ToolsScratchWireframe()
            }

            // 2. Dark Obsidian Scrim Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF050811).copy(alpha = 0.82f))
            )

            // 3. Foreground: Future Release Note Card
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                FutureReleaseCard(
                    badgeText = "TOOLS SUITE IN ACTIVE DEVELOPMENT",
                    title = "V2RAYOPS NETWORK SUITE",
                    subtitle = "Downloader • P2P Local Sharing • LAN Utilities",
                    description = "We are engineering high-speed network utilities including a multi-threaded parallel downloader, direct P2P local Wi-Fi file sharing, and subnet diagnostics. Available in future updates.",
                    icon = Icons.Default.Build,
                    versionTag = "TARGET: V1.2.0 TOOLS UPDATE"
                )
            }
        }
    }
}

/**
 * Scratch wireframe sketch of the planned network tools (Downloader, Local File Sharing, LAN Tools)
 */
@Composable
private fun ToolsScratchWireframe() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Tool 1: Multi-Connection Turbo Downloader Wireframe
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0B1020))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(width = 160.dp, height = 12.dp).background(Color(0xFF334155), RoundedCornerShape(3.dp)))
                    Box(modifier = Modifier.size(width = 60.dp, height = 14.dp).background(Color(0xFF06B6D4).copy(alpha = 0.5f), RoundedCornerShape(10.dp)))
                }
                Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color(0xFF1E293B), RoundedCornerShape(4.dp)))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(6) {
                        Box(modifier = Modifier.weight(1f).height(4.dp).background(Color(0xFF6366F1).copy(alpha = 0.4f), RoundedCornerShape(2.dp)))
                    }
                }
            }
        }

        // Tool 2: P2P Local Wi-Fi File Sharing Wireframe
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0B1020))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(width = 140.dp, height = 12.dp).background(Color(0xFF334155), RoundedCornerShape(3.dp)))
                    Box(modifier = Modifier.size(width = 50.dp, height = 14.dp).background(Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(10.dp)))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).height(40.dp).background(Color(0xFF162035), RoundedCornerShape(8.dp)))
                    Box(modifier = Modifier.weight(1f).height(40.dp).background(Color(0xFF162035), RoundedCornerShape(8.dp)))
                }
            }
        }

        // Tool 3: LAN & Subnet Inspection Wireframe
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0B1020))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(width = 150.dp, height = 12.dp).background(Color(0xFF334155), RoundedCornerShape(3.dp)))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(width = 80.dp, height = 24.dp).background(Color(0xFF1E293B), RoundedCornerShape(6.dp)))
                    Box(modifier = Modifier.size(width = 80.dp, height = 24.dp).background(Color(0xFF1E293B), RoundedCornerShape(6.dp)))
                    Box(modifier = Modifier.size(width = 80.dp, height = 24.dp).background(Color(0xFF1E293B), RoundedCornerShape(6.dp)))
                }
            }
        }
    }
}
