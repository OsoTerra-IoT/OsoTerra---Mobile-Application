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
    /** Mensaje del primer fallo al traer las lecturas; null si todas llegaron. */
    val errorMessage: String? = null,
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

    fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val names = (advisorRepository.getSupervisedPlots() as? DataResult.Success)?.data
                ?.associate { it.id to it.plotName } ?: emptyMap()
            var firstError: String? = null
            val series = plotIds.mapNotNull { id ->
                when (val result = advisorRepository.getSeries(id)) {
                    is DataResult.Success -> PlotSeries(plotName = names[id] ?: id, readings = result.data)
                    is DataResult.Error -> {
                        if (firstError == null) firstError = result.message
                        null
                    }
                }
            }
            _uiState.update { it.copy(isLoading = false, series = series, errorMessage = firstError) }
        }
    }

    companion object {
        fun provideFactory(container: AppContainer, plotIds: List<String>) = viewModelFactory {
            initializer { AdvisorCompareViewModel(container.advisorRepository, plotIds) }
        }
    }
}
