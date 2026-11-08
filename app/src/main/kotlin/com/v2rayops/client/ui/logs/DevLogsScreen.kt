package com.v2rayops.client.ui.logs

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.core.logger.LogEntry
import com.v2rayops.client.core.logger.LogLevel
import com.v2rayops.client.designsystem.components.CyberCard
import com.v2rayops.client.designsystem.components.CyberTopAppBar
import com.v2rayops.client.designsystem.components.ImportConfigDialog
import com.v2rayops.client.designsystem.theme.CyberAccent
import com.v2rayops.client.designsystem.theme.CyberBackground
import com.v2rayops.client.designsystem.theme.CyberBorder
import com.v2rayops.client.designsystem.theme.CyberBorderSubtle
import com.v2rayops.client.designsystem.theme.CyberCard as CyberCardBg
import com.v2rayops.client.designsystem.theme.CyberCardElevated
import com.v2rayops.client.designsystem.theme.CyberDanger
import com.v2rayops.client.designsystem.theme.CyberPrimary
import com.v2rayops.client.designsystem.theme.CyberSuccess
import com.v2rayops.client.designsystem.theme.CyberTextMuted
import com.v2rayops.client.designsystem.theme.CyberTextPrimary
import com.v2rayops.client.designsystem.theme.CyberTextSecondary
import com.v2rayops.client.designsystem.theme.CyberWarning
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Speed
import com.v2rayops.client.network.DnsServerCheckResult
import com.v2rayops.client.network.NodeProbeReport
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class LogFilter(val label: String) {
    ALL("ALL"),
    ERRORS("ERRORS"),
    WARN("WARNINGS"),
    VPN("VPN"),
    XRAY("XRAY CORE"),
    DIAG("DIAGNOSTICS")
}

enum class DevTab(val label: String) {
    LOGS("LIVE LOGS"),
    PROBER("SOCKET PROBER"),
    DNS("DNS INSPECTOR")
}

