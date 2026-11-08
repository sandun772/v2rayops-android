package com.v2rayops.client.ui.routefinder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.v2rayops.client.designsystem.components.CyberCard
import com.v2rayops.client.designsystem.components.CyberTopAppBar
import com.v2rayops.client.designsystem.theme.CyberAccent
import com.v2rayops.client.designsystem.theme.CyberBackground
import com.v2rayops.client.designsystem.theme.CyberBorder
import com.v2rayops.client.designsystem.theme.CyberBorderSubtle
import com.v2rayops.client.designsystem.theme.CyberCard
import com.v2rayops.client.designsystem.theme.CyberCardElevated
import com.v2rayops.client.designsystem.theme.CyberDanger
import com.v2rayops.client.designsystem.theme.CyberPrimary
import com.v2rayops.client.designsystem.theme.CyberSuccess
import com.v2rayops.client.designsystem.theme.CyberTextMuted
import com.v2rayops.client.designsystem.theme.CyberTextPrimary
import com.v2rayops.client.designsystem.theme.CyberTextSecondary
import com.v2rayops.client.designsystem.theme.CyberWarning
import com.v2rayops.client.network.CommonProxyPorts
import com.v2rayops.client.network.CongestionStressProgress
import com.v2rayops.client.network.CongestionStressResult
import com.v2rayops.client.network.Download100MbProgress
import com.v2rayops.client.network.Download100MbResult
import com.v2rayops.client.network.PortScanResult
import com.v2rayops.client.network.RouteStrategy
import com.v2rayops.client.network.ScanProfile
import com.v2rayops.client.network.SniCategory
import com.v2rayops.client.network.SniRouteCandidate
import com.v2rayops.client.network.SniStatus
import com.v2rayops.client.network.SpeedTestPhase
import com.v2rayops.client.network.SpeedTestProgress
import com.v2rayops.client.network.SpeedTestResult
import com.v2rayops.client.network.StabilityProgress
import com.v2rayops.client.network.ThreeXUiInboundSpec
import com.v2rayops.client.network.TwoMinuteStabilityResult
import java.util.Locale

