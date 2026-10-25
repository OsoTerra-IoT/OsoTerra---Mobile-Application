package com.osoterra.mobile.ui.plot

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osoterra.mobile.R
import com.osoterra.mobile.data.remote.displayShortDate
import com.osoterra.mobile.data.remote.displayTime
import com.osoterra.mobile.data.remote.salinityLevel
import com.osoterra.mobile.domain.model.Reading
import com.osoterra.mobile.domain.model.SalinityLevel
import com.osoterra.mobile.ui.common.OsoMotion
import com.osoterra.mobile.ui.common.SalinityChip
import com.osoterra.mobile.ui.common.containerColor
import com.osoterra.mobile.ui.common.dsPerM
import com.osoterra.mobile.ui.common.fmt
import com.osoterra.mobile.ui.common.motionTween
import com.osoterra.mobile.ui.common.rememberReducedMotion
import com.osoterra.mobile.ui.theme.Emerald700
import com.osoterra.mobile.ui.theme.Line
import com.osoterra.mobile.ui.theme.Neutral600
import com.osoterra.mobile.ui.theme.SalinityCritical
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** Escalones «redondos» para el eje Y: 0.5, 1, 2, 2.5, 5… por potencia de diez. */
internal fun niceStep(maxValue: Double, ticks: Int = 4): Double {
    if (maxValue <= 0.0) return 0.5
    val raw = maxValue / ticks
    val magnitude = 10.0.pow(floor(log10(raw)))
    val residual = raw / magnitude
    val nice = when {
        residual <= 1.0 -> 1.0
        residual <= 2.0 -> 2.0
        residual <= 2.5 -> 2.5
        residual <= 5.0 -> 5.0
        else -> 10.0
    }
    return nice * magnitude
}

/**
 * «Conductividad en el tiempo» según la guía 5.1: línea de ECe en verde de 3 dp sobre bandas
 * suaves de cada nivel, eje Y desde cero con la unidad, fechas cortas en el eje X, umbral del
 * cultivo punteado en rojo con su rótulo y el último valor rotulado junto a su punto.
 * TalkBack lee un resumen; debajo hay un resumen en texto y la tabla de lecturas.
 */
