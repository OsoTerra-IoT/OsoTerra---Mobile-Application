package com.osoterra.mobile.ui.settings

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.osoterra.mobile.R
import com.osoterra.mobile.di.rememberAppContainer
import com.osoterra.mobile.domain.model.AlertSeverity
import com.osoterra.mobile.ui.common.ErrorCard
import com.osoterra.mobile.ui.common.SerenaTopBar
import com.osoterra.mobile.ui.common.SerenaCard
import com.osoterra.mobile.ui.common.SoftIcon
import com.osoterra.mobile.ui.theme.OliveLine
import com.osoterra.mobile.ui.theme.SageInk
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.osoterra.mobile.ui.common.SeverityChip
import com.osoterra.mobile.ui.common.SkeletonCard

/** Niveles que el Backend distingue: En vigilancia, Alto y Muy alto (WATCH, WARNING, CRITICAL). */
private val selectableSeverities = AlertSeverity.entries

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
) {
    val container = rememberAppContainer()
    val viewModel: NotificationSettingsViewModel =
        viewModel(factory = NotificationSettingsViewModel.provideFactory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.notif_saved)

    LaunchedEffect(state.saved) {
        if (state.saved) {
            viewModel.savedShown()
            snackbar.showSnackbar(savedMessage)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            SerenaTopBar(title = stringResource(R.string.notif_settings_title), onBack = onBack)
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                state.isLoading -> {
                    SkeletonCard(height = 220.dp)
                    SkeletonCard(height = 140.dp)
                }
                state.errorMessage != null && !state.loaded ->
                    ErrorCard(state.errorMessage!!, onRetry = viewModel::load)
                else -> {
                    Section(
                        title = stringResource(R.string.notif_min_severity),
                        hint = stringResource(R.string.notif_min_severity_hint),
                        icon = Icons.Outlined.NotificationsActive,
                    ) {
                        Column(Modifier.selectableGroup()) {
                            selectableSeverities.forEach { severity ->
                                val selected = state.preferences.minSeverity == severity
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp)
                                        .selectable(
                                            selected = selected,
                                            role = Role.RadioButton,
                                            onClick = { viewModel.onMinSeverityChange(severity) },
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(selected = selected, onClick = null)
                                    Spacer(Modifier.padding(start = 8.dp))
                                    Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                                        Text(
                                            stringResource(
                                                when (severity) {
                                                    AlertSeverity.WATCH -> R.string.notif_option_watch
                                                    AlertSeverity.HIGH -> R.string.notif_option_high
                                                    AlertSeverity.CRITICAL -> R.string.notif_option_critical
                                                },
                                            ),
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                        SeverityChip(severity, modifier = Modifier.padding(top = 6.dp))
                                    }
                                }
                            }
                        }
                    }
                    Section(title = stringResource(R.string.notif_channels), hint = null, icon = Icons.AutoMirrored.Outlined.Send) {
                        ToggleRow(
                            label = stringResource(R.string.notif_channel_push),
                            checked = state.preferences.pushEnabled,
                            onCheckedChange = viewModel::onPushChange,
                        )
                        HorizontalDivider(color = OliveLine)
                        ToggleRow(
                            label = stringResource(R.string.notif_channel_email),
                            checked = state.preferences.emailEnabled,
                            onCheckedChange = viewModel::onEmailChange,
                        )
                        val message = if (state.needsOneChannel) {
                            stringResource(R.string.notif_keep_one_channel)
                        } else {
                            state.errorMessage
                        }
                        if (message != null) {
                            Text(
                                message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .semantics { liveRegion = LiveRegionMode.Polite },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, hint: String?, icon: ImageVector, content: @Composable () -> Unit) {
    SerenaCard(padding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SoftIcon(icon, size = 36.dp)
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .semantics { heading() },
            )
        }
        if (hint != null) {
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = SageInk,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = null)
    }
}