@Composable
fun RouteFinderScreen(
    onBackClick: () -> Unit,
    viewModel: RouteFinderViewModel = hiltViewModel()
) {
    val candidates by viewModel.candidates.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scannedCount by viewModel.scannedCount.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val activeCategory by viewModel.activeCategory.collectAsState()
    val activeStrategy by viewModel.activeStrategy.collectAsState()
    val activeScanProfile by viewModel.activeScanProfile.collectAsState()
    val appliedRouteKey by viewModel.appliedRouteKey.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val wordQuery by viewModel.wordQuery.collectAsState()
    val vpnConnectionState by viewModel.vpnConnectionState.collectAsState()

    // Route Benchmarking States
    val selectedCandidate by viewModel.selectedCandidate.collectAsState()
    val activeBenchmarkType by viewModel.activeBenchmarkType.collectAsState()
    val isBenchmarkSheetOpen by viewModel.isBenchmarkSheetOpen.collectAsState()
    val stabilityProgress by viewModel.stabilityProgress.collectAsState()
    val stabilityResult by viewModel.stabilityResult.collectAsState()
    val download100MbProgress by viewModel.download100MbProgress.collectAsState()
    val download100MbResult by viewModel.download100MbResult.collectAsState()
    val congestionProgress by viewModel.congestionProgress.collectAsState()
    val congestionResult by viewModel.congestionResult.collectAsState()
    val speedTestProgress by viewModel.speedTestProgress.collectAsState()
    val speedTestResult by viewModel.speedTestResult.collectAsState()

    // 3X-UI Inbound Guide States
    val selectedInboundCandidate by viewModel.selectedInboundCandidate.collectAsState()
    val selectedInboundPort by viewModel.selectedInboundPort.collectAsState()
    val inboundSpec by viewModel.inboundSpec.collectAsState()
    val isInboundGuideOpen by viewModel.isInboundGuideOpen.collectAsState()
    val isAuditingPorts by viewModel.isAuditingPorts.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    val filteredCandidates = remember(candidates, activeCategory, activeStrategy) {
        candidates.filter { candidate ->
            val matchCategory = activeCategory == null || candidate.category == activeCategory
            val matchStrategy = activeStrategy == null || candidate.strategy == activeStrategy
            matchCategory && matchStrategy
        }
    }

    val readyCount = candidates.count { it.status == SniStatus.WS_READY }
    val tlsCount = candidates.count { it.status == SniStatus.TLS_OK }
    val blockedCount = candidates.count { it.status == SniStatus.BLOCKED_TIMEOUT || it.status == SniStatus.TCP_FAILED }

    val bestRoute = remember(candidates) {
        candidates.firstOrNull { it.status == SniStatus.WS_READY || it.status == SniStatus.TLS_OK }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // App Bar
            CyberTopAppBar(
                title = "SNI & ROUTE FINDER",
                onBackClick = onBackClick,
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Custom Route",
                            tint = CyberAccent
                        )
                    }
                    IconButton(
                        onClick = { viewModel.startScan() },
                        enabled = !isScanning
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Re-scan Routes",
                            tint = if (isScanning) CyberTextMuted else CyberAccent
                        )
                    }
                }
            )

            // 1. Pure Direct Carrier Mode Banner
            CyberCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                padding = 8.dp,
                backgroundColor = CyberCard
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(CyberSuccess)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "PURE CARRIER DIRECT: ${viewModel.physicalNetworkType}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextPrimary
                            )
                            Text(
                                text = if (vpnConnectionState.isConnected) "VPN Active: Probes bind directly to physical radio to verify 0-balance pass" else "Pure physical network link (No VPN interference)",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextMuted,
                                maxLines = 1
                            )
                        }
                    }

                    if (vpnConnectionState.isConnected) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = { viewModel.pauseVpn() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberDanger.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberDanger.copy(alpha = 0.5f)),
                            modifier = Modifier.height(28.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "PAUSE VPN",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyberDanger
                            )
                        }
                    }
                }
            }

            // 2. Word-Based SNI Discovery Card
            CyberCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 3.dp),
                padding = 8.dp,
                backgroundColor = CyberCardElevated
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = CyberAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "WORD-BASED SNI DISCOVERY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextPrimary
                            )
                        }
                        Text(
                            text = "100% WS 101 READY",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberSuccess
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Preset Quick Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PresetChip("EDU / WORK") { viewModel.scanPresetPack("edu") }
                        PresetChip("TIKTOK") { viewModel.scanPresetPack("tiktok") }
                        PresetChip("WHATSAPP") { viewModel.scanPresetPack("social") }
                        PresetChip("CLOUDFLARE") { viewModel.scanPresetPack("cloudflare") }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Keyword Search Field + Scan Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = wordQuery,
                            onValueChange = { viewModel.setWordQuery(it) },
                            placeholder = {
                                Text(
                                    text = "Enter words (e.g. zoom, dialog, tiktok, slt)",
                                    fontSize = 9.sp,
                                    color = CyberTextMuted
                                )
                            },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextPrimary
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberAccent,
                                unfocusedBorderColor = CyberBorderSubtle,
                                focusedContainerColor = CyberBackground,
                                unfocusedContainerColor = CyberBackground
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(4.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Button(
                            onClick = { viewModel.scanWords(wordQuery) },
                            enabled = wordQuery.isNotBlank() && !isScanning,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberAccent),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.height(40.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "SCAN WORDS",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            // Quick Route Benchmarking Launcher Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuickBenchmarkPill(
                    icon = Icons.Default.Speed,
                    label = "SPEEDTEST",
                    onClick = {
                        val target = bestRoute ?: filteredCandidates.firstOrNull() ?: candidates.firstOrNull()
                        if (target != null) {
                            viewModel.openBenchmark(target, CandidateBenchmarkType.SPEEDTEST)
                            viewModel.runActiveBenchmark()
                        }
                    }
                )
                QuickBenchmarkPill(
                    icon = Icons.Default.NetworkCheck,
                    label = "2-MIN AUDIT",
                    onClick = {
                        val target = bestRoute ?: filteredCandidates.firstOrNull() ?: candidates.firstOrNull()
                        if (target != null) {
                            viewModel.openBenchmark(target, CandidateBenchmarkType.STABILITY_2MIN)
                            viewModel.runActiveBenchmark()
                        }
                    }
                )
                QuickBenchmarkPill(
                    icon = Icons.Default.ArrowDownward,
                    label = "100MB TEST",
                    onClick = {
                        val target = bestRoute ?: filteredCandidates.firstOrNull() ?: candidates.firstOrNull()
                        if (target != null) {
                            viewModel.openBenchmark(target, CandidateBenchmarkType.DOWNLOAD_100MB)
                            viewModel.runActiveBenchmark()
                        }
                    }
                )
                QuickBenchmarkPill(
                    icon = Icons.Default.Tune,
                    label = "CONGESTION TEST",
                    onClick = {
                        val target = bestRoute ?: filteredCandidates.firstOrNull() ?: candidates.firstOrNull()
                        if (target != null) {
                            viewModel.openBenchmark(target, CandidateBenchmarkType.CONGESTION_STRESS)
                            viewModel.runActiveBenchmark()
                        }
                    }
                )
            }

            // Strategy Filter Pills: [ ALL MODES | STANDARD SNI | ADDR SNI (FRONTING) ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterPill(
                    label = "ALL MODES",
                    isSelected = activeStrategy == null,
                    onClick = { viewModel.setStrategy(null) },
                    modifier = Modifier.weight(1f)
                )
                FilterPill(
                    label = "STANDARD SNI",
                    isSelected = activeStrategy == RouteStrategy.STANDARD_SNI,
                    onClick = { viewModel.setStrategy(RouteStrategy.STANDARD_SNI) },
                    modifier = Modifier.weight(1f)
                )
                FilterPill(
                    label = "ADDR FRONTING",
                    isSelected = activeStrategy == RouteStrategy.ADDR_SNI,
                    onClick = { viewModel.setStrategy(RouteStrategy.ADDR_SNI) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Stability Scan Profile Selector: [ DEEP STABILITY (MULTI-ROUND) | ULTRA 5-ROUND | QUICK AUDIT ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ScanProfile.values().forEach { profile ->
                    val isSelected = activeScanProfile == profile
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) CyberCardElevated else CyberCard)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) CyberAccent else CyberBorderSubtle,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable(enabled = !isScanning) { viewModel.setScanProfile(profile) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.shortLabel,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) CyberAccent else CyberTextMuted
                        )
                    }
                }
            }

            // Category Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CategoryPill(
                    label = "ALL PACKAGES",
                    isSelected = activeCategory == null,
                    onClick = { viewModel.setCategory(null) }
                )
                SniCategory.values().forEach { cat ->
                    CategoryPill(
                        label = cat.displayName.uppercase(),
                        isSelected = activeCategory == cat,
                        onClick = { viewModel.setCategory(cat) }
                    )
                }
            }

            // Summary Hero Card
            CyberCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                padding = 12.dp,
                backgroundColor = CyberCardElevated
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = CyberAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isScanning) "SCANNING ROUTES ($scannedCount / $totalCount)" else "ROUTE HEALTH AUDIT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextPrimary,
                                letterSpacing = 1.sp
                            )
                        }

                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = CyberAccent
                            )
                        }
                    }

                    if (isScanning && totalCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { scannedCount.toFloat() / totalCount.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = CyberAccent,
                            trackColor = CyberBorder
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Metric Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MetricBadge(
                            label = "WS 101 READY",
                            count = readyCount,
                            color = CyberSuccess,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadge(
                            label = "TLS PASS",
                            count = tlsCount,
                            color = CyberAccent,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadge(
                            label = "BLOCKED / DROP",
                            count = blockedCount,
                            color = CyberDanger,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (statusMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = statusMessage ?: "",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberAccent
                        )
                    }
                }
            }

            // Route Candidate List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredCandidates, key = { "${it.strategy.name}:${it.domain}" }) { candidate ->
                    val isApplied = appliedRouteKey == "${candidate.strategy.name}:${candidate.domain}"
                    RouteCandidateCard(
                        candidate = candidate,
                        isApplied = isApplied,
                        onApply = { viewModel.applyCandidate(candidate) },
                        onBenchmark = { viewModel.openBenchmark(candidate, CandidateBenchmarkType.SPEEDTEST) },
                        onSelectPort = { port -> viewModel.selectCandidatePort(candidate, port) },
                        onOpenInboundGuide = { viewModel.openInboundGuide(candidate, candidate.targetPort) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Bottom Sticky Action Bar: Apply Fastest Route
        if (bestRoute != null && !isScanning) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(CyberBackground.copy(alpha = 0.95f))
                    .border(1.dp, CyberBorderSubtle)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { viewModel.applyCandidate(bestRoute) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "APPLY BEST [${bestRoute.strategy.shortTag}] ${bestRoute.domain} (${bestRoute.stabilityScore}% • ${bestRoute.avgLatencyMs}ms)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }
        }

        // Add Custom Bug Host Dialog
        if (showAddDialog) {
            AddCustomHostDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { rawDomain, strategy ->
                    viewModel.addCustomHost(rawDomain, strategy)
                    showAddDialog = false
                }
            )
        }

        // Candidate Benchmark Modal / Sheet
        if (isBenchmarkSheetOpen && selectedCandidate != null) {
            CandidateBenchmarkDialog(
                candidate = selectedCandidate!!,
                activeType = activeBenchmarkType,
                isApplied = appliedRouteKey == "${selectedCandidate!!.strategy.name}:${selectedCandidate!!.domain}",
                stabilityProgress = stabilityProgress,
                stabilityResult = stabilityResult,
                download100MbProgress = download100MbProgress,
                download100MbResult = download100MbResult,
                congestionProgress = congestionProgress,
                congestionResult = congestionResult,
                speedTestProgress = speedTestProgress,
                speedTestResult = speedTestResult,
                onSelectType = { viewModel.selectBenchmarkType(it) },
                onRun = { viewModel.runActiveBenchmark() },
                onStop = { viewModel.stopActiveBenchmark() },
                onApplyAndRun = { viewModel.applyAndRunBenchmark(selectedCandidate!!, activeBenchmarkType) },
                onDismiss = { viewModel.closeBenchmark() }
            )
        }

        // 3X-UI Inbound Configuration Modal Dialog
        if (isInboundGuideOpen && selectedInboundCandidate != null) {
            ThreeXUiInboundDialog(
                candidate = selectedInboundCandidate!!,
                selectedPort = selectedInboundPort,
                inboundSpec = inboundSpec,
                isAuditingPorts = isAuditingPorts,
                onSelectPort = { viewModel.selectInboundPort(it) },
                onAuditAllPorts = { viewModel.runFullPortAudit(selectedInboundCandidate!!) },
                onApplyPort = { port ->
                    viewModel.applyPortRoute(selectedInboundCandidate!!, port)
                    viewModel.closeInboundGuide()
                },
                onDismiss = { viewModel.closeInboundGuide() }
            )
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) CyberPrimary else CyberCard)
            .border(
                width = 1.dp,
                color = if (isSelected) CyberAccent else CyberBorder,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (isSelected) Color.White else CyberTextSecondary
        )
    }
}