@Composable
fun EcChart(readings: List<Reading>, threshold: Double?, modifier: Modifier = Modifier) {
    val values = readings.map { it.ecDsPerM }
    val step = niceStep(maxOf(values.max(), threshold ?: 0.0) * 1.1)
    val top = ceil(maxOf(values.max(), threshold ?: 0.0) * 1.1 / step) * step
    val ticks = generateSequence(0.0) { it + step }.takeWhile { it <= top + 1e-9 }.toList()

    // Fechas cortas en el eje X; si todas las lecturas son del mismo día, la hora.
    val sameDay = readings.sameDay()
    fun axisLabel(reading: Reading): String? =
        if (sameDay) displayTime(reading.capturedAt) else displayShortDate(reading.capturedAt)
    val firstDate = axisLabel(readings.first()) ?: readings.first().timestamp
    val lastDate = axisLabel(readings.last()) ?: readings.last().timestamp
    val midDate = axisLabel(readings[readings.size / 2])

    val summary = chartSummary(readings, threshold)
    val unit = stringResource(R.string.unit_ds_m)
    val thresholdLabel = threshold?.let { stringResource(R.string.chart_threshold_label, it.fmt(1)) }

    val reducedMotion = rememberReducedMotion()
    val reveal = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(readings) {
        if (!reducedMotion) {
            reveal.snapTo(0f)
            reveal.animateTo(1f, motionTween(OsoMotion.FILL, reducedMotion = false))
        }
    }

    val measurer = rememberTextMeasurer()
    val axisStyle = TextStyle(fontSize = 12.sp, color = Neutral600)
    val thresholdStyle = TextStyle(fontSize = 12.sp, color = SalinityCritical, fontWeight = FontWeight.SemiBold)
    val lastStyle = TextStyle(fontSize = 13.sp, color = Emerald700, fontWeight = FontWeight.Bold)
    val lineColor = Emerald700
    val bands = threshold?.let {
        listOf(
            Triple(0.0, it, SalinityLevel.NORMAL),
            Triple(it, it * 1.2, SalinityLevel.WATCH),
            Triple(it * 1.2, it * 1.5, SalinityLevel.HIGH),
            Triple(it * 1.5, Double.MAX_VALUE, SalinityLevel.CRITICAL),
        )
    }
    val bandColors = SalinityLevel.entries.associateWith { it.containerColor() }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(220.dp)
            .clearAndSetSemantics { contentDescription = summary },
    ) {
        val unitLayout = measurer.measure(unit, axisStyle)
        val tickLayouts = ticks.map { measurer.measure(it.fmt(if (step < 1) 1 else 0), axisStyle) }
        val left = maxOf(tickLayouts.maxOf { it.size.width }, unitLayout.size.width) + 8.dp.toPx()
        val topPad = unitLayout.size.height + 6.dp.toPx()
        val bottomPad = 22.dp.toPx()
        val right = 12.dp.toPx()
        val plotW = size.width - left - right
        val plotH = size.height - topPad - bottomPad
        fun y(v: Double) = topPad + plotH - (v / top).toFloat() * plotH
        fun x(i: Int) = if (values.size == 1) left + plotW / 2 else left + plotW * i / (values.size - 1)

        // Bandas suaves de nivel detrás de todo.
        bands?.forEach { (from, to, level) ->
            val yTop = y(minOf(to, top))
            val yBottom = y(from)
            if (yBottom > yTop) {
                drawRect(bandColors.getValue(level).copy(alpha = 0.55f), Offset(left, yTop), Size(plotW, yBottom - yTop))
            }
        }
        // Unidad arriba del eje y valores con guías finas.
        drawText(unitLayout, topLeft = Offset(0f, 0f))
        ticks.forEachIndexed { i, tick ->
            val ty = y(tick)
            drawLine(Line, Offset(left, ty), Offset(left + plotW, ty), strokeWidth = 1.dp.toPx())
            val layout = tickLayouts[i]
            drawText(layout, topLeft = Offset(left - 6.dp.toPx() - layout.size.width, ty - layout.size.height / 2))
        }
        // Fechas cortas: primera, del medio y última.
        val dateY = topPad + plotH + 6.dp.toPx()
        val first = measurer.measure(firstDate, axisStyle)
        drawText(first, topLeft = Offset(left, dateY))
        val last = measurer.measure(lastDate, axisStyle)
        drawText(last, topLeft = Offset(left + plotW - last.size.width, dateY))
        if (midDate != null && values.size > 4 && midDate != firstDate && midDate != lastDate) {
            val mid = measurer.measure(midDate, axisStyle)
            drawText(mid, topLeft = Offset(x(values.size / 2) - mid.size.width / 2, dateY))
        }
        // Umbral del cultivo: línea punteada y rótulo a la izquierda, sobre la línea.
        if (threshold != null && thresholdLabel != null) {
            val ty = y(threshold)
            drawLine(
                color = SalinityCritical,
                start = Offset(left, ty),
                end = Offset(left + plotW, ty),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
            )
            val label = measurer.measure(thresholdLabel, thresholdStyle)
            drawText(label, topLeft = Offset(left + 4.dp.toPx(), ty - label.size.height - 2.dp.toPx()))
        }
        // Línea de ECe; se dibuja de izquierda a derecha al aparecer.
        val points = values.mapIndexed { i, v -> Offset(x(i), y(v)) }
        clipRect(right = left + plotW * reveal.value + 8.dp.toPx()) {
            if (points.size > 1) {
                val path = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path, lineColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        // Último valor: punto y rótulo directo.
        if (reveal.value >= 0.98f) {
            val lastPoint = points.last()
            drawCircle(androidx.compose.ui.graphics.Color.White, radius = 6.dp.toPx(), center = lastPoint)
            drawCircle(lineColor, radius = 4.5.dp.toPx(), center = lastPoint)
            val valueLabel = measurer.measure(values.last().fmt(2), lastStyle)
            val lx = (lastPoint.x - valueLabel.size.width - 8.dp.toPx()).coerceAtLeast(left)
            val above = lastPoint.y - valueLabel.size.height - 6.dp.toPx()
            val ly = if (above < topPad) lastPoint.y + 8.dp.toPx() else above
            drawText(valueLabel, topLeft = Offset(lx, ly))
        }
    }
}

/** true si todas las lecturas tienen la misma fecha (AAAA-MM-DD). */
private fun List<Reading>.sameDay(): Boolean {
    val days = mapNotNull { it.capturedAt?.take(10) }
    return days.size == size && days.distinct().size == 1
}

/** Resumen del gráfico en una frase, para TalkBack y para leer debajo del gráfico. */
@Composable
fun chartSummary(readings: List<Reading>, threshold: Double?): String {
    val values = readings.map { it.ecDsPerM }
    val from = displayShortDate(readings.first().capturedAt) ?: readings.first().timestamp
    val to = displayShortDate(readings.last().capturedAt) ?: readings.last().timestamp
    val base = if (readings.sameDay()) pluralStringResource(
        R.plurals.chart_summary_same_day,
        values.size,
        values.size,
        from,
        values.min().fmt(2),
        values.max().fmt(2),
        values.last().dsPerM(),
    ) else pluralStringResource(
        R.plurals.chart_summary,
        values.size,
        values.size,
        from,
        to,
        values.min().fmt(2),
        values.max().fmt(2),
        values.last().dsPerM(),
    )
    if (threshold == null) return base
    val over = values.last() > threshold
    return base + " " + stringResource(
        if (over) R.string.chart_summary_over else R.string.chart_summary_under,
        threshold.dsPerM(1),
    )
}

/**
 * Alternativa en tabla del gráfico (fecha, ECe y nivel), como «Ver tabla de datos» de la
 * Web App. Se despliega con una animación corta; muestra hasta las 12 lecturas más recientes.
 */
@Composable
fun ReadingsTable(readings: List<Reading>, threshold: Double?) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val reducedMotion = rememberReducedMotion()
    val toggleLabel = stringResource(if (expanded) R.string.chart_table_hide else R.string.chart_table_show)
    val stateText = stringResource(if (expanded) R.string.common_expanded else R.string.common_collapsed)
    TextButton(
        onClick = { expanded = !expanded },
        modifier = Modifier
            .heightIn(min = 48.dp)
            .semantics { stateDescription = stateText },
    ) {
        Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
        Text(toggleLabel, modifier = Modifier.padding(start = 6.dp), color = Emerald700)
    }
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(motionTween(OsoMotion.ENTER, reducedMotion)) + fadeIn(motionTween(OsoMotion.ENTER, reducedMotion)),
        exit = shrinkVertically(motionTween(OsoMotion.STATE * 2, reducedMotion)) + fadeOut(motionTween(OsoMotion.STATE, reducedMotion)),
    ) {
        Column {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(
                    stringResource(R.string.chart_col_reading),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.chart_col_level),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            readings.takeLast(12).asReversed().forEach { reading ->
                HorizontalDivider(color = Line)
                val level = salinityLevel(reading.ecDsPerM, threshold)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(vertical = 8.dp)
                        .semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            reading.ecDsPerM.dsPerM(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            reading.timestamp,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    SalinityChip(level)
                }
            }
        }
    }
}
