package com.osoterra.mobile.ui.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.osoterra.mobile.R
import com.osoterra.mobile.di.rememberAppContainer
import com.osoterra.mobile.domain.model.Plot
import com.osoterra.mobile.ui.common.SalinityChip
import com.osoterra.mobile.ui.common.SalinityStrip
import com.osoterra.mobile.ui.common.color
import com.osoterra.mobile.ui.common.rememberReducedMotion
import com.osoterra.mobile.ui.theme.Emerald700
import com.osoterra.mobile.ui.theme.Track
import java.util.Locale

private val ES = Locale.forLanguageTag("es-PE")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onPlotClick: (String) -> Unit,
    onCreatePlot: () -> Unit,
) {
    val container = rememberAppContainer()
    val viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.provideFactory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreatePlot,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.plot_new), style = MaterialTheme.typography.labelLarge) },
                containerColor = Emerald700,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "header") { Header(state, onRefresh = viewModel::refresh) }
                when {
                    state.isLoading -> items(3) { PlotSkeleton() }
                    state.errorMessage != null && state.plots.isEmpty() -> item(key = "error") {
                        ErrorState(state.errorMessage!!, onRetry = viewModel::loadPlots)
                    }
                    state.plots.isEmpty() -> item(key = "empty") { EmptyState() }
                    else -> {
                        item(key = "stats") { Stats(state) }
                        item(key = "section") {
                            Text(
                                text = stringResource(R.string.dashboard_section),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .semantics { heading() },
                            )
                        }
                        itemsIndexed(state.sortedPlots, key = { _, plot -> plot.id }) { index, plot ->
                            StaggeredEntrance(index) {
                                PlotRow(plot = plot, index = index, onClick = { onPlotClick(plot.id) })
                            }
                        }
                        item(key = "legend") {
                            Text(
                                text = stringResource(R.string.dashboard_legend),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(state: DashboardUiState, onRefresh: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (state.firstName.isBlank()) stringResource(R.string.dashboard_hello_anonymous)
                else stringResource(R.string.dashboard_hello, state.firstName),
                style = MaterialTheme.typography.titleMedium,
                color = Emerald700,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRefresh) {
                Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.dashboard_refresh))
            }
        }
        if (!state.isLoading && state.plots.isNotEmpty()) {
            val attention = state.needingAttention.size
            val total = state.plots.size
            Text(
                text = if (attention == 0) pluralStringResource(R.plurals.dashboard_all_clear, total, total)
                else pluralStringResource(R.plurals.dashboard_attention, attention, attention, total),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(top = 4.dp, bottom = 8.dp)
                    .semantics { heading() },
            )
        }
    }
}

@Composable
private fun Stats(state: DashboardUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(
            value = state.plots.size.toString(),
            suffix = String.format(ES, "%.1f ha", state.totalHectares),
            label = stringResource(R.string.dashboard_stat_plots),
            modifier = Modifier.weight(1f),
        )
        StatTile(
            value = state.activeAlerts.toString(),
            suffix = null,
            label = stringResource(R.string.dashboard_stat_alerts),
            modifier = Modifier.weight(1f),
        )
        StatTile(
            value = state.devicesOnline.toString(),
            suffix = "/ ${state.plots.size}",
            label = stringResource(R.string.dashboard_stat_devices),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatTile(value: String, suffix: String?, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            Text(
                text = buildAnnotatedString {
                    append(value)
                    if (suffix != null) {
                        withStyle(
                            SpanStyle(
                                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        ) { append(" $suffix") }
                    }
                },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlotRow(plot: Plot, index: Int, onClick: () -> Unit) {
    val threshold = plot.crop?.thresholdDsPerM
    val ratio = if (plot.lastEcDsPerM != null && threshold != null && threshold > 0) plot.lastEcDsPerM / threshold else null
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(plot.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = listOfNotNull(
                            plot.farmName,
                            plot.crop?.name ?: stringResource(R.string.plot_crop_none),
                            String.format(ES, "%.1f ha", plot.areaHectares),
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = plot.lastEcDsPerM?.let { String.format(ES, "%.2f", it) } ?: "—",
                        style = MaterialTheme.typography.headlineMedium,
                        color = plot.salinityLevel.color(),
                    )
                    Text("dS/m", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            SalinityStrip(ratio = ratio, level = plot.salinityLevel, delayMillis = 120 + index * 90)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SalinityChip(level = plot.salinityLevel)
                Spacer(Modifier.weight(1f))
                if (ratio != null && threshold != null) {
                    Text(
                        text = stringResource(R.string.dashboard_ratio, (ratio * 100).toInt(), String.format(ES, "%.1f", threshold)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!plot.deviceOnline) {
                    Icon(
                        Icons.Outlined.CloudOff,
                        contentDescription = stringResource(R.string.dashboard_device_offline),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = plot.lastReadingAt?.let { stringResource(R.string.dashboard_last_reading, it) }
                        ?: stringResource(R.string.dashboard_no_readings),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Aparición escalonada: cada tarjeta sube y se aclara un poco después de la anterior. */
@Composable
private fun StaggeredEntrance(index: Int, content: @Composable () -> Unit) {
    val reducedMotion = rememberReducedMotion()
    val progress = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = 420, delayMillis = index * 70, easing = FastOutSlowInEasing))
    }
    Box(
        Modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * 24.dp.toPx()
        },
    ) { content() }
}

@Composable
private fun PlotSkeleton() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth(0.5f).height(20.dp).background(Track, RoundedCornerShape(8.dp)))
            Box(Modifier.fillMaxWidth(0.7f).height(14.dp).background(Track, RoundedCornerShape(8.dp)))
            Box(Modifier.fillMaxWidth().height(12.dp).background(Track, RoundedCornerShape(8.dp)))
        }
    }
}

@Composable
private fun EmptyState() {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            Text(stringResource(R.string.dashboard_empty), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.dashboard_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.errorContainer) {
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry, shape = RoundedCornerShape(14.dp)) { Text(stringResource(R.string.dashboard_retry)) }
        }
    }
}
