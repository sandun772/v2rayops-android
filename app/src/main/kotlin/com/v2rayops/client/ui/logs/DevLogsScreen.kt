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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.core.logger.LogEntry
import com.v2rayops.client.core.logger.LogLevel
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
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val allLogs by AppLogger.logsFlow.collectAsState()

    var selectedFilter by remember { mutableStateOf(LogFilter.ALL) }
    var isCopied by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val filteredLogs = remember(allLogs, selectedFilter) {
        when (selectedFilter) {
            LogFilter.ALL -> allLogs
            LogFilter.ERRORS -> allLogs.filter { it.level == LogLevel.ERROR }
            LogFilter.WARN -> allLogs.filter { it.level == LogLevel.WARN }
            LogFilter.VPN -> allLogs.filter { it.tag.contains("Vpn", ignoreCase = true) }
            LogFilter.XRAY -> allLogs.filter { it.tag.contains("Xray", ignoreCase = true) }
            LogFilter.DIAG -> allLogs.filter { it.tag.contains("Diagnostic", ignoreCase = true) }
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
                title = "DEV LOGS",
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { AppLogger.clear() }) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Logs",
                            tint = CyberTextMuted
                        )
                    }
                }
            )

            // Header Action & Easy Copy Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Easy Copy Button
                Button(
                    onClick = { copyLogsToClipboard() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCopied) CyberSuccess else CyberPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCopied) "COPIED TO CLIPBOARD ✓" else "COPY ALL LOGS (${allLogs.size} ENTRIES)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LogFilter.entries.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        val count = remember(allLogs, filter) {
                            when (filter) {
                                LogFilter.ALL -> allLogs.size
                                LogFilter.ERRORS -> allLogs.count { it.level == LogLevel.ERROR }
                                LogFilter.WARN -> allLogs.count { it.level == LogLevel.WARN }
                                LogFilter.VPN -> allLogs.count { it.tag.contains("Vpn", ignoreCase = true) }
                                LogFilter.XRAY -> allLogs.count { it.tag.contains("Xray", ignoreCase = true) }
                                LogFilter.DIAG -> allLogs.count { it.tag.contains("Diagnostic", ignoreCase = true) }
                            }
                        }

                        val chipBg = if (isSelected) CyberAccent.copy(alpha = 0.2f) else CyberCard
                        val chipBorder = if (isSelected) CyberAccent else CyberBorder
                        val chipText = if (isSelected) CyberAccent else CyberTextMuted

                        Box(
                            modifier = Modifier
                                .background(chipBg, RoundedCornerShape(6.dp))
                                .border(1.dp, chipBorder, RoundedCornerShape(6.dp))
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${filter.label} ($count)",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = chipText,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Terminal View
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .background(Color(0xFF030509), RoundedCornerShape(8.dp))
                    .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                if (filteredLogs.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No logs matching current filter.",
                            color = CyberTextMuted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Toggle VPN or run a test to stream events.",
                            color = CyberTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredLogs, key = { it.id }) { log ->
                            LogItemRow(log = log)
                        }
                    }
                }
            }

            // Bottom Bar status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Buffered: ${allLogs.size} logs (max 1000)",
                    fontSize = 11.sp,
                    color = CyberTextMuted,
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    modifier = Modifier.clickable {
                        coroutineScope.launch {
                            if (filteredLogs.isNotEmpty()) {
                                listState.animateScrollToItem(filteredLogs.size - 1)
                            }
                        }
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Scroll to bottom",
                        tint = CyberAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Scroll to Latest",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyberAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun LogItemRow(log: LogEntry) {
    val levelColor = when (log.level) {
        LogLevel.ERROR -> CyberDanger
        LogLevel.WARN -> CyberWarning
        LogLevel.INFO -> CyberAccent
        LogLevel.DEBUG -> CyberTextMuted
    }

    val rowBg = when (log.level) {
        LogLevel.ERROR -> CyberDanger.copy(alpha = 0.08f)
        LogLevel.WARN -> CyberWarning.copy(alpha = 0.05f)
        else -> Color.Transparent
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = log.formattedTime,
                color = CyberTextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 1.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "[${log.level.name.take(3)}]",
                color = levelColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 1.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "${log.tag}: ",
                color = CyberTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = log.message,
                color = when (log.level) {
                    LogLevel.ERROR -> CyberDanger
                    LogLevel.WARN -> CyberWarning
                    else -> CyberTextPrimary
                },
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        if (log.throwableStackTrace != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = log.throwableStackTrace,
                color = CyberDanger.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
