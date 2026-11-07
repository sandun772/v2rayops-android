package com.v2rayops.client.ui.optimizer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.v2rayops.client.network.NetworkOptimizationReport

@Composable
fun NetworkOptimizerScreen(
    onBackClick: () -> Unit,
    viewModel: NetworkOptimizerViewModel = hiltViewModel()
) {
    val report by viewModel.report.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val isApplying by viewModel.isApplying.collectAsState()
    val applySuccess by viewModel.applySuccess.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            CyberTopAppBar(
                title = "NETWORK OPTIMIZER",
                onBackClick = onBackClick,
                actions = {
                    IconButton(
                        onClick = { viewModel.runAnalysis() },
                        enabled = !isAnalyzing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Re-analyze",
                            tint = if (isAnalyzing) CyberTextMuted else CyberAccent
                        )
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Analyzing State Banner
                AnimatedVisibility(
                    visible = isAnalyzing,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    CyberCard(
                        backgroundColor = CyberCardElevated,
                        borderColor = CyberAccent,
                        padding = 16.dp
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = CyberAccent
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "ANALYZING CLIENT NETWORK...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberAccent
                                )
                                Text(
                                    text = "Testing PMTU, latency jitter, and DNS resolver race",
                                    fontSize = 11.sp,
                                    color = CyberTextMuted
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                if (report != null) {
                    val rep = report!!

                    // 1. Hero Stability Rating Card
                    HeroRatingCard(rep)

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Latency & Jitter Grid
                    LatencyMetricsCard(rep)

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. DNS Benchmark Race Card
                    DnsRaceCard(rep)

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. Recommended Tuning Card
                    RecommendedTuningCard(rep)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Applied Success Notification
                    if (applySuccess || rep.isAlreadyOptimized) {
                        CyberCard(
                            backgroundColor = CyberCard,
                            borderColor = CyberSuccess.copy(alpha = 0.6f),
                            padding = 12.dp
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Applied",
                                    tint = CyberSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "CONFIGURATION IS FULLY OPTIMIZED FOR THIS NETWORK",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberSuccess
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Action Buttons
                    Button(
                        onClick = { viewModel.applyOptimization() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isApplying && !isAnalyzing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (rep.isAlreadyOptimized) CyberCardElevated else CyberPrimary
                        )
                    ) {
                        if (isApplying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "APPLYING CONFIGURATION...", fontSize = 12.sp, color = Color.White)
                        } else {
                            Icon(
                                imageVector = if (rep.isAlreadyOptimized) Icons.Default.Check else Icons.Default.Tune,
                                contentDescription = "Apply",
                                tint = if (rep.isAlreadyOptimized) CyberSuccess else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (rep.isAlreadyOptimized) "APPLIED & ACTIVE ✓" else "APPLY OPTIMAL CONFIGURATION",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (rep.isAlreadyOptimized) CyberSuccess else Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { viewModel.runAnalysis() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isAnalyzing,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorderSubtle)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Re-run",
                            tint = CyberTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RE-RUN NETWORK BENCHMARK",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}

@Composable
private fun HeroRatingCard(report: NetworkOptimizationReport) {
    val gradeColor = when (report.stabilityGrade) {
        "A+", "A" -> CyberSuccess
        "B" -> CyberWarning
        else -> CyberDanger
    }

    CyberCard(
        backgroundColor = CyberCard,
        borderColor = gradeColor.copy(alpha = 0.5f),
        padding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CellTower,
                        contentDescription = "Carrier",
                        tint = CyberAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = report.carrierName.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberAccent
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${report.networkType} · ${if (report.isMetered) "Metered Mobile" else "Unmetered"}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyberTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = report.stabilityDescription,
                    fontSize = 11.sp,
                    color = CyberTextMuted,
                    lineHeight = 15.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(gradeColor.copy(alpha = 0.15f))
                    .border(2.dp, gradeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = report.stabilityGrade,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = gradeColor
                )
            }
        }
    }
}

@Composable
private fun LatencyMetricsCard(report: NetworkOptimizationReport) {
    CyberCard(padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = "Latency",
                tint = CyberAccent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "NETWORK LATENCY & JITTER METRICS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = CyberTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricItem(
                label = "AVG PING",
                value = "${report.avgPingMs}ms",
                color = if (report.avgPingMs < 250) CyberSuccess else CyberWarning,
                modifier = Modifier.weight(1f)
            )
            MetricItem(
                label = "JITTER",
                value = "±${report.jitterMs}ms",
                color = if (report.jitterMs < 50) CyberSuccess else CyberDanger,
                modifier = Modifier.weight(1f)
            )
            MetricItem(
                label = "MIN PING",
                value = "${report.minPingMs}ms",
                color = CyberAccent,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFF060810), RoundedCornerShape(6.dp))
            .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = CyberTextMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = color
            )
        }
    }
}

@Composable
private fun DnsRaceCard(report: NetworkOptimizationReport) {
    CyberCard(padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Dns,
                contentDescription = "DNS Race",
                tint = CyberAccent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "DNS RESOLVER SPEED RACE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = CyberTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        report.dnsResults.forEach { dns ->
            val isFastest = dns.serverIp == report.optimalDns
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = dns.providerName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isFastest) CyberSuccess else CyberTextPrimary
                        )
                        if (isFastest) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(CyberSuccess.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "FASTEST",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberSuccess
                                )
                            }
                        }
                    }
                    Text(
                        text = dns.serverIp,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextMuted
                    )
                }

                Text(
                    text = if (dns.isReachable) "${dns.latencyMs}ms" else "TIMEOUT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (dns.isReachable) (if (isFastest) CyberSuccess else CyberTextSecondary) else CyberDanger
                )
            }
        }
    }
}

@Composable
private fun RecommendedTuningCard(report: NetworkOptimizationReport) {
    CyberCard(padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Memory,
                contentDescription = "Tuning",
                tint = CyberAccent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AUTO-COMPUTED XRAY PARAMETERS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = CyberTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        TuningRow(
            label = "Optimized TUN MTU",
            value = "${report.optimalMtu} Bytes",
            annotation = if (report.optimalMtu <= 1400) "Cellular GTP Anti-Fragmentation" else "Standard Broadband"
        )

        HorizontalDivider(color = CyberBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

        TuningRow(
            label = "TCP Buffer Backpressure",
            value = "${report.optimalBufferSizeKb} KB",
            annotation = "Smooths queue to prevent app timeouts"
        )

        HorizontalDivider(color = CyberBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

        TuningRow(
            label = "Primary DNS Route",
            value = "${report.optimalDns} (${report.optimalDnsProvider})",
            annotation = "Lowest lookup latency for current carrier"
        )

        HorizontalDivider(color = CyberBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

        TuningRow(
            label = "Smart QoS Anti-Bufferbloat",
            value = "ACTIVE (Level 0 Priority)",
            annotation = "Bypasses bulk downloads for messaging & calls"
        )
    }
}

@Composable
private fun TuningRow(
    label: String,
    value: String,
    annotation: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyberTextPrimary
            )
            Text(
                text = annotation,
                fontSize = 10.sp,
                color = CyberTextMuted
            )
        }
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = CyberAccent
        )
    }
}
