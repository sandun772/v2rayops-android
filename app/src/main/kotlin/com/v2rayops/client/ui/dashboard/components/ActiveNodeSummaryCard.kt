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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2rayops.client.domain.model.VpnConfig

data class ConfigPackageDetails(
    val icon: ImageVector,
    val iconTint: Color,
    val isp: String,
    val limit: String
)

fun deriveConfigPackageDetails(config: VpnConfig?): ConfigPackageDetails {
    if (config == null) {
        return ConfigPackageDetails(
            icon = Icons.Filled.Bolt,
            iconTint = Color(0xFF6366F1),
            isp = "SELECT ISP",
            limit = "100 GB"
        )
    }

    val raw = (config.displayName + " " + config.remark).lowercase()

    val isp = when {
        "mobitel" in raw -> "MOBITEL"
        "dialog" in raw -> "DIALOG"
        "hutch" in raw -> "HUTCH"
        "airtel" in raw -> "AIRTEL"
        "slt" in raw -> "SLT"
        config.serverName.isNotBlank() -> config.serverName.uppercase()
        else -> "V2RAYOPS"
    }

    // Extract quota / limit like 100 GB, 50 GB, 76.36 GB or PAYG
    val gbRegex = Regex("""(\d+(\.\d+)?\s*(?:gb|mb))""", RegexOption.IGNORE_CASE)
    val match = gbRegex.find(config.displayName) ?: gbRegex.find(config.remark)
    val limit = when {
        match != null -> match.value.uppercase()
        "payg" in raw || "pay-as-you-go" in raw -> "PAYG"
        "unlimited" in raw -> "UNLIMITED"
        else -> "100 GB"
    }

    // Contextual Package Icon matching package content
    val (icon, tint) = when {
        "zoom" in raw || "teams" in raw || "meet" in raw || "conference" in raw || "work" in raw || "learn" in raw ->
            Icons.Filled.Videocam to Color(0xFF38BDF8) // Cyan for Video conferencing / Education
        "youtube" in raw || "video" in raw || "tiktok" in raw || "stream" in raw ->
            Icons.Filled.PlayArrow to Color(0xFFF43F5E) // Red for Streaming / TikTok
        "game" in raw || "free fire" in raw || "pubg" in raw || "cod" in raw ->
            Icons.Filled.SportsEsports to Color(0xFFA855F7) // Purple for Gaming
        "chat" in raw || "whatsapp" in raw || "social" in raw || "facebook" in raw ->
            Icons.Filled.Forum to Color(0xFF10B981) // Green for Messaging / Social
        else ->
            Icons.Filled.Bolt to Color(0xFF6366F1) // Indigo for General routes
    }

    return ConfigPackageDetails(
        icon = icon,
        iconTint = tint,
        isp = isp,
        limit = limit
    )
}

@Composable
fun ActiveNodeSummaryCard(
    config: VpnConfig?,
    pingMs: Long = 48L,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val meta = remember(config) { deriveConfigPackageDetails(config) }
    val title = config?.displayName?.ifBlank { "Tokyo 03" } ?: "Tokyo 03"
    // User instruction: "dont show the remarks show the config isp and limit in remarks"
    val subtitle = "${meta.isp} • ${meta.limit}"
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
            // Package Icon Badge (Replacing text SG/JP squircle)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(meta.iconTint.copy(alpha = 0.14f))
                    .border(1.dp, meta.iconTint.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = meta.icon,
                    contentDescription = meta.isp,
                    tint = meta.iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Center Column: Title + ISP & Limit Subtitle
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
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF94A3B8),
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
