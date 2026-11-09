package com.v2rayops.client.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2rayops.client.domain.model.ConnectionState
import com.v2rayops.client.domain.model.SmartHuntProgress
import com.v2rayops.client.domain.model.SmartHuntStage

@Composable
fun StatusPill(
    connectionState: ConnectionState,
    isSmartHunting: Boolean = false,
    isCyclingAirplane: Boolean = false,
    smartHuntProgress: SmartHuntProgress = SmartHuntProgress(),
    modifier: Modifier = Modifier
) {
    val isConnected = connectionState.isConnected && !smartHuntProgress.isActive && !isSmartHunting
    val isConnecting = smartHuntProgress.isActive || isSmartHunting || connectionState.isConnecting || connectionState is ConnectionState.Disconnecting

    val (bg, border, textColor, text) = when {
        smartHuntProgress.stage == SmartHuntStage.WAITING_AIRPLANE_ON -> Quadruple(
            Color(0xFF240A12),
            Color(0xFF5E1B2C),
            Color(0xFFFB7185),
            "TOGGLE AIRPLANE ON"
        )
        smartHuntProgress.stage == SmartHuntStage.AIRPLANE_ON -> Quadruple(
            Color(0xFF1E1405),
            Color(0xFF4A320A),
            Color(0xFFF59E0B),
            "AIRPLANE ON • TURN OFF"
        )
        smartHuntProgress.stage == SmartHuntStage.ACQUIRING_IP -> Quadruple(
            Color(0xFF041B1C),
            Color(0xFF0B4648),
            Color(0xFF06B6D4),
            "ACQUIRING IP..."
        )
        smartHuntProgress.stage == SmartHuntStage.CONNECTING_VPN -> Quadruple(
            Color(0xFF160B24),
            Color(0xFF3B1D5E),
            Color(0xFFA855F7),
            if (!smartHuntProgress.assignedIp.isNullOrBlank()) "CONNECTING • ${smartHuntProgress.assignedIp}" else "AUTO-CONNECTING"
        )
        smartHuntProgress.stage == SmartHuntStage.VERIFYING_TRAFFIC -> Quadruple(
            Color(0xFF1E1405),
            Color(0xFF4A320A),
            Color(0xFFF59E0B),
            "TESTING DATA FLOW..."
        )
        smartHuntProgress.stage == SmartHuntStage.DEAD_IP_RETRY -> Quadruple(
            Color(0xFF240A12),
            Color(0xFF5E1B2C),
            Color(0xFFFB7185),
            "NO DATA FLOW • TOGGLE ON"
        )
        smartHuntProgress.stage == SmartHuntStage.IP_MISMATCH -> Quadruple(
            Color(0xFF1E1405),
            Color(0xFF4A320A),
            Color(0xFFF59E0B),
            "MISMATCH • TOGGLE ON"
        )
        smartHuntProgress.stage == SmartHuntStage.CHECKING_VPN -> Quadruple(
            Color(0xFF1E1405),
            Color(0xFF4A320A),
            Color(0xFFF59E0B),
            "CHECKING CURRENT IP..."
        )
        isSmartHunting -> Quadruple(
            Color(0xFF1E1405),
            Color(0xFF4A320A),
            Color(0xFFF59E0B),
            "HUNTING IP"
        )
        isConnecting -> Quadruple(
            Color(0xFF1E1405),
            Color(0xFF4A320A),
            Color(0xFFF59E0B),
            "HANDSHAKING"
        )
        isConnected -> Quadruple(
            Color(0xFF061E14),
            Color(0xFF0E432A),
            Color(0xFF22C55E),
            "CONNECTED"
        )
        else -> Quadruple(
            Color(0xFF0F1524),
            Color(0xFF1E283D),
            Color(0xFF6B7A99),
            "DISCONNECTED"
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(textColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = textColor
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
