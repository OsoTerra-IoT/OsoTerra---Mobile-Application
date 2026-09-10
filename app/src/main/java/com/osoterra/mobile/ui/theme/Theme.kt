package com.osoterra.mobile.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkBackground = Color(0xFF121412)
private val DarkSurface = Color(0xFF1E201D)

private val LightColors = lightColorScheme(
    primary = OsoGreen,
    onPrimary = NeutralWhite,
    primaryContainer = OsoGreenLight,
    onPrimaryContainer = OsoGreenDark,
    secondary = OsoEarth,
    onSecondary = NeutralWhite,
    secondaryContainer = OsoEarthLight,
    background = OsoSand,
    onBackground = NeutralBlack,
    surface = NeutralWhite,
    onSurface = NeutralBlack,
    surfaceVariant = OsoEarthLight,
    onSurfaceVariant = NeutralGray,
    error = SalinityCritical,
    onError = NeutralWhite,
)

private val DarkColors = darkColorScheme(
    primary = OsoGreenLight,
    onPrimary = OsoGreenDark,
    primaryContainer = OsoGreenDark,
    onPrimaryContainer = OsoGreenLight,
    secondary = OsoEarthLight,
    onSecondary = OsoEarth,
    background = DarkBackground,
    onBackground = NeutralWhite,
    surface = DarkSurface,
    onSurface = NeutralWhite,
    error = SalinityCritical,
    onError = NeutralWhite,
)

@Composable
fun OsoTerraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OsoTypography,
        content = content,
    )
}
