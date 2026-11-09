package com.v2rayops.client.ui.servers.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2rayops.client.domain.model.VpnConfig
import com.v2rayops.client.network.FineTunePhase
import com.v2rayops.client.network.FineTuneState
import com.v2rayops.client.network.OptimizationMode
import com.v2rayops.client.ui.dashboard.components.deriveConfigPackageDetails
import java.util.Locale

@Composable
fun OneClickFineTuneCard(
    state: FineTuneState,
    selectedConfig: VpnConfig?,
    onStartFineTune: () -> Unit,
    onStopFineTune: () -> Unit,
    onDismissReport: () -> Unit,
    onSelectMode: (OptimizationMode) -> Unit = {},
    onSelectPreset: (VpnConfig) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val meta = remember(selectedConfig) { deriveConfigPackageDetails(selectedConfig) }
    val isRunning = state.isRunning
    val isCompleted = state.phase == FineTunePhase.COMPLETED
    val hasError = state.error != null

    val borderColor by animateColorAsState(
        targetValue = when {
            isRunning -> Color(0xFF6366F1)
            isCompleted -> Color(0xFF22C55E)
            hasError -> Color(0xFFF43F5E)
            else -> Color(0xFF1E2842)
        },
        label = "BorderColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF090D1A))
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // --- 1. Top Badges & Countdown / Phase Row ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Badge: "ONE-CLICK AUTO FINE TUNE"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF131A33))
                        .border(1.dp, Color(0xFF233157), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = if (isCompleted) Color(0xFF22C55E) else Color(0xFF818CF8),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCompleted) "TUNED & CONNECTED" else "ONE-CLICK AUTO TUNE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isCompleted) Color(0xFF22C55E) else Color(0xFFA5B4FC)
                    )
                }

                // Right Status / Countdown Pill
                if (isRunning) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E1B4B))
                            .border(1.dp, Color(0xFF4338CA), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(10.dp),
                            strokeWidth = 1.2.dp,
                            color = Color(0xFFA5B4FC)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "~${state.estimatedSecondsRemaining}s remaining",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA5B4FC)
                        )
                    }
                } else if (isCompleted) {
                    Text(
                        text = "DISMISS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onDismissReport)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                } else {
                    Text(
                        text = "PURE NETWORK PROBE",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- 2. Title & Package Identification ---
            if (isRunning) {
                Text(
                    text = state.phase.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else if (isCompleted) {
                Text(
                    text = "Active Route Tuned to Maximum Speed",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            } else {
                Text(
                    text = "Carrier Fronting & Kernel Calibration",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Disables VPN • Probes pure-network carrier fronts • Auto-applies winner & starts VPN with speed test & dev tuning.",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            // --- 3. Body Content Based on State ---
            if (isRunning) {
                Spacer(modifier = Modifier.height(10.dp))

                // Smooth Progress Bar
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF6366F1),
                    trackColor = Color(0xFF141C30)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Real-time Cyber Telemetry Console
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF04060A))
                        .border(1.dp, Color(0xFF141B2D), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        val recentLogs = state.logHistory.takeLast(3)
                        for (log in recentLogs) {
                            Text(
                                text = "> $log",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (log.contains("Winner", ignoreCase = true) || log.contains("Complete", ignoreCase = true)) {
                                    Color(0xFF22C55E)
                                } else {
                                    Color(0xFF818CF8)
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Probing Counter & Cancel Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (state.totalToProbe > 0) "Probing: ${state.probedCount}/${state.totalToProbe} fronts" else "Calibrating baseline...",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B)
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1A121E))
                            .border(1.dp, Color(0xFF4A1A28), RoundedCornerShape(6.dp))
                            .clickable(onClick = onStopFineTune)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Stop",
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Stop",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFF43F5E)
                        )
                    }
                }

            } else if (isCompleted) {
                Spacer(modifier = Modifier.height(10.dp))

                // Presets Title Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "3 TUNED MODE PRESETS",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA5B4FC)
                    )
                    Text(
                        text = "1-TAP TO ACTIVATE",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3 Mode Preset Mini Cards
                val presets = listOfNotNull(
                    state.balancedPreset ?: selectedConfig?.takeIf { it.tuningMode == "BALANCED" },
                    state.speedPreset ?: selectedConfig?.takeIf { it.tuningMode == "SPEED" },
                    state.stabilityPreset ?: selectedConfig?.takeIf { it.tuningMode == "STABILITY" }
                )

                if (presets.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        presets.forEach { preset ->
                            val isPresetActive = selectedConfig?.id == preset.id
                            val modeColor = when (preset.tuningMode?.uppercase()) {
                                "SPEED" -> Color(0xFF6366F1)
                                "STABILITY" -> Color(0xFF10B981)
                                else -> Color(0xFF8B5CF6)
                            }
                            val modeTitle = preset.tuningMode ?: "BALANCED"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isPresetActive) Color(0xFF12182E) else Color(0xFF090D1A))
                                    .border(
                                        width = 1.dp,
                                        color = if (isPresetActive) modeColor else Color(0xFF1B243B),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectPreset(preset) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isPresetActive) modeColor else Color(0xFF475569))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = modeTitle,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = modeColor
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "• ${preset.displayRouteDomain}",
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFF94A3B8),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        val dlStr = if (preset.measuredSpeedMbps != null && preset.measuredSpeedMbps > 0) "${preset.measuredSpeedMbps}M DL" else ""
                                        val ulStr = if (preset.measuredUploadMbps != null && preset.measuredUploadMbps > 0.0) " • ${"%.1f".format(Locale.US, preset.measuredUploadMbps)}M UL" else ""
                                        val throughputStr = if (dlStr.isNotBlank()) "$dlStr$ulStr • " else ""
                                        Text(
                                            text = "${throughputStr}MTU ${preset.customMtu ?: 1380} • ${preset.customTcpBufferKb ?: 512}KB",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (preset.measuredPingMs != null && preset.measuredPingMs > 0) {
                                        Text(
                                            text = "${preset.measuredPingMs}ms",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF22C55E)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isPresetActive) modeColor.copy(alpha = 0.2f) else Color(0xFF141C30))
                                            .border(1.dp, if (isPresetActive) modeColor else Color(0xFF243252), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isPresetActive) "ACTIVE" else "SWITCH",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPresetActive) modeColor else Color(0xFFA5B4FC)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Fallback Single Metric Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF060913))
                            .border(1.dp, Color(0xFF141E33), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "OPTIMAL FRONT",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = state.winningRoute?.domain ?: "support.zoom.us",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "SPEED",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (state.measuredSpeedMbps > 0) "${state.measuredSpeedMbps} Mbps" else "Active",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF818CF8)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "PING / STABILITY",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${state.measuredPingMs}ms • ${"%.1f".format(state.stabilityPercent)}%",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF22C55E)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dev Optimizer Settings Capsule
                Text(
                    text = state.appliedSettingsSummary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Action: Tune Again
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF141C30))
                        .clickable(onClick = onStartFineTune)
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = Color(0xFFA5B4FC),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RE-TUNE ALL 3 PRESETS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFA5B4FC)
                    )
                }

            } else {
                Spacer(modifier = Modifier.height(10.dp))

                // Target Package Chip: e.g. "TARGET PACKAGE: DIALOG ZOOM (76.36 GB)"
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F1528))
                        .border(1.dp, Color(0xFF1E2845), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TARGET: ${meta.isp} • ${meta.limit}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFA5B4FC)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode Selector Bar: [ BALANCED ] | [ SPEED ] | [ STABILITY ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OptimizationMode.entries.forEach { mode ->
                        val isSelected = state.targetMode == mode
                        val modeColor = Color(mode.badgeColor)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) modeColor.copy(alpha = 0.2f) else Color(0xFF0A0F1F))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) modeColor else Color(0xFF1E2742),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectMode(mode) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.displayName.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSelected) modeColor else Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Big Action Button: START AUTO FINE TUNE
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF4F46E5),
                                    Color(0xFF6366F1),
                                    Color(0xFF4338CA)
                                )
                            )
                        )
                        .clickable(onClick = onStartFineTune),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "START AUTO FINE TUNE",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
