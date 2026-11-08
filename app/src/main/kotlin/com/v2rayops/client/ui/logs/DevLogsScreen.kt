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
import com.v2rayops.client.designsystem.components.CyberTopAppBar
import com.v2rayops.client.designsystem.components.ImportConfigDialog
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
                        onClick = { viewModel.runProbe() },
                        enabled = !isProbing
                    ) {
                        if (isProbing) {
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
                                    color = if (isSelected) CyberAccent else CyberCard,
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
