package com.v2rayops.client.ui.dashboard.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2rayops.client.designsystem.theme.CyberAccent
import com.v2rayops.client.designsystem.theme.CyberBorder
import com.v2rayops.client.designsystem.theme.CyberCard
import com.v2rayops.client.designsystem.theme.CyberCardElevated
import com.v2rayops.client.designsystem.theme.CyberPrimary
import com.v2rayops.client.designsystem.theme.CyberSuccess
import com.v2rayops.client.designsystem.theme.CyberTextMuted
import com.v2rayops.client.designsystem.theme.CyberTextPrimary
import com.v2rayops.client.designsystem.theme.CyberTextSecondary
import com.v2rayops.client.domain.model.VpnConfig

data class ConfigPackageMeta(
    val ispName: String,
    val packageName: String,
    val icon: ImageVector,
    val iconTint: Color,
    val protocol: String
)

fun parseConfigMeta(config: VpnConfig?): ConfigPackageMeta {
    if (config == null) {
        return ConfigPackageMeta(
            ispName = "SELECT ISP",
            packageName = "Tap to choose active configuration",
            icon = Icons.Filled.Bolt,
            iconTint = CyberAccent,
            protocol = "VLESS"
        )
    }

    val rawName = config.displayName
    val parts = rawName.split("-", limit = 2).map { it.trim() }

    val isp: String
    val pkg: String

    if (parts.size >= 2 && parts[0].isNotBlank()) {
        isp = parts[0].uppercase()
        pkg = parts[1]
    } else {
        val lower = rawName.lowercase()
        isp = when {
            "mobitel" in lower -> "MOBITEL"
            "dialog" in lower -> "DIALOG"
            "hutch" in lower -> "HUTCH"
            "airtel" in lower -> "AIRTEL"
            "slt" in lower -> "SLT"
            config.serverName.isNotBlank() -> config.serverName.uppercase()
            else -> "CUSTOM ISP"
        }
        pkg = rawName
    }

    val lowerName = rawName.lowercase()
    val (icon, tint) = when {
        "zoom" in lowerName || "teams" in lowerName || "meet" in lowerName || "conference" in lowerName ->
            Icons.Filled.Videocam to Color(0xFF38BDF8)
        "youtube" in lowerName || "video" in lowerName || "stream" in lowerName || "tiktok" in lowerName ->
            Icons.Filled.PlayArrow to Color(0xFFF43F5E)
        "game" in lowerName || "free fire" in lowerName || "pubg" in lowerName || "cod" in lowerName ->
            Icons.Filled.SportsEsports to Color(0xFFA855F7)
        "chat" in lowerName || "whatsapp" in lowerName || "social" in lowerName || "facebook" in lowerName ->
            Icons.Filled.Forum to Color(0xFF10B981)
        else ->
            Icons.Filled.Bolt to CyberAccent
    }

    return ConfigPackageMeta(
        ispName = isp,
        packageName = pkg,
        icon = icon,
        iconTint = tint,
        protocol = config.protocol.uppercase()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveConfigButton(
    selectedConfig: VpnConfig?,
    configs: List<VpnConfig>,
    onSelectConfig: (VpnConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val meta = remember(selectedConfig) { parseConfigMeta(selectedConfig) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { showSheet = true }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF141C2E),
                        Color(0xFF0C101B)
                    )
                )
            )
            .border(1.dp, CyberBorder.copy(alpha = 0.9f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Package Icon Badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(meta.iconTint.copy(alpha = 0.14f))
                    .border(1.dp, meta.iconTint.copy(alpha = 0.35f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = meta.icon,
                    contentDescription = meta.ispName,
                    tint = meta.iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 2. Primary ISP Name & Package Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = meta.ispName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 0.8.sp,
                        color = CyberTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Protocol pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CyberPrimary.copy(alpha = 0.15f))
                            .border(1.dp, CyberPrimary.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = meta.protocol,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = meta.packageName,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = CyberTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 3. Switch Chevron
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF161F30).copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Switch Configuration",
                    tint = CyberTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showSheet) {
        ConfigSelectionSheet(
            sheetState = sheetState,
            configs = configs,
            selectedConfig = selectedConfig,
            onSelectConfig = { config ->
                onSelectConfig(config)
                showSheet = false
            },
            onDismiss = { showSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigSelectionSheet(
    sheetState: SheetState,
    configs: List<VpnConfig>,
    selectedConfig: VpnConfig?,
    onSelectConfig: (VpnConfig) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0C101B),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(CyberBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SELECT CONFIGURATION",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    color = CyberTextPrimary
                )
                Text(
                    text = "${configs.size} AVAILABLE",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = CyberAccent
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (configs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No configurations found. Syncing with server...",
                        fontSize = 13.sp,
                        color = CyberTextMuted
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(configs, key = { it.id }) { config ->
                        val isSelected = config.id == selectedConfig?.id
                        val itemMeta = remember(config) { parseConfigMeta(config) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectConfig(config) }
                                .background(if (isSelected) Color(0xFF161F32) else Color(0xFF0F1524))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) CyberPrimary else CyberBorder.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(itemMeta.iconTint.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = itemMeta.icon,
                                    contentDescription = null,
                                    tint = itemMeta.iconTint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = itemMeta.ispName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberTextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = itemMeta.protocol,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyberAccent
                                    )
                                }
                                Text(
                                    text = itemMeta.packageName,
                                    fontSize = 12.sp,
                                    color = CyberTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(CyberSuccess),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
