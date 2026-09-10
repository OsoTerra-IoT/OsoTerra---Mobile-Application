package com.osoterra.mobile.ui.crops

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.di.AppContainer
import com.osoterra.mobile.domain.model.Crop
import com.osoterra.mobile.domain.repository.PlotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CropCatalogUiState(
    val isLoading: Boolean = true,
    val crops: List<Crop> = emptyList(),
)

class CropCatalogViewModel(
    private val plotRepository: PlotRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CropCatalogUiState())
    val uiState: StateFlow<CropCatalogUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val crops = (plotRepository.getCrops() as? DataResult.Success)?.data ?: emptyList()
            _uiState.update { it.copy(isLoading = false, crops = crops) }
        }
    }

    companion object {
        fun provideFactory(container: AppContainer) = viewModelFactory {
            initializer { CropCatalogViewModel(container.plotRepository) }
        }
    }
}
