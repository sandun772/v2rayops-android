package com.v2rayops.client.ui.dashboard.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2rayops.client.designsystem.theme.CyberAccent
import com.v2rayops.client.designsystem.theme.CyberBorder
import com.v2rayops.client.designsystem.theme.CyberCard
import com.v2rayops.client.designsystem.theme.CyberDanger
import com.v2rayops.client.designsystem.theme.CyberPrimary
import com.v2rayops.client.designsystem.theme.CyberSuccess
import com.v2rayops.client.designsystem.theme.CyberTextMuted
import com.v2rayops.client.designsystem.theme.CyberTextPrimary
import com.v2rayops.client.designsystem.theme.CyberWarning
import com.v2rayops.client.domain.model.ConnectionState
import com.v2rayops.client.domain.model.TrafficStats

@Composable
fun TopStatsRibbon(
    connectionState: ConnectionState,
    trafficStats: TrafficStats,
    pingMs: Long = 0,
    modifier: Modifier = Modifier
) {
    val durationText = when (connectionState) {
        is ConnectionState.Connected -> connectionState.durationFormatted
        else -> "--:--"
    }

    val downSpeedText = if (connectionState.isConnected) {
        formatTrafficRate(trafficStats.downloadBytesPerSec)
    } else {
        "0 KB/s"
    }

    val upSpeedText = if (connectionState.isConnected) {
        formatTrafficRate(trafficStats.uploadBytesPerSec)
    } else {
        "0 KB/s"
    }

    val pingText = if (pingMs > 0) {
        "$pingMs ms"
    } else {
        "-- ms"
    }

    val pingDotColor = when {
        pingMs <= 0 -> CyberTextMuted
        pingMs < 100 -> CyberSuccess
        pingMs < 250 -> CyberWarning
        else -> CyberDanger
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xE60C101B))
            .border(1.dp, CyberBorder.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Connection Duration
            StatColumn(
                icon = Icons.Filled.Schedule,
                iconColor = CyberTextMuted,
                label = "TIME",
                value = durationText,
                modifier = Modifier.weight(1.1f)
            )

            StatDivider()

            // 2. Download Speed
            StatColumn(
                icon = Icons.Filled.ArrowDownward,
                iconColor = CyberAccent,
                label = "DOWN",
                value = downSpeedText,
                modifier = Modifier.weight(1f)
            )

            StatDivider()

            // 3. Upload Speed
            StatColumn(
                icon = Icons.Filled.ArrowUpward,
                iconColor = CyberPrimary,
                label = "UP",
                value = upSpeedText,
                modifier = Modifier.weight(1f)
            )

            StatDivider()

            // 4. Ping Latency
            StatColumn(
                icon = Icons.Filled.Speed,
                iconColor = pingDotColor,
                label = "PING",
                value = pingText,
                modifier = Modifier.weight(0.9f)
            )
        }
    }
}

@Composable
private fun StatColumn(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = CyberTextMuted
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = CyberTextPrimary,
            maxLines = 1
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .height(24.dp)
            .width(1.dp)
            .background(CyberBorder.copy(alpha = 0.6f))
    )
}

private fun formatTrafficRate(bytesPerSec: Long): String {
    if (bytesPerSec <= 0) return "0 KB/s"
    val kb = bytesPerSec / 1024.0
    if (kb < 1000) {
        return "%.0f KB/s".format(kb)
    }
    val mb = kb / 1024.0
    return "%.1f MB/s".format(mb)
}
