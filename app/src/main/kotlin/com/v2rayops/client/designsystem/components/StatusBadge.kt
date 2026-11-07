package com.v2rayops.client.designsystem.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2rayops.client.designsystem.theme.CyberDanger
import com.v2rayops.client.designsystem.theme.CyberSuccess
import com.v2rayops.client.designsystem.theme.CyberTextMuted
import com.v2rayops.client.designsystem.theme.CyberTextPrimary
import com.v2rayops.client.designsystem.theme.CyberWarning

enum class StatusIndicatorType {
    CONNECTED,
    CONNECTING,
    DISCONNECTED,
    WARNING,
    ERROR
}

@Composable
fun StatusBadge(
    text: String,
    status: StatusIndicatorType,
    modifier: Modifier = Modifier
) {
    val (dotColor, backgroundColor, borderColor) = when (status) {
        StatusIndicatorType.CONNECTED -> Triple(
            CyberSuccess,
            CyberSuccess.copy(alpha = 0.12f),
            CyberSuccess.copy(alpha = 0.35f)
        )
        StatusIndicatorType.CONNECTING -> Triple(
            CyberWarning,
            CyberWarning.copy(alpha = 0.12f),
            CyberWarning.copy(alpha = 0.35f)
        )
        StatusIndicatorType.DISCONNECTED -> Triple(
            CyberTextMuted,
            CyberTextMuted.copy(alpha = 0.12f),
            CyberTextMuted.copy(alpha = 0.3f)
        )
        StatusIndicatorType.WARNING -> Triple(
            CyberWarning,
            CyberWarning.copy(alpha = 0.12f),
            CyberWarning.copy(alpha = 0.35f)
        )
        StatusIndicatorType.ERROR -> Triple(
            CyberDanger,
            CyberDanger.copy(alpha = 0.12f),
            CyberDanger.copy(alpha = 0.35f)
        )
    }

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(20.dp))
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text.uppercase(),
                color = CyberTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }
    }
}
