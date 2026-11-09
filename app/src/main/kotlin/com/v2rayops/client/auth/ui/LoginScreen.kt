package com.v2rayops.client.auth.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.v2rayops.client.R
import com.v2rayops.client.auth.AuthUiState
import com.v2rayops.client.auth.AuthViewModel
import com.v2rayops.client.domain.repository.AuthDeepLinkPayload
import com.v2rayops.client.ui.dashboard.components.V2RayOpsTopBar
import kotlin.math.cos
import kotlin.math.sin

/**
 * Redesigned V2RayOPS Mobile Authorization Screen
 * Matches the exact design language, color palette, and cyber radar aesthetic of the Home Screen:
 * - Deep Cosmic background (`#050811`)
 * - Official Top Bar (`V2RayOpsTopBar`) with trimmed brand logo and Xray-core capsule
 * - Status Capsule Pill (`• AUTHORIZATION REQUIRED`)
 * - Concentric Cyber Radar Halo Lockup with slow orbital rotation
 * - High-tech Summary Telemetry Cards (styled identically to ActiveNodeSummaryCard)
 * - Exclusive 1-Click Web / Google Authorization CTA (Zero manual code inputs)
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    deepLinkPayload: AuthDeepLinkPayload? = null,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Auto-trigger authorization when deep link payload arrives from browser/Google OAuth
    LaunchedEffect(deepLinkPayload) {
        if (deepLinkPayload != null && deepLinkPayload.customerCode.isNotBlank()) {
            viewModel.loginWithPayload(deepLinkPayload)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onLoginSuccess()
        }
    }

    val isLoading = uiState is AuthUiState.Loading
    val errorMessage = (uiState as? AuthUiState.Error)?.message

    // Orbital animation matching ConcentricRadarDial
    val infiniteTransition = rememberInfiniteTransition(label = "LoginRadarTransition")
    val orbitRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(45000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitRotation"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseGlow"
    )

    Scaffold(
        topBar = {
            V2RayOpsTopBar(coreVersion = "Xray-core 25.3")
        },
        containerColor = Color(0xFF050811)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Status Capsule Pill (Matches StatusPill aesthetic)
                Row(
                    modifier = Modifier
                        .background(
                            if (isLoading) Color(0xFF041B1C) else Color(0xFF0F1528),
                            RoundedCornerShape(20.dp)
                        )
                        .border(
                            1.dp,
                            if (isLoading) Color(0xFF0B4648) else Color(0xFF1D2845),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isLoading) Color(0xFF06B6D4) else Color(0xFF818CF8))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isLoading) "AUTHORIZING CLIENT..." else "AUTHORIZATION REQUIRED",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = if (isLoading) Color(0xFF06B6D4) else Color(0xFF818CF8)
                    )
                }

                // 2. Compact Concentric Cyber Radar Dial Hero Lockup (180dp)
                Box(
                    modifier = Modifier
                        .size(175.dp)
                        .drawBehind {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val coreRadius = 42.dp.toPx()

                            // Outer Dashed Orbital Tracks
                            val orbitInnerRadius = coreRadius * 1.50f
                            val orbitOuterRadius = coreRadius * 1.85f
                            val orbitColor = if (isLoading) Color(0xFF06B6D4).copy(alpha = 0.35f) else Color(0xFF1D2845)

                            rotate(orbitRotation, Offset(cx, cy)) {
                                drawCircle(
                                    color = orbitColor,
                                    radius = orbitInnerRadius,
                                    style = Stroke(
                                        width = 1.2.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 12f), 0f)
                                    )
                                )
                                drawCircle(
                                    color = orbitColor.copy(alpha = 0.20f),
                                    radius = orbitOuterRadius,
                                    style = Stroke(
                                        width = 1.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 16f), 0f)
                                    )
                                )
                            }

                            // Concentric Halo Dots
                            val dotRadii = listOf(coreRadius * 1.15f, coreRadius * 1.30f)
                            val dotCounts = listOf(22, 28)
                            val dotColor = if (isLoading) Color(0xFF06B6D4).copy(alpha = 0.45f) else Color(0xFF1E293B)

                            for (rIdx in dotRadii.indices) {
                                val r = dotRadii[rIdx]
                                val count = dotCounts[rIdx]
                                for (i in 0 until count) {
                                    val angle = (i.toFloat() / count) * (2f * Math.PI.toFloat())
                                    val dx = cx + r * cos(angle)
                                    val dy = cy + r * sin(angle)
                                    drawCircle(
                                        color = dotColor,
                                        radius = 1.4.dp.toPx(),
                                        center = Offset(dx, dy)
                                    )
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Central Matte Obsidian Core (86dp)
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0A0F1E))
                            .border(
                                2.dp,
                                if (isLoading) Color(0xFF6366F1).copy(alpha = (0.6f * pulseGlow).coerceIn(0.3f, 1f))
                                else Color(0xFF1B2438).copy(alpha = (0.8f * pulseGlow).coerceIn(0.5f, 1f)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.v2rayops_logo_trimmed),
                                contentDescription = "V2RayOPS Gateway",
                                modifier = Modifier.size(36.dp, 22.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "GATEWAY",
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = if (isLoading) Color(0xFF06B6D4) else Color(0xFF818CF8)
                            )
                        }
                    }
                }

                // 3. Digital Monospace Header & Description
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "V2RAYOPS CLOUD ACCESS",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        color = Color(0xFFF8FAFC)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Link this mobile client to your V2RayOPS web account to synchronize private Singapore VPS nodes and activate unlimited routing tunnels.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 17.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // 4. In-Flight Authorization HUD Banner
                AnimatedVisibility(
                    visible = isLoading,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0B1424))
                            .border(1.dp, Color(0xFF06B6D4), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF06B6D4)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "EXCHANGING CRYPTOGRAPHIC TOKENS...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF06B6D4)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Synchronizing private servers and tunnels...",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                // 5. Error Message HUD Banner
                if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF240A12))
                            .border(1.dp, Color(0xFF5E1B2C), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Error",
                                tint = Color(0xFFFB7185),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMessage,
                                fontSize = 11.sp,
                                color = Color(0xFFFB7185),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // 6. High-Tech Telemetry Cards (Styled exactly like ActiveNodeSummaryCard)
                // Card A: One-Tap Web Authorization
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0B0F1E))
                        .border(1.dp, Color(0xFF151E34), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0E1424))
                                .border(1.dp, Color(0xFF1C2740), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "One-Tap Web Authorization",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                color = Color(0xFFF1F5F9)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "Shared Browser Session • Zero Password Typing",
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                // Card B: Realtime Node Sync
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0B0F1E))
                        .border(1.dp, Color(0xFF151E34), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0E1424))
                                .border(1.dp, Color(0xFF1C2740), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Automatic SG1 Node Routing",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                color = Color(0xFFF1F5F9)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "VLESS • Multi-SNI Dynamic Route Discovery",
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 7. Signature Primary CTA: Continue with Google / Web Account
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = { viewModel.openWebAuthorization(context) },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6366F1),
                            disabledContainerColor = Color(0xFF6366F1).copy(alpha = 0.4f),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "AUTHORIZING...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null,
                                modifier = Modifier.size(19.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "CONTINUE WITH GOOGLE / WEB",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Auto-authorizes if signed in • Redirects to Google login if new",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center
                    )
                }

                // 8. Footer Telemetry Notice
                Text(
                    text = "256-BIT ENCRYPTED TUNNEL • CLIENT v1.0.0",
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF334155),
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
