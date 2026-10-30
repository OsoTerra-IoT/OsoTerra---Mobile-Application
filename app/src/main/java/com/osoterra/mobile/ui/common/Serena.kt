package com.osoterra.mobile.ui.common

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.osoterra.mobile.R
import com.osoterra.mobile.domain.model.SalinityLevel
import com.osoterra.mobile.ui.theme.BandAccent
import com.osoterra.mobile.ui.theme.BandMuted
import com.osoterra.mobile.ui.theme.Emerald100
import com.osoterra.mobile.ui.theme.Emerald700
import com.osoterra.mobile.ui.theme.Emerald800
import com.osoterra.mobile.ui.theme.Forest
import com.osoterra.mobile.ui.theme.MeterCritical
import com.osoterra.mobile.ui.theme.MeterHigh
import com.osoterra.mobile.ui.theme.MeterNoData
import com.osoterra.mobile.ui.theme.MeterNormal
import com.osoterra.mobile.ui.theme.MeterWatch
import com.osoterra.mobile.ui.theme.OliveLine
import com.osoterra.mobile.ui.theme.SageInk
import com.osoterra.mobile.ui.theme.SalinityCritical
import com.osoterra.mobile.ui.theme.SalinityCriticalContainer
import com.osoterra.mobile.ui.theme.White
import java.util.Locale

/*
 * Piezas del diseño «Banda serena y tarjeta» (variante 6): banda verde con el estado del día,
 * una tarjeta blanca grande para lo urgente y mosaicos de dos columnas para el resto.
 * Todo con aire: pocas cosas por pantalla, radios de 20 a 28 dp y sombras suaves.
 */

/** Chip del modo de prueba: lo dibujan la banda y la cabecera de cada pantalla con sesión. */
data class DemoChipModel(val scenarioNumber: Int?, val onOpen: () -> Unit)

val LocalDemoChip = staticCompositionLocalOf<DemoChipModel?> { null }

/** Espacio duro (U+00A0). */
private const val NBSP = '\u00A0'

/** Margen lateral de las pantallas serenas. */
val SerenaGutter = 20.dp

private val BandShape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
private val CardShape = RoundedCornerShape(28.dp)
private val TileShape = RoundedCornerShape(20.dp)

/** Bandas verdes visibles ahora; al cambiar de pestaña una entra antes de que la otra salga. */
private var greenBands = 0

/** Íconos claros en la barra de estado mientras se ve una banda verde. */
@Composable
fun LightStatusBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        greenBands++
        controller?.isAppearanceLightStatusBars = false
        onDispose {
            greenBands = (greenBands - 1).coerceAtLeast(0)
            controller?.isAppearanceLightStatusBars = greenBands == 0
        }
    }
}

/**
 * Banda superior verde bosque, redondeada abajo, que pasa por detrás de la barra de estado.
 * El contenido va en blanco y [BandMuted]; [compact] la usa en Alertas y Perfil.
 */
@Composable
fun SerenaBand(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    LightStatusBarIcons()
    Column(
        modifier
            .fillMaxWidth()
            .background(Forest, BandShape)
            .statusBarsPadding()
            .padding(start = SerenaGutter, end = SerenaGutter, top = 4.dp, bottom = if (compact) 22.dp else 28.dp),
        content = content,
    )
}

/**
 * Franja del color de la banda detrás de la barra de estado: al desplazar la pantalla, el
 * contenido no pasa por debajo de la hora ni de los íconos.
 */
@Composable
fun StatusBarScrim(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
            .background(Forest),
    )
}

/** Fila superior de la banda: chip de prueba (o la marca) a la izquierda y una acción a la derecha. */
@Composable
fun BandTopRow(trailing: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val demo = LocalDemoChip.current
        if (demo != null) {
            DemoChip(demo, onBand = true)
        } else {
            Text("OsoSense", style = MaterialTheme.typography.titleSmall, color = White)
        }
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

/** Acción de texto sobre la banda («+ Nueva parcela»), con 48 dp de alto. */
@Composable
fun BandAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = Color.Transparent,
        contentColor = White,
        modifier = Modifier.heightIn(min = 48.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = BandAccent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = White, maxLines = 2)
        }
    }
}

