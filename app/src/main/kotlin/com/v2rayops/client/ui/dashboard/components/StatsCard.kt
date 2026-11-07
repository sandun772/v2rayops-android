package com.v2rayops.client.ui.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2rayops.client.designsystem.components.CyberCard
import com.v2rayops.client.designsystem.components.SpeedIndicator
import com.v2rayops.client.designsystem.theme.CyberAccent
import com.v2rayops.client.designsystem.theme.CyberBorder
import com.v2rayops.client.designsystem.theme.CyberCard
import com.v2rayops.client.designsystem.theme.CyberTextMuted
import com.v2rayops.client.designsystem.theme.CyberTextPrimary
import com.v2rayops.client.domain.model.TrafficStats
import com.v2rayops.client.domain.model.UserProfile

@Composable
fun StatsCard(
    stats: TrafficStats,
    profile: UserProfile?,
    modifier: Modifier = Modifier
) {
    CyberCard(
        modifier = modifier,
        backgroundColor = CyberCard,
        borderColor = CyberBorder,
        padding = 18.dp
    ) {
        // Real-time Traffic Speeds
        SpeedIndicator(
            downloadBytesPerSec = stats.downloadBytesPerSec,
            uploadBytesPerSec = stats.uploadBytesPerSec,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Wallet Balance & Plan Info Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "WALLET BALANCE",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberTextMuted,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = if (profile != null) "LKR %.2f".format(profile.walletBalanceLkr) else "LKR --",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyberAccent
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "ACTIVE PLAN",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberTextMuted,
                    letterSpacing = 0.5.sp
                )
                val plan = profile?.activeSubscriptions?.firstOrNull()?.planName
                    ?: if (profile?.trial?.isExpired == false) "Free Trial" else "PAYG Edge"
                Text(
                    text = plan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyberTextPrimary
                )
            }
        }
    }
}
