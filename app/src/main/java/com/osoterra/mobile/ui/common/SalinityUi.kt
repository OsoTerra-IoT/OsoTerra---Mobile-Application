package com.osoterra.mobile.ui.common

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.osoterra.mobile.R
import com.osoterra.mobile.domain.model.AlertSeverity
import com.osoterra.mobile.domain.model.SalinityLevel
import com.osoterra.mobile.ui.theme.Ink
import com.osoterra.mobile.ui.theme.SalinityCritical
import com.osoterra.mobile.ui.theme.SalinityCriticalContainer
import com.osoterra.mobile.ui.theme.SalinityNoData
import com.osoterra.mobile.ui.theme.SalinityNoDataContainer
import com.osoterra.mobile.ui.theme.SalinityNormal
import com.osoterra.mobile.ui.theme.SalinityNormalContainer
import com.osoterra.mobile.ui.theme.SalinityWarning
import com.osoterra.mobile.ui.theme.SalinityWarningContainer
import com.osoterra.mobile.ui.theme.SalinityWatch
import com.osoterra.mobile.ui.theme.SalinityWatchContainer
import com.osoterra.mobile.ui.theme.Track

/** Color de texto e íconos del nivel (contraste AA sobre su fondo y sobre blanco). */
fun SalinityLevel.color(): Color = when (this) {
    SalinityLevel.NORMAL -> SalinityNormal
    SalinityLevel.WATCH -> SalinityWatch
    SalinityLevel.WARNING -> SalinityWarning
    SalinityLevel.CRITICAL -> SalinityCritical
    SalinityLevel.NO_DATA -> SalinityNoData
}

fun SalinityLevel.containerColor(): Color = when (this) {
    SalinityLevel.NORMAL -> SalinityNormalContainer
    SalinityLevel.WATCH -> SalinityWatchContainer
    SalinityLevel.WARNING -> SalinityWarningContainer
    SalinityLevel.CRITICAL -> SalinityCriticalContainer
    SalinityLevel.NO_DATA -> SalinityNoDataContainer
}

fun SalinityLevel.icon(): ImageVector = when (this) {
    SalinityLevel.NORMAL -> Icons.Outlined.CheckCircle
    SalinityLevel.WATCH -> Icons.Outlined.Visibility
    SalinityLevel.WARNING -> Icons.Outlined.WarningAmber
    SalinityLevel.CRITICAL -> Icons.Outlined.ErrorOutline
    SalinityLevel.NO_DATA -> Icons.Outlined.HelpOutline
}

@Composable
fun SalinityLevel.label(): String = stringResource(
    when (this) {
        SalinityLevel.NORMAL -> R.string.salinity_normal
        SalinityLevel.WATCH -> R.string.salinity_watch
        SalinityLevel.WARNING -> R.string.salinity_warning
        SalinityLevel.CRITICAL -> R.string.salinity_critical
        SalinityLevel.NO_DATA -> R.string.salinity_no_data
    }
)

fun AlertSeverity.color(): Color = when (this) {
    AlertSeverity.LOW -> SalinityNormal
    AlertSeverity.MEDIUM -> SalinityWatch
    AlertSeverity.HIGH -> SalinityWarning
    AlertSeverity.CRITICAL -> SalinityCritical
}

/** Píldora de estado: nunca depende solo del color, lleva ícono y texto. */
@Composable
fun SalinityChip(level: SalinityLevel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(level.containerColor(), RoundedCornerShape(50))
            .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(level.icon(), contentDescription = null, tint = level.color(), modifier = Modifier.size(18.dp))
        Text(text = level.label(), color = level.color(), style = MaterialTheme.typography.labelMedium)
    }
}

/** Si la persona desactivó las animaciones del sistema, se muestran los estados finales. */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/**
 * Franja de salinidad: la conductividad llena la barra en proporción al umbral del cultivo,
 * marcado con una línea vertical al 66 % del ancho (el umbral cae siempre en el mismo sitio,
 * así se comparan parcelas de un vistazo). Se rellena con una animación corta al aparecer.
 */
@Composable
fun SalinityStrip(
    ratio: Double?,
    level: SalinityLevel,
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
) {
    val thresholdAt = 0.66f
    val target = ratio?.let { (it.toFloat() * thresholdAt).coerceIn(0.02f, 1f) } ?: 0f
    val reducedMotion = rememberReducedMotion()
    val progress = remember { Animatable(if (reducedMotion) target else 0f) }
    LaunchedEffect(target) {
        if (reducedMotion) progress.snapTo(target)
        else progress.animateTo(target, tween(durationMillis = 700, delayMillis = delayMillis, easing = FastOutSlowInEasing))
    }
    val description = stringResource(
        R.string.salinity_strip_description,
        ratio?.let { (it * 100).toInt() } ?: 0,
    )
    val fill = level.color()
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .semantics { contentDescription = description },
    ) {
        val radius = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(color = Track, cornerRadius = radius)
        if (progress.value > 0f) {
            drawRoundRect(color = fill, size = Size(size.width * progress.value, size.height), cornerRadius = radius)
        }
        val x = size.width * thresholdAt
        drawRoundRect(
            color = Ink,
            topLeft = Offset(x - 1.5.dp.toPx(), -3.dp.toPx()),
            size = Size(3.dp.toPx(), size.height + 6.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
        )
    }
}
