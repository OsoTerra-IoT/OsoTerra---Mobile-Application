package com.osoterra.mobile.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
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
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.osoterra.mobile.R
import com.osoterra.mobile.domain.model.AlertSeverity
import com.osoterra.mobile.domain.model.SalinityLevel
import com.osoterra.mobile.ui.theme.Ink
import com.osoterra.mobile.ui.theme.SalinityCritical
import com.osoterra.mobile.ui.theme.SalinityCriticalContainer
import com.osoterra.mobile.ui.theme.SalinityHigh
import com.osoterra.mobile.ui.theme.SalinityHighContainer
import com.osoterra.mobile.ui.theme.SalinityNoData
import com.osoterra.mobile.ui.theme.SalinityNoDataContainer
import com.osoterra.mobile.ui.theme.SalinityNormal
import com.osoterra.mobile.ui.theme.SalinityNormalContainer
import com.osoterra.mobile.ui.theme.SalinityWatch
import com.osoterra.mobile.ui.theme.SalinityWatchContainer
import com.osoterra.mobile.ui.theme.Track

/** Color de texto e íconos del nivel (contraste AA sobre su fondo y sobre blanco). */
fun SalinityLevel.color(): Color = when (this) {
    SalinityLevel.NORMAL -> SalinityNormal
    SalinityLevel.WATCH -> SalinityWatch
    SalinityLevel.HIGH -> SalinityHigh
    SalinityLevel.CRITICAL -> SalinityCritical
    SalinityLevel.NO_DATA -> SalinityNoData
}

fun SalinityLevel.containerColor(): Color = when (this) {
    SalinityLevel.NORMAL -> SalinityNormalContainer
    SalinityLevel.WATCH -> SalinityWatchContainer
    SalinityLevel.HIGH -> SalinityHighContainer
    SalinityLevel.CRITICAL -> SalinityCriticalContainer
    SalinityLevel.NO_DATA -> SalinityNoDataContainer
}

/** Íconos de la guía 5.1: círculo con check, ojo, triángulo, círculo con X y signo de pregunta. */
fun SalinityLevel.icon(): ImageVector = when (this) {
    SalinityLevel.NORMAL -> Icons.Outlined.CheckCircle
    SalinityLevel.WATCH -> Icons.Outlined.Visibility
    SalinityLevel.HIGH -> Icons.Outlined.WarningAmber
    SalinityLevel.CRITICAL -> Icons.Outlined.Cancel
    SalinityLevel.NO_DATA -> Icons.Outlined.HelpOutline
}

@Composable
fun SalinityLevel.label(): String = stringResource(
    when (this) {
        SalinityLevel.NORMAL -> R.string.salinity_normal
        SalinityLevel.WATCH -> R.string.salinity_watch
        SalinityLevel.HIGH -> R.string.salinity_high
        SalinityLevel.CRITICAL -> R.string.salinity_critical
        SalinityLevel.NO_DATA -> R.string.salinity_no_data
    }
)

/**
 * Severidad de una alerta en la misma escala que los niveles de salinidad: el Backend envía
 * WATCH, WARNING y CRITICAL, que se muestran como En vigilancia, Alto y Muy alto.
 */
fun AlertSeverity.asLevel(): SalinityLevel = when (this) {
    AlertSeverity.WATCH -> SalinityLevel.WATCH
    AlertSeverity.HIGH -> SalinityLevel.HIGH
    AlertSeverity.CRITICAL -> SalinityLevel.CRITICAL
}

@Composable
fun AlertSeverity.label(): String = asLevel().label()

fun AlertSeverity.color(): Color = asLevel().color()

/** Píldora de severidad de una alerta: la misma que la del nivel equivalente. */
@Composable
fun SeverityChip(severity: AlertSeverity, modifier: Modifier = Modifier) {
    SalinityChip(level = severity.asLevel(), modifier = modifier)
}

/**
 * Píldora de estado: nunca depende solo del color, lleva ícono y palabra. Si el nivel cambia
 * (por ejemplo, al actualizar), el color pasa de uno a otro en 150 ms.
 */
@Composable
fun SalinityChip(level: SalinityLevel, modifier: Modifier = Modifier) {
    val reducedMotion = rememberReducedMotion()
    val container by animateColorAsState(level.containerColor(), motionTween(OsoMotion.STATE, reducedMotion), label = "pill")
    val content by animateColorAsState(level.color(), motionTween(OsoMotion.STATE, reducedMotion), label = "pillText")
    Row(
        modifier = modifier
            .background(container, RoundedCornerShape(50))
            .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(level.icon(), contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        Text(text = level.label(), color = content, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

/**
 * Franja de salinidad: la ECe llena la barra en proporción al umbral del cultivo,
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
        else progress.animateTo(target, motionTween(OsoMotion.FILL, reducedMotion = false, delayMillis = delayMillis))
    }
    val description = if (ratio == null) stringResource(R.string.salinity_strip_no_data)
    else stringResource(R.string.salinity_strip_description, (ratio * 100).toInt())
    val fill = level.color()
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .clearAndSetSemantics { contentDescription = description },
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
