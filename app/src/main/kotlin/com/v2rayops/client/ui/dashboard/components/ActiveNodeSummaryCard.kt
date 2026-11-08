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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2rayops.client.domain.model.VpnConfig

@Composable
fun ActiveNodeSummaryCard(
    config: VpnConfig?,
    pingMs: Long = 48L,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val countryCode = deriveCountryCode(config)
    val title = config?.displayName?.ifBlank { "Tokyo 03" } ?: "Tokyo 03"
    val subtitle = deriveSubtitle(config)
    val pingDisplay = if (pingMs > 0) "$pingMs ms" else "48 ms"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0B0F1E))
            .border(1.dp, Color(0xFF151E34), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Country Squircle Badge [JP]
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF12192D))
                    .border(1.dp, Color(0xFF1A243D), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = countryCode,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Center Column: Title + Protocol Specs
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Latency + Chevron
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pingDisplay,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF22C55E)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Details",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

fun deriveCountryCode(config: VpnConfig?): String {
    if (config != null && config.serverCountry.isNotBlank()) {
        return config.serverCountry.uppercase().take(2)
    }
    val name = (config?.displayName ?: "").lowercase()
    return when {
        "tokyo" in name || "japan" in name || "jp" in name -> "JP"
        "singapore" in name || "sg" in name -> "SG"
        "frankfurt" in name || "germany" in name || "de" in name -> "DE"
        "angeles" in name || "us" in name || "america" in name -> "US"
        "amsterdam" in name || "nl" in name -> "NL"
        "hong kong" in name || "hk" in name -> "HK"
        "sri lanka" in name || "dialog" in name || "mobitel" in name || "lk" in name -> "LK"
        else -> "JP"
    }
}

fun deriveSubtitle(config: VpnConfig?): String {
    if (config == null) return "VLESS • TCP • Reality"
    val proto = config.protocol.ifBlank { "VLESS" }.uppercase()
    val net = if (config.wsPath.isNotBlank()) "WS" else "TCP"
    val sec = if (config.configUri.contains("security=reality", ignoreCase = true)) "Reality"
              else if (config.configUri.contains("security=tls", ignoreCase = true)) "TLS"
              else "Reality"
    return "$proto • $net • $sec"
}
