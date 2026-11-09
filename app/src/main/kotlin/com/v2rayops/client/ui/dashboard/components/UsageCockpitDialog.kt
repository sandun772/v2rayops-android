package com.v2rayops.client.ui.dashboard.components

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
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2rayops.client.designsystem.components.FutureReleaseCard
import com.v2rayops.client.domain.model.ConnectionState
import com.v2rayops.client.domain.model.TrafficStats
import com.v2rayops.client.domain.model.VpnConfig

/**
 * Usage & Cockpit Telemetry Modal
 * Displays a blurred wireframe/scratch of the upcoming telemetry dashboard in the background,
 * with an informative "Future Updates" cyber note card in the foreground.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageCockpitDialog(
    connectionState: ConnectionState = ConnectionState.Disconnected,
    trafficStats: TrafficStats = TrafficStats(),
    selectedConfig: VpnConfig? = null,
    pingMs: Long? = null,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF070B16))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(20.dp))
        ) {
            // Background: Blurred Scratch / Wireframe of Usage Cockpit UI
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .blur(16.dp)
                    .background(Color(0xFF070B16))
                    .padding(20.dp)
            ) {
                UsageScratchBackground()
            }

            // Dark Frosted Glass Scrim Overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0xFF050811).copy(alpha = 0.82f))
            )

            // Foreground: Future Release Note Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Top header with Close (X) icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "02 // TELEMETRY SUITE",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp
                    )

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF111827))
                            .border(1.dp, Color(0xFF1F2937), CircleShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                FutureReleaseCard(
                    badgeText = "COMING IN V1.1.0 UPDATE",
                    title = "USAGE & TELEMETRY COCKPIT",
                    subtitle = "Real-Time Quota & Bandwidth Accounting",
                    description = "Live per-app traffic accounting, real-time ISP quota meters, historical tunnel throughput charts, and subscription wallet ledgers are under active engineering.",
                    icon = Icons.AutoMirrored.Filled.ShowChart,
                    versionTag = "SCHEDULED: V1.1.0 RELEASE",
                    additionalAction = {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "DISMISS",
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                )
            }
        }
    }
}

/**
 * Scratch wireframe sketch of the usage telemetry cockpit
 */
@Composable
private fun UsageScratchBackground() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Wireframe: Circular Gauge Scratch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A))
                    .border(2.dp, Color(0xFF334155), CircleShape)
            )
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Box(modifier = Modifier.size(width = 120.dp, height = 12.dp).background(Color(0xFF1E293B), RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.size(width = 80.dp, height = 8.dp).background(Color(0xFF1E293B), RoundedCornerShape(4.dp)))
            }
        }

        // Wireframe: Throughput Breakdown Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0B1020))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Box(modifier = Modifier.size(width = 40.dp, height = 8.dp).background(Color(0xFF334155), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.size(width = 65.dp, height = 14.dp).background(Color(0xFF475569), RoundedCornerShape(2.dp)))
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0B1020))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Box(modifier = Modifier.size(width = 40.dp, height = 8.dp).background(Color(0xFF334155), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.size(width = 65.dp, height = 14.dp).background(Color(0xFF475569), RoundedCornerShape(2.dp)))
                }
            }
        }

        // Wireframe: Session & Quota Meters
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0B1020))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.size(width = 90.dp, height = 10.dp).background(Color(0xFF334155), RoundedCornerShape(2.dp)))
                Box(modifier = Modifier.size(width = 50.dp, height = 10.dp).background(Color(0xFF475569), RoundedCornerShape(2.dp)))
            }
        }
    }
}
