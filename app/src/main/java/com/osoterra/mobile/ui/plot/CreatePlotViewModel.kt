package com.osoterra.mobile.ui.plot

import androidx.lifecycle.ViewModel
import com.osoterra.mobile.R
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.di.AppContainer
import com.osoterra.mobile.domain.model.Crop
import com.osoterra.mobile.domain.model.Farm
import com.osoterra.mobile.domain.model.NewPlot
import com.osoterra.mobile.domain.repository.FarmRepository
import com.osoterra.mobile.domain.repository.PlotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreatePlotUiState(
    val isLoading: Boolean = true,
    val farms: List<Farm> = emptyList(),
    val crops: List<Crop> = emptyList(),
    val selectedFarmId: String? = null,
    val name: String = "",
    val area: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val selectedCropId: String? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    /** Error de validación propio de la pantalla, como recurso para mostrarlo en el idioma de la app. */
    val errorRes: Int? = null,
    val success: Boolean = false,
)

class CreatePlotViewModel(
    private val plotRepository: PlotRepository,
    private val farmRepository: FarmRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePlotUiState())
    val uiState: StateFlow<CreatePlotUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val farms = (farmRepository.getFarms() as? DataResult.Success)?.data ?: emptyList()
            val crops = (plotRepository.getCrops() as? DataResult.Success)?.data ?: emptyList()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    farms = farms,
                    crops = crops,
                    selectedFarmId = farms.firstOrNull()?.id,
                )
            }
        }
    }

    fun onFarmSelected(id: String) = _uiState.update { it.copy(selectedFarmId = id) }
    fun onNameChange(v: String) = _uiState.update { it.copy(name = v, errorMessage = null, errorRes = null) }
    fun onAreaChange(v: String) = _uiState.update { it.copy(area = v, errorMessage = null, errorRes = null) }
    fun onLatChange(v: String) = _uiState.update { it.copy(latitude = v) }
    fun onLngChange(v: String) = _uiState.update { it.copy(longitude = v) }
    fun onCropSelected(id: String?) = _uiState.update { it.copy(selectedCropId = id) }

    fun createPlot() {
        val s = _uiState.value
        if (s.isSaving) return
        val farmId = s.selectedFarmId
        if (farmId == null) {
            _uiState.update { it.copy(errorRes = R.string.plot_select_farm) }
            return
        }
        val area = s.area.replace(",", ".").toDoubleOrNull()
        if (area == null || area <= 0.0) {
            _uiState.update { it.copy(errorRes = R.string.plot_area_error) }
            return
        }
        _uiState.update { it.copy(isSaving = true, errorMessage = null, errorRes = null) }
        viewModelScope.launch {
            val newPlot = NewPlot(
                farmId = farmId,
                name = s.name.trim(),
                areaHectares = area,
                latitude = s.latitude.replace(",", ".").toDoubleOrNull(),
                longitude = s.longitude.replace(",", ".").toDoubleOrNull(),
                cropId = s.selectedCropId,
            )
            when (val result = plotRepository.createPlot(newPlot)) {
                is DataResult.Success -> _uiState.update { it.copy(isSaving = false, success = true) }
                is DataResult.Error -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.message)
                }
            }
        }
    }

    companion object {
        fun provideFactory(container: AppContainer) = viewModelFactory {
            initializer { CreatePlotViewModel(container.plotRepository, container.farmRepository) }
        }
    }
}