/** Saludo y titular de estado en blanco. */
@Composable
fun BandHeadline(greeting: String?, title: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(top = 4.dp)) {
        if (!greeting.isNullOrBlank()) {
            Text(greeting, style = MaterialTheme.typography.bodyLarge, color = BandMuted)
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = White,
            modifier = Modifier
                .padding(top = 2.dp)
                .semantics { heading() },
        )
    }
}

/** Banda reducida de las pestañas Alertas y Perfil: rótulo, titular y, si hace falta, algo más. */
@Composable
fun BandHeader(
    eyebrow: String,
    title: String,
    trailing: @Composable RowScope.() -> Unit = {},
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    SerenaBand(compact = true) {
        BandTopRow(trailing)
        Text(eyebrow, style = MaterialTheme.typography.bodyLarge, color = BandMuted, modifier = Modifier.padding(top = 4.dp))
        val reducedMotion = rememberReducedMotion()
        androidx.compose.animation.Crossfade(
            targetState = title,
            animationSpec = motionTween(OsoMotion.ENTER, reducedMotion),
            label = "bandTitle",
        ) { text ->
            Text(
                text.ifBlank { " " },
                style = MaterialTheme.typography.headlineSmall,
                color = White,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .semantics { heading() },
            )
        }
        extra()
    }
}

/** Orden de gravedad para el medidor y las listas. */
fun SalinityLevel.urgencyRank(): Int = when (this) {
    SalinityLevel.CRITICAL -> 4
    SalinityLevel.HIGH -> 3
    SalinityLevel.WATCH -> 2
    SalinityLevel.NORMAL -> 1
    SalinityLevel.NO_DATA -> 0
}

private fun SalinityLevel.meterColor(): Color = when (this) {
    SalinityLevel.CRITICAL -> MeterCritical
    SalinityLevel.HIGH -> MeterHigh
    SalinityLevel.WATCH -> MeterWatch
    SalinityLevel.NORMAL -> MeterNormal
    SalinityLevel.NO_DATA -> MeterNoData
}

/**
 * Medidor de la banda: un segmento por parcela en el color de su nivel (de la más grave a la
 * más leve) y la leyenda en palabras, «1 muy alto · 1 en vigilancia · 1 normal». Con muchas
 * parcelas, un segmento por nivel con ancho proporcional. Los segmentos crecen al aparecer.
 */
