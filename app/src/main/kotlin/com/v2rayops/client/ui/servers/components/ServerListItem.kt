package com.v2rayops.client.ui.servers.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2rayops.client.designsystem.components.CyberCard
import com.v2rayops.client.designsystem.components.StatusBadge
import com.v2rayops.client.designsystem.components.StatusIndicatorType
import com.v2rayops.client.designsystem.theme.CyberAccent
import com.v2rayops.client.designsystem.theme.CyberBorder
import com.v2rayops.client.designsystem.theme.CyberCard
import com.v2rayops.client.designsystem.theme.CyberTextMuted
import com.v2rayops.client.designsystem.theme.CyberTextPrimary
import com.v2rayops.client.domain.model.ServerNode

@Composable
fun ServerListItem(
    server: ServerNode,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    CyberCard(
        modifier = modifier,
        backgroundColor = if (isSelected) CyberCard.copy(alpha = 0.95f) else CyberCard,
        borderColor = if (isSelected) CyberAccent else CyberBorder,
        borderWidth = if (isSelected) 1.5.dp else 1.dp,
        padding = 16.dp,
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Dns,
                    contentDescription = "Server Node",
                    tint = if (isSelected) CyberAccent else CyberTextMuted,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = server.displayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyberTextPrimary
                    )
                    Text(
                        text = server.displayEndpoint,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextMuted
                    )
                }
            }

            val latency = server.latencyMs
            if (latency != null) {
                StatusBadge(
                    text = "${latency}ms",
                    status = if (latency < 150) StatusIndicatorType.CONNECTED else StatusIndicatorType.WARNING
                )
            } else {
                StatusBadge(
                    text = "STANDBY",
                    status = StatusIndicatorType.DISCONNECTED
                )
            }
        }
    }
}
