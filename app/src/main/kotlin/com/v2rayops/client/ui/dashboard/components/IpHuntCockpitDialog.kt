package com.v2rayops.client.ui.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.v2rayops.client.network.iphunt.IpHuntState

private val PurplePrimary = Color(0xFF6366F1)
private val PurpleLavender = Color(0xFFA5B4FC)
private val DarkCardBg = Color(0xFF090D1A)
private val DarkInsetBg = Color(0xFF050812)
private val BorderDark = Color(0xFF1E283E)
private val GreenAccent = Color(0xFF22C55E)
private val GreenText = Color(0xFF4ADE80)
private val AmberAccent = Color(0xFFF59E0B)
private val AmberText = Color(0xFFFBBF24)

/**
 * IP Hunt Cockpit Dialog:
 * Triggered by pressing and holding the center Connect button dial.
 * Fully automates cellular IP acquisition for Mobitel/carrier zero-rated packages by
 * automatically cycling airplane mode / mobile data radio in a background loop until
 * the assigned IP matches the target subnet (e.g. 100.*), then auto-connects the VPN.
 */
@Composable
fun IpHuntCockpitDialog(
    state: IpHuntState,
    onStartHunt: (String) -> Unit,
    onStopHunt: () -> Unit,
    onSetTargetPrefix: (String) -> Unit,
    onOpenAirplaneSettings: () -> Unit,
    onConnectNow: () -> Unit,
    onDismiss: () -> Unit
) {
    var isCustomSelected by remember { mutableStateOf(false) }
    var customPrefixText by remember { mutableStateOf(state.selectedTargetPrefix) }

    // Start automated hunt immediately upon cockpit opening
    LaunchedEffect(Unit) {
        if (!state.isMatched && !state.isHunting) {
            onStartHunt(state.selectedTargetPrefix)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ipHuntPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(18.dp))
                .background(DarkCardBg)
                .border(
                    width = 1.2.dp,
                    color = when {
                        state.isMatched -> GreenAccent
                        state.isHunting -> PurplePrimary.copy(alpha = 0.75f)
                        else -> BorderDark
                    },
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (state.isMatched) Color(0xFF0E3820) else Color(0xFF1E1B4B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (state.isMatched) Icons.Filled.CheckCircle else Icons.Filled.Speed,
                                contentDescription = null,
                                tint = if (state.isMatched) GreenText else PurpleLavender,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AUTOMATED IP HUNTER",
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "MOBITEL / CARRIER IP LOCK",
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // 2. Target IP Subnet Selector Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "TARGET CARRIER SUBNET",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = PurpleLavender,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val targets = listOf("100.", "10.", "100.64.")
                        targets.forEach { target ->
                            val isSelected = !isCustomSelected && state.selectedTargetPrefix == target
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF1E1B4B) else DarkInsetBg)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) PurplePrimary else BorderDark,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        isCustomSelected = false
                                        onSetTargetPrefix(target)
                                        onStartHunt(target)
                                    }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$target*",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Custom chip
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCustomSelected) Color(0xFF1E1B4B) else DarkInsetBg)
                                .border(
                                    width = 1.dp,
                                    color = if (isCustomSelected) PurplePrimary else BorderDark,
                                    shape = RoundedCornerShape(8.dp)
                                    )
                                .clickable {
                                    isCustomSelected = true
                                }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "CUSTOM",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCustomSelected) Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }

                    // If custom is selected, show input field
                    if (isCustomSelected) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkInsetBg)
                                .border(1.dp, PurplePrimary.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = customPrefixText,
                                onValueChange = {
                                    customPrefixText = it
                                    onSetTargetPrefix(it)
                                    onStartHunt(it)
                                },
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                cursorBrush = SolidColor(PurpleLavender),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    if (customPrefixText.isEmpty()) {
                                        Text(
                                            text = "e.g. 100.74 or 100.82",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }
                }

                // 3. Hero Live Cellular IP Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkInsetBg)
                        .border(
                            width = 1.dp,
                            color = when {
                                state.isMatched -> GreenAccent
                                state.isHunting -> AmberAccent
                                else -> BorderDark
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CURRENT MOBILE IP",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )

                            // Pulsing radar indicator
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                state.isMatched -> GreenAccent
                                                state.isHunting -> AmberAccent.copy(alpha = pulseAlpha)
                                                else -> Color(0xFF475569)
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = when {
                                        state.isMatched -> "LOCKED"
                                        state.isCyclingNow -> "AUTO-CYCLING"
                                        state.isHunting -> "HUNTING"
                                        else -> "IDLE"
                                    },
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        state.isMatched -> GreenText
                                        state.isCyclingNow -> AmberText
                                        state.isHunting -> AmberText
                                        else -> Color(0xFF64748B)
                                    }
                                )
                            }
                        }

                        // Big Monospace IP Text
                        val displayIp = when {
                            state.isCyclingNow -> "CYCLING RADIO..."
                            state.currentIp != null -> state.currentIp
                            else -> "NEGOTIATING IP..."
                        }
                        Text(
                            text = displayIp,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            color = when {
                                state.isMatched -> GreenText
                                state.isCyclingNow -> AmberText
                                state.currentIp != null -> Color.White
                                else -> Color(0xFF64748B)
                            }
                        )

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        state.isMatched -> Color(0xFF0E3820)
                                        state.isCyclingNow -> Color(0xFF332008)
                                        state.isHunting -> Color(0xFF1E1B4B)
                                        else -> Color(0xFF161F30)
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = state.statusMessage.uppercase(),
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    state.isMatched -> GreenText
                                    state.isCyclingNow -> AmberText
                                    state.isHunting -> PurpleLavender
                                    else -> Color(0xFF94A3B8)
                                }
                            )
                        }
                    }
                }

                // 4. Automation Privilege Indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkInsetBg)
                        .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (state.isRootGranted) Icons.Filled.Security else Icons.Filled.AirplanemodeActive,
                            contentDescription = null,
                            tint = if (state.isRootGranted) GreenText else AmberText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (state.isRootGranted) "ZERO-TOUCH AUTO CYCLING ACTIVE" else "ROOT PERMISSION REQUIRED",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (state.isRootGranted) Color.White else AmberText
                            )
                            Text(
                                text = if (state.isRootGranted)
                                    "Cycles airplane mode in background without manual taps"
                                else
                                    "Tap settings to toggle airplane mode if root unavailable",
                                fontSize = 7.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    if (!state.isRootGranted) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E283E))
                                .clickable { onOpenAirplaneSettings() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "SETTINGS",
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = PurpleLavender
                            )
                        }
                    }
                }

                // 5. Action Controls
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.isMatched) {
                        // Success Connect Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF16A34A))
                                .clickable {
                                    onConnectNow()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CONNECT VPN (TARGET LOCKED)",
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    } else if (state.isHunting) {
                        // Stop Auto-Hunt Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E1B4B))
                                .border(1.dp, PurplePrimary, RoundedCornerShape(10.dp))
                                .clickable {
                                    onStopHunt()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(15.dp),
                                    color = PurpleLavender,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "STOP AUTO-HUNT (CYCLE #${state.attempts})",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleLavender
                                )
                            }
                        }
                    } else {
                        // Start Auto-Hunt Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PurplePrimary)
                                .clickable {
                                    onStartHunt(state.selectedTargetPrefix)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "START AUTO-HUNT",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Cancel & Close Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF141A2E))
                            .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                            .clickable {
                                onStopHunt()
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CLOSE IP HUNTER",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}
