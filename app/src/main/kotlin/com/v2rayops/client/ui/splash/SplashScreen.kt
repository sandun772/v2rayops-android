package com.v2rayops.client.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.v2rayops.client.R
import com.v2rayops.client.designsystem.theme.CyberAccent
import com.v2rayops.client.designsystem.theme.CyberBackground
import com.v2rayops.client.designsystem.theme.CyberCard
import com.v2rayops.client.designsystem.theme.CyberCardElevated
import com.v2rayops.client.designsystem.theme.CyberPrimary
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

    // Snappy choreographing:
    // 1. Logo squircle mounts immediately (matching starting window background).
    // 2. Almost immediately (at 60ms), it smoothly slides left as "RayOPS" expands.
    // 3. As text completes reveal (~400ms), an ethereal cyber end-glow blooms behind the lockup.
    var isExpanded by remember { mutableStateOf(false) }
    var triggerEndGlow by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(60)
        isExpanded = true
        delay(380)
        triggerEndGlow = true
    }

    val endGlowAlpha by animateFloatAsState(
        targetValue = if (triggerEndGlow) 0.7f else 0f,
        animationSpec = tween(durationMillis = 420, easing = LinearOutSlowInEasing),
        label = "endGlowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground),
        contentAlignment = Alignment.Center
    ) {
        // Ethereal Cyber End-Glow Blooming Behind Lockup
        if (endGlowAlpha > 0f) {
            Box(
                modifier = Modifier
                    .size(width = 340.dp, height = 180.dp)
                    .scale(1.15f)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                CyberPrimary.copy(alpha = endGlowAlpha * 0.45f),
                                CyberAccent.copy(alpha = endGlowAlpha * 0.25f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Pure Minimalist Lockup: V2 Icon in Rounded Square + "RayOPS" Text
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Stylized "V2" Logo in Elevated Rounded Square (Squircle)
            Box(
                modifier = Modifier
                    .size(84.dp)
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
                                    CyberPrimary.copy(alpha = 0.5f + endGlowAlpha * 0.5f),
                                    CyberAccent.copy(alpha = 0.4f + endGlowAlpha * 0.4f)
                                )
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.v2rayops_logo_trimmed),
                    contentDescription = "V2 Logo",
                    modifier = Modifier
                        .size(60.dp, 44.dp)
                        .scale(1.05f),
                    contentScale = ContentScale.Fit
                )
            }

            // Snappy Horizontal Expand Reveal of "RayOPS" Brand Text
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(
                    animationSpec = tween(380, easing = FastOutSlowInEasing)
                ) + expandHorizontally(
                    animationSpec = tween(380, easing = FastOutSlowInEasing),
                    expandFrom = Alignment.Start
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 14.dp)
                ) {
                    Text(
                        text = "Ray",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 0.5.sp,
                        color = CyberTextPrimary
                    )
                    Text(
                        text = "OPS",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 1.sp,
                        color = CyberPrimary
                    )
                }
            }
        }
    }
}
