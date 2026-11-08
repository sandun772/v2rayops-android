package com.v2rayops.client.ui.dashboard.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2rayops.client.R
import com.v2rayops.client.domain.model.VpnConfig

data class ConfigPackageDetails(
    val drawableRes: Int,
    val isp: String,
    val limit: String
)

fun deriveConfigPackageDetails(config: VpnConfig?): ConfigPackageDetails {
    if (config == null) {
        return ConfigPackageDetails(
            drawableRes = R.drawable.ic_package_bolt,
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
        "payg" in raw || "pay-as-you-go" in raw -> "PAYG (76.36 GB)"
        "unlimited" in raw -> "UNLIMITED"
        else -> "100 GB"
    }

    // Authentic Brand Package Icon (Real Zoom, TikTok, YouTube, Gaming, WhatsApp, Bolt)
    val drawable = when {
        "zoom" in raw || "teams" in raw || "meet" in raw || "conference" in raw || "work" in raw || "learn" in raw ->
            R.drawable.ic_package_zoom
        "tiktok" in raw ->
            R.drawable.ic_package_tiktok
        "youtube" in raw || "video" in raw || "stream" in raw ->
            R.drawable.ic_package_youtube
        "game" in raw || "free fire" in raw || "pubg" in raw || "cod" in raw ->
            R.drawable.ic_package_gaming
        "chat" in raw || "whatsapp" in raw || "social" in raw || "facebook" in raw ->
            R.drawable.ic_package_whatsapp
        else ->
            R.drawable.ic_package_bolt
    }

    return ConfigPackageDetails(
        drawableRes = drawable,
        isp = isp,
        limit = limit
    )
}

@Composable
fun ActiveNodeSummaryCard(
    config: VpnConfig?,
    pingMs: Long? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val meta = remember(config) { deriveConfigPackageDetails(config) }
    val title = config?.displayName?.ifBlank { "Tokyo 03" } ?: "Tokyo 03"
    // User instruction: "dont show the remarks show the config isp and limit in remarks"
    val subtitle = "${meta.isp} • ${meta.limit}"
    val (pingDisplay, pingColor) = when {
        pingMs == null -> "-- ms" to Color(0xFF64748B)
        pingMs <= 0L -> "-- ms" to Color(0xFF64748B)
        pingMs < 100L -> "$pingMs ms" to Color(0xFF22C55E)
        pingMs < 200L -> "$pingMs ms" to Color(0xFFF59E0B)
        else -> "$pingMs ms" to Color(0xFFF97316)
    }

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
            // Real Brand Package Icon Badge (Zoom, TikTok, YouTube, Gaming, WhatsApp)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0E1424))
                    .border(1.dp, Color(0xFF1C2740), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = meta.drawableRes),
                    contentDescription = meta.isp,
                    modifier = Modifier.size(24.dp)
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
                    color = pingColor
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
