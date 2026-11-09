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
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
 * Supports 100% automated background cycling (Root, Accessibility, or Secure Settings)
 * and direct one-tap system permission setup navigation.
 */
@Composable
fun IpHuntCockpitDialog(
    state: IpHuntState,
    onStartHunt: (String) -> Unit,
    onStopHunt: () -> Unit,
    onSetTargetPrefix: (String) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenAppPermissionSettings: () -> Unit,
    onOpenAirplaneSettings: () -> Unit,
    onRefreshCapabilities: () -> Unit,
    onConnectNow: () -> Unit,
    onDismiss: () -> Unit
) {
    var isCustomSelected by remember { mutableStateOf(false) }
    var customPrefixText by remember { mutableStateOf(state.selectedTargetPrefix) }

    // Re-check permissions whenever user returns to the app from system settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefreshCapabilities()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Start hunt immediately if not already active
    LaunchedEffect(Unit) {
        onRefreshCapabilities()
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
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(18.dp))
                .background(DarkCardBg)
                .border(
                    width = 1.2.dp,
                    color = when {
                        state.isMatched -> GreenAccent
                        state.isCyclingNow -> AmberAccent
                        state.isHunting -> PurplePrimary.copy(alpha = 0.75f)
                        else -> BorderDark
                    },
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(13.dp)
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
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = "TARGET CARRIER SUBNET",
                        fontSize = 8.5.sp,
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

                    // Custom input field
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
                                state.isCyclingNow -> AmberAccent
                                state.isHunting -> PurplePrimary.copy(alpha = 0.5f)
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
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )

                            // Status radar dot
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                state.isMatched -> GreenAccent
                                                state.isCyclingNow -> AmberAccent.copy(alpha = pulseAlpha)
                                                state.isHunting -> PurpleLavender.copy(alpha = pulseAlpha)
                                                else -> Color(0xFF475569)
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = when {
                                        state.isMatched -> "LOCKED"
                                        state.isCyclingNow -> "CYCLING RADIO"
                                        state.isHunting -> "HUNTING"
                                        else -> "IDLE"
                                    },
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        state.isMatched -> GreenText
                                        state.isCyclingNow -> AmberText
                                        state.isHunting -> PurpleLavender
                                        else -> Color(0xFF64748B)
                                    }
                                )
                            }
                        }

                        // Big Monospace IP
                        val displayIp = when {
                            state.isCyclingNow -> "CYCLING AIRPLANE..."
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

                        // Live status message ticker
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
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    state.isMatched -> GreenText
                                    state.isCyclingNow -> AmberText
                                    state.isHunting -> PurpleLavender
                                    else -> Color(0xFF94A3B8)
                                },
                                maxLines = 2
                            )
                        }
                    }
                }

                // 4. Automation Privilege Status or Setup Action Card
                if (state.isFullyAutomated) {
                    // Fully automated active badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkInsetBg)
                            .border(1.dp, Color(0xFF1B3D26), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = GreenText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            val methodText = when {
                                state.isRootGranted -> "ROOT ZERO-TOUCH ACTIVE"
                                state.isAccessibilityActive -> "ACCESSIBILITY AUTO-PILOT ACTIVE"
                                else -> "SECURE SETTINGS AUTOMATION ACTIVE"
                            }
                            Text(
                                text = methodText,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Airplane Mode cycles automatically in background until IP matches",
                                fontSize = 7.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                } else {
                    // Setup action card for unprivileged / non-root devices
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF10162A))
                            .border(1.dp, Color(0xFF263353), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Engineering,
                                contentDescription = null,
                                tint = AmberText,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AUTOMATION PERMISSION REQUIRED",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = AmberText
                            )
                        }

                        Text(
                            text = "To allow the app to cycle airplane mode automatically without manual tapping, enable Auto-Pilot in Accessibility:",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF94A3B8),
                            lineHeight = 11.sp
                        )

                        // Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Primary: Enable Auto-Pilot in Accessibility Settings
                            Box(
                                modifier = Modifier
                                    .weight(1.3f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PurplePrimary)
                                    .clickable { onOpenAccessibilitySettings() }
                                    .padding(vertical = 7.dp, horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "ENABLE AUTO-PILOT",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // Secondary: App Permissions Settings
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E283E))
                                    .clickable { onOpenAppPermissionSettings() }
                                    .padding(vertical = 7.dp, horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "APP INFO",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleLavender
                                )
                            }
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
                    } else if (state.isFullyAutomated && state.isHunting) {
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
                    } else if (state.isFullyAutomated) {
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
                    } else {
                        // Fallback manual toggle button while permissions are pending
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PurplePrimary)
                                .clickable {
                                    onOpenAirplaneSettings()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.AirplanemodeActive,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "TOGGLE AIRPLANE MODE (MANUAL)",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Close Button
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
