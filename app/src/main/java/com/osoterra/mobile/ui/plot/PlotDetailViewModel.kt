package com.osoterra.mobile.ui.plot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.di.AppContainer
import com.osoterra.mobile.domain.model.Crop
import com.osoterra.mobile.domain.model.Plot
import com.osoterra.mobile.domain.model.Reading
import com.osoterra.mobile.domain.repository.PlotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlotDetailUiState(
    val isLoading: Boolean = true,
    val plot: Plot? = null,
    val readings: List<Reading> = emptyList(),
    val crops: List<Crop> = emptyList(),
    val deactivated: Boolean = false,
    val errorMessage: String? = null,
)

class PlotDetailViewModel(
    private val plotRepository: PlotRepository,
    private val plotId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlotDetailUiState())
    val uiState: StateFlow<PlotDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val plotResult = plotRepository.getPlot(plotId)
            val readingsResult = plotRepository.getReadings(plotId)
            val cropsResult = plotRepository.getCrops()
            val plot = (plotResult as? DataResult.Success)?.data
            val readings = (readingsResult as? DataResult.Success)?.data ?: emptyList()
            val crops = (cropsResult as? DataResult.Success)?.data ?: emptyList()
            if (plot == null) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "No se pudo cargar la parcela")
                }
            } else {
                _uiState.update {
                    it.copy(isLoading = false, plot = plot, readings = readings, crops = crops)
                }
            }
        }
    }

    fun assignCrop(cropId: String) {
        viewModelScope.launch {
            when (val result = plotRepository.assignCrop(plotId, cropId)) {
                is DataResult.Success -> _uiState.update { it.copy(plot = result.data) }
                is DataResult.Error -> _uiState.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    fun deactivate() {
        viewModelScope.launch {
            when (val result = plotRepository.deactivatePlot(plotId)) {
                is DataResult.Success -> _uiState.update { it.copy(deactivated = true) }
                is DataResult.Error -> _uiState.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    companion object {
        fun provideFactory(container: AppContainer, plotId: String) = viewModelFactory {
            initializer {
                PlotDetailViewModel(container.plotRepository, plotId)
            }
        }
    }
}
