package com.v2rayops.client.designsystem.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CyberColorScheme = darkColorScheme(
    primary = CyberPrimary,
    onPrimary = CyberTextPrimary,
    primaryContainer = CyberPrimaryDark,
    onPrimaryContainer = CyberTextPrimary,
    secondary = CyberAccent,
    onSecondary = CyberTextPrimary,
    secondaryContainer = CyberAccentDark,
    onSecondaryContainer = CyberTextPrimary,
    tertiary = CyberSuccess,
    background = CyberBackground,
    onBackground = CyberTextPrimary,
    surface = CyberSurface,
    onSurface = CyberTextPrimary,
    surfaceVariant = CyberCard,
    onSurfaceVariant = CyberTextSecondary,
    outline = CyberBorder,
    outlineVariant = CyberBorderSubtle,
    error = CyberDanger,
    onError = CyberTextPrimary
)

@Composable
fun V2RayOpsTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = CyberColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = CyberBackground.toArgb()
            window.navigationBarColor = CyberBackground.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CyberTypography,
        shapes = CyberShapes,
        content = content
    )
}