@Composable
private fun CategoryPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) CyberCardElevated else CyberCard)
            .border(
                width = 1.dp,
                color = if (isSelected) CyberAccent else CyberBorderSubtle,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = if (isSelected) CyberAccent else CyberTextSecondary
        )
    }
}

@Composable
private fun MetricBadge(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CyberBackground)
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(vertical = 6.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count.toString(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = color
            )
            Text(
                text = label,
                fontSize = 7.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = CyberTextMuted
            )
        }
    }
}

@Composable
private fun RouteCandidateCard(
    candidate: SniRouteCandidate,
    isApplied: Boolean,
    onApply: () -> Unit,
    onBenchmark: () -> Unit,
    onSelectPort: (Int) -> Unit,
    onOpenInboundGuide: () -> Unit
) {
    val statusColor = when (candidate.status) {
        SniStatus.WS_READY -> CyberSuccess
        SniStatus.TLS_OK -> CyberAccent
        SniStatus.BLOCKED_TIMEOUT -> CyberDanger
        SniStatus.TCP_FAILED -> CyberTextMuted
    }

    val statusText = when (candidate.status) {
        SniStatus.WS_READY -> "WS 101 READY"
        SniStatus.TLS_OK -> "TLS PASS"
        SniStatus.BLOCKED_TIMEOUT -> "BLOCKED / TIMEOUT"
        SniStatus.TCP_FAILED -> "PORT CLOSED"
    }

    val isAddrSni = candidate.strategy == RouteStrategy.ADDR_SNI

    CyberCard(
        modifier = Modifier.fillMaxWidth(),
        padding = 12.dp,
        borderColor = if (isApplied) CyberAccent else CyberBorderSubtle
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = candidate.domain,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Strategy Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isAddrSni) CyberAccent.copy(alpha = 0.2f) else CyberPrimary.copy(alpha = 0.2f))
                            .border(1.dp, if (isAddrSni) CyberAccent else CyberPrimary, RoundedCornerShape(3.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isAddrSni) "ADDR FRONTING" else "STANDARD SNI",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isAddrSni) CyberAccent else Color.White
                        )
                    }

                    // Status Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = statusColor
                        )
                    }

                    // Stability Score Pill
                    if (candidate.samplesCount > 0 && candidate.stabilityScore > 0) {
                        val stabilityColor = when {
                            candidate.isRockSolid -> CyberSuccess
                            candidate.stabilityScore >= 70 -> CyberAccent
                            candidate.stabilityScore > 0 -> CyberWarning
                            else -> CyberDanger
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(stabilityColor.copy(alpha = 0.15f))
                                .border(1.dp, stabilityColor.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (candidate.isRockSolid) "ROCK SOLID ${candidate.stabilityScore}%" else "${candidate.stabilityScore}% STABLE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = stabilityColor
                            )
                        }
                    }

                    if (candidate.latencyMs > 0L) {
                        Text(
                            text = "${candidate.latencyMs}ms",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (candidate.latencyMs < 400) CyberSuccess else if (candidate.latencyMs < 1000) CyberWarning else CyberDanger
                        )
                    }
                }

                if (candidate.samplesCount > 1) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${candidate.successCount}/${candidate.samplesCount} Pass • ${candidate.packetLossPercent}% Loss • ±${candidate.jitterMs}ms Jitter • avg ${candidate.avgLatencyMs}ms (${candidate.minLatencyMs}-${candidate.maxLatencyMs}ms)",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (candidate.packetLossPercent == 0) CyberSuccess.copy(alpha = 0.9f) else CyberWarning,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Detail showing address vs SNI routing and current target port
                val routeDetail = if (isAddrSni) {
                    "Addr: ${candidate.domain}:${candidate.targetPort} • SNI: ${candidate.sni}"
                } else {
                    "Addr: ${candidate.targetHost}:${candidate.targetPort} • SNI: ${candidate.sni}"
                }

                Text(
                    text = routeDetail,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberTextMuted,
                    maxLines = 1
                )

                // Discovered Ports row
                if (candidate.verifiedPorts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "PORTS:",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberTextMuted
                        )
                        candidate.verifiedPorts.forEach { port ->
                            val isSelected = port == candidate.targetPort
                            val isTls = CommonProxyPorts.isTlsPort(port)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isSelected) CyberAccent.copy(alpha = 0.25f) else CyberBackground)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) CyberAccent else CyberBorderSubtle,
                                        shape = RoundedCornerShape(3.dp)
                                    )
                                    .clickable { onSelectPort(port) }
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(CyberAccent)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                    }
                                    Text(
                                        text = if (isTls) "$port:TLS" else "$port:HTTP",
                                        fontSize = 8.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isSelected) CyberAccent else CyberTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Actions Row [ 3X-UI | TEST | APPLY/ACTIVE ]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // 3X-UI Inbound Guide button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberCardElevated)
                        .border(1.dp, CyberPrimary.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                        .clickable(onClick = onOpenInboundGuide)
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = "3X-UI Inbound Guide",
                            tint = CyberPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "3X-UI",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberPrimary
                        )
                    }
                }

                // Test Route button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberCardElevated)
                        .border(1.dp, CyberAccent.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .clickable(onClick = onBenchmark)
                        .padding(horizontal = 7.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Test Route",
                            tint = CyberAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "TEST",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberAccent
                        )
                    }
                }

                if (isApplied) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberAccent.copy(alpha = 0.2f))
                            .border(1.dp, CyberAccent, RoundedCornerShape(6.dp))
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = CyberAccent,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "ACTIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyberAccent
                            )
                        }
                    }
                } else {
                    val isUsable = candidate.status == SniStatus.WS_READY || candidate.status == SniStatus.TLS_OK
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isUsable) CyberCardElevated else CyberCard)
                            .border(
                                width = 1.dp,
                                color = if (isUsable) CyberBorder else CyberBorderSubtle,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable(enabled = isUsable, onClick = onApply)
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "APPLY",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isUsable) CyberTextPrimary else CyberTextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCustomHostDialog(
    onDismiss: () -> Unit,
    onAdd: (domain: String, strategy: RouteStrategy) -> Unit
) {
    var rawInput by remember { mutableStateOf("") }
    var selectedStrategy by remember { mutableStateOf(RouteStrategy.ADDR_SNI) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberCardElevated,
        title = {
            Text(
                text = "ADD CUSTOM BUG HOST ROUTE",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = CyberTextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "Select routing strategy mode:",
                    fontSize = 11.sp,
                    color = CyberTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterPill(
                        label = "ADDR FRONTING",
                        isSelected = selectedStrategy == RouteStrategy.ADDR_SNI,
                        onClick = { selectedStrategy = RouteStrategy.ADDR_SNI },
                        modifier = Modifier.weight(1f)
                    )
                    FilterPill(
                        label = "STANDARD SNI",
                        isSelected = selectedStrategy == RouteStrategy.STANDARD_SNI,
                        onClick = { selectedStrategy = RouteStrategy.STANDARD_SNI },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (selectedStrategy == RouteStrategy.ADDR_SNI) {
                        "Domain will be used as the TCP connection Address with VPS SNI fronting."
                    } else {
                        "Domain will be injected into TLS SNI with direct VPS address."
                    },
                    fontSize = 10.sp,
                    color = CyberTextMuted
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = rawInput,
                    onValueChange = { rawInput = it },
                    placeholder = {
                        Text(
                            text = "e.g. support.zoom.us",
                            fontSize = 11.sp,
                            color = CyberTextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberAccent,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = CyberTextPrimary,
                        unfocusedTextColor = CyberTextPrimary,
                        cursorColor = CyberAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (rawInput.isNotBlank()) {
                        onAdd(rawInput, selectedStrategy)
                    }
                },
                enabled = rawInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "ADD & BENCHMARK",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "CANCEL",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberTextMuted
                )
            }
        }
    )
}

@Composable
private fun PresetChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(CyberCard)
            .border(1.dp, CyberBorderSubtle, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = CyberTextSecondary
        )
    }
}