@Composable
fun DevLogsScreen(
    onBackClick: () -> Unit,
    viewModel: DevLogsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val allLogs by AppLogger.logsFlow.collectAsState()
    val isProbing by viewModel.isProbing.collectAsState()
    val lastProbeReport by viewModel.lastProbeReport.collectAsState()
    val isUploadingReport by viewModel.isUploadingReport.collectAsState()
    val uploadResult by viewModel.uploadResult.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()

    val customProbeReport by viewModel.customProbeReport.collectAsState()
    val isCustomProbing by viewModel.isCustomProbing.collectAsState()
    val probeTargetHost by viewModel.probeTargetHost.collectAsState()
    val probeTargetPort by viewModel.probeTargetPort.collectAsState()
    val probeSni by viewModel.probeSni.collectAsState()
    val probeWsHost by viewModel.probeWsHost.collectAsState()
    val probeWsPath by viewModel.probeWsPath.collectAsState()

    val dnsCheckResults by viewModel.dnsCheckResults.collectAsState()
    val isInspectingDns by viewModel.isInspectingDns.collectAsState()

    var activeTab by remember { mutableStateOf(DevTab.LOGS) }
    var selectedFilter by remember { mutableStateOf(LogFilter.ALL) }
    var isCopied by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var userNoteText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val filteredLogs = remember(allLogs, selectedFilter) {
        when (selectedFilter) {
            LogFilter.ALL -> allLogs
            LogFilter.ERRORS -> allLogs.filter { it.level == LogLevel.ERROR }
            LogFilter.WARN -> allLogs.filter { it.level == LogLevel.WARN }
            LogFilter.VPN -> allLogs.filter { it.tag.contains("Vpn", ignoreCase = true) }
            LogFilter.XRAY -> allLogs.filter { it.tag.contains("Xray", ignoreCase = true) }
            LogFilter.DIAG -> allLogs.filter { it.tag.contains("Diagnostic", ignoreCase = true) || it.tag.contains("Probe", ignoreCase = true) }
        }
    }

    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    fun copyLogsToClipboard() {
        val dump = AppLogger.getFullDiagnosticDump()
        clipboardManager.setText(AnnotatedString(dump))
        isCopied = true
        Toast.makeText(context, "Copied ${allLogs.size} log entries to clipboard", Toast.LENGTH_SHORT).show()
        coroutineScope.launch {
            delay(2500)
            isCopied = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            CyberTopAppBar(
                title = "DEV LOGS & DIAGNOSTICS",
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.AddLink,
                            contentDescription = "Import V2Ray Config",
                            tint = CyberAccent
                        )
                    }
                    IconButton(
                        onClick = {
                            when (activeTab) {
                                DevTab.LOGS -> viewModel.runProbe()
                                DevTab.PROBER -> viewModel.runCustomProbe()
                                DevTab.DNS -> viewModel.runDnsInspection()
                            }
                        },
                        enabled = !isProbing && !isCustomProbing && !isInspectingDns
                    ) {
                        if (isProbing || isCustomProbing || isInspectingDns) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = CyberAccent
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.NetworkCheck,
                                contentDescription = "Run Probe",
                                tint = CyberAccent
                            )
                        }
                    }
                    IconButton(
                        onClick = { showUploadDialog = true },
                        enabled = !isUploadingReport
                    ) {
                        if (isUploadingReport) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = CyberAccent
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Upload Dev Report to VPS",
                                tint = CyberAccent
                            )
                        }
                    }
                    IconButton(onClick = { AppLogger.clear() }) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Logs",
                            tint = CyberTextMuted
                        )
                    }
                }
            )

            // Primary Navigation Tab Row: [ LIVE LOGS | SOCKET PROBER | DNS INSPECTOR ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DevTab.values().forEach { tab ->
                    val isSelected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) CyberCardElevated else CyberCardBg)
                            .border(
                                1.dp,
                                if (isSelected) CyberAccent else CyberBorderSubtle,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { activeTab = tab }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.label,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) CyberAccent else CyberTextMuted
                        )
                    }
                }
            }

            when (activeTab) {
                DevTab.LOGS -> {
                    // Header Action & Upload Cards
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                // Action Buttons Row (Copy Logs + Upload Dev Report to VPS)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Copy Button
                    Button(
                        onClick = { copyLogsToClipboard() },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCopied) CyberSuccess else CyberCardElevated
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorderSubtle)
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = if (isCopied) Color.White else CyberTextPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCopied) "COPIED" else "COPY LOGS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isCopied) Color.White else CyberTextPrimary
                        )
                    }

                    // Upload Dev Report to VPS Button
                    Button(
                        onClick = { showUploadDialog = true },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberPrimary
                        ),
                        enabled = !isUploadingReport
                    ) {
                        if (isUploadingReport) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(15.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "UPLOADING...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Upload",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "UPLOAD TO VPS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                    }
                }

                // Upload Result Banner (Success)
                if (uploadResult != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberSuccess.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .border(1.dp, CyberSuccess.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(10.dp)
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
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = CyberSuccess,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "DEV REPORT UPLOADED TO VPS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyberSuccess
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = uploadResult!!,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyberTextPrimary
                                    )
                                }
                            }
                            IconButton(
                                onClick = { viewModel.clearUploadFeedback() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = CyberTextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Upload Error Banner
                if (uploadError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberDanger.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .border(1.dp, CyberDanger.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "UPLOAD FAILED",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberDanger
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = uploadError!!,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberTextPrimary
                                )
                            }
                            IconButton(
                                onClick = { viewModel.clearUploadFeedback() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = CyberTextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Node Pre-flight Probe Button & Banner
                OutlinedButton(
                    onClick = { viewModel.runProbe() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberAccent.copy(alpha = 0.7f)),
                    enabled = !isProbing
                ) {
                    if (isProbing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = CyberAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PROBING NODE (DNS, TCP, TLS, WS)...",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberAccent
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = "Probe",
                            tint = CyberAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RUN SOCKET & BUG HOST PROBE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberAccent
                        )
                    }
                }

                if (lastProbeReport != null) {
                    val report = lastProbeReport!!
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF04060A), RoundedCornerShape(6.dp))
                            .border(1.dp, if (report.tcpSuccess) CyberSuccess.copy(alpha = 0.5f) else CyberDanger.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "PROBE: ${report.nodeName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (report.tcpSuccess) CyberSuccess else CyberDanger
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = report.summary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextPrimary,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LogFilter.values().forEach { filter ->
                        val isSelected = filter == selectedFilter
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) CyberAccent else CyberCardBg,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) CyberAccent else CyberBorderSubtle,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = filter.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSelected) Color.Black else CyberTextMuted
                            )
                        }
                    }
                }
            }

            // Logs Output Terminal View
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(Color(0xFF020408), RoundedCornerShape(8.dp))
                    .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
            ) {
                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No log entries found for filter [${selectedFilter.label}]",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberTextMuted
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredLogs, key = { it.timestamp.toString() + it.message.hashCode() }) { log ->
                            LogItemRow(log)
                        }
                    }
                }
            }
        }

        DevTab.PROBER -> {
                    // Interactive Socket Prober Tab Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "PRE-CONFIGURED PROBE PRESETS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberAccent
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DevPresetChip("ZOOM FRONTING") {
                                viewModel.setProbeTargetHost("support.zoom.us")
                                viewModel.setProbeTargetPort(443)
                                viewModel.setProbeSni("sg-v2rayops.vpnxhub.com")
                                viewModel.setProbeWsHost("sg-v2rayops.vpnxhub.com")
                            }
                            DevPresetChip("STANDARD SNI") {
                                viewModel.setProbeTargetHost("172.236.134.215")
                                viewModel.setProbeTargetPort(443)
                                viewModel.setProbeSni("support.zoom.us")
                                viewModel.setProbeWsHost("sg-v2rayops.vpnxhub.com")
                            }
                            DevPresetChip("TIKTOK CDN") {
                                viewModel.setProbeTargetHost("p16-va.tiktokcdn.com")
                                viewModel.setProbeTargetPort(443)
                                viewModel.setProbeSni("sg-v2rayops.vpnxhub.com")
                                viewModel.setProbeWsHost("sg-v2rayops.vpnxhub.com")
                            }
                            DevPresetChip("CLOUDFLARE") {
                                viewModel.setProbeTargetHost("one.one.one.one")
                                viewModel.setProbeTargetPort(443)
                                viewModel.setProbeSni("sg-v2rayops.vpnxhub.com")
                                viewModel.setProbeWsHost("sg-v2rayops.vpnxhub.com")
                            }
                        }

                        CyberCard(
                            modifier = Modifier.fillMaxWidth(),
                            padding = 10.dp,
                            backgroundColor = CyberCardElevated
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "TARGET PROBE PARAMETERS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberTextPrimary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = probeTargetHost,
                                        onValueChange = { viewModel.setProbeTargetHost(it) },
                                        label = { Text("Target Host / IP", fontSize = 9.sp) },
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = CyberTextPrimary
                                        ),
                                        modifier = Modifier.weight(2.5f)
                                    )
                                    OutlinedTextField(
                                        value = probeTargetPort.toString(),
                                        onValueChange = { viewModel.setProbeTargetPort(it.toIntOrNull() ?: 443) },
                                        label = { Text("Port", fontSize = 9.sp) },
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = CyberTextPrimary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                OutlinedTextField(
                                    value = probeSni,
                                    onValueChange = { viewModel.setProbeSni(it) },
                                    label = { Text("TLS SNI Header", fontSize = 9.sp) },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyberTextPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = probeWsHost,
                                    onValueChange = { viewModel.setProbeWsHost(it) },
                                    label = { Text("WebSocket Host Header", fontSize = 9.sp) },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyberTextPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = probeWsPath,
                                    onValueChange = { viewModel.setProbeWsPath(it) },
                                    label = { Text("WebSocket Path", fontSize = 9.sp) },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyberTextPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = { viewModel.runCustomProbe() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp),
                                    enabled = !isCustomProbing,
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary)
                                ) {
                                    if (isCustomProbing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "EXECUTING DIRECT PROBE...",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.White
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "RUN ADVANCED SOCKET PROBE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        if (customProbeReport != null) {
                            val rep = customProbeReport!!
                            CyberCard(
                                modifier = Modifier.fillMaxWidth(),
                                padding = 12.dp,
                                borderColor = if (rep.wsSuccess) CyberSuccess else if (rep.tlsSuccess) CyberAccent else CyberDanger,
                                backgroundColor = Color(0xFF020408)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "PROBE EXECUTION REPORT",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (rep.wsSuccess) CyberSuccess else if (rep.tlsSuccess) CyberAccent else CyberDanger
                                        )
                                        Text(
                                            text = if (rep.wsSuccess) "101 WS READY" else if (rep.tlsSuccess) "TLS OK" else "FAILED",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (rep.wsSuccess) CyberSuccess else if (rep.tlsSuccess) CyberAccent else CyberDanger
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    ProbeStepRow(
                                        step = "1. DNS RESOLUTION",
                                        success = rep.dnsResolvedIps.isNotEmpty(),
                                        latency = rep.dnsLatencyMs,
                                        detail = if (rep.dnsResolvedIps.isNotEmpty()) rep.dnsResolvedIps.joinToString() else "Failed"
                                    )

                                    ProbeStepRow(
                                        step = "2. TCP CONNECT",
                                        success = rep.tcpSuccess,
                                        latency = rep.tcpLatencyMs,
                                        detail = if (rep.tcpSuccess) "Direct Socket Connected" else rep.tcpError
                                    )

                                    ProbeStepRow(
                                        step = "3. TLS HANDSHAKE",
                                        success = rep.tlsSuccess,
                                        latency = rep.tlsLatencyMs,
                                        detail = if (rep.tlsSuccess) "TLS 1.3 OK (SNI: ${rep.sni})" else rep.tlsError
                                    )

                                    ProbeStepRow(
                                        step = "4. WS UPGRADE",
                                        success = rep.wsSuccess,
                                        latency = 0L,
                                        detail = if (rep.wsSuccess) "HTTP/1.1 101 Switching Protocols" else if (rep.wsStatusLine.isNotBlank()) rep.wsStatusLine else rep.wsError
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "Full Summary: ${rep.summary}",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyberTextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }

                DevTab.DNS -> {
                    // DNS Inspector Tab Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DNS RESOLVER REACHABILITY AUDIT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextPrimary
                            )
                            Button(
                                onClick = { viewModel.runDnsInspection() },
                                enabled = !isInspectingDns,
                                colors = ButtonDefaults.buttonColors(containerColor = CyberAccent),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(30.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                            ) {
                                if (isInspectingDns) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.Black
                                    )
                                } else {
                                    Text(
                                        text = "RE-TEST DNS",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.Black
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Audits Cloudflare, Google, Quad9, and Carrier resolvers via direct TCP port 53 and UDP datagrams to detect cellular DPI port 53 filtering.",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberTextMuted
                        )

                        if (dnsCheckResults.isEmpty() && isInspectingDns) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = CyberAccent
                                )
                            }
                        }

                        dnsCheckResults.forEach { check ->
                            val statusColor = if (check.isReachable) CyberSuccess else CyberDanger
                            CyberCard(
                                modifier = Modifier.fillMaxWidth(),
                                padding = 10.dp,
                                borderColor = statusColor.copy(alpha = 0.4f),
                                backgroundColor = CyberCardBg
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = check.provider,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = CyberTextPrimary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(CyberAccent.copy(alpha = 0.15f))
                                                    .border(1.dp, CyberAccent.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = check.protocol,
                                                    fontSize = 8.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CyberAccent
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = "${check.serverIp} • ${check.details}",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = CyberTextSecondary
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (check.isReachable) "PASS" else "BLOCKED",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = statusColor
                                        )
                                        if (check.isReachable && check.latencyMs > 0L) {
                                            Text(
                                                text = "${check.latencyMs}ms",
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = CyberSuccess
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }

        if (showImportDialog) {
            ImportConfigDialog(
                onDismiss = { showImportDialog = false },
                onImport = { viewModel.importConfig(it) }
            )
        }

        if (showUploadDialog) {
            AlertDialog(
                onDismissRequest = { showUploadDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = CyberAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "UPLOAD REPORT TO VPS",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberTextPrimary
                        )
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "Gathers active VPN session metrics, active ADDR & SNI parameters, socket probe latencies, QoS states, and in-app logs to help debug and optimize the app.",
                            fontSize = 12.sp,
                            color = CyberTextSecondary,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = userNoteText,
                            onValueChange = { userNoteText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    "Optional debug note (e.g. TikTok images slow, carrier timeout)",
                                    fontSize = 11.sp,
                                    color = CyberTextMuted
                                )
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextPrimary
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberAccent,
                                unfocusedBorderColor = CyberBorderSubtle,
                                cursorColor = CyberAccent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            maxLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val note = userNoteText.trim()
                            showUploadDialog = false
                            viewModel.uploadDevReport(note)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "UPLOAD NOW",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUploadDialog = false }) {
                        Text(
                            text = "CANCEL",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberTextMuted
                        )
                    }
                },
                containerColor = CyberCardElevated,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun LogItemRow(log: LogEntry) {
    val levelColor = when (log.level) {
        LogLevel.ERROR -> CyberDanger
        LogLevel.WARN -> CyberWarning
        LogLevel.INFO -> CyberAccent
        LogLevel.DEBUG -> CyberTextMuted
    }

    val timeFormatted = remember(log.timestamp) {
        java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date(log.timestamp))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = timeFormatted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = CyberTextMuted,
                modifier = Modifier.width(76.dp)
            )

            Text(
                text = "[${log.level.name.take(4)}]",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = levelColor,
                modifier = Modifier.width(46.dp)
            )

            Text(
                text = "[${log.tag.take(14)}]",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = CyberPrimary,
                modifier = Modifier.width(96.dp)
            )

            Text(
                text = log.message,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = CyberTextPrimary,
                lineHeight = 13.sp,
                modifier = Modifier.weight(1f)
            )
        }

        if (log.throwableStackTrace != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = log.throwableStackTrace.take(400),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = CyberDanger.copy(alpha = 0.8f),
                lineHeight = 12.sp,
                modifier = Modifier.padding(start = 122.dp)
            )
        }
    }
}

@Composable
private fun DevPresetChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(CyberCardBg)
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
private fun ProbeStepRow(
    step: String,
    success: Boolean,
    latency: Long,
    detail: String
) {
    val statusColor = if (success) CyberSuccess else CyberDanger
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = step,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyberTextPrimary
                )
            }
            Text(
                text = detail,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = CyberTextMuted,
                maxLines = 1
            )
        }
        if (latency > 0L) {
            Text(
                text = "${latency}ms",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (latency < 400) CyberSuccess else CyberWarning
            )
        }
    }
}
