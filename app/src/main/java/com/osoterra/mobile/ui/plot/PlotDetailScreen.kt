package com.osoterra.mobile.ui.plot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.osoterra.mobile.R
import com.osoterra.mobile.domain.model.Crop
import com.osoterra.mobile.domain.model.Plot
import com.osoterra.mobile.domain.model.Reading
import com.osoterra.mobile.di.rememberAppContainer
import com.osoterra.mobile.ui.common.SalinityChip
import com.osoterra.mobile.ui.common.color

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
        topBar = {
            TopAppBar(
                title = { Text(state.plot?.name ?: "Parcela") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (state.plot != null) {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val plot = state.plot
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                plot == null -> Text(
                    text = state.errorMessage ?: "Error",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
                else -> Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CurrentStatusCard(plot)
                    HistoryCard(state.readings)
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
            androidx.compose.foundation.lazy.LazyColumn {
                items(crops.size) { index ->
                    val crop = crops[index]
                    DropdownMenuItem(
                        text = {
                            Text(
                                "${crop.name}  ·  ${"%.1f".format(crop.thresholdDsPerM)} dS/m",
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

@Composable
private fun CurrentStatusCard(plot: Plot) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Estado actual", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = plot.lastEcDsPerM?.let { "%.1f".format(it) } ?: "—",
                        style = MaterialTheme.typography.headlineLarge,
                        color = plot.salinityLevel.color(),
                        fontWeight = FontWeight.Bold,
                    )
                    Text("dS/m", style = MaterialTheme.typography.bodyMedium)
                }
                SalinityChip(level = plot.salinityLevel)
            }
            Spacer(Modifier.height(12.dp))
            plot.crop?.let { crop ->
                Text(
                    "Cultivo: ${crop.name} · umbral ${"%.1f".format(crop.thresholdDsPerM)} dS/m",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            plot.lastReadingAt?.let {
                Text(
                    "Última medición: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HistoryCard(readings: List<Reading>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Histórico de conductividad eléctrica",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(16.dp))
            if (readings.isEmpty()) {
                Text("Sin lecturas disponibles", style = MaterialTheme.typography.bodyLarge)
            } else {
                EcLineChart(readings)
            }
        }
    }
}

@Composable
private fun EcLineChart(readings: List<Reading>) {
    val values = readings.map { it.ecDsPerM }
    val maxV = (values.maxOrNull() ?: 1.0)
    val minV = (values.minOrNull() ?: 0.0)
    val range = (maxV - minV).takeIf { it > 0.0 } ?: 1.0
    val lineColor = MaterialTheme.colorScheme.primary

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(160.dp),
    ) {
        if (values.size < 2) return@Canvas
        val stepX = size.width / (values.size - 1)
        val points = values.mapIndexed { i, v ->
            val x = stepX * i
            val y = size.height - ((v - minV) / range).toFloat() * size.height
            Offset(x, y)
        }
        for (i in 0 until points.size - 1) {
            drawLine(
                color = lineColor,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 5f,
            )
        }
    }
}