@Composable
private fun QuickBenchmarkPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CyberCardElevated)
            .border(1.dp, CyberBorderSubtle, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CyberAccent,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = CyberTextPrimary
            )
        }
    }
}

@Composable
private fun CandidateBenchmarkDialog(
    candidate: SniRouteCandidate,
    activeType: CandidateBenchmarkType,
    isApplied: Boolean,
    stabilityProgress: StabilityProgress,
    stabilityResult: TwoMinuteStabilityResult?,
    download100MbProgress: Download100MbProgress,
    download100MbResult: Download100MbResult?,
    congestionProgress: CongestionStressProgress,
    congestionResult: CongestionStressResult?,
    speedTestProgress: SpeedTestProgress,
    speedTestResult: SpeedTestResult?,
    onSelectType: (CandidateBenchmarkType) -> Unit,
    onRun: () -> Unit,
    onStop: () -> Unit,
    onApplyAndRun: () -> Unit,
    onDismiss: () -> Unit
) {
    val isAnyRunning = stabilityProgress.isRunning || download100MbProgress.isRunning ||
            congestionProgress.isRunning || speedTestProgress.isRunning

    AlertDialog(
        onDismissRequest = { if (!isAnyRunning) onDismiss() },
        containerColor = CyberCardElevated,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ROUTE BENCHMARK",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextPrimary,
                        letterSpacing = 1.sp
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (candidate.strategy == RouteStrategy.ADDR_SNI) CyberAccent.copy(alpha = 0.2f) else CyberPrimary.copy(alpha = 0.2f))
                            .border(1.dp, if (candidate.strategy == RouteStrategy.ADDR_SNI) CyberAccent else CyberPrimary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = candidate.strategy.shortTag,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (candidate.strategy == RouteStrategy.ADDR_SNI) CyberAccent else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = candidate.domain,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyberAccent
                )
                Text(
                    text = "Addr: ${if (candidate.strategy == RouteStrategy.ADDR_SNI) candidate.domain else candidate.targetHost}:443 • SNI: ${candidate.sni}",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberTextMuted
                )
            }
        },
        text = {
            Column {
                // 4-Way Type Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CandidateBenchmarkType.values().forEach { type ->
                        val isSelected = activeType == type
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) CyberPrimary else CyberBackground)
                                .border(1.dp, if (isSelected) CyberAccent else CyberBorderSubtle, RoundedCornerShape(6.dp))
                                .clickable(enabled = !isAnyRunning) { onSelectType(type) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = type.title,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSelected) Color.White else CyberTextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Benchmark Card Body
                when (activeType) {
                    CandidateBenchmarkType.SPEEDTEST -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberBackground)
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("DOWNLOAD", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = CyberTextMuted)
                                        Text(
                                            text = "${"%.2f".format(Locale.US, speedTestResult?.downloadSpeedMbps ?: speedTestProgress.downloadSpeedMbps)} M",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = CyberSuccess
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("UPLOAD", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = CyberTextMuted)
                                        Text(
                                            text = "${"%.2f".format(Locale.US, speedTestResult?.uploadSpeedMbps ?: speedTestProgress.uploadSpeedMbps)} M",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = CyberAccent
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Ping: ${speedTestResult?.pingMs ?: speedTestProgress.pingMs}ms • Jitter: ±${speedTestResult?.jitterMs ?: speedTestProgress.jitterMs}ms",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberTextSecondary
                                )

                                if (speedTestProgress.isRunning) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { speedTestProgress.progressPercent },
                                        modifier = Modifier.fillMaxWidth().height(4.dp),
                                        color = CyberAccent
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(speedTestProgress.statusText, fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = CyberTextMuted)
                                }

                                if (speedTestResult != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(speedTestResult.rating, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = CyberSuccess)
                                }
                            }
                        }
                    }

                    CandidateBenchmarkType.STABILITY_2MIN -> {
                        val remainingSec = (stabilityProgress.totalSeconds - stabilityProgress.elapsedSeconds).coerceAtLeast(0)
                        val timeStr = String.format(Locale.US, "%02d:%02d", remainingSec / 60, remainingSec % 60)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberBackground)
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("TIME: $timeStr", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = CyberAccent)
                                    Text("SCORE: ${stabilityProgress.stabilityScorePercent}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = CyberSuccess)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Avg ${stabilityProgress.avgLatencyMs}ms • Loss ${"%.1f".format(Locale.US, stabilityProgress.packetLossPercent)}% • Jitter ±${stabilityProgress.jitterMs}ms (${stabilityProgress.packetsReceived}/${stabilityProgress.packetsSent})",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberTextSecondary
                                )

                                if (stabilityProgress.isRunning) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { stabilityProgress.progressPercent },
                                        modifier = Modifier.fillMaxWidth().height(4.dp),
                                        color = CyberAccent
                                    )
                                }

                                if (stabilityResult != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(stabilityResult.stabilityGrade, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = CyberSuccess)
                                }
                            }
                        }
                    }

                    CandidateBenchmarkType.DOWNLOAD_100MB -> {
                        val mb = download100MbProgress.bytesReceived / (1024.0 * 1024.0)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberBackground)
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${"%.1f".format(Locale.US, mb)} / 100 MB", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = CyberTextPrimary)
                                    Text("${"%.2f".format(Locale.US, download100MbProgress.currentMbps)} Mbps", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = CyberSuccess)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Avg ${"%.2f".format(Locale.US, download100MbResult?.avgSpeedMbps ?: download100MbProgress.avgMbps)} Mbps • Peak ${"%.2f".format(Locale.US, download100MbResult?.peakSpeedMbps ?: download100MbProgress.peakMbps)} Mbps (${download100MbProgress.elapsedSeconds}s)",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberTextSecondary
                                )

                                if (download100MbProgress.isRunning) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { download100MbProgress.progressPercent },
                                        modifier = Modifier.fillMaxWidth().height(4.dp),
                                        color = CyberSuccess
                                    )
                                }

                                if (download100MbResult?.devReportUploaded == true) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Report #${download100MbResult.devReportId} uploaded to VPS", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = CyberSuccess)
                                }
                            }
                        }
                    }

                    CandidateBenchmarkType.CONGESTION_STRESS -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberBackground)
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Idle: ${congestionProgress.baselineIdlePingMs}ms • Loaded: ${congestionProgress.currentPingMs}ms (+${congestionProgress.bufferbloatDeltaMs}ms)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberAccent
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Concurrent DL ${"%.1f".format(Locale.US, congestionProgress.currentDownloadMbps)}M • UL ${"%.1f".format(Locale.US, congestionProgress.currentUploadMbps)}M • Loss ${"%.1f".format(Locale.US, congestionProgress.packetLossPercent)}%",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberTextSecondary
                                )

                                if (congestionProgress.isRunning) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { congestionProgress.progressPercent },
                                        modifier = Modifier.fillMaxWidth().height(4.dp),
                                        color = CyberAccent
                                    )
                                }

                                if (congestionResult != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(congestionResult.bufferbloatGrade, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = CyberSuccess)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isAnyRunning) {
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberDanger)
                ) {
                    Text("STOP TEST", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.White)
                }
            } else {
                if (isApplied) {
                    Button(
                        onClick = onRun,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary)
                    ) {
                        Text("RUN TEST", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.White)
                    }
                } else {
                    Button(
                        onClick = onApplyAndRun,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberAccent)
                    ) {
                        Text("APPLY & TEST", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.Black)
                    }
                }
            }
        },
        dismissButton = {
            if (!isAnyRunning) {
                TextButton(onClick = onDismiss) {
                    Text("CLOSE", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyberTextSecondary)
                }
            }
        }
    )
}

