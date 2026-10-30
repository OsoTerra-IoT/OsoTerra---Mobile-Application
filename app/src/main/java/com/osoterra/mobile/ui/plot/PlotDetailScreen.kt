package com.osoterra.mobile.ui.plot

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.SensorsOff
import androidx.compose.material.icons.outlined.SquareFoot
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.osoterra.mobile.ui.common.EceSparkline
import com.osoterra.mobile.ui.common.FlowTiles
import com.osoterra.mobile.ui.common.IconBadge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import com.osoterra.mobile.ui.common.EceHelpButton
import com.osoterra.mobile.ui.common.ErrorCard
import com.osoterra.mobile.ui.common.OsoMotion
import com.osoterra.mobile.ui.common.SalinityChip
import com.osoterra.mobile.ui.common.StaggeredEntrance
import com.osoterra.mobile.ui.common.dsPerM
import com.osoterra.mobile.ui.common.fmt
import com.osoterra.mobile.ui.common.motionTween
import com.osoterra.mobile.ui.common.rememberReducedMotion
import com.osoterra.mobile.ui.common.SalinityStrip
import com.osoterra.mobile.ui.common.SkeletonCard
import com.osoterra.mobile.ui.common.color
import com.osoterra.mobile.ui.common.label
import com.osoterra.mobile.ui.theme.Line
import java.util.Locale


private enum class DetailPhase { LOADING, ERROR, CONTENT }

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
                title = { Text(state.plot?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
            val reducedMotion = rememberReducedMotion()
            // Carga → contenido con un fundido corto, sin saltos de tamaño bruscos.
            AnimatedContent(
                targetState = when {
                    state.isLoading -> DetailPhase.LOADING
                    plot == null -> DetailPhase.ERROR
                    else -> DetailPhase.CONTENT
                },
                transitionSpec = {
                    fadeIn(motionTween(OsoMotion.ENTER, reducedMotion)) togetherWith
                        fadeOut(motionTween(OsoMotion.STATE, reducedMotion)) using
                        SizeTransform(clip = false)
                },
                label = "detail",
            ) { phase ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (phase) {
                        DetailPhase.LOADING -> {
                            SkeletonCard(height = 200.dp)
                            SkeletonCard(height = 280.dp)
                        }
                        DetailPhase.ERROR -> ErrorCard(
                            state.errorMessage ?: stringResource(R.string.detail_load_error),
                            onRetry = viewModel::load,
                        )
                        DetailPhase.CONTENT -> if (plot != null) {
                            StaggeredEntrance(0) { StatusHero(plot, state.readings.takeLast(30).map { it.ecDsPerM }) }
                            StaggeredEntrance(1) { Facts(plot, state.readings.lastOrNull()) }
                            StaggeredEntrance(2) { History(plot.crop, state.readings) }
                        }
                    }
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
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusHero(plot: Plot, trend: List<Double>) {
    val threshold = plot.crop?.thresholdDsPerM
    val ratio = if (plot.lastEcDsPerM != null && threshold != null && threshold > 0) plot.lastEcDsPerM / threshold else null
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(20.dp).animateContentSize()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                SalinityChip(plot.salinityLevel, modifier = Modifier.semantics { heading() })
                if (ratio != null && threshold != null) {
                    Text(
                        stringResource(R.string.ratio_threshold_short, (ratio * 100).toInt(), threshold.fmt(1)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = buildAnnotatedString {
                        append(plot.lastEcDsPerM?.fmt(2) ?: "—")
                        withStyle(
                            SpanStyle(
                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        ) { append(" " + stringResource(R.string.unit_ds_m)) }
                    },
                    style = MaterialTheme.typography.displaySmall,
                    color = plot.salinityLevel.color(),
                    modifier = Modifier.weight(1f),
                )
                EceHelpButton()
            }
            when {
                plot.crop == null -> HintLine(stringResource(R.string.detail_no_crop))
                plot.lastEcDsPerM == null -> HintLine(stringResource(R.string.detail_no_readings))
            }
            if (trend.size >= 2) {
                Spacer(Modifier.height(12.dp))
                EceSparkline(values = trend, threshold = threshold, height = 56.dp)
            }
            Spacer(Modifier.height(14.dp))
            SalinityStrip(ratio = ratio, level = plot.salinityLevel)
        }
    }
}

@Composable
private fun HintLine(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

private data class Fact(val icon: ImageVector, val label: String, val value: String)

/** Datos de la parcela en mosaicos con ícono (dos por fila; uno por fila con fuente grande). */
@Composable
private fun Facts(plot: Plot, latest: Reading?) {
    val facts = listOfNotNull(
        latest?.let { Fact(Icons.Outlined.WaterDrop, stringResource(R.string.detail_moisture), "${it.humidityPct.fmt(0)} %") },
        latest?.let { Fact(Icons.Outlined.Thermostat, stringResource(R.string.detail_temperature), "${it.temperatureC.fmt(1)} °C") },
        Fact(Icons.Outlined.Grass, stringResource(R.string.detail_crop), plot.crop?.name ?: stringResource(R.string.plot_crop_none)),
        Fact(Icons.Outlined.SquareFoot, stringResource(R.string.detail_area), "${plot.areaHectares.fmt(1)} ha"),
        Fact(
            if (plot.deviceOnline) Icons.Outlined.Sensors else Icons.Outlined.SensorsOff,
            stringResource(R.string.detail_sensor),
            stringResource(if (plot.deviceOnline) R.string.detail_sensor_online else R.string.detail_sensor_offline),
        ),
        plot.lastReadingAt?.let { Fact(Icons.Outlined.Schedule, stringResource(R.string.detail_last_reading), it) },
    )
    FlowTiles { tileModifier ->
        facts.forEach { fact ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = tileModifier.semantics(mergeDescendants = true) {},
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(fact.icon)
                    Column(Modifier.padding(start = 10.dp)) {
                        Text(
                            fact.value,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            fact.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun History(crop: Crop?, readings: List<Reading>) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.detail_history),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                EceHelpButton(
                    extra = stringResource(if (crop != null) R.string.detail_history_hint else R.string.detail_history_hint_no_crop),
                )
            }
            if (readings.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.detail_no_readings),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            Spacer(Modifier.height(8.dp))
            EcChart(readings, crop?.thresholdDsPerM)
            Spacer(Modifier.height(12.dp))
            // Resumen en texto (guía 5.1: cada gráfico tiene resumen y tabla alternativa).
            Text(
                chartSummary(readings, crop?.thresholdDsPerM),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ReadingsTable(readings, crop?.thresholdDsPerM)
        }
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
                                "${crop.name} (${crop.thresholdDsPerM.dsPerM(1)})",
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
