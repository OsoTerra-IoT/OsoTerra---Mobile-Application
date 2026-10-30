package com.osoterra.mobile.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import com.osoterra.mobile.ui.common.isLargeFont
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.ui.text.style.TextOverflow
import com.osoterra.mobile.ui.common.AdaptiveTiles
import com.osoterra.mobile.ui.common.EceSparkline
import com.osoterra.mobile.ui.common.IconStat
import com.osoterra.mobile.ui.common.LevelCounts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.osoterra.mobile.R
import com.osoterra.mobile.di.rememberAppContainer
import com.osoterra.mobile.domain.model.AdvisoryRequest
import com.osoterra.mobile.domain.model.Plot
import com.osoterra.mobile.domain.model.SalinityLevel
import com.osoterra.mobile.ui.common.EceHelpButton
import com.osoterra.mobile.ui.common.EmptyCard
import com.osoterra.mobile.ui.common.ErrorCard
import com.osoterra.mobile.ui.common.SalinityChip
import com.osoterra.mobile.ui.common.SkeletonCard
import com.osoterra.mobile.ui.common.SalinityStrip
import com.osoterra.mobile.ui.common.OsoMotion
import com.osoterra.mobile.ui.common.StaggeredEntrance
import com.osoterra.mobile.ui.common.color
import com.osoterra.mobile.ui.common.dsPerM
import com.osoterra.mobile.ui.common.fmt
import com.osoterra.mobile.ui.common.icon
import com.osoterra.mobile.ui.common.label
import com.osoterra.mobile.ui.common.motionTween
import com.osoterra.mobile.ui.common.rememberReducedMotion
import com.osoterra.mobile.ui.theme.Emerald700
import com.osoterra.mobile.ui.theme.SurfaceGreen
import com.osoterra.mobile.ui.theme.SurfaceGreenBorder
import com.osoterra.mobile.ui.theme.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onPlotClick: (String) -> Unit,
    onCreatePlot: () -> Unit,
) {
    val container = rememberAppContainer()
    val viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.provideFactory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val largeFont = isLargeFont()
    val fabExpanded by remember(largeFont) {
        derivedStateOf { !largeFont && listState.firstVisibleItemIndex == 0 }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            // Se reduce a solo el ícono al bajar por la lista o con fuente grande: no tapa las tarjetas.
            val label = stringResource(R.string.plot_new)
            ExtendedFloatingActionButton(
                onClick = onCreatePlot,
                expanded = fabExpanded,
                modifier = Modifier.semantics { contentDescription = label },
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
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "header") { Header(state, onRefresh = viewModel::refresh) }
                state.requests.forEach { request ->
                    item(key = "request-${request.id}") {
                        Box(Modifier.animateItem()) {
                            AdvisoryRequestCard(
                                request = request,
                                busy = state.respondingTo == request.id,
                                onAccept = { viewModel.acceptRequest(request) },
                                onDecline = { viewModel.declineRequest(request) },
                            )
                        }
                    }
                }
                state.requestOutcome?.let { outcome ->
                    item(key = "request-outcome") {
                        Box(Modifier.animateItem()) { AdvisoryOutcomeCard(outcome) }
                    }
                }
                when {
                    state.isLoading -> items(3, key = { "skeleton-$it" }) {
                        Box(Modifier.animateItem()) { SkeletonCard(height = 190.dp) }
                    }
                    state.errorMessage != null && state.plots.isEmpty() -> item(key = "error") {
                        Box(Modifier.animateItem()) { ErrorCard(state.errorMessage!!, onRetry = viewModel::loadPlots) }
                    }
                    state.plots.isEmpty() -> item(key = "empty") {
                        Box(Modifier.animateItem()) {
                            EmptyCard(
                                title = stringResource(R.string.dashboard_empty),
                                hint = stringResource(R.string.dashboard_empty_hint),
                                icon = Icons.Outlined.Landscape,
                                actionLabel = stringResource(R.string.plot_new),
                                onAction = onCreatePlot,
                            )
                        }
                    }
                    else -> {
                        item(key = "stats") { Box(Modifier.animateItem()) { StaggeredEntrance(0) { Stats(state) } } }
                        item(key = "section") {
                            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.dashboard_section),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier
                                        .weight(1f)
                                        .semantics { heading() },
                                )
                                // La explicación de la franja y del gráfico vive en la ayuda (ícono de info).
                                EceHelpButton(extra = stringResource(R.string.dashboard_legend))
                            }
                        }
                        itemsIndexed(state.sortedPlots, key = { _, plot -> plot.id }) { index, plot ->
                            StaggeredEntrance(index + 1, Modifier.animateItem()) {
                                PlotRow(plot = plot, trend = state.trends[plot.id].orEmpty(), index = index, onClick = { onPlotClick(plot.id) })
                            }
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
        val reducedMotion = rememberReducedMotion()
        AnimatedVisibility(
            visible = !state.isLoading && state.plots.isNotEmpty(),
            enter = fadeIn(motionTween(OsoMotion.ENTER, reducedMotion)) + expandVertically(motionTween(OsoMotion.ENTER, reducedMotion)),
            exit = fadeOut(motionTween(OsoMotion.STATE, reducedMotion)) + shrinkVertically(motionTween(OsoMotion.STATE, reducedMotion)),
        ) {
            val attention = state.needingAttention.size
            val watch = state.onWatch.size
            val total = state.plots.size
            Column(Modifier.padding(top = 4.dp, bottom = 8.dp)) {
                Text(
                    text = when {
                        attention > 0 -> pluralStringResource(R.plurals.dashboard_attention, attention, attention, total)
                        watch > 0 -> stringResource(R.string.dashboard_no_attention)
                        else -> pluralStringResource(R.plurals.dashboard_all_clear, total, total)
                    },
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .animateContentSize(motionTween(OsoMotion.STATE, reducedMotion))
                        .semantics {
                            heading()
                            liveRegion = LiveRegionMode.Polite
                        },
                )
                // Conteo por nivel en píldoras (ícono + número + palabra), sin frases largas.
                LevelCounts(
                    levels = state.plots.map { it.salinityLevel },
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun Stats(state: DashboardUiState) {
    val plotsLabel = stringResource(R.string.dashboard_stat_plots)
    val alertsLabel = stringResource(R.string.dashboard_stat_alerts)
    val devicesLabel = stringResource(R.string.dashboard_stat_devices)
    AdaptiveTiles(count = 3) { index, modifier, inline ->
        when (index) {
            0 -> IconStat(
                icon = Icons.Outlined.Landscape,
                value = state.plots.size.toString(),
                label = "$plotsLabel · ${state.totalHectares.fmt(1)}\u00A0ha",
                modifier = modifier,
                inline = inline,
            )
            1 -> IconStat(
                icon = Icons.Outlined.NotificationsActive,
                value = state.activeAlerts.toString(),
                label = alertsLabel,
                valueColor = if (state.activeAlerts > 0) SalinityLevel.CRITICAL.color() else MaterialTheme.colorScheme.onSurface,
                modifier = modifier,
                inline = inline,
            )
            else -> IconStat(
                icon = Icons.Outlined.Sensors,
                value = "${state.devicesOnline}/${state.plots.size}",
                label = devicesLabel,
                modifier = modifier,
                inline = inline,
            )
        }
    }
}

/**
 * Tarjeta de parcela compacta: nombre, cultivo y área en una línea, la ECe en grande, el mini
 * gráfico de tendencia y la franja frente al umbral. Los detalles se leen en el detalle.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlotRow(plot: Plot, trend: List<Double>, index: Int, onClick: () -> Unit) {
    val threshold = plot.crop?.thresholdDsPerM
    val ratio = if (plot.lastEcDsPerM != null && threshold != null && threshold > 0) plot.lastEcDsPerM / threshold else null
    val description = stringResource(
        R.string.dashboard_plot_description,
        plot.name,
        plot.salinityLevel.label(),
        plot.lastEcDsPerM?.dsPerM() ?: stringResource(R.string.salinity_no_data),
    )
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(20.dp).animateContentSize()) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        plot.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(
                            Icons.Outlined.Grass,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(16.dp),
                        )
                        Text(
                            text = listOfNotNull(
                                plot.crop?.name ?: stringResource(R.string.plot_crop_none),
                                "${plot.areaHectares.fmt(1)}\u00A0ha",
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = plot.lastEcDsPerM?.fmt(2) ?: "—",
                        style = MaterialTheme.typography.headlineMedium,
                        color = plot.salinityLevel.color(),
                        maxLines = 1,
                    )
                    Text(stringResource(R.string.unit_ds_m), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (trend.size >= 2) {
                Spacer(Modifier.height(12.dp))
                EceSparkline(values = trend, threshold = threshold)
            }
            Spacer(Modifier.height(12.dp))
            SalinityStrip(ratio = ratio, level = plot.salinityLevel, delayMillis = 120 + index * 90)
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                SalinityChip(level = plot.salinityLevel)
                if (ratio != null) {
                    Text(
                        text = stringResource(R.string.ratio_short, (ratio * 100).toInt()),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (plot.deviceOnline) Icons.Outlined.Schedule else Icons.Outlined.CloudOff,
                        contentDescription = if (plot.deviceOnline) null else stringResource(R.string.dashboard_device_offline),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = plot.lastReadingAt ?: stringResource(R.string.dashboard_no_readings),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }
}

/** Solicitud de un asesor para ver las parcelas: el productor la acepta o la rechaza. */
@Composable
private fun AdvisoryRequestCard(
    request: AdvisoryRequest,
    busy: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SurfaceGreen,
        border = BorderStroke(1.dp, SurfaceGreenBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.PersonAdd, contentDescription = null, tint = Emerald700)
                Spacer(Modifier.size(10.dp))
                Text(
                    text = stringResource(R.string.advisory_request_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.advisory_request_body, request.advisorName),
                style = MaterialTheme.typography.bodyLarge,
            )
            val details = listOfNotNull(request.advisorEmail.takeIf { it.isNotBlank() }, request.requestedAt)
            if (details.isNotEmpty()) {
                Text(
                    text = details.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAccept,
                    enabled = !busy,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.advisory_request_accept), style = MaterialTheme.typography.labelLarge) }
                OutlinedButton(
                    onClick = onDecline,
                    enabled = !busy,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.advisory_request_decline), style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

@Composable
private fun AdvisoryOutcomeCard(outcome: RequestOutcome) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceGreen, RoundedCornerShape(20.dp))
            .padding(16.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Emerald700)
        Spacer(Modifier.size(10.dp))
        Text(
            text = stringResource(
                if (outcome.accepted) R.string.advisory_request_accepted else R.string.advisory_request_declined,
                outcome.advisorName,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

