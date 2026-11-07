package com.v2rayops.client.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2rayops.client.designsystem.theme.CyberAccent
import com.v2rayops.client.designsystem.theme.CyberPrimary
import com.v2rayops.client.designsystem.theme.CyberTextMuted
import com.v2rayops.client.designsystem.theme.CyberTextPrimary

@Composable
fun SpeedIndicator(
    downloadBytesPerSec: Long,
    uploadBytesPerSec: Long,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpeedItem(
            label = "DOWNLOAD",
            bytesPerSec = downloadBytesPerSec,
            icon = Icons.Default.ArrowDownward,
            iconTint = CyberAccent
        )
        Spacer(modifier = Modifier.width(24.dp))
        SpeedItem(
            label = "UPLOAD",
            bytesPerSec = uploadBytesPerSec,
            icon = Icons.Default.ArrowUpward,
            iconTint = CyberPrimary
        )
    }
}

@Composable
private fun SpeedItem(
    label: String,
    bytesPerSec: Long,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                fontSize = 10.sp,
                color = CyberTextMuted,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
            Text(
                text = formatBytesPerSecond(bytesPerSec),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = CyberTextPrimary
            )
        }
    }
}

fun formatBytesPerSecond(bytes: Long): String {
    if (bytes <= 0) return "0.0 KB/s"
    val kb = bytes / 1024.0
    if (kb < 1000) {
        return "%.1f KB/s".format(kb)
    }
    val mb = kb / 1024.0
    return "%.2f MB/s".format(mb)
}
