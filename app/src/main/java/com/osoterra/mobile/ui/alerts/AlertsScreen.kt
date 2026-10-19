package com.osoterra.mobile.ui.alerts

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DoneAll
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.osoterra.mobile.ui.common.ScreenHeader
import com.osoterra.mobile.ui.common.SeverityChip
import com.osoterra.mobile.ui.common.SkeletonCard
import com.osoterra.mobile.ui.theme.Emerald700

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen() {
    val container = rememberAppContainer()
    val viewModel: AlertsViewModel = viewModel(factory = AlertsViewModel.provideFactory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.action_saved)

    LaunchedEffect(state.notice) {
        if (state.notice == Notice.ACTION_SAVED) {
            viewModel.noticeShown()
            snackbar.showSnackbar(savedMessage)
        }
    }

    Scaffold(
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
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "header") {
                    val pending = state.alerts.count { !it.acknowledged }
                    ScreenHeader(
                        eyebrow = stringResource(R.string.alerts_heading),
                        title = if (state.alerts.isEmpty()) stringResource(R.string.alerts_summary_none)
                        else pluralStringResource(R.plurals.alerts_summary, maxOf(pending, 1), maxOf(pending, 1)),
                    )
                }
                when {
                    state.isLoading -> items(3) { SkeletonCard(height = 170.dp) }
                    state.errorMessage != null && state.alerts.isEmpty() -> item(key = "error") {
                        ErrorCard(state.errorMessage!!, onRetry = viewModel::loadAlerts)
                    }
                    state.alerts.isEmpty() -> item(key = "empty") {
                        EmptyCard(
                            title = stringResource(R.string.alerts_empty),
                            hint = stringResource(R.string.alerts_empty_hint),
                        )
                    }
                    else -> items(state.alerts, key = { it.id }) { alert ->
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

@Composable
private fun AlertCard(alert: Alert, onAcknowledge: () -> Unit, onRecordAction: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SeverityChip(alert.severity)
                Spacer(Modifier.weight(1f))
                if (alert.acknowledged) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.DoneAll,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(
                            stringResource(R.string.alerts_acknowledged),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(alert.plotName, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(alert.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(alert.createdAt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onRecordAction,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.alerts_record_action)) }
                if (!alert.acknowledged) {
                    TextButton(onClick = onAcknowledge, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.alerts_acknowledge), color = Emerald700)
                    }
                }
            }
        }
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
                        label = { Text(option.label) },
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
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                ) { Text(stringResource(R.string.common_cancel)) }
                Button(
                    onClick = { onSave(type, notes) },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                ) { Text(stringResource(R.string.action_save)) }
            }
        }
    }
}
