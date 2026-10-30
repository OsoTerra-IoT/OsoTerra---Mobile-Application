package com.osoterra.mobile.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.osoterra.mobile.R
import com.osoterra.mobile.domain.model.SalinityLevel
import com.osoterra.mobile.ui.theme.Emerald700
import com.osoterra.mobile.ui.theme.Emerald800
import com.osoterra.mobile.ui.theme.SalinityCritical
import com.osoterra.mobile.ui.theme.SalinityCriticalContainer
import com.osoterra.mobile.ui.theme.SalinityHighContainer
import com.osoterra.mobile.ui.theme.SalinityNormalContainer
import com.osoterra.mobile.ui.theme.SalinityWatchContainer
import com.osoterra.mobile.ui.theme.SurfaceGreen

/*
 * Piezas compactas para mostrar datos con menos texto: mini gráfico de tendencia, conteo de
 * parcelas por nivel y mosaicos de dato con ícono. Todas escalan con el tamaño de fuente del
 * sistema (sin alturas fijas para el texto) y respetan «Quitar animaciones».
 */

/** true si el tamaño de fuente del sistema es grande (≥ 1,3): las filas pasan a columnas. */
@Composable
fun isLargeFont(): Boolean = LocalDensity.current.fontScale >= 1.3f

/**
 * Mini gráfico de la ECe (guía 5.1): bandas de fondo con los cuatro niveles, umbral del cultivo
 * en rojo punteado y la línea en #047857, que se dibuja de izquierda a derecha al aparecer.
 * Para lectores de pantalla se resume en una frase (valor inicial, final y si sube o baja).
 */
@Composable
fun EceSparkline(
    values: List<Double>,
    threshold: Double?,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
) {
    if (values.size < 2) return
    val first = values.first()
    val last = values.last()
    val description = stringResource(
        when {
            last - first > 0.05 -> R.string.trend_up
            first - last > 0.05 -> R.string.trend_down
            else -> R.string.trend_flat
        },
        values.size,
        first.fmt(2),
        last.fmt(2),
    )
    val reducedMotion = rememberReducedMotion()
    val reveal = remember(values) { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(values) {
        if (!reducedMotion) reveal.animateTo(1f, motionTween(OsoMotion.FILL, reducedMotion = false))
    }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        // Escala ajustada a los datos (no desde cero) para que la tendencia se note,
        // siempre con el umbral dentro del rango.
        val reference = threshold?.takeIf { it > 0 }
        val top = maxOf(values.max(), (reference ?: values.max()) * 1.2) * 1.08
        val bottom = minOf(values.min(), (reference ?: values.min()) * 0.8) * 0.9
        val span = (top - bottom).coerceAtLeast(0.01)
        fun y(value: Double) = (size.height * (1 - (value.coerceIn(bottom, top) - bottom) / span)).toFloat()

        // Bandas de nivel: Normal, En vigilancia, Alto y Muy alto según la razón ECe / umbral.
        if (threshold != null && threshold > 0) {
            val bands = listOf(
                0.0 to 1.0 to SalinityNormalContainer,
                1.0 to 1.2 to SalinityWatchContainer,
                1.2 to 1.5 to SalinityHighContainer,
                1.5 to 99.0 to SalinityCriticalContainer,
            )
            bands.forEach { (range, color) ->
                val yTop = y(minOf(range.second * threshold, top)).coerceAtLeast(0f)
                val yBottom = y(range.first * threshold)
                if (yBottom > yTop) {
                    drawRect(color, topLeft = Offset(0f, yTop), size = Size(size.width, yBottom - yTop))
                }
            }
            drawLine(
                color = SalinityCritical,
                start = Offset(0f, y(threshold)),
                end = Offset(size.width, y(threshold)),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
            )
        }

        val step = size.width / (values.size - 1)
        val path = Path().apply {
            values.forEachIndexed { i, v -> if (i == 0) moveTo(0f, y(v)) else lineTo(i * step, y(v)) }
        }
        clipRect(right = size.width * reveal.value) {
            drawPath(
                path,
                color = Emerald700,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        if (reveal.value >= 1f) {
            drawCircle(Emerald700, radius = 4.dp.toPx(), center = Offset(size.width, y(last)))
        }
    }
}

/**
 * Conteo de parcelas por nivel en píldoras (ícono + número + palabra), del más grave al más leve.
 * Reemplaza las frases del tipo «1 parcela está en vigilancia…».
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LevelCounts(levels: List<SalinityLevel>, modifier: Modifier = Modifier) {
    val order = listOf(SalinityLevel.CRITICAL, SalinityLevel.HIGH, SalinityLevel.WATCH, SalinityLevel.NORMAL, SalinityLevel.NO_DATA)
    val counts = order.map { level -> level to levels.count { it == level } }.filter { it.second > 0 }
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        counts.forEach { (level, count) ->
            val description = pluralStringResource(R.plurals.level_count, count, count, level.label())
            Row(
                modifier = Modifier
                    .background(level.containerColor(), RoundedCornerShape(50))
                    .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)
                    .clearAndSetSemantics { contentDescription = description },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(level.icon(), contentDescription = null, tint = level.color(), modifier = Modifier.size(18.dp))
                Text("$count", style = MaterialTheme.typography.titleSmall, color = level.color())
                Text(level.label(), style = MaterialTheme.typography.labelMedium, color = level.color(), maxLines = 1)
            }
        }
    }
}

/** Dato con ícono: número grande y rótulo corto debajo. Se lee como «rótulo: valor». */
@Composable
fun IconStat(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    /** En columna (pantalla angosta o fuente grande): ícono, número y rótulo en una sola fila. */
    inline: Boolean = false,
) {
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "$label: $value" },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        if (inline) {
            Row(
                Modifier
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBadge(icon)
                Text(
                    value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = valueColor,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 10.dp),
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp),
                )
            }
            return@Surface
        }
        Column(
            Modifier
                .heightIn(min = 48.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon)
                Text(
                    value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = valueColor,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** Ícono de línea en un círculo verde claro (guía 5.1: 40 dp para datos). */
@Composable
fun IconBadge(icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(50), color = SurfaceGreen, modifier = modifier.size(36.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Emerald800, modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * Coloca los mosaicos en una fila si caben, o en una columna si la pantalla es angosta o la
 * fuente es grande: nunca se parten las palabras ni se superponen.
 */
@Composable
fun AdaptiveTiles(
    count: Int,
    modifier: Modifier = Modifier,
    minTileWidth: Dp = 104.dp,
    tile: @Composable (index: Int, modifier: Modifier, inline: Boolean) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val fontScale = LocalDensity.current.fontScale
        val fits = maxWidth / count >= minTileWidth * fontScale.coerceAtLeast(1f)
        if (fits) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(count) { tile(it, Modifier.weight(1f), false) }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(count) { tile(it, Modifier.fillMaxWidth(), true) }
            }
        }
    }
}

/** Mosaicos que fluyen en filas de ancho mínimo; con fuente grande, uno por fila. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowTiles(modifier: Modifier = Modifier, content: @Composable (Modifier) -> Unit) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val large = LocalDensity.current.fontScale >= 1.3f
        val tileModifier = if (large) Modifier.fillMaxWidth() else Modifier.width((maxWidth - 10.dp) / 2)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) { content(tileModifier) }
    }
}
