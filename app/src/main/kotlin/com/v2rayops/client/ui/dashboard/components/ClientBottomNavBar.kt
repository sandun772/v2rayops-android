package com.v2rayops.client.ui.dashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2rayops.client.domain.model.ConnectionState

enum class ClientNavTab {
    FINETUNE,
    USAGE,
    CONNECT,
    TOOLS,
    SETTINGS
}

/**
 * Ultra-minimalist cyber cockpit bottom navigation bar:
 * - Narrow, smooth cubic curved cradle contour along the top edge around the middle connect button
 * - Minimalist obsidian Connect disc with dynamic visual states mirroring the main connect button:
 *     * Connected: Luminous Electric Indigo border (#6366F1), periwinkle icon, "Stop" label, ambient glow
 *     * Connecting: Amber Gold border (#F59E0B), amber icon, "Cancel" label
 *     * Disconnected: Deep steel slate border (#232E44), slate icon, "Connect" label
 * - When on Home and tapped again: directly triggers connect/disconnect toggle
 * - Clean, low-noise aesthetic with micro-typography and subtle dot indicators
 */
@Composable
fun ClientBottomNavBar(
    selectedTab: ClientNavTab,
    onTabSelected: (ClientNavTab) -> Unit,
    modifier: Modifier = Modifier,
    connectionState: ConnectionState = ConnectionState.Disconnected
) {
    val density = LocalDensity.current

    // Dimensions for the narrow curved cradle
    val curveHalfWidthPx = with(density) { 36.dp.toPx() }
    val curveControlHalfWidthPx = with(density) { 22.dp.toPx() }
    val curveDepthPx = with(density) { 13.dp.toPx() }
    val strokeWidthPx = with(density) { 1.dp.toPx() }

    val isConnected = connectionState.isConnected
    val isConnecting = connectionState.isConnecting || connectionState is ConnectionState.Disconnecting

    // Cradle glow color based on connection state
    val cradleGlowColor = when {
        isConnected -> Color(0xFF6366F1).copy(alpha = 0.18f)
        isConnecting -> Color(0xFFF59E0B).copy(alpha = 0.16f)
        else -> Color(0xFF6366F1).copy(alpha = 0.06f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val w = size.width
                val h = size.height
                val cx = w / 2f

                // --- 1. Custom Filled Background with Narrow Curved Cradle ---
                val bgPath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(cx - curveHalfWidthPx, 0f)

                    // Smooth entry into narrow cradle
                    cubicTo(
                        cx - curveControlHalfWidthPx, 0f,
                        cx - curveControlHalfWidthPx + 2f, curveDepthPx,
                        cx, curveDepthPx
                    )
                    // Smooth exit out of narrow cradle
                    cubicTo(
                        cx + curveControlHalfWidthPx - 2f, curveDepthPx,
                        cx + curveControlHalfWidthPx, 0f,
                        cx + curveHalfWidthPx, 0f
                    )

                    lineTo(w, 0f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }

                // Fill with deep cyber obsidian
                drawPath(path = bgPath, color = Color(0xFF070A14))

                // --- 2. Top Border with Narrow Curved Cradle ---
                val borderPath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(cx - curveHalfWidthPx, 0f)
                    cubicTo(
                        cx - curveControlHalfWidthPx, 0f,
                        cx - curveControlHalfWidthPx + 2f, curveDepthPx,
                        cx, curveDepthPx
                    )
                    cubicTo(
                        cx + curveControlHalfWidthPx - 2f, curveDepthPx,
                        cx + curveControlHalfWidthPx, 0f,
                        cx + curveHalfWidthPx, 0f
                    )
                    lineTo(w, 0f)
                }

                drawPath(
                    path = borderPath,
                    color = Color(0xFF141C30),
                    style = Stroke(width = strokeWidthPx)
                )

                // Subtle dynamic ambient glow behind the center cradle
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            cradleGlowColor,
                            Color.Transparent
                        ),
                        center = Offset(cx, curveDepthPx * 0.5f),
                        radius = curveHalfWidthPx * 1.25f
                    ),
                    radius = curveHalfWidthPx * 1.25f,
                    center = Offset(cx, curveDepthPx * 0.5f)
                )
            }
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Finetune
            MinimalNavItem(
                icon = Icons.Filled.Tune,
                label = "Finetune",
                isSelected = selectedTab == ClientNavTab.FINETUNE,
                onClick = { onTabSelected(ClientNavTab.FINETUNE) }
            )

            // 2. Usage
            MinimalNavItem(
                icon = Icons.AutoMirrored.Filled.ShowChart,
                label = "Usage",
                isSelected = selectedTab == ClientNavTab.USAGE,
                onClick = { onTabSelected(ClientNavTab.USAGE) }
            )

            // 3. Connect (Minimalist Center Hero Button with dynamic visual state)
            MinimalConnectCenterItem(
                isSelected = selectedTab == ClientNavTab.CONNECT,
                connectionState = connectionState,
                onClick = { onTabSelected(ClientNavTab.CONNECT) }
            )

            // 4. Tools
            MinimalNavItem(
                icon = Icons.Filled.Build,
                label = "Tools",
                isSelected = selectedTab == ClientNavTab.TOOLS,
                onClick = { onTabSelected(ClientNavTab.TOOLS) }
            )

            // 5. Settings
            MinimalNavItem(
                icon = Icons.Filled.Settings,
                label = "Settings",
                isSelected = selectedTab == ClientNavTab.SETTINGS,
                onClick = { onTabSelected(ClientNavTab.SETTINGS) }
            )
        }
    }
}

