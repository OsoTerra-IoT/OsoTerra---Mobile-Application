package com.osoterra.mobile.ui.dashboard

import com.osoterra.mobile.ui.common.StatusBarScrim
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.osoterra.mobile.R
import com.osoterra.mobile.di.rememberAppContainer
import com.osoterra.mobile.domain.model.AdvisoryRequest
import com.osoterra.mobile.domain.model.Plot
import com.osoterra.mobile.domain.model.SalinityLevel
import com.osoterra.mobile.ui.common.ArrowButton
import com.osoterra.mobile.ui.common.BandAction
import com.osoterra.mobile.ui.common.BandHeadline
import com.osoterra.mobile.ui.common.BandTopRow
import com.osoterra.mobile.ui.common.BigReading
import com.osoterra.mobile.ui.common.ErrorCard
import com.osoterra.mobile.ui.common.LevelMeter
import com.osoterra.mobile.ui.common.PlotTile
import com.osoterra.mobile.ui.common.RiskSparkline
import com.osoterra.mobile.ui.common.SalinityChip
import com.osoterra.mobile.ui.common.SerenaBand
import com.osoterra.mobile.ui.common.SerenaCard
import com.osoterra.mobile.ui.common.SerenaGutter
import com.osoterra.mobile.ui.common.SkeletonCard
import com.osoterra.mobile.ui.common.SoftIcon
import com.osoterra.mobile.ui.common.StaggeredEntrance
import com.osoterra.mobile.ui.common.TileGrid
import com.osoterra.mobile.ui.theme.Emerald700
import com.osoterra.mobile.ui.theme.SageInk
import com.osoterra.mobile.ui.theme.SurfaceGreen
import com.osoterra.mobile.ui.theme.SurfaceGreenBorder
import kotlin.math.roundToInt

/**
 * Inicio «Banda serena y tarjeta»: la banda dice en una frase cómo está el campo y muestra el
 * medidor por parcela; debajo, una sola tarjeta grande con la parcela más urgente y el resto en
 * mosaicos. Para el asesor, lo mismo con las parcelas de sus clientes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onPlotClick: (String) -> Unit,
    onCreatePlot: () -> Unit,
    onOpenAlerts: () -> Unit = {},
) {
    val container = rememberAppContainer()
    val viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.provideFactory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(contentWindowInsets = WindowInsets(0), containerColor = MaterialTheme.colorScheme.background) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "band") { Band(state, onCreatePlot) }
                state.requests.forEach { request ->
                    item(key = "request-${request.id}") {
                        Gutter(Modifier.animateItem()) {
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
                        Gutter(Modifier.animateItem()) { AdvisoryOutcomeCard(outcome) }
                    }
                }
                when {
                    state.isLoading -> {
                        item(key = "skeleton-focus") { Gutter(Modifier.animateItem()) { SkeletonCard(height = 300.dp) } }
                        item(key = "skeleton-tiles") { Gutter(Modifier.animateItem()) { SkeletonCard(height = 96.dp) } }
                    }
                    state.errorMessage != null && state.plots.isEmpty() -> item(key = "error") {
                        Gutter(Modifier.animateItem()) { ErrorCard(state.errorMessage!!, onRetry = viewModel::loadPlots) }
                    }
                    state.plots.isEmpty() -> item(key = "empty") {
                        Gutter(Modifier.animateItem()) {
                            StaggeredEntrance(0) { EmptyHome(isAdvisor = state.isAdvisor, onCreatePlot = onCreatePlot) }
                        }
                    }
                    else -> {
                        state.focusPlot?.let { focus ->
                            item(key = "focus-${focus.id}") {
                                Gutter(Modifier.animateItem()) {
                                    StaggeredEntrance(0) {
                                        FocusCard(
                                            plot = focus,
                                            title = state.titleOf(focus),
                                            client = state.clients[focus.id],
                                            trend = state.trends[focus.id].orEmpty(),
                                            trendDays = state.trendDays[focus.id],
                                            onOpen = { onPlotClick(focus.id) },
                                            onWhatToDo = onOpenAlerts,
                                        )
                                    }
                                }
                            }
                        }
                        if (state.otherPlots.isNotEmpty()) {
                            item(key = "tiles") {
                                Gutter(Modifier.animateItem()) {
                                    StaggeredEntrance(1) {
                                        TileGrid(state.otherPlots) { plot, modifier ->
                                            PlotTile(
                                                title = state.titleOf(plot),
                                                subtitle = state.clients[plot.id],
                                                level = plot.salinityLevel,
                                                onClick = { onPlotClick(plot.id) },
                                                modifier = modifier,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            StatusBarScrim()
        }
    }
}

@Composable
private fun Gutter(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.padding(horizontal = SerenaGutter)) { content() }
}

@Composable
private fun Band(state: DashboardUiState, onCreatePlot: () -> Unit) {
    SerenaBand {
        BandTopRow {
            if (!state.isAdvisor) BandAction(Icons.Outlined.Add, stringResource(R.string.plot_new), onCreatePlot)
        }
        val total = state.plots.size
        val attention = state.needingAttention.size
        val title = when {
            state.isLoading -> " "
            state.errorMessage != null && total == 0 -> stringResource(R.string.dashboard_hello_anonymous)
            total == 0 -> stringResource(if (state.isAdvisor) R.string.band_advisor_empty else R.string.band_empty)
            state.isAdvisor && attention > 0 -> pluralStringResource(R.plurals.band_advisor_attention, attention, attention, total)
            state.isAdvisor -> stringResource(R.string.band_advisor_clear)
            attention > 0 -> pluralStringResource(R.plurals.band_attention, attention, attention, total)
            state.onWatch.isNotEmpty() -> stringResource(R.string.band_no_attention)
            else -> pluralStringResource(R.plurals.band_all_clear, total, total)
        }
        BandHeadline(
            greeting = if (state.firstName.isBlank()) null else stringResource(R.string.dashboard_hello, state.firstName),
            title = title,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (!state.isLoading && total > 0) {
            LevelMeter(state.plots.map { it.salinityLevel }, modifier = Modifier.padding(top = 20.dp))
        }
    }
}

/**
 * Tarjeta de la parcela más urgente: nivel con ícono, nombre, ECe grande, gráfico de 30 días
 * con la franja de riesgo y el umbral, una línea de contexto y la acción.
 */
