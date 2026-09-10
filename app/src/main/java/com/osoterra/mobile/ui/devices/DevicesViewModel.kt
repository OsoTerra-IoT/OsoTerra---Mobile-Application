package com.osoterra.mobile.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.di.AppContainer
import com.osoterra.mobile.domain.model.Calibration
import com.osoterra.mobile.domain.model.Device
import com.osoterra.mobile.domain.model.Plot
import com.osoterra.mobile.domain.repository.DeviceRepository
import com.osoterra.mobile.domain.repository.PlotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DevicesUiState(
    val isLoading: Boolean = true,
    val devices: List<Device> = emptyList(),
    val plots: List<Plot> = emptyList(),
    val calibrationsByDevice: Map<String, List<Calibration>> = emptyMap(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

class DevicesViewModel(
    private val deviceRepository: DeviceRepository,
    private val plotRepository: PlotRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevicesUiState())
    val uiState: StateFlow<DevicesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val devices = (deviceRepository.getDevices() as? DataResult.Success)?.data ?: emptyList()
            val plots = (plotRepository.getPlots() as? DataResult.Success)?.data ?: emptyList()
            val calibs = devices.associate { device ->
                device.id to ((deviceRepository.getCalibrations(device.id) as? DataResult.Success)?.data ?: emptyList())
            }
            _uiState.update {
                it.copy(isLoading = false, devices = devices, plots = plots, calibrationsByDevice = calibs)
            }
        }
    }

    fun registerDevice(code: String, plotId: String, onDone: () -> Unit) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = deviceRepository.registerDevice(code, plotId)) {
                is DataResult.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    load()
                    onDone()
                }
                is DataResult.Error -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.message)
                }
            }
        }
    }

    fun registerCalibration(deviceId: String, referenceValue: Double, date: String, onDone: () -> Unit) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = deviceRepository.registerCalibration(deviceId, referenceValue, date)) {
                is DataResult.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    load()
                    onDone()
                }
                is DataResult.Error -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.message)
                }
            }
        }
    }

    companion object {
        fun provideFactory(container: AppContainer) = viewModelFactory {
            initializer { DevicesViewModel(container.deviceRepository, container.plotRepository) }
        }
    }
}