/**
 * Minimalist Center Connect Item:
 * Matte obsidian disc with dynamic visual states mirroring the main connect button:
 * - Connected: Electric Indigo border (#6366F1), periwinkle icon, "Stop" label
 * - Connecting: Amber Gold border (#F59E0B), amber icon, "Cancel" label
 * - Disconnected: Steel slate border (#232E44), slate icon, "Connect" label
 */
@Composable
private fun MinimalConnectCenterItem(
    isSelected: Boolean,
    connectionState: ConnectionState,
    onClick: () -> Unit
) {
    val isConnected = connectionState.isConnected
    val isConnecting = connectionState.isConnecting || connectionState is ConnectionState.Disconnecting

    // Gentle breathing pulse for the ambient glow when connected or connecting
    val infiniteTransition = rememberInfiniteTransition(label = "NavHeroPulse")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isConnected) 1.08f else if (isConnecting) 1.12f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HeroGlowScale"
    )

    val targetBorder = when {
        isConnected -> Color(0xFF6366F1)
        isConnecting -> Color(0xFFF59E0B)
        isSelected -> Color(0xFF354466)
        else -> Color(0xFF222F46)
    }

    val targetIcon = when {
        isConnected -> Color(0xFFA5B4FC)
        isConnecting -> Color(0xFFF59E0B)
        isSelected -> Color(0xFFA5B4FC)
        else -> Color(0xFF64748B)
    }

    val actionLabel = when {
        isConnected -> "Stop"
        isConnecting -> "Cancel"
        else -> "Connect"
    }

    val borderColor by animateColorAsState(
        targetValue = targetBorder,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "HeroBorderColor"
    )

    val iconColor by animateColorAsState(
        targetValue = targetIcon,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "HeroIconColor"
    )

    val labelColor by animateColorAsState(
        targetValue = if (isConnected || isConnecting || isSelected) targetIcon else Color(0xFF64748B),
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "HeroLabelColor"
    )

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
        Box(
            modifier = Modifier.size(46.dp),
            contentAlignment = Alignment.Center
        ) {
            // Luminous ambient glow aura when connected (visible even when user navigates to Finetune!)
            if (isConnected || isConnecting) {
                val auraColor = if (isConnected) Color(0xFF6366F1) else Color(0xFFF59E0B)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .scale(glowScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    auraColor.copy(alpha = 0.42f),
                                    auraColor.copy(alpha = 0.16f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // Minimalist Obsidian Disc nestled inside the narrow curve
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF090D1A))
                    .border(
                        width = if (isConnected || isConnecting) 1.8.dp else 1.2.dp,
                        color = borderColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PowerSettingsNew,
                    contentDescription = actionLabel,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = actionLabel,
            fontSize = 9.5.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isConnected || isConnecting || isSelected) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = 0.5.sp,
            color = labelColor
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Vibrant 3dp indicator dot (Emerald green when connected, amber when connecting)
        Box(
            modifier = Modifier
                .size(3.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isConnected -> Color(0xFF22C55E)
                        isConnecting -> Color(0xFFF59E0B)
                        isSelected -> Color(0xFF818CF8)
                        else -> Color.Transparent
                    }
                )
        )
    }
}

/**
 * Minimalist standard navigation tab with subtle indicator dot when selected
 */
@Composable
private fun MinimalNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tint by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFA5B4FC) else Color(0xFF4A5568),
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "NavTint"
    )

    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = label,
            fontSize = 9.5.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = tint
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Tiny 2.5dp minimalist indicator dot when active
        Box(
            modifier = Modifier
                .size(2.5.dp)
                .clip(CircleShape)
                .background(if (isSelected) Color(0xFF818CF8) else Color.Transparent)
        )
    }
}
