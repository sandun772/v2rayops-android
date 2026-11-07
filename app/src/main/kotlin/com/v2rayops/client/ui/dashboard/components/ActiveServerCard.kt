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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.v2rayops.client.designsystem.theme.CyberBorderSubtle
import com.v2rayops.client.designsystem.theme.CyberCard
import com.v2rayops.client.designsystem.theme.CyberCardElevated
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
        Column(modifier = Modifier.fillMaxWidth()) {
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
                                "${config.protocol.uppercase()} • ${if (config.isAddrFronting) "ADDR FRONTING" else "STANDARD SNI"}"
                            } else {
                                "Tap to select active package configuration"
                            },
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (config?.isAddrFronting == true) CyberAccent else CyberTextMuted,
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

            // Explicit Telemetry Badges: Active ADDR & Active SNI
            if (config != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ADDR Badge
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(CyberCardElevated, RoundedCornerShape(4.dp))
                            .border(1.dp, CyberBorderSubtle, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Column {
                            Text(
                                text = "ACTIVE ADDR",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextMuted,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = config.effectiveAddr,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // SNI Badge
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (config.isAddrFronting) CyberAccent.copy(alpha = 0.12f) else CyberCardElevated,
                                RoundedCornerShape(4.dp)
                            )
                            .border(
                                1.dp,
                                if (config.isAddrFronting) CyberAccent.copy(alpha = 0.4f) else CyberBorderSubtle,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Column {
                            Text(
                                text = if (config.isAddrFronting) "FRONT SNI" else "ACTIVE SNI",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (config.isAddrFronting) CyberAccent else CyberTextMuted,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = config.effectiveSni,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (config.isAddrFronting) CyberAccent else CyberTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
