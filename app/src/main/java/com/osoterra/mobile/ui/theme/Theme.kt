package com.osoterra.mobile.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Emerald700,
    onPrimary = White,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald900,
    secondary = Emerald600,
    onSecondary = White,
    secondaryContainer = SurfaceGreen,
    onSecondaryContainer = Emerald900,
    tertiary = Emerald800,
    onTertiary = White,
    background = Salt,
    onBackground = Ink,
    surface = White,
    onSurface = Ink,
    surfaceVariant = Track,
    onSurfaceVariant = Neutral600,
    surfaceContainerLowest = White,
    surfaceContainerLow = White,
    surfaceContainer = White,
    surfaceContainerHigh = Salt,
    surfaceContainerHighest = Track,
    outline = Neutral600,
    outlineVariant = Line,
    error = SalinityCritical,
    onError = White,
    errorContainer = SalinityCriticalContainer,
    onErrorContainer = SalinityCritical,
)

/** Radios de la guía: 8 dp controles, 12–20 dp tarjetas, píldoras completas. */
private val OsoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Tema claro único, como la Web App: el fondo «sal» y los verdes de la marca en toda la app.
 */
@Composable
fun OsoTerraTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = Salt.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = LightColors,
        typography = OsoTypography,
        shapes = OsoShapes,
        content = content,
    )
}
