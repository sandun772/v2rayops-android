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

@Composable
fun StatusPill(
    connectionState: ConnectionState,
    isSmartHunting: Boolean = false,
    isCyclingAirplane: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionState.isConnected && !isSmartHunting && !isCyclingAirplane
    val isConnecting = isCyclingAirplane || isSmartHunting || connectionState.isConnecting || connectionState is ConnectionState.Disconnecting

    val (bg, border, textColor, text) = when {
        isCyclingAirplane -> Quadruple(
            Color(0xFF240A12),
            Color(0xFF5E1B2C),
            Color(0xFFFB7185),
            "CYCLING AIRPLANE"
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
