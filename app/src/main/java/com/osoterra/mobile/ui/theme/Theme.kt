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
    background = Ivory,
    onBackground = TextStrong,
    surface = White,
    onSurface = TextStrong,
    surfaceVariant = Ivory,
    onSurfaceVariant = SageInk,
    surfaceContainerLowest = White,
    surfaceContainerLow = White,
    surfaceContainer = White,
    surfaceContainerHigh = White,
    surfaceContainerHighest = Ivory,
    outline = Sage,
    outlineVariant = OliveLine,
    error = SalinityCritical,
    onError = White,
    errorContainer = SalinityCriticalContainer,
    onErrorContainer = SalinityCritical,
)

/** Radios «Tierra Serena»: campos 16 dp, mosaicos 20 dp, tarjetas 24–28 dp, píldoras completas. */
private val OsoShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Tema claro único «Tierra Serena»: fondo marfil, superficies blancas y los verdes de la marca.
 */
@Composable
fun OsoTerraTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = Ivory.toArgb()
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
