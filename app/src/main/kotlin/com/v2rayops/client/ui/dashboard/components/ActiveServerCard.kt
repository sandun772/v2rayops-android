package com.v2rayops.client.ui.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import com.v2rayops.client.designsystem.theme.CyberAccent
import com.v2rayops.client.designsystem.theme.CyberBorder
import com.v2rayops.client.designsystem.theme.CyberCard
import com.v2rayops.client.designsystem.theme.CyberTextMuted
import com.v2rayops.client.designsystem.theme.CyberTextPrimary
import com.v2rayops.client.domain.model.VpnConfig

@Composable
fun ActiveServerCard(
    config: VpnConfig?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CyberCard(
        modifier = modifier,
        backgroundColor = CyberCard,
        borderColor = CyberBorder,
        padding = 16.dp,
        onClick = onClick
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
                    imageVector = Icons.Default.VpnKey,
                    contentDescription = "Active Config",
                    tint = CyberAccent,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = config?.displayName ?: "No Config Selected",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyberTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (config != null) {
                            "${config.displayEndpoint} • ${config.protocol.uppercase()}"
                        } else {
                            "Tap to select active package configuration"
                        },
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (config != null) {
                    StatusBadge(
                        text = config.serverCountry.ifBlank { "ACTIVE" },
                        status = StatusIndicatorType.CONNECTED
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Select",
                    tint = CyberTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
