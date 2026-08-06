package com.buchou.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.buchou.app.ui.AppTheme

private val LightColors = lightColorScheme(
    primary = Sage,
    onPrimary = WarmSurface,
    primaryContainer = SageSoft,
    onPrimaryContainer = Ink,
    background = WarmWhite,
    onBackground = Ink,
    surface = WarmSurface,
    onSurface = Ink,
    onSurfaceVariant = MutedInk,
    surfaceContainer = WarmSurface,
    surfaceContainerHigh = ElevatedSurface,
    outline = OutlineStrong,
    outlineVariant = Divider,
    error = Smoked,
)

private val DarkColors = darkColorScheme(
    primary = NightSage,
    onPrimary = Night,
    primaryContainer = NightSageContainer,
    onPrimaryContainer = NightText,
    background = Night,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    onSurfaceVariant = NightMuted,
    surfaceContainer = NightSurface,
    surfaceContainerHigh = NightElevatedSurface,
    outline = NightOutlineStrong,
    outlineVariant = NightDivider,
    error = ColorTokens.DarkError,
)

private object ColorTokens {
    val DarkError = androidx.compose.ui.graphics.Color(0xFFFFB4AB)
}

@Composable
fun BuchouTheme(
    appTheme: AppTheme = AppTheme.System,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (appTheme) {
        AppTheme.System -> isSystemInDarkTheme()
        AppTheme.Light -> false
        AppTheme.Dark -> true
    }
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BuchouTypography,
        content = content,
    )
}