@Composable
private fun ThreeXUiInboundDialog(
    candidate: SniRouteCandidate,
    selectedPort: Int,
    inboundSpec: ThreeXUiInboundSpec?,
    isAuditingPorts: Boolean,
    onSelectPort: (Int) -> Unit,
    onAuditAllPorts: () -> Unit,
    onApplyPort: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    var copiedNotice by remember { mutableStateOf<String?>(null) }
    val isTls = CommonProxyPorts.isTlsPort(selectedPort)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberBackground,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        tint = CyberPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "3X-UI INBOUND GUIDE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isTls) CyberSuccess.copy(alpha = 0.2f) else CyberWarning.copy(alpha = 0.2f))
                        .border(1.dp, if (isTls) CyberSuccess else CyberWarning, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isTls) "PORT $selectedPort (TLS)" else "PORT $selectedPort (HTTP / CLEARTEXT)",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isTls) CyberSuccess else CyberWarning
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Copied notice banner
                if (copiedNotice != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(CyberAccent.copy(alpha = 0.2f))
                            .border(1.dp, CyberAccent, RoundedCornerShape(4.dp))
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = copiedNotice ?: "",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberAccent
                        )
                    }
                }

                // Node & Strategy Context Card
                CyberCard(
                    modifier = Modifier.fillMaxWidth(),
                    padding = 8.dp,
                    backgroundColor = CyberCard
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "BUG HOST / DOMAIN",
                                fontSize = 7.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextMuted
                            )
                            Text(
                                text = candidate.domain,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "ROUTING MODE",
                                fontSize = 7.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextMuted
                            )
                            Text(
                                text = candidate.strategy.displayName,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyberAccent
                            )
                        }
                    }
                }

                // Port Selector Bar
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SELECT PORT TO ALLOW IN 3X-UI:",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberTextSecondary
                        )

                        // Audit all ports button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyberCardElevated)
                                .border(1.dp, CyberPrimary.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .clickable(enabled = !isAuditingPorts, onClick = onAuditAllPorts)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isAuditingPorts) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(8.dp),
                                        color = CyberAccent,
                                        strokeWidth = 1.dp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "AUDITING...",
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyberAccent
                                    )
                                } else {
                                    Text(
                                        text = "AUDIT ALL 13 PORTS",
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyberPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Horizontal row of discovered + common ports
                    val displayPorts = (candidate.verifiedPorts + CommonProxyPorts.QUICK_MULTI_PORTS).distinct().sorted()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        displayPorts.forEach { p ->
                            val isSelected = p == selectedPort
                            val pTls = CommonProxyPorts.isTlsPort(p)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) CyberPrimary else CyberCard)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) CyberAccent else CyberBorderSubtle,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable { onSelectPort(p) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = p.toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isSelected) Color.White else CyberTextPrimary
                                    )
                                    Text(
                                        text = if (pTls) "TLS" else "HTTP",
                                        fontSize = 6.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isSelected) CyberAccent else CyberTextMuted
                                    )
                                }
                            }
                        }
                    }
                }

                // 3X-UI Form Fields Table
                if (inboundSpec != null) {
                    CyberCard(
                        modifier = Modifier.fillMaxWidth(),
                        padding = 10.dp,
                        backgroundColor = CyberCard
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "3X-UI WEB INBOUND PARAMETERS",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyberAccent
                            )
                            Spacer(modifier = Modifier.height(2.dp))

                            InboundFieldRow("Remark", inboundSpec.remark) {
                                clipboardManager.setText(AnnotatedString(inboundSpec.remark))
                                copiedNotice = "Copied Remark: ${inboundSpec.remark}"
                            }
                            InboundFieldRow("Protocol", inboundSpec.protocol) {
                                clipboardManager.setText(AnnotatedString(inboundSpec.protocol))
                                copiedNotice = "Copied Protocol: ${inboundSpec.protocol}"
                            }
                            InboundFieldRow("Listening IP", inboundSpec.listen) {
                                clipboardManager.setText(AnnotatedString(inboundSpec.listen))
                                copiedNotice = "Copied Listening IP"
                            }
                            InboundFieldRow("Port", inboundSpec.port.toString()) {
                                clipboardManager.setText(AnnotatedString(inboundSpec.port.toString()))
                                copiedNotice = "Copied Port: ${inboundSpec.port}"
                            }
                            InboundFieldRow("Transmission", inboundSpec.network) {
                                clipboardManager.setText(AnnotatedString(inboundSpec.network))
                                copiedNotice = "Copied Transmission: ${inboundSpec.network}"
                            }
                            InboundFieldRow("WS Path", inboundSpec.wsPath) {
                                clipboardManager.setText(AnnotatedString(inboundSpec.wsPath))
                                copiedNotice = "Copied Path: ${inboundSpec.wsPath}"
                            }
                            InboundFieldRow("WS Header (Host)", inboundSpec.wsHost) {
                                clipboardManager.setText(AnnotatedString(inboundSpec.wsHost))
                                copiedNotice = "Copied Host: ${inboundSpec.wsHost}"
                            }
                            InboundFieldRow("Security", inboundSpec.security) {
                                clipboardManager.setText(AnnotatedString(inboundSpec.security))
                                copiedNotice = "Copied Security: ${inboundSpec.security}"
                            }
                            if (inboundSpec.isTls) {
                                InboundFieldRow("TLS SNI", inboundSpec.sni) {
                                    clipboardManager.setText(AnnotatedString(inboundSpec.sni))
                                    copiedNotice = "Copied SNI: ${inboundSpec.sni}"
                                }
                            }
                            InboundFieldRow("Client UUID", inboundSpec.clientUuid.take(16) + "...") {
                                clipboardManager.setText(AnnotatedString(inboundSpec.clientUuid))
                                copiedNotice = "Copied Client UUID"
                            }
                        }
                    }

                    // Step-by-Step Instructions Card
                    CyberCard(
                        modifier = Modifier.fillMaxWidth(),
                        padding = 10.dp,
                        backgroundColor = CyberCard
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "HOW TO CONFIGURE IN 3X-UI PANEL",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyberPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            inboundSpec.instructions.forEach { step ->
                                Text(
                                    text = step,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberTextSecondary
                                )
                            }
                        }
                    }

                    // Raw JSON Snippet Card
                    CyberCard(
                        modifier = Modifier.fillMaxWidth(),
                        padding = 10.dp,
                        backgroundColor = CyberBackground
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "INBOUND JSON CONFIGURATION",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberTextMuted
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(CyberCardElevated)
                                        .border(1.dp, CyberBorderSubtle, RoundedCornerShape(3.dp))
                                        .clickable {
                                            clipboardManager.setText(AnnotatedString(inboundSpec.fullJsonSnippet))
                                            copiedNotice = "Copied Full Inbound JSON"
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = null,
                                            tint = CyberAccent,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "COPY JSON",
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = CyberAccent
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = inboundSpec.fullJsonSnippet,
                                fontSize = 7.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextPrimary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApplyPort(selectedPort) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberAccent)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "APPLY PORT $selectedPort TO APP",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Black
                )
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (inboundSpec != null) {
                    TextButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(inboundSpec.summaryText))
                            copiedNotice = "Copied 3X-UI Specification Summary"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = CyberTextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "COPY SPECS",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberTextSecondary
                        )
                    }

                    TextButton(
                        onClick = {
                            try {
                                uriHandler.openUri(inboundSpec.panelUrl)
                            } catch (_: Exception) {}
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            tint = CyberPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "OPEN 3X-UI",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberPrimary
                        )
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text(
                        text = "CLOSE",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextMuted
                    )
                }
            }
        }
    )
}

@Composable
private fun InboundFieldRow(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCopy)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            color = CyberTextMuted
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = CyberTextPrimary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy $label",
                tint = CyberTextMuted,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}
