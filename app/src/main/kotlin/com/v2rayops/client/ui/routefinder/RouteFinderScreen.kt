package com.v2rayops.client.ui.routefinder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
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
import com.v2rayops.client.network.RouteStrategy
import com.v2rayops.client.network.ScanProfile
import com.v2rayops.client.network.SniCategory
import com.v2rayops.client.network.SniRouteCandidate
import com.v2rayops.client.network.SniStatus

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
                        onApply = { viewModel.applyCandidate(candidate) }
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
    onApply: () -> Unit
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

                // Detail showing address vs SNI routing
                val routeDetail = if (isAddrSni) {
                    "Addr: ${candidate.domain}:443 • SNI: ${candidate.sni}"
                } else {
                    "Addr: ${candidate.targetHost}:443 • SNI: ${candidate.sni}"
                }

                Text(
                    text = routeDetail,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberTextMuted,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Apply Button
            if (isApplied) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberAccent.copy(alpha = 0.2f))
                        .border(1.dp, CyberAccent, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = CyberAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ACTIVE",
                            fontSize = 10.sp,
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
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "APPLY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isUsable) CyberTextPrimary else CyberTextMuted
                    )
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
