package com.v2rayops.client.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2rayops.client.domain.model.ConnectionState
import com.v2rayops.client.domain.model.TrafficStats
import kotlin.math.roundToInt

@Composable
fun SpeedDividedSection(
    connectionState: ConnectionState,
    trafficStats: TrafficStats,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionState.isConnected

    val downMbps: Int = if (isConnected) {
        formatMbps(trafficStats.downloadBytesPerSec)
    } else {
        0
    }

    val upMbps: Int = if (isConnected) {
        formatMbps(trafficStats.uploadBytesPerSec)
    } else {
        0
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Top Thin Divider Line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF141C30))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: DOWNLOAD
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "↓ DOWNLOAD",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = downMbps.toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Mbps",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            // Center Vertical Divider Line
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .width(1.dp)
                    .background(Color(0xFF141C30))
            )

            Spacer(modifier = Modifier.width(20.dp))

            // Right: UPLOAD
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "↑ UPLOAD",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = upMbps.toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Mbps",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }

        // Bottom Thin Divider Line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF141C30))
        )
    }
}

/**
 * Converts raw bytes/sec into an integer megabit/sec (Mbps) value:
 * 1 Byte = 8 bits, 1 Mbps = 1,000,000 bits/sec
 */
private fun formatMbps(bytesPerSec: Long): Int {
    if (bytesPerSec <= 0L) return 0
    val bitsPerSec = bytesPerSec * 8.0
    val mbps = bitsPerSec / 1_000_000.0
    return mbps.roundToInt().coerceAtLeast(0)
}
