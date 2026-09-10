package com.osoterra.mobile.ui.advisor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.di.AppContainer
import com.osoterra.mobile.domain.model.SupervisedPlot
import com.osoterra.mobile.domain.model.criticalityRank
import com.osoterra.mobile.domain.repository.AdvisorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val MAX_COMPARE = 4

data class AdvisorDashboardUiState(
    val isLoading: Boolean = true,
    val allPlots: List<SupervisedPlot> = emptyList(),
    val producerFilter: String? = null,
    val cropFilter: String? = null,
    val sortByCriticality: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
) {
    val producers: List<String> get() = allPlots.map { it.producerName }.distinct().sorted()
    val crops: List<String> get() = allPlots.mapNotNull { it.cropName }.distinct().sorted()

    val visiblePlots: List<SupervisedPlot>
        get() {
            var list = allPlots
            producerFilter?.let { p -> list = list.filter { it.producerName == p } }
            cropFilter?.let { c -> list = list.filter { it.cropName == c } }
            if (sortByCriticality) {
                list = list.sortedByDescending { it.salinityLevel.criticalityRank() }
            }
            return list
        }

    val canCompare: Boolean get() = selectedIds.size in 2..MAX_COMPARE
}

class AdvisorDashboardViewModel(
    private val advisorRepository: AdvisorRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdvisorDashboardUiState())
    val uiState: StateFlow<AdvisorDashboardUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val plots = (advisorRepository.getSupervisedPlots() as? DataResult.Success)?.data ?: emptyList()
            _uiState.update { it.copy(isLoading = false, allPlots = plots) }
        }
    }

    fun setProducerFilter(producer: String?) = _uiState.update { it.copy(producerFilter = producer) }
    fun setCropFilter(crop: String?) = _uiState.update { it.copy(cropFilter = crop) }
    fun toggleSort() = _uiState.update { it.copy(sortByCriticality = !it.sortByCriticality) }

    fun toggleSelection(id: String) = _uiState.update {
        val current = it.selectedIds
        val updated = when {
            id in current -> current - id
            current.size >= MAX_COMPARE -> current
            else -> current + id
        }
        it.copy(selectedIds = updated)
    }

    fun clearSelection() = _uiState.update { it.copy(selectedIds = emptySet()) }

    companion object {
        fun provideFactory(container: AppContainer) = viewModelFactory {
            initializer { AdvisorDashboardViewModel(container.advisorRepository) }
        }
    }
}
