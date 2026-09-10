package com.osoterra.mobile.ui.devices

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.osoterra.mobile.R
import com.osoterra.mobile.di.rememberAppContainer
import com.osoterra.mobile.domain.model.Calibration
import com.osoterra.mobile.domain.model.Device
import com.osoterra.mobile.domain.model.DeviceStatus
import com.osoterra.mobile.ui.theme.SalinityCritical
import com.osoterra.mobile.ui.theme.SalinityNoData
import com.osoterra.mobile.ui.theme.SalinityNormal
import com.osoterra.mobile.ui.theme.SalinityWatch
import java.time.LocalDate

private fun DeviceStatus.color(): Color = when (this) {
    DeviceStatus.ONLINE -> SalinityNormal
    DeviceStatus.LOW_BATTERY -> SalinityWatch
    DeviceStatus.OFFLINE -> SalinityNoData
}

@Composable
private fun DeviceStatus.label(): String = stringResource(
    when (this) {
        DeviceStatus.ONLINE -> R.string.device_online
        DeviceStatus.LOW_BATTERY -> R.string.device_low_battery
        DeviceStatus.OFFLINE -> R.string.device_offline
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    onBack: () -> Unit,
) {
    val container = rememberAppContainer()
    val viewModel: DevicesViewModel = viewModel(factory = DevicesViewModel.provideFactory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showRegister by remember { mutableStateOf(false) }
    var calibrateDevice by remember { mutableStateOf<Device?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.devices_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showRegister = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.device_register)) },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.devices.isEmpty() -> Text(
                    stringResource(R.string.devices_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.devices, key = { it.id }) { device ->
                        DeviceCard(
                            device = device,
                            lastCalibration = state.calibrationsByDevice[device.id]?.firstOrNull(),
                            onCalibrate = { calibrateDevice = device },
                        )
                    }
                }
            }
        }
    }

    if (showRegister) {
        RegisterDeviceDialog(
            plots = state.plots.map { it.id to it.name },
            isSaving = state.isSaving,
            errorMessage = state.errorMessage,
            onDismiss = { showRegister = false },
            onConfirm = { code, plotId ->
                viewModel.registerDevice(code, plotId) { showRegister = false }
            },
        )
    }

    calibrateDevice?.let { device ->
        CalibrationDialog(
            device = device,
            isSaving = state.isSaving,
            errorMessage = state.errorMessage,
            onDismiss = { calibrateDevice = null },
            onConfirm = { value, date ->
                viewModel.registerCalibration(device.id, value, date) { calibrateDevice = null }
            },
        )
    }
}

@Composable
private fun DeviceCard(device: Device, lastCalibration: Calibration?, onCalibrate: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(device.code, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        device.plotName ?: stringResource(R.string.device_unlinked),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusChip(device.status)
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.BatteryFull,
                    contentDescription = null,
                    tint = if (device.batteryPct < 20) SalinityCritical else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(4.dp))
                Text("${device.batteryPct}%", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                device.lastSeen?.let {
                    Text(
                        stringResource(R.string.device_last_seen, it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            lastCalibration?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.device_last_calibration, it.date, "%.2f".format(it.factor)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onCalibrate) {
                Text(stringResource(R.string.device_calibrate))
            }
        }
    }
}

@Composable
private fun StatusChip(status: DeviceStatus) {
    Box(
        Modifier
            .background(status.color(), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(
            status.label(),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterDeviceDialog(
    plots: List<Pair<String, String>>,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (code: String, plotId: String) -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var selectedPlotId by remember { mutableStateOf(plots.firstOrNull()?.first) }
    var expanded by remember { mutableStateOf(false) }
    val selectedName = plots.firstOrNull { it.first == selectedPlotId }?.second ?: ""

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(stringResource(R.string.device_register)) },
        text = {
            Column {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(stringResource(R.string.device_code)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selectedName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.device_plot)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        plots.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = { selectedPlotId = id; expanded = false },
                            )
                        }
                    }
                }
                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedPlotId?.let { onConfirm(code, it) } },
                enabled = !isSaving && code.isNotBlank() && selectedPlotId != null,
            ) { Text(stringResource(R.string.device_register)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun CalibrationDialog(
    device: Device,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (referenceValue: Double, date: String) -> Unit,
) {
    var value by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(stringResource(R.string.device_calibrate)) },
        text = {
            Column {
                Text(device.code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.device_reference_value)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text(stringResource(R.string.device_sample_date)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val v = value.replace(",", ".").toDoubleOrNull()
                    if (v != null) onConfirm(v, date)
                },
                enabled = !isSaving && value.isNotBlank(),
            ) { Text(stringResource(R.string.common_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}
