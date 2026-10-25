package com.osoterra.mobile.ui.common

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.osoterra.mobile.ui.theme.Track

/** Movimiento de la guía de estilo 5.1: los mismos tiempos y la misma curva que la Web App. */
object OsoMotion {
    /** cubic-bezier(0.4, 0, 0.2, 1), la curva estándar de Material. */
    val Standard = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    /** Cambios de estado (color, selección, píldoras): 150 ms. */
    const val STATE = 150

    /** Aparición de contenido y cambio de carga a contenido. */
    const val ENTER = 360

    /** Llenado de franjas y trazo del gráfico. */
    const val FILL = 700

    /** Retraso entre tarjetas de una lista que aparecen en cascada. */
    const val STAGGER = 60
}

/** tween con la curva de la guía; sin animación si la persona las quitó del sistema. */
fun <T> motionTween(durationMillis: Int, reducedMotion: Boolean, delayMillis: Int = 0): FiniteAnimationSpec<T> =
    if (reducedMotion) snap() else tween(durationMillis, delayMillis, OsoMotion.Standard)

/**
 * true si la persona desactivó las animaciones del sistema (Opciones de accesibilidad,
 * «Quitar animaciones», o la escala de animación en 0). Entonces se muestran los estados finales.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/**
 * Aparición en cascada de una tarjeta de lista: sube 16 dp y se aclara, un poco después de la
 * anterior. Solo la primera vez que entra; con animaciones quitadas aparece directamente.
 */
@Composable
fun StaggeredEntrance(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val reducedMotion = rememberReducedMotion()
    val progress = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            progress.animateTo(
                1f,
                motionTween(OsoMotion.ENTER, reducedMotion = false, delayMillis = index.coerceAtMost(8) * OsoMotion.STAGGER),
            )
        }
    }
    Box(
        modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * 16.dp.toPx()
        },
    ) { content() }
}

/**
 * Brillo de carga (shimmer) para los esqueletos: una franja clara cruza el bloque cada 1,2 s.
 * Con animaciones quitadas queda el bloque gris fijo.
 */
fun Modifier.shimmer(shape: Shape): Modifier = composed {
    val reducedMotion = rememberReducedMotion()
    if (reducedMotion) return@composed background(Track, shape)
    var width by remember { mutableFloatStateOf(0f) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX",
    )
    val highlight = Color.White.copy(alpha = 0.65f)
    this
        .onSizeChanged { width = it.width.toFloat() }
        .background(
            Brush.linearGradient(
                colors = listOf(Track, highlight, Track),
                start = Offset(width * x - width * 0.4f, 0f),
                end = Offset(width * x, 0f),
            ),
            shape,
        )
}
