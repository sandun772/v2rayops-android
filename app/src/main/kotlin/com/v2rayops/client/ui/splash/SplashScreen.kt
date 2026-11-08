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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
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
    val cardProgress = remember { Animatable(0f) }
    val slideProgress = remember { Animatable(0f) }
    val glowProgress = remember { Animatable(0f) }
    val exitAlpha = remember { Animatable(1f) }

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
        // Stage 1: Brief 80ms pause showing the pristine 1:1 logo centered
        delay(80)

        // Stage 2: The round square (squircle) emerges and frames the centered logo
        cardProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
        )

        // Stage 3: The squircle glides to the left and "RayOPS" brand text animates in
        slideProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
        )

        // Stage 4: Ambient Cyber End-Glow blooms once lockup forms
        glowProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 240, easing = LinearOutSlowInEasing)
        )

        // Stage 5: User requirement: 250ms end idle time to admire the completed glowing lockup
        delay(250)

        // Stage 6: Seamless dissolution blending into the main dashboard
        exitAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
        )

        // Handoff to target screen
        if (target == SplashNavigationTarget.Login) {
            onNavigateToLogin()
        } else {
            onNavigateToDashboard()
        }
    }

    val cardVal = cardProgress.value
    val slideVal = slideProgress.value
    val glowVal = glowProgress.value

    // GPU-translated positions
    val logoTranslationX = (1f - slideVal) * initialShiftPx
    val textTranslationX = (1f - slideVal) * with(density) { (-18).dp.toPx() }
    val textAlpha = slideVal.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = exitAlpha.value }
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF090E1D), // Atmospheric midnight indigo core
                        Color(0xFF050812), // Seamless subtle transition
                        CyberBackground    // #04060A deep cosmic edge
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Ethereal Multi-Tiered Cyber Ambient Bloom Behind Lockup
        if (glowVal > 0f) {
            // Tier 1: Wide Ethereal Aurora Halo
            Box(
                modifier = Modifier
                    .size(width = 440.dp, height = 240.dp)
                    .graphicsLayer {
                        alpha = glowVal * 0.9f
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                CyberPrimary.copy(alpha = 0.32f),
                                CyberAccent.copy(alpha = 0.16f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Tier 2: Radiant Plasma Core directly behind brand lockup
            Box(
                modifier = Modifier
                    .size(width = 280.dp, height = 120.dp)
                    .graphicsLayer {
                        alpha = glowVal
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF818CF8).copy(alpha = 0.42f),
                                CyberAccent.copy(alpha = 0.22f),
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
            // 1. Stylized "V2" Logo: 1:1 on frame 0, then squircle emerges around it, then glides left
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .graphicsLayer {
                        translationX = logoTranslationX
                    },
                contentAlignment = Alignment.Center
            ) {
                // Round Square (Squircle) that emerges in Stage 2 around the 1:1 logo
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 0.84f + 0.16f * cardVal
                            scaleY = 0.84f + 0.16f * cardVal
                            alpha = cardVal
                        }
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF141C2E), // Subtle titanium-tinted upper glass rim
                                    CyberCard          // #0C101B deep obsidian base
                                )
                            )
                        )
                        .border(
                            border = BorderStroke(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF818CF8).copy(alpha = 0.7f + glowVal * 0.3f),
                                        Color(0xFF06B6D4).copy(alpha = 0.5f + glowVal * 0.5f)
                                    )
                                )
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                )

                // 1:1 Logo Graphic: visible from frame 0, perfectly centered
                Image(
                    painter = painterResource(id = R.drawable.v2rayops_logo_trimmed),
                    contentDescription = "V2 Logo",
                    modifier = Modifier
                        .size(width = 68.dp, height = 42.dp),
                    contentScale = ContentScale.Fit
                )
            }

            // 2. "RayOPS" Brand Text (Metallic Brushed-Platinum "Ray" + Electric Neon Aurora "OPS")
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
                    style = TextStyle(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFFFFF), // Pure white specular highlight
                                Color(0xFFE2E8F0), // Platinum silver midtone
                                Color(0xFF94A3B8)  // Brushed titanium shadow
                            )
                        ),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 0.5.sp,
                        shadow = Shadow(
                            color = Color(0xAA000000),
                            offset = Offset(0f, 2f),
                            blurRadius = 6f
                        )
                    )
                )
                Text(
                    text = "OPS",
                    style = TextStyle(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF818CF8), // Electric violet
                                Color(0xFF6366F1), // Cyber indigo
                                Color(0xFF06B6D4)  // Neon cyan
                            )
                        ),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 1.5.sp,
                        shadow = Shadow(
                            color = Color(0xFF6366F1).copy(alpha = 0.75f),
                            offset = Offset(0f, 0f),
                            blurRadius = 18f
                        )
                    )
                )
            }
        }
    }
}
