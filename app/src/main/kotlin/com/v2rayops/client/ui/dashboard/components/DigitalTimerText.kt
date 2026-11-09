package com.v2rayops.client.ui.dashboard.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.v2rayops.client.domain.model.ConnectionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

@Composable
fun DigitalTimerText(
    connectionState: ConnectionState,
    countdownSeconds: Int = 0,
    modifier: Modifier = Modifier
) {
    var timerDisplay by remember { mutableStateOf("00:00:00") }

    LaunchedEffect(connectionState, countdownSeconds) {
        if (countdownSeconds > 0) {
            timerDisplay = String.format(Locale.US, "00:00:%02d", countdownSeconds)
        } else if (connectionState is ConnectionState.Connected) {
            val startMs = connectionState.connectedSinceMs
            while (isActive) {
                val elapsedSec = ((System.currentTimeMillis() - startMs).coerceAtLeast(0L)) / 1000L
                val hours = elapsedSec / 3600
                val minutes = (elapsedSec % 3600) / 60
                val seconds = elapsedSec % 60
                timerDisplay = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
                delay(1000L)
            }
        } else {
            timerDisplay = "00:00:00"
        }
    }

    Text(
        text = timerDisplay,
        fontSize = 38.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 2.sp,
        color = Color.White,
        modifier = modifier
    )
}
