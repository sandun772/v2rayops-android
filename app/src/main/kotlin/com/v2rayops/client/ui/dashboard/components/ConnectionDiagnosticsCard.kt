package com.v2rayops.client.ui.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2rayops.client.designsystem.components.CyberCard
import com.v2rayops.client.designsystem.theme.CyberAccent
import com.v2rayops.client.designsystem.theme.CyberBorder
import com.v2rayops.client.designsystem.theme.CyberCardElevated
import com.v2rayops.client.designsystem.theme.CyberDanger
import com.v2rayops.client.designsystem.theme.CyberPrimary
import com.v2rayops.client.designsystem.theme.CyberSuccess
import com.v2rayops.client.designsystem.theme.CyberSuccessGlow
import com.v2rayops.client.designsystem.theme.CyberTextMuted
import com.v2rayops.client.designsystem.theme.CyberTextPrimary
import com.v2rayops.client.designsystem.theme.CyberTextSecondary
import com.v2rayops.client.designsystem.theme.CyberWarning
import com.v2rayops.client.network.ConnectionDiagnosticResult
import com.v2rayops.client.network.DiagnosticStatus

@Composable
fun ConnectionDiagnosticsCard(
    result: ConnectionDiagnosticResult?,
    isTesting: Boolean,
    isVpnConnected: Boolean,
    onRunTest: () -> Unit,
    onViewLogsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    CyberCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = CyberCardElevated,
        borderColor = if (result?.isV2RayOpsExit == true) CyberSuccess.copy(alpha = 0.5f) else CyberBorder,
        padding = 16.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NetworkCheck,
                        contentDescription = "Diagnostics",
                        tint = if (result?.isV2RayOpsExit == true) CyberSuccess else CyberAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CONNECTION DIAGNOSTICS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = CyberTextSecondary
                    )
                }

                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = CyberAccent
                    )
                } else {
                    OutlinedButton(
                        onClick = onRunTest,
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberAccent.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Test",
                            tint = CyberAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (result != null) "Retest" else "Test VPN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CyberAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Body Content
            if (isTesting && result == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Probing tunnel latency & public IP...",
                        fontSize = 12.sp,
                        color = CyberTextMuted
                    )
                }
            } else if (result != null && result.status != DiagnosticStatus.IDLE) {
                // Status Verification Banner
                val bannerBg = when {
                    result.isV2RayOpsExit -> CyberSuccessGlow
                    result.status == DiagnosticStatus.FAILED -> CyberDanger.copy(alpha = 0.15f)
                    isVpnConnected -> CyberAccent.copy(alpha = 0.15f)
                    else -> CyberWarning.copy(alpha = 0.15f)
                }

                val bannerBorder = when {
                    result.isV2RayOpsExit -> CyberSuccess.copy(alpha = 0.6f)
                    result.status == DiagnosticStatus.FAILED -> CyberDanger.copy(alpha = 0.5f)
                    isVpnConnected -> CyberAccent.copy(alpha = 0.5f)
                    else -> CyberWarning.copy(alpha = 0.5f)
                }

                val bannerText = when {
                    result.isV2RayOpsExit -> "VERIFIED: TUNNEL ACTIVE VIA SINGAPORE"
                    result.status == DiagnosticStatus.FAILED -> "DIAGNOSTIC TEST FAILED"
                    isVpnConnected -> "TUNNEL ACTIVE (EXTERNAL NODE)"
                    else -> "DIRECT INTERNET (VPN DISCONNECTED)"
                }

                val bannerColor = when {
                    result.isV2RayOpsExit -> CyberSuccess
                    result.status == DiagnosticStatus.FAILED -> CyberDanger
                    isVpnConnected -> CyberAccent
                    else -> CyberWarning
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bannerBg, RoundedCornerShape(6.dp))
                        .border(1.dp, bannerBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (result.isV2RayOpsExit) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = bannerColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = bannerText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = bannerColor,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Diagnostic Details Grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF060913), RoundedCornerShape(6.dp))
                        .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DiagnosticDetailRow(
                        label = "Exit IP Address",
                        value = result.publicIp.ifBlank { "Unresolved" },
                        valueColor = if (result.isV2RayOpsExit) CyberSuccess else CyberTextPrimary,
                        isMono = true
                    )

                    DiagnosticDetailRow(
                        label = "Ping / Latency",
                        value = if (result.pingMs > 0) "${result.pingMs} ms" else "N/A",
                        valueColor = when {
                            result.pingMs in 1..150 -> CyberSuccess
                            result.pingMs in 151..300 -> CyberWarning
                            else -> CyberTextPrimary
                        },
                        isMono = true
                    )

                    DiagnosticDetailRow(
                        label = "Exit Location",
                        value = if (result.country.isNotBlank()) "${result.country} (${result.countryCode})" else "Unknown",
                        valueColor = CyberTextPrimary,
                        isMono = false
                    )

                    DiagnosticDetailRow(
                        label = "ISP Provider",
                        value = result.isp.ifBlank { "Unknown" },
                        valueColor = CyberTextSecondary,
                        isMono = false
                    )
                }

                if (result.message.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = result.message,
                        fontSize = 11.sp,
                        color = CyberTextMuted,
                        lineHeight = 15.sp
                    )
                }

                if (result.debugDetails.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF04060A), RoundedCornerShape(4.dp))
                            .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = result.debugDetails,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberAccent,
                            lineHeight = 14.sp
                        )
                    }
                }
            } else {
                // Idle state before any test
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Verify that your internet traffic is encrypted and exiting via the V2RayOPS server.",
                        fontSize = 12.sp,
                        color = CyberTextSecondary,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "Checks public IP, tunnel ping latency, ISP, and geo-exit point.",
                        fontSize = 11.sp,
                        color = CyberTextMuted
                    )
                }
            }

            if (onViewLogsClick != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onViewLogsClick,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "View Dev Logs",
                            tint = CyberAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "View Dev Logs & Copy",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CyberAccent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticDetailRow(
    label: String,
    value: String,
    valueColor: Color,
    isMono: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = CyberTextMuted
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (isMono) FontFamily.Monospace else FontFamily.Default,
            color = valueColor
        )
    }
}
