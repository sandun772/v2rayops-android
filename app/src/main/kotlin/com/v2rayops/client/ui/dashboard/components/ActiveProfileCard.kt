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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2rayops.client.designsystem.theme.CyberAccent
import com.v2rayops.client.designsystem.theme.CyberBorder
import com.v2rayops.client.designsystem.theme.CyberBorderSubtle
import com.v2rayops.client.designsystem.theme.CyberPrimary
import com.v2rayops.client.designsystem.theme.CyberSuccess
import com.v2rayops.client.designsystem.theme.CyberTextMuted
import com.v2rayops.client.designsystem.theme.CyberTextPrimary
import com.v2rayops.client.designsystem.theme.CyberTextSecondary
import com.v2rayops.client.domain.model.VpnConfig

data class ConfigCardMeta(
    val isp: String,
    val packageName: String,
    val billingTag: String,
    val routeId: String,
    val protocol: String,
    val icon: ImageVector,
    val iconColor: Color
)

fun extractCardMeta(config: VpnConfig?): ConfigCardMeta {
    if (config == null) {
        return ConfigCardMeta(
            isp = "SELECT ISP",
            packageName = "No configuration selected",
            billingTag = "STANDBY",
            routeId = "#--",
            protocol = "VLESS",
            icon = Icons.Filled.Bolt,
            iconColor = CyberAccent
        )
    }

    val rawName = config.displayName
    val lower = rawName.lowercase()

    val isp = when {
        "mobitel" in lower -> "MOBITEL"
        "dialog" in lower -> "DIALOG"
        "hutch" in lower -> "HUTCH"
        "airtel" in lower -> "AIRTEL"
        "slt" in lower -> "SLT"
        config.serverName.isNotBlank() -> config.serverName.uppercase()
        else -> "V2RAYOPS"
    }

    val (icon, tint) = when {
        "zoom" in lower || "teams" in lower || "meet" in lower || "conference" in lower ->
            Icons.Filled.Videocam to Color(0xFF38BDF8)
        "youtube" in lower || "video" in lower || "tiktok" in lower || "stream" in lower ->
            Icons.Filled.PlayArrow to Color(0xFFF43F5E)
        "game" in lower || "free fire" in lower || "pubg" in lower || "cod" in lower ->
            Icons.Filled.SportsEsports to Color(0xFFA855F7)
        "chat" in lower || "whatsapp" in lower || "social" in lower || "facebook" in lower ->
            Icons.Filled.Forum to Color(0xFF10B981)
        else ->
            Icons.Filled.Bolt to CyberAccent
    }

    val billingTag = if ("payg" in lower || "pay-as-you-go" in lower) "PAY-AS-YOU-GO" else "PRESET"
    val routeId = "#${config.id}"

    return ConfigCardMeta(
        isp = isp,
        packageName = rawName,
        billingTag = billingTag,
        routeId = routeId,
        protocol = config.protocol.uppercase(),
        icon = icon,
        iconColor = tint
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveProfileCard(
    selectedConfig: VpnConfig?,
    configs: List<VpnConfig>,
    onSelectConfig: (VpnConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val meta = remember(selectedConfig) { extractCardMeta(selectedConfig) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Monospace section header: 01 // ACTIVE PROFILE
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "01 // ACTIVE PROFILE",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = CyberAccent
            )
            Text(
                text = "${configs.size} AVAILABLE",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = CyberTextMuted
            )
        }

        // Main Profile Card (Styled after desktop #01 Assigned VPN Configurations)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF090D18))
                .border(1.dp, CyberBorderSubtle, RoundedCornerShape(12.dp))
                .clickable { showSheet = true }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Radio Bullet Indicator (•) matching web client dashboard
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, CyberPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(CyberAccent)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Center Content: ISP + Name + Badges
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = meta.isp,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 0.5.sp,
                            color = CyberTextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "•",
                            fontSize = 12.sp,
                            color = CyberTextMuted
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = meta.protocol,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = meta.packageName,
                        fontSize = 12.sp,
                        color = CyberTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Badges Row: [PAY-AS-YOU-GO] [#136] [PRESET]
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Billing Tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF131A2D))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = meta.billingTag,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF818CF8)
                            )
                        }

                        // Route ID Tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = meta.routeId,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = CyberTextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Change Chevron / Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF111827))
                        .border(1.dp, CyberBorderSubtle, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SWITCH",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextSecondary
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Switch",
                        tint = CyberTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
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
        containerColor = Color(0xFF090D18),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(CyberBorderSubtle)
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
                    text = "ASSIGNED VPN PROFILES",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    color = CyberTextPrimary
                )
                Text(
                    text = "${configs.size} ACTIVE",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
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
                        text = "No configurations available",
                        fontSize = 13.sp,
                        color = CyberTextMuted
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(configs, key = { it.id }) { config ->
                        val isSelected = config.id == selectedConfig?.id
                        val itemMeta = remember(config) { extractCardMeta(config) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectConfig(config) }
                                .background(if (isSelected) Color(0xFF131B2F) else Color(0xFF0C111E))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) CyberPrimary else CyberBorderSubtle,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isSelected) CyberPrimary else CyberTextMuted,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(CyberAccent)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = itemMeta.isp,
                                        fontSize = 14.sp,
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
                                    fontSize = 11.sp,
                                    color = CyberTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Active",
                                    tint = CyberSuccess,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
