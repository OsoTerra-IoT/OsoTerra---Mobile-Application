package com.osoterra.mobile.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.di.AppContainer
import com.osoterra.mobile.domain.model.Plot
import com.osoterra.mobile.domain.repository.PlotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val plots: List<Plot> = emptyList(),
    val errorMessage: String? = null,
)

class DashboardViewModel(
    private val plotRepository: PlotRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadPlots()
    }

    fun loadPlots() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = plotRepository.getPlots()) {
                is DataResult.Success -> _uiState.update {
                    it.copy(isLoading = false, plots = result.data)
                }
                is DataResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    companion object {
        fun provideFactory(container: AppContainer) = viewModelFactory {
            initializer {
                DashboardViewModel(container.plotRepository)
            }
        }
    }
}
