package com.v2rayops.client.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
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
    val density = LocalDensity.current

    // GPU-accelerated animatables: Zero relayout jank, 120fps smooth
    val animProgress = remember { Animatable(0f) }
    val glowProgress = remember { Animatable(0f) }

    // Measured text width for exact sub-pixel centering on frame 0
    var textWidthPx by remember { mutableFloatStateOf(0f) }
    val defaultTextWidthPx = with(density) { 140.dp.toPx() }
    val spacingPx = with(density) { 14.dp.toPx() }

    // Distance needed to shift the logo from screen center to the left
    val initialShiftPx = remember(textWidthPx) {
        val widthToUse = if (textWidthPx > 0f) textWidthPx else defaultTextWidthPx
        (widthToUse + spacingPx) / 2f
    }

    LaunchedEffect(Unit) {
        // 1. Start immediately on first frame - zero pre-stuck delay!
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
        )
        // 2. Ambient Cyber End-Glow blooms once lockup forms
        glowProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 260, easing = LinearOutSlowInEasing)
        )
        // 3. User requirement: 250ms end idle time to admire the completed glowing lockup
        delay(250)

        // 4. Handoff to target screen
        if (target == SplashNavigationTarget.Login) {
            onNavigateToLogin()
        } else {
            onNavigateToDashboard()
        }
    }

    val progress = animProgress.value
    val glowVal = glowProgress.value

    // GPU-translated positions
    val logoTranslationX = (1f - progress) * initialShiftPx
    val textTranslationX = (1f - progress) * with(density) { (-18).dp.toPx() }
    val textAlpha = progress.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground),
        contentAlignment = Alignment.Center
    ) {
        // Ethereal Cyber End-Glow Blooming Behind Lockup
        if (glowVal > 0f) {
            Box(
                modifier = Modifier
                    .size(width = 340.dp, height = 180.dp)
                    .scale(1.2f)
                    .graphicsLayer {
                        alpha = glowVal
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                CyberPrimary.copy(alpha = 0.45f),
                                CyberAccent.copy(alpha = 0.25f),
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
            // 1. Stylized "V2" Logo in Elevated Rounded Square (Squircle)
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .graphicsLayer {
                        translationX = logoTranslationX
                    }
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
                                    CyberPrimary.copy(alpha = 0.5f + glowVal * 0.5f),
                                    CyberAccent.copy(alpha = 0.4f + glowVal * 0.4f)
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
                        .size(58.dp, 42.dp)
                        .scale(1.05f),
                    contentScale = ContentScale.Fit
                )
            }

            // 2. "RayOPS" Brand Text (GPU-accelerated slide & fade, zero relayout lag)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = 14.dp)
                    .onGloballyPositioned { coordinates ->
                        textWidthPx = coordinates.size.width.toFloat()
                    }
                    .graphicsLayer {
                        translationX = textTranslationX
                        alpha = textAlpha
                    }
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
