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
    val limit: String,
    val packageName: String = "Universal",
    val displayRemark: String = "Universal - 100GB",
    val expiryFormatted: String = "Never"
)

fun formatConfigExpiry(rawExpiry: String?): String {
    if (rawExpiry.isNullOrBlank()) return "Never"
    val dateStr = rawExpiry.trim().take(10)
    return try {
        val parts = dateStr.split("-")
        if (parts.size == 3) {
            val months = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val monthIdx = parts[1].toIntOrNull() ?: 0
            val monthName = if (monthIdx in 1..12) months[monthIdx] else parts[1]
            "${parts[2]} $monthName ${parts[0]}"
        } else dateStr
    } catch (_: Exception) {
        dateStr
    }
}

fun deriveConfigPackageDetails(config: VpnConfig?): ConfigPackageDetails {
    if (config == null) {
        return ConfigPackageDetails(
            drawableRes = R.drawable.ic_package_bolt,
            isp = "SELECT ISP",
            limit = "100GB",
            packageName = "Universal",
            displayRemark = "Universal - 100GB",
            expiryFormatted = "Never"
        )
    }

    val raw = (config.displayName + " " + config.remark + " " + config.configName).lowercase()

    // 1. Detect ISP
    val isp = when {
        "mobitel" in raw -> "MOBITEL"
        "dialog" in raw -> "DIALOG"
        "hutch" in raw -> "HUTCH"
        "airtel" in raw -> "AIRTEL"
        "slt" in raw -> "SLT"
        config.serverName.isNotBlank() -> config.serverName.uppercase()
        else -> "V2RAYOPS"
    }

    // 2. Extract Quota / Limit (e.g. 100GB, 50GB, Unlimited)
    val gbRegex = Regex("""(\d+(\.\d+)?)\s*(gb|mb)""", RegexOption.IGNORE_CASE)
    val match = gbRegex.find(config.displayName) ?: gbRegex.find(config.remark) ?: gbRegex.find(config.configName)
    val limit = when {
        "unlimited" in raw -> "Unlimited"
        "payg" in raw || "pay-as-you-go" in raw -> "PAYG"
        match != null -> {
            val num = match.groupValues[1]
            val unit = match.groupValues[3].uppercase()
            "$num$unit"
        }
        else -> "100GB"
    }

    // 3. Package Name (TikTok, Zoom, YouTube, Gaming, WhatsApp, Social, etc.)
    val pkgName = when {
        "tiktok" in raw -> "TikTok"
        "zoom" in raw -> "Zoom"
        "teams" in raw -> "Teams"
        "youtube" in raw || "video" in raw || "stream" in raw -> "YouTube"
        "game" in raw || "free fire" in raw || "pubg" in raw || "cod" in raw -> "Gaming"
        "chat" in raw || "whatsapp" in raw -> "WhatsApp"
        "social" in raw || "facebook" in raw -> "Social"
        "work" in raw || "learn" in raw -> "Work & Learn"
        "conference" in raw -> "Conference"
        else -> {
            val parts = config.displayName.split("-").map { it.trim() }
            if (parts.size >= 2) {
                parts[1].substringBefore("(").substringBefore("#").trim().ifBlank { "Universal" }
            } else {
                config.displayName.substringBefore("(").substringBefore("#").trim().ifBlank { "Universal" }
            }
        }
    }

    // 4. Authentic Brand Package Icon (Real Zoom, TikTok, YouTube, Gaming, WhatsApp, Bolt)
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

    val displayRemark = "$pkgName - $limit"
    val expiryFormatted = formatConfigExpiry(config.expiresAt)

    return ConfigPackageDetails(
        drawableRes = drawable,
        isp = isp,
        limit = limit,
        packageName = pkgName,
        displayRemark = displayRemark,
        expiryFormatted = expiryFormatted
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
    val title = if (config != null) meta.displayRemark else "Tokyo 03"
    val subtitle = if (config != null) "${meta.isp} • Exp: ${meta.expiryFormatted}" else "${meta.isp} • ${meta.limit}"
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

            // Center Column: Title (Package - Limit) + Subtitle (ISP • Exp: Date)
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

            // Ping Pill Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0D1426))
                    .border(1.dp, Color(0xFF16213D), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = pingDisplay,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = pingColor
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF475569),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
