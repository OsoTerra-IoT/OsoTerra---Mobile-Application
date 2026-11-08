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
    val errorMessage: String? = null,
)

class CropCatalogViewModel(
    private val plotRepository: PlotRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CropCatalogUiState())
    val uiState: StateFlow<CropCatalogUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = plotRepository.getCrops()) {
                is DataResult.Success -> _uiState.update { it.copy(isLoading = false, crops = result.data) }
                is DataResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    companion object {
        fun provideFactory(container: AppContainer) = viewModelFactory {
            initializer { CropCatalogViewModel(container.plotRepository) }
        }
    }
}
