package com.osoterra.mobile.ui.alerts

import com.osoterra.mobile.ui.common.StatusBarScrim
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import com.osoterra.mobile.ui.common.SalinityStrip
import com.osoterra.mobile.ui.common.asLevel
import com.osoterra.mobile.ui.common.fmt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.osoterra.mobile.R
import com.osoterra.mobile.di.rememberAppContainer
import com.osoterra.mobile.domain.model.Alert
import com.osoterra.mobile.domain.model.CorrectiveActionType
import com.osoterra.mobile.ui.common.EmptyCard
import com.osoterra.mobile.ui.common.ErrorCard
import com.osoterra.mobile.ui.common.ArrowButton
import com.osoterra.mobile.ui.common.BandHeader
import com.osoterra.mobile.ui.common.BigReading
import com.osoterra.mobile.ui.common.SectionLabel
import com.osoterra.mobile.ui.common.SerenaCard
import com.osoterra.mobile.ui.common.SerenaGutter
import com.osoterra.mobile.ui.theme.SageInk
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import com.osoterra.mobile.ui.common.SeverityChip
import com.osoterra.mobile.ui.common.OsoMotion
import com.osoterra.mobile.ui.common.SkeletonCard
import com.osoterra.mobile.ui.common.StaggeredEntrance
import com.osoterra.mobile.ui.common.color
import com.osoterra.mobile.ui.common.dsPerM
import com.osoterra.mobile.ui.common.motionTween
import com.osoterra.mobile.ui.common.rememberReducedMotion
import com.osoterra.mobile.ui.theme.Emerald700
import com.osoterra.mobile.ui.theme.Line
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(onPendingCount: (Int) -> Unit = {}) {
    val container = rememberAppContainer()
    val viewModel: AlertsViewModel = viewModel(factory = AlertsViewModel.provideFactory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.action_saved)
    val acknowledgedMessage = stringResource(R.string.alerts_acknowledged_notice)

    // La insignia de la pestaña se actualiza al marcar o resolver una alerta.
    LaunchedEffect(state.alerts, state.isLoading) {
        if (!state.isLoading) onPendingCount(state.alerts.count { !it.acknowledged })
    }

    LaunchedEffect(state.notice) {
        val message = when (state.notice) {
            Notice.ACTION_SAVED -> savedMessage
            Notice.ACKNOWLEDGED -> acknowledgedMessage
            null -> return@LaunchedEffect
        }
        viewModel.noticeShown()
        snackbar.showSnackbar(message)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = false,
            onRefresh = viewModel::loadAlerts,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "header") {
                    val pendingCount = state.alerts.count { !it.acknowledged }
                    BandHeader(
                        eyebrow = stringResource(R.string.alerts_heading),
                        title = when {
                            // Mientras carga no se afirma nada: el titular llega con los datos.
                            state.isLoading -> ""
                            pendingCount == 0 -> stringResource(R.string.alerts_summary_none)
                            else -> pluralStringResource(R.plurals.alerts_summary, pendingCount, pendingCount)
                        },
                    )
                }
                val pending = state.alerts.filter { !it.acknowledged }
                val done = state.alerts.filter { it.acknowledged }
                when {
                    state.isLoading -> items(2, key = { "skeleton-$it" }) {
                        Gutter(Modifier.animateItem()) { SkeletonCard(height = 220.dp) }
                    }
                    state.errorMessage != null && state.alerts.isEmpty() -> item(key = "error") {
                        Gutter(Modifier.animateItem()) { ErrorCard(state.errorMessage!!, onRetry = viewModel::loadAlerts) }
                    }
                    state.alerts.isEmpty() -> item(key = "empty") {
                        Gutter(Modifier.animateItem()) {
                            EmptyCard(
                                title = stringResource(R.string.alerts_empty),
                                hint = stringResource(R.string.alerts_empty_hint),
                                icon = Icons.Outlined.NotificationsNone,
                            )
                        }
                    }
                    else -> {
                        if (pending.isNotEmpty() && done.isNotEmpty()) {
                            item(key = "label-pending") {
                                Gutter(Modifier.animateItem()) { SectionLabel(stringResource(R.string.alerts_pending_section)) }
                            }
                        }
                        itemsIndexed(pending, key = { _, alert -> alert.id }) { index, alert ->
                            // animateItem: al marcarla como atendida, la tarjeta baja con movimiento.
                            Gutter(Modifier.animateItem()) {
                                StaggeredEntrance(index) {
                                    AlertCard(
                                        alert = alert,
                                        onAcknowledge = { viewModel.acknowledge(alert.id) },
                                        onRecordAction = { viewModel.openAction(alert) },
                                    )
                                }
                            }
                        }
                        if (done.isNotEmpty()) {
                            item(key = "label-done") {
                                Gutter(Modifier.animateItem()) { SectionLabel(stringResource(R.string.alerts_done_section)) }
                            }
                        }
                        itemsIndexed(done, key = { _, alert -> alert.id }) { index, alert ->
                            Gutter(Modifier.animateItem()) {
                                StaggeredEntrance(pending.size + index) {
                                    AlertCard(
                                        alert = alert,
                                        onAcknowledge = { viewModel.acknowledge(alert.id) },
                                        onRecordAction = { viewModel.openAction(alert) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            StatusBarScrim()
        }
    }

    state.actionFor?.let { alert ->
        CorrectiveActionSheet(
            alert = alert,
            isSaving = state.isSaving,
            error = state.actionError,
            onDismiss = viewModel::dismissAction,
            onSave = viewModel::recordAction,
        )
    }
}

/** Mensaje de la alerta en el idioma de la app, con la ECe, el porcentaje y el umbral. */
@Composable
private fun Alert.displayMessage(): String {
    val observed = observedDsPerM
    val threshold = thresholdDsPerM
    if (observed == null || threshold == null || threshold <= 0.0) return message
    return stringResource(R.string.alerts_message, observed.dsPerM(), (observed / threshold * 100).roundToInt(), threshold.dsPerM(1))
}

@Composable
private fun CorrectiveActionType.label(): String = stringResource(
    when (this) {
        CorrectiveActionType.LEACHING -> R.string.action_leaching
        CorrectiveActionType.IRRIGATION_ADJUSTMENT -> R.string.action_irrigation_adjustment
        CorrectiveActionType.DRAINAGE_IMPROVEMENT -> R.string.action_drainage_improvement
        CorrectiveActionType.SOIL_AMENDMENT -> R.string.action_soil_amendment
        CorrectiveActionType.CROP_ROTATION -> R.string.action_crop_rotation
    },
)

@Composable
private fun Gutter(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.padding(horizontal = SerenaGutter)) { content() }
}

/**
 * Tarjeta de alerta serena: píldora del nivel, parcela, la ECe en grande con su % del umbral,
 * la hora y las dos acciones visibles. Atendida, la tarjeta se aclara y queda abajo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlertCard(alert: Alert, onAcknowledge: () -> Unit, onRecordAction: () -> Unit) {
    val reducedMotion = rememberReducedMotion()
    val alpha by animateFloatAsState(
        if (alert.acknowledged) 0.8f else 1f,
        motionTween(OsoMotion.STATE, reducedMotion),
        label = "alertAlpha",
    )
    SerenaCard(modifier = Modifier.graphicsLayer { this.alpha = alpha }) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            SeverityChip(alert.severity)
            AnimatedVisibility(
                visible = alert.acknowledged,
                enter = fadeIn(motionTween(OsoMotion.STATE, reducedMotion)),
                exit = fadeOut(motionTween(OsoMotion.STATE, reducedMotion)),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.DoneAll, contentDescription = null, tint = SageInk, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(stringResource(R.string.alerts_acknowledged), style = MaterialTheme.typography.labelMedium, color = SageInk)
                }
            }
        }
        Text(
            alert.plotName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 12.dp),
        )
        AlertReading(alert)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = SageInk, modifier = Modifier.size(16.dp))
            Text(
                alert.createdAt,
                style = MaterialTheme.typography.bodySmall,
                color = SageInk,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        if (alert.acknowledged) {
            OutlinedButton(
                onClick = onRecordAction,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Emerald700),
                modifier = Modifier
                    .padding(top = 16.dp)
                    .heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.alerts_record_action), color = Emerald700) }
        } else {
            ArrowButton(
                text = stringResource(R.string.alerts_record_action),
                onClick = onRecordAction,
                modifier = Modifier.padding(top = 16.dp),
            )
            TextButton(
                onClick = onAcknowledge,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = Emerald700)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.alerts_acknowledge), color = Emerald700, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * La lectura de la alerta en números: la ECe en grande y el % del umbral. El lector de
 * pantalla oye la frase completa de siempre.
 */
@Composable
private fun AlertReading(alert: Alert) {
    val observed = alert.observedDsPerM
    val threshold = alert.thresholdDsPerM
    val message = alert.displayMessage()
    if (observed == null || threshold == null || threshold <= 0.0) {
        if (message.isNotBlank()) {
            Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        }
        return
    }
    val ratio = observed / threshold
    Column(
        Modifier
            .padding(top = 4.dp)
            .clearAndSetSemantics { contentDescription = message },
    ) {
        BigReading(observed)
        Text(
            text = stringResource(R.string.ratio_threshold_short, (ratio * 100).roundToInt(), threshold.fmt(1)),
            style = MaterialTheme.typography.bodySmall,
            color = SageInk,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CorrectiveActionSheet(
    alert: Alert,
    isSaving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (CorrectiveActionType, String) -> Unit,
) {
    var type by rememberSaveable(alert.id) { mutableStateOf(CorrectiveActionType.LEACHING) }
    var notes by rememberSaveable(alert.id) { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                stringResource(R.string.action_sheet_title, alert.plotName),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.action_sheet_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CorrectiveActionType.entries.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = { type = option },
                        label = { Text(option.label()) },
                        leadingIcon = if (type == option) {
                            { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null,
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.action_notes)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isSaving,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                ) { Text(stringResource(R.string.common_cancel)) }
                Button(
                    onClick = { onSave(type, notes) },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                ) { Text(stringResource(R.string.action_save)) }
            }
        }
    }
}
