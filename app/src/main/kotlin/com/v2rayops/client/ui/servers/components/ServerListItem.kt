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
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2rayops.client.designsystem.components.CyberCard
import com.v2rayops.client.designsystem.components.StatusBadge
import com.v2rayops.client.designsystem.components.StatusIndicatorType
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.IconButton
import com.v2rayops.client.designsystem.theme.CyberDanger
import com.v2rayops.client.designsystem.theme.CyberAccent
import com.v2rayops.client.designsystem.theme.CyberBorder
import com.v2rayops.client.designsystem.theme.CyberCard
import com.v2rayops.client.designsystem.theme.CyberTextMuted
import com.v2rayops.client.designsystem.theme.CyberTextPrimary
import com.v2rayops.client.domain.model.VpnConfig

@Composable
fun ServerListItem(
    config: VpnConfig,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)? = null,
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
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.VpnKey,
                    contentDescription = "Config",
                    tint = if (isSelected) CyberAccent else CyberTextMuted,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = config.displayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyberTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "ADDR: ${config.effectiveAddr} • SNI: ${config.effectiveSni}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (config.isAddrFronting) CyberAccent else CyberTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${config.protocol.uppercase()} • ${if (config.isAddrFronting) "ADDR FRONTING" else "STANDARD SNI"}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!config.expiresAt.isNullOrBlank()) {
                        Text(
                            text = "Expires: ${config.expiresAt.take(10)}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberAccent.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            StatusBadge(
                text = if (isSelected) "SELECTED" else config.serverCountry.ifBlank { "ACTIVE" },
                status = if (isSelected) StatusIndicatorType.CONNECTED else StatusIndicatorType.DISCONNECTED
            )

            if (onDelete != null) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete config",
                        tint = CyberTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
