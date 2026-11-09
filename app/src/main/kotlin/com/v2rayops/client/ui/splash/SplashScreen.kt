package com.v2rayops.client.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SplashStartBg = Color(0xFF000000) // Pure black matching Android cold-start window
private val SplashEndBg = Color(0xFF050811)   // Exact Home Screen background

@Composable
fun SplashScreen(
    onNavigateToDashboard: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val target by viewModel.target.collectAsState()
    val density = LocalDensity.current

    // GPU-accelerated animatables: 120fps smooth, zero relayout jank
    val slideProgress = remember { Animatable(0f) }

    // Measured text width for exact sub-pixel positioning
    var textWidthPx by remember { mutableFloatStateOf(0f) }
    val defaultTextWidthPx = with(density) { 150.dp.toPx() }
    val spacingPx = with(density) { 10.dp.toPx() }

    // On frame 0, shift the entire row to the right by half the (text + spacing) width
    // so the V2 logo is placed at the exact mathematical center of the screen,
    // seamlessly matching the preload window background with 0px jump.
    val initialShiftPx = remember(textWidthPx) {
        val widthToUse = if (textWidthPx > 0f) textWidthPx else defaultTextWidthPx
        (widthToUse + spacingPx) / 2f
    }

    LaunchedEffect(Unit) {
        // Stage 1: Brief 120ms pause holding the 1:1 centered logo on pure black
        delay(120)

        // Stage 2: Logo glides smoothly to the left, revealing solid white "RayOPS"
        // and background interpolates from black to Home background (#050811)
        slideProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing)
        )

        // Stage 3: Brief hold to establish brand lockup
        delay(250)

        // Stage 4: Trigger crossfade directly. Splash stays at 100% opacity,
        // and Navigation Compose fades Home in over it with zero black/blank frames.
        if (target == SplashNavigationTarget.Login) {
            onNavigateToLogin()
        } else {
            onNavigateToDashboard()
        }
    }

    val slideVal = slideProgress.value

    // Logo slides from center (initialShiftPx) to left (0)
    val rowTranslationX = (1f - slideVal) * initialShiftPx
    val textTranslationX = (1f - slideVal) * with(density) { (-16).dp.toPx() }
    val textAlpha = slideVal.coerceIn(0f, 1f)

    // Background color smoothly interpolates from #000000 (black) to #050811 (home screen bg)
    val currentBgColor = lerp(SplashStartBg, SplashEndBg, slideVal)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentBgColor),
        contentAlignment = Alignment.Center
    ) {
        // Clean Minimalist Brand Lockup: V2 Icon on left + Solid White "RayOPS" on right
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer {
                translationX = rowTranslationX
            }
        ) {
            // 1. Pristine 1:1 V2 Logo Graphic (68dp x 42dp - identical to preload window background)
            Image(
                painter = painterResource(id = R.drawable.v2rayops_logo_trimmed),
                contentDescription = "V2 Logo",
                modifier = Modifier.size(width = 68.dp, height = 42.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.width(10.dp))

            // 2. Pure Solid White "RayOPS" Typography (no glow, no gradients, clean & sharp)
            Text(
                text = "RayOPS",
                fontSize = 35.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 0.5.sp,
                color = Color.White,
                modifier = Modifier
                    .onGloballyPositioned { coordinates ->
                        textWidthPx = coordinates.size.width.toFloat()
                    }
                    .graphicsLayer {
                        translationX = textTranslationX
                        alpha = textAlpha
                    }
            )
        }
    }
}
