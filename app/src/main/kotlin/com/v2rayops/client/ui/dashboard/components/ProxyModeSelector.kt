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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class SpeedOptimizationMode(val label: String, val description: String) {
    BALANCED("Balanced", "Balanced routing"),
    SPEED("Speed", "Max download & upload throughput"),
    STABILITY("Stability", "Ultra-low jitter & latency")
}

// Deprecated alias for backwards compatibility if needed
typealias AppProxyMode = SpeedOptimizationMode

@Composable
fun ProxyModeSelector(
    selectedMode: SpeedOptimizationMode,
    onModeSelected: (SpeedOptimizationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Label: SPEED & OPTIMIZATION MODE
        Text(
            text = "SPEED & OPTIMIZATION MODE",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Segmented Control Capsule (Balanced | Speed | Stability)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF090D1A))
                .border(1.dp, Color(0xFF141C30), RoundedCornerShape(10.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpeedOptimizationMode.entries.forEach { mode ->
                val isSelected = mode == selectedMode
                val modeColor = when (mode) {
                    SpeedOptimizationMode.SPEED -> Color(0xFF6366F1)
                    SpeedOptimizationMode.STABILITY -> Color(0xFF10B981)
                    SpeedOptimizationMode.BALANCED -> Color(0xFF8B5CF6)
                }
                val bg by animateColorAsState(
                    targetValue = if (isSelected) modeColor else Color.Transparent,
                    label = "ModeBg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color(0xFF64748B),
                    label = "ModeTextColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onModeSelected(mode) }
                        )
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }
    }
}
