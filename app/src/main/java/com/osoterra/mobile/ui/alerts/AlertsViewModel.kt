package com.osoterra.mobile.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.di.AppContainer
import com.osoterra.mobile.domain.model.Alert
import com.osoterra.mobile.domain.repository.AlertRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlertsUiState(
    val isLoading: Boolean = true,
    val alerts: List<Alert> = emptyList(),
    val errorMessage: String? = null,
)

class AlertsViewModel(
    private val alertRepository: AlertRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        loadAlerts()
    }

    fun loadAlerts() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = alertRepository.getAlerts()) {
                is DataResult.Success -> _uiState.update {
                    it.copy(isLoading = false, alerts = result.data.sortedByDescending { a -> !a.acknowledged })
                }
                is DataResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun acknowledge(alertId: String) {
        viewModelScope.launch {
            alertRepository.acknowledge(alertId)
            loadAlerts()
        }
    }

    companion object {
        fun provideFactory(container: AppContainer) = viewModelFactory {
            initializer {
                AlertsViewModel(container.alertRepository)
            }
        }
    }
}
