package com.v2rayops.client.ui.dashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ClientNavTab {
    FINETUNE,
    USAGE,
    CONNECT,
    TOOLS,
    SETTINGS
}

@Composable
fun ClientBottomNavBar(
    selectedTab: ClientNavTab,
    onTabSelected: (ClientNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF050812))
            .border(width = 1.dp, color = Color(0xFF121A2C))
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Finetune / Diagnose
            StandardNavItem(
                icon = Icons.Filled.Tune,
                label = "Finetune",
                isSelected = selectedTab == ClientNavTab.FINETUNE,
                onClick = { onTabSelected(ClientNavTab.FINETUNE) }
            )

            // 2. Usage
            StandardNavItem(
                icon = Icons.AutoMirrored.Filled.ShowChart,
                label = "Usage",
                isSelected = selectedTab == ClientNavTab.USAGE,
                onClick = { onTabSelected(ClientNavTab.USAGE) }
            )

            // 3. Connect (Centerpiece Hero Button — Elevated & Highlighted)
            CenterConnectHeroItem(
                isSelected = selectedTab == ClientNavTab.CONNECT,
                onClick = { onTabSelected(ClientNavTab.CONNECT) }
            )

            // 4. Tools
            StandardNavItem(
                icon = Icons.Filled.Build,
                label = "Tools",
                isSelected = selectedTab == ClientNavTab.TOOLS,
                onClick = { onTabSelected(ClientNavTab.TOOLS) }
            )

            // 5. Settings
            StandardNavItem(
                icon = Icons.Filled.Settings,
                label = "Settings",
                isSelected = selectedTab == ClientNavTab.SETTINGS,
                onClick = { onTabSelected(ClientNavTab.SETTINGS) }
            )
        }
    }
}

@Composable
private fun CenterConnectHeroItem(
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val buttonBrush = if (isSelected) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF3B82F6), Color(0xFF6366F1))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF1E2D48), Color(0xFF101726))
        )
    }

    val iconColor = if (isSelected) Color.White else Color(0xFF818CF8)
    val labelColor = if (isSelected) Color(0xFF818CF8) else Color(0xFF64748B)

    Column(
        modifier = Modifier
            .offset(y = (-6).dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glowing Elevated Center Disc
        Box(
            modifier = Modifier
                .size(46.dp)
                .shadow(elevation = if (isSelected) 8.dp else 4.dp, shape = CircleShape, spotColor = Color(0xFF6366F1))
                .clip(CircleShape)
                .background(buttonBrush)
                .border(
                    width = 1.5.dp,
                    color = if (isSelected) Color(0xFFA5B4FC) else Color(0xFF2E3D5B),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PowerSettingsNew,
                contentDescription = "Connect",
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Connect",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = labelColor
        )
    }
}

@Composable
private fun StandardNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tint by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF818CF8) else Color(0xFF64748B),
        label = "NavTint"
    )

    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = tint
        )
    }
}
