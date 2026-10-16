package com.osoterra.mobile.ui.plot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.osoterra.mobile.R
import com.osoterra.mobile.di.rememberAppContainer
import com.osoterra.mobile.domain.model.Crop
import com.osoterra.mobile.domain.model.Plot
import com.osoterra.mobile.domain.model.Reading
import com.osoterra.mobile.ui.common.ErrorCard
import com.osoterra.mobile.ui.common.SalinityStrip
import com.osoterra.mobile.ui.common.SkeletonCard
import com.osoterra.mobile.ui.common.color
import com.osoterra.mobile.ui.common.label
import com.osoterra.mobile.ui.theme.Emerald700
import com.osoterra.mobile.ui.theme.Line
import com.osoterra.mobile.ui.theme.SalinityCritical
import java.util.Locale

private val ES = Locale.forLanguageTag("es-PE")

private fun Double.ds(decimals: Int = 2) = String.format(ES, "%.${decimals}f", this)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlotDetailScreen(
    plotId: String,
    onBack: () -> Unit,
) {
    val container = rememberAppContainer()
    val viewModel: PlotDetailViewModel =
        viewModel(factory = PlotDetailViewModel.provideFactory(container, plotId))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var menuExpanded by remember { mutableStateOf(false) }
    var showCropDialog by remember { mutableStateOf(false) }
    var showDeactivateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.deactivated) {
        if (state.deactivated) onBack()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(state.plot?.name.orEmpty(), maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (state.plot != null) {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.detail_more))
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.plot_change_crop)) },
                                onClick = {
                                    menuExpanded = false
                                    showCropDialog = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.plot_deactivate)) },
                                onClick = {
                                    menuExpanded = false
                                    showDeactivateDialog = true
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val plot = state.plot
            when {
                state.isLoading -> {
                    SkeletonCard(height = 200.dp)
                    SkeletonCard(height = 220.dp)
                }
                plot == null -> ErrorCard(
                    state.errorMessage ?: stringResource(R.string.detail_load_error),
                    onRetry = viewModel::load,
                )
                else -> {
                    StatusHero(plot)
                    Facts(plot, state.readings.lastOrNull())
                    History(plot.crop, state.readings)
                }
            }
        }
    }

    if (showCropDialog) {
        ChangeCropDialog(
            crops = state.crops,
            currentCropId = state.plot?.crop?.id,
            onDismiss = { showCropDialog = false },
            onSelect = { cropId ->
                viewModel.assignCrop(cropId)
                showCropDialog = false
            },
        )
    }

    if (showDeactivateDialog) {
        AlertDialog(
            onDismissRequest = { showDeactivateDialog = false },
            title = { Text(stringResource(R.string.plot_deactivate)) },
            text = { Text(stringResource(R.string.plot_deactivate_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeactivateDialog = false
                    viewModel.deactivate()
                }) { Text(stringResource(R.string.common_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeactivateDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

/**
 * Lo primero que responde la pantalla es «¿cómo está mi suelo?» en palabras: el nivel y
 * cuánto del umbral del cultivo usa. El valor en dS/m queda como dato de apoyo.
 */
@Composable
private fun StatusHero(plot: Plot) {
    val threshold = plot.crop?.thresholdDsPerM
    val ratio = if (plot.lastEcDsPerM != null && threshold != null && threshold > 0) plot.lastEcDsPerM / threshold else null
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = plot.salinityLevel.label(),
                style = MaterialTheme.typography.headlineLarge,
                color = plot.salinityLevel.color(),
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = when {
                    ratio != null && plot.crop != null -> stringResource(
                        R.string.detail_ratio,
                        (ratio * 100).toInt(),
                        plot.crop.name.lowercase(ES),
                        plot.crop.thresholdDsPerM.ds(1),
                    )
                    plot.crop == null -> stringResource(R.string.detail_no_crop)
                    else -> stringResource(R.string.detail_no_readings)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))
            SalinityStrip(ratio = ratio, level = plot.salinityLevel)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                plot.lastEcDsPerM?.let {
                    Text(
                        stringResource(R.string.detail_reading, it.ds()),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Facts(plot: Plot, latest: Reading?) {
    val facts = listOfNotNull(
        latest?.let { stringResource(R.string.detail_moisture) to "${it.humidityPct.ds(0)} %" },
        latest?.let { stringResource(R.string.detail_temperature) to "${it.temperatureC.ds(1)} °C" },
        stringResource(R.string.detail_crop) to (plot.crop?.name ?: stringResource(R.string.plot_crop_none)),
        stringResource(R.string.detail_area) to "${plot.areaHectares.ds(1)} ha",
        stringResource(R.string.detail_sensor) to stringResource(
            if (plot.deviceOnline) R.string.detail_sensor_online else R.string.detail_sensor_offline,
        ),
        plot.lastReadingAt?.let { stringResource(R.string.detail_last_reading) to it },
    )
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            facts.forEachIndexed { index, (label, value) ->
                if (index > 0) HorizontalDivider(color = Line)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun History(crop: Crop?, readings: List<Reading>) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                stringResource(R.string.detail_history),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            if (readings.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.detail_no_readings),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            if (crop != null) {
                Text(
                    stringResource(R.string.detail_history_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            EcChart(readings, crop?.thresholdDsPerM)
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.detail_recent), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            readings.takeLast(5).asReversed().forEachIndexed { index, reading ->
                if (index > 0) HorizontalDivider(color = Line)
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    Text(
                        reading.timestamp,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    val over = crop != null && reading.ecDsPerM > crop.thresholdDsPerM
                    Text(
                        "${reading.ecDsPerM.ds()} dS/m",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (over) SalinityCritical else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/** Línea de la CE con el umbral punteado; el lector de pantalla recibe un resumen numérico. */
@Composable
private fun EcChart(readings: List<Reading>, threshold: Double?) {
    val values = readings.map { it.ecDsPerM }
    val top = maxOf(values.max(), threshold ?: 0.0) * 1.15
    val description = stringResource(
        R.string.detail_chart_description,
        values.size,
        values.min().ds(),
        values.max().ds(),
        values.last().ds(),
    )
    val lineColor = Emerald700
    val thresholdColor = SalinityCritical
    val gridColor = Line
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(160.dp)
            .semantics { contentDescription = description },
    ) {
        fun y(v: Double) = size.height - (v / top).toFloat() * size.height
        drawLine(gridColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
        threshold?.let {
            drawLine(
                color = thresholdColor,
                start = Offset(0f, y(it)),
                end = Offset(size.width, y(it)),
                strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
            )
        }
        if (values.size == 1) {
            drawCircle(lineColor, radius = 8f, center = Offset(size.width / 2, y(values[0])))
            return@Canvas
        }
        val stepX = size.width / (values.size - 1)
        val points = values.mapIndexed { i, v -> Offset(stepX * i, y(v)) }
        for (i in 0 until points.size - 1) {
            drawLine(lineColor, points[i], points[i + 1], strokeWidth = 6f, cap = StrokeCap.Round)
        }
        drawCircle(lineColor, radius = 9f, center = points.last())
    }
}

@Composable
private fun ChangeCropDialog(
    crops: List<Crop>,
    currentCropId: String?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.plot_change_crop)) },
        text = {
            LazyColumn {
                items(crops, key = { it.id }) { crop ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                "${crop.name} (${crop.thresholdDsPerM.ds(1)} dS/m)",
                                fontWeight = if (crop.id == currentCropId) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        onClick = { onSelect(crop.id) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