@Composable
fun LevelMeter(levels: List<SalinityLevel>, modifier: Modifier = Modifier) {
    if (levels.isEmpty()) return
    val sorted = levels.sortedByDescending { it.urgencyRank() }
    val counts = sorted.groupingBy { it }.eachCount().toList().sortedByDescending { it.first.urgencyRank() }
    val locale = Locale.getDefault()
    val labels = SalinityLevel.entries.associateWith { it.label().lowercase(locale) }
    // Espacios duros: «1 en vigilancia» nunca se parte entre el número y la palabra.
    val legend = counts.joinToString(" · ") { (level, count) -> "$count$NBSP" + labels.getValue(level).replace(' ', NBSP) }
    val reducedMotion = rememberReducedMotion()
    val grow = remember(levels) { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(levels) {
        if (!reducedMotion) grow.animateTo(1f, motionTween(OsoMotion.FILL, reducedMotion = false, delayMillis = 120))
    }
    Column(modifier.clearAndSetSemantics { contentDescription = legend }) {
        val segments: List<Pair<Color, Float>> = if (sorted.size <= 10) {
            sorted.map { it.meterColor() to 1f }
        } else {
            counts.map { (level, count) -> level.meterColor() to count.toFloat() }
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(6.dp),
        ) {
            val gap = 6.dp.toPx()
            val total = segments.sumOf { it.second.toDouble() }.toFloat()
            val usable = size.width - gap * (segments.size - 1)
            var x = 0f
            segments.forEach { (color, weight) ->
                val w = usable * weight / total
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, 0f),
                    size = Size(w * grow.value, size.height),
                    cornerRadius = CornerRadius(size.height / 2, size.height / 2),
                )
                x += w + gap
            }
        }
        Text(
            legend,
            style = MaterialTheme.typography.bodySmall,
            color = BandMuted,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

/** Chip «Escenario N» del modo de prueba; abre la hoja de escenarios. */
@Composable
fun DemoChip(model: DemoChipModel, onBand: Boolean) {
    val actionLabel = stringResource(R.string.demo_chip_action)
    val text = model.scenarioNumber?.let { stringResource(R.string.demo_chip, it) } ?: stringResource(R.string.demo_chip_none)
    Box(
        Modifier
            .heightIn(min = 48.dp)
            .semantics {
                contentDescription = "$text. $actionLabel"
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = model.onOpen,
            shape = RoundedCornerShape(50),
            color = if (onBand) Color.Transparent else Emerald100,
            contentColor = if (onBand) BandMuted else Emerald800,
            border = if (onBand) BorderStroke(1.dp, BandMuted.copy(alpha = 0.45f)) else null,
        ) {
            Row(
                Modifier.padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                Icon(Icons.Outlined.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/**
 * Cabecera serena de las pantallas internas: fondo marfil, botón de volver en círculo,
 * título y acciones. Sin barra de color: la pantalla respira.
 */
@Composable
fun SerenaTopBar(
    title: String,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(White, CircleShape)
                            .border(1.dp, OliveLine, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
            } else {
                Spacer(Modifier.width(12.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            actions()
        }
        LocalDemoChip.current?.let { demo ->
            Row(Modifier.padding(start = 12.dp)) { DemoChip(demo, onBand = false) }
        }
    }
}

/** Tarjeta blanca grande con sombra verde muy suave (nivel 1 de la guía). */
@Composable
fun SerenaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shadowed = modifier.shadow(
        elevation = 10.dp,
        shape = CardShape,
        ambientColor = Forest.copy(alpha = 0.10f),
        spotColor = Forest.copy(alpha = 0.10f),
    )
    if (onClick != null) {
        Surface(onClick = onClick, shape = CardShape, color = White, modifier = shadowed.fillMaxWidth()) {
            Column(Modifier.padding(padding), content = content)
        }
    } else {
        Surface(shape = CardShape, color = White, modifier = shadowed.fillMaxWidth()) {
            Column(Modifier.padding(padding), content = content)
        }
    }
}

/** Superficie de mosaico: sin relleno, borde de 1 dp olivo claro y radio 20. */
@Composable
fun SerenaTile(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    filled: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val color = if (filled) White else Color.Transparent
    val border = BorderStroke(1.dp, OliveLine)
    val inner: @Composable () -> Unit = {
        Column(
            Modifier
                .heightIn(min = 88.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            content = content,
        )
    }
    if (onClick != null) {
        Surface(onClick = onClick, shape = TileShape, color = color, border = border, modifier = modifier) { inner() }
    } else {
        Surface(shape = TileShape, color = color, border = border, modifier = modifier) { inner() }
    }
}

/** Mosaico de parcela: nombre arriba (y una línea opcional), píldora del nivel abajo. */
@Composable
fun PlotTile(
    title: String,
    level: SalinityLevel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val description = listOfNotNull(title, subtitle, level.label()).joinToString(", ")
    SerenaTile(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        onClick = onClick,
    ) {
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SageInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        SalinityChip(level, compact = true, modifier = Modifier.padding(top = 10.dp))
    }
}

/**
 * Rejilla de dos columnas con filas de igual altura. Con fuente grande (≥ 1,3) o pantalla
 * angosta, un mosaico por fila: nada se recorta ni se monta.
 */
@Composable
fun <T> TileGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    tile: @Composable (item: T, modifier: Modifier) -> Unit,
) {
    val single = LocalDensity.current.fontScale >= 1.3f
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (single) {
            items.forEach { tile(it, Modifier.fillMaxWidth()) }
        } else {
            items.chunked(2).forEach { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { tile(it, Modifier.weight(1f).fillMaxHeight()) }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** Botón principal de la guía: píldora verde, texto blanco y círculo blanco con flecha. */
@Composable
fun ArrowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = White),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 22.dp, end = 7.dp, top = 7.dp, bottom = 7.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(34.dp)
                .background(White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = Emerald700, modifier = Modifier.size(20.dp))
        }
    }
}

/** Rótulo de sección sobre el marfil. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = SageInk,
        modifier = modifier
            .padding(top = 8.dp)
            .semantics { heading() },
    )
}

/** Ícono de línea en círculo verde claro de 40 dp (guía 5.1 para datos). */
@Composable
fun SoftIcon(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier
            .size(size)
            .background(Emerald100.copy(alpha = 0.7f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Emerald800, modifier = Modifier.size(size * 0.5f))
    }
}

/**
 * Gráfico de la tarjeta urgente: franja de riesgo sobre el umbral, umbral rojo punteado y la
 * línea de la ECe en verde que se dibuja de izquierda a derecha, con un punto en el último valor.
 */
@Composable
fun RiskSparkline(
    values: List<Double>,
    threshold: Double?,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
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
        if (!reducedMotion) reveal.animateTo(1f, motionTween(OsoMotion.FILL, reducedMotion = false, delayMillis = 150))
    }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        val reference = threshold?.takeIf { it > 0 }
        val top = maxOf(values.max(), (reference ?: values.max()) * 1.25) * 1.04
        val bottom = minOf(values.min(), (reference ?: values.min()) * 0.75) * 0.95
        val span = (top - bottom).coerceAtLeast(0.01)
        val pad = 5.dp.toPx()
        fun y(value: Double) = (pad + (size.height - 2 * pad) * (1 - (value.coerceIn(bottom, top) - bottom) / span)).toFloat()

        if (reference != null) {
            val yThreshold = y(reference)
            drawRoundRect(
                color = SalinityCriticalContainer,
                topLeft = Offset(0f, 0f),
                size = Size(size.width, yThreshold),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            )
            drawLine(
                color = SalinityCritical,
                start = Offset(0f, yThreshold),
                end = Offset(size.width, yThreshold),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
            )
        }
        val startX = pad
        val endX = size.width - pad
        val step = (endX - startX) / (values.size - 1)
        val path = Path().apply {
            values.forEachIndexed { i, v -> if (i == 0) moveTo(startX, y(v)) else lineTo(startX + i * step, y(v)) }
        }
        clipRect(right = size.width * reveal.value) {
            drawPath(path, color = Emerald700, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        if (reveal.value >= 1f) {
            drawCircle(Emerald700, radius = 4.dp.toPx(), center = Offset(endX, y(last)))
        }
    }
}

/** Valor grande en dS/m (estilo «dato» de la guía: 800, 36 sp) con la unidad al lado. */
@Composable
fun BigReading(value: Double?, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            value?.fmt(2) ?: "—",
            style = MaterialTheme.typography.displaySmall,
            color = color,
            maxLines = 1,
        )
        Text(
            stringResource(R.string.unit_ds_m),
            style = MaterialTheme.typography.titleSmall,
            color = SageInk,
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
        )
    }
}

/** Ancho mínimo de los botones de texto pequeño para no quedar por debajo de 48 dp. */
val MinTouch = Modifier.widthIn(min = 48.dp).heightIn(min = 48.dp)