@Composable
private fun FocusCard(
    plot: Plot,
    title: String,
    client: String?,
    trend: List<Double>,
    trendDays: Int?,
    onOpen: () -> Unit,
    onWhatToDo: () -> Unit,
) {
    val threshold = plot.crop?.thresholdDsPerM
    val ratio = if (plot.lastEcDsPerM != null && threshold != null && threshold > 0) plot.lastEcDsPerM / threshold else null
    // Toda la tarjeta abre la parcela; el botón lleva a lo que hay que hacer (la alerta).
    SerenaCard(onClick = onOpen) {
        SalinityChip(plot.salinityLevel)
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = 12.dp)
                .semantics { heading() },
        )
        if (client != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Icon(Icons.Outlined.Person, contentDescription = null, tint = SageInk, modifier = Modifier.size(16.dp))
                Text(
                    stringResource(R.string.focus_client, client),
                    style = MaterialTheme.typography.bodySmall,
                    color = SageInk,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
        BigReading(plot.lastEcDsPerM, modifier = Modifier.padding(top = 6.dp))
        if (trend.size >= 2) {
            RiskSparkline(values = trend, threshold = threshold, modifier = Modifier.padding(top = 12.dp))
        }
        val caption = focusCaption(ratio, trend, trendDays)
            ?: when {
                plot.lastEcDsPerM == null -> stringResource(R.string.detail_no_readings)
                threshold == null -> stringResource(R.string.detail_no_crop)
                else -> null
            }
        if (caption != null) {
            Text(
                caption,
                style = MaterialTheme.typography.bodySmall,
                color = SageInk,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        val needsAction = plot.salinityLevel == SalinityLevel.CRITICAL || plot.salinityLevel == SalinityLevel.HIGH
        ArrowButton(
            text = stringResource(if (needsAction) R.string.focus_see_what_to_do else R.string.focus_see_plot),
            onClick = if (needsAction) onWhatToDo else onOpen,
            modifier = Modifier.padding(top = 18.dp),
        )
    }
}

/** «153 % del umbral · subiendo en 30 días»: solo con datos reales; si falta todo, null. */
@Composable
private fun focusCaption(ratio: Double?, trend: List<Double>, days: Int?): String? {
    val parts = mutableListOf<String>()
    if (ratio != null) parts += stringResource(R.string.ratio_short, (ratio * 100).roundToInt())
    if (trend.size >= 2) {
        val delta = trend.last() - trend.first()
        val direction = when {
            delta > 0.05 -> 1
            delta < -0.05 -> -1
            else -> 0
        }
        parts += if (days != null) {
            stringResource(
                when (direction) {
                    1 -> R.string.trend_days_up
                    -1 -> R.string.trend_days_down
                    else -> R.string.trend_days_flat
                },
                days,
            )
        } else {
            stringResource(
                when (direction) {
                    1 -> R.string.trend_readings_up
                    -1 -> R.string.trend_readings_down
                    else -> R.string.trend_readings_flat
                },
                trend.size,
            )
        }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

/** Sin parcelas: un mensaje amable y una sola acción, «Registrar parcela». */
@Composable
private fun EmptyHome(isAdvisor: Boolean, onCreatePlot: () -> Unit) {
    SerenaCard {
        SoftIcon(Icons.Outlined.Landscape, size = 56.dp)
        Text(
            stringResource(if (isAdvisor) R.string.band_advisor_empty else R.string.dashboard_empty),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(top = 16.dp)
                .semantics { heading() },
        )
        Text(
            stringResource(if (isAdvisor) R.string.advisor_empty_hint else R.string.dashboard_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = SageInk,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (!isAdvisor) {
            ArrowButton(
                text = stringResource(R.string.plot_register),
                onClick = onCreatePlot,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
    }
}

/** Solicitud de un asesor para ver las parcelas: el productor la acepta o la rechaza. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdvisoryRequestCard(
    request: AdvisoryRequest,
    busy: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = SurfaceGreen,
        border = BorderStroke(1.dp, SurfaceGreenBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SoftIcon(Icons.Outlined.PersonAdd)
                Spacer(Modifier.size(12.dp))
                Text(
                    text = stringResource(R.string.advisory_request_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.advisory_request_body, request.advisorName),
                style = MaterialTheme.typography.bodyMedium,
            )
            val details = listOfNotNull(request.advisorEmail.takeIf { it.isNotBlank() }, request.requestedAt)
            if (details.isNotEmpty()) {
                Text(
                    text = details.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = SageInk,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    border = BorderStroke(1.dp, Emerald700),
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
            .background(SurfaceGreen, RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Emerald700)
        Spacer(Modifier.size(12.dp))
        Text(
            text = stringResource(
                if (outcome.accepted) R.string.advisory_request_accepted else R.string.advisory_request_declined,
                outcome.advisorName,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
