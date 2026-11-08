package com.v2rayops.client.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.v2rayops.client.R
import com.v2rayops.client.designsystem.theme.CyberAccent
import com.v2rayops.client.designsystem.theme.CyberAccentGlow
import com.v2rayops.client.designsystem.theme.CyberBackground
import com.v2rayops.client.designsystem.theme.CyberBorderSubtle
import com.v2rayops.client.designsystem.theme.CyberCard
import com.v2rayops.client.designsystem.theme.CyberCardElevated
import com.v2rayops.client.designsystem.theme.CyberPrimary
import com.v2rayops.client.designsystem.theme.CyberTextMuted
import com.v2rayops.client.designsystem.theme.CyberTextPrimary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToDashboard: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val target by viewModel.target.collectAsState()

    LaunchedEffect(target) {
        when (target) {
            SplashNavigationTarget.Dashboard -> onNavigateToDashboard()
            SplashNavigationTarget.Login -> onNavigateToLogin()
            SplashNavigationTarget.Loading -> Unit
        }
    }

    // Animation Stage:
    // 0: Initial Mount (matches starting window background seamlessly)
    // 1: Squircle Badge entry & scale
    // 2: Squircle translates left + "RayOPS" brand text expands into view
    // 3: Subtitle & Cyber-ops loader materialize
    var animationStage by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        delay(80)
        animationStage = 1
        delay(380)
        animationStage = 2
        delay(320)
        animationStage = 3
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val squircleScale by animateFloatAsState(
        targetValue = if (animationStage >= 1) 1.0f else 0.85f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "squircleScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            // Main Brand Lockup Row:
            // Squircle Logo starts dead-center; as "RayOPS" reveals, it translates left naturally.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // 1. Stylized "V2" Logo in Elevated Squircle (Rounded Square)
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .scale(squircleScale)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(CyberCard, CyberCardElevated)
                            )
                        )
                        .border(
                            border = BorderStroke(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        CyberPrimary.copy(alpha = glowAlpha),
                                        CyberAccent.copy(alpha = glowAlpha * 0.75f)
                                    )
                                )
                            ),
                            shape = RoundedCornerShape(22.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.v2rayops_logo_trimmed),
                        contentDescription = "V2RayOps Logo",
                        modifier = Modifier
                            .size(62.dp, 44.dp)
                            .scale(1.05f),
                        contentScale = ContentScale.Fit
                    )
                }

                // 2. Animated Brand Text "RayOPS"
                AnimatedVisibility(
                    visible = animationStage >= 2,
                    enter = fadeIn(
                        animationSpec = tween(550, easing = FastOutSlowInEasing)
                    ) + expandHorizontally(
                        animationSpec = tween(600, easing = FastOutSlowInEasing),
                        expandFrom = Alignment.Start
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 14.dp)
                    ) {
                        Text(
                            text = "Ray",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 0.5.sp,
                            color = CyberTextPrimary
                        )
                        Text(
                            text = "OPS",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 1.sp,
                            color = CyberPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyberAccentGlow)
                                .border(1.dp, CyberAccent.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "CORE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyberAccent
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 3. Subtitle Tagline & Cyber-Ops Status Loader
            AnimatedVisibility(
                visible = animationStage >= 3,
                enter = fadeIn(
                    animationSpec = tween(450, easing = FastOutSlowInEasing)
                ) + slideInVertically(
                    animationSpec = tween(450, easing = FastOutSlowInEasing)
                ) { 20 }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CYBER EDGE OPERATIONAL NETWORK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.8.sp,
                        color = CyberAccent
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    // Sleek 2dp glowing linear cyber progress line
                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .height(2.dp)
                            .clip(CircleShape)
                            .background(CyberBorderSubtle)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(CyberPrimary, CyberAccent)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "INITIALIZING SECURE PROTOCOLS...",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                        color = CyberTextMuted
                    )
                }
            }
        }
    }
}
