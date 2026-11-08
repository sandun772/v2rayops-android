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
import java.util.Locale

@Composable
fun SpeedDividedSection(
    connectionState: ConnectionState,
    trafficStats: TrafficStats,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionState.isConnected

    val (downNum, downUnit) = if (isConnected) {
        splitRate(trafficStats.downloadBytesPerSec)
    } else {
        "0.0" to "MB/s"
    }

    val (upNum, upUnit) = if (isConnected) {
        splitRate(trafficStats.uploadBytesPerSec)
    } else {
        "0.0" to "MB/s"
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
                        text = downNum,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = downUnit,
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
                        text = upNum,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = upUnit,
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

private fun splitRate(bytesPerSec: Long): Pair<String, String> {
    if (bytesPerSec <= 0) return "0.0" to "MB/s"
    val kb = bytesPerSec / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.US, "%.1f", mb) to "MB/s"
    } else {
        String.format(Locale.US, "%.0f", kb) to "KB/s"
    }
}
