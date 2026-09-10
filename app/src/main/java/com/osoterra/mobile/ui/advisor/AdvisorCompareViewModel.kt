package com.osoterra.mobile.ui.advisor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.di.AppContainer
import com.osoterra.mobile.domain.model.Reading
import com.osoterra.mobile.domain.repository.AdvisorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlotSeries(
    val plotName: String,
    val readings: List<Reading>,
)

data class AdvisorCompareUiState(
    val isLoading: Boolean = true,
    val series: List<PlotSeries> = emptyList(),
)

class AdvisorCompareViewModel(
    private val advisorRepository: AdvisorRepository,
    private val plotIds: List<String>,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdvisorCompareUiState())
    val uiState: StateFlow<AdvisorCompareUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val names = (advisorRepository.getSupervisedPlots() as? DataResult.Success)?.data
                ?.associate { it.id to it.plotName } ?: emptyMap()
            val series = plotIds.mapNotNull { id ->
                val readings = (advisorRepository.getSeries(id) as? DataResult.Success)?.data
                    ?: return@mapNotNull null
                PlotSeries(plotName = names[id] ?: id, readings = readings)
            }
            _uiState.update { it.copy(isLoading = false, series = series) }
        }
    }

    companion object {
        fun provideFactory(container: AppContainer, plotIds: List<String>) = viewModelFactory {
            initializer { AdvisorCompareViewModel(container.advisorRepository, plotIds) }
        }
    }
}
