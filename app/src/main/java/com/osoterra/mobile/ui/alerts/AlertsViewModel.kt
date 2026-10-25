package com.osoterra.mobile.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.di.AppContainer
import com.osoterra.mobile.domain.model.Alert
import com.osoterra.mobile.domain.model.CorrectiveActionType
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
    /** Alerta para la que se está registrando una acción correctiva (hoja abierta). */
    val actionFor: Alert? = null,
    val isSaving: Boolean = false,
    val actionError: String? = null,
    /** Mensaje breve para el snackbar tras una operación. */
    val notice: Notice? = null,
)

enum class Notice { ACTION_SAVED, ACKNOWLEDGED }

class AlertsViewModel(
    private val alertRepository: AlertRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        loadAlerts()
    }

    fun loadAlerts() {
        _uiState.update { it.copy(isLoading = it.alerts.isEmpty(), errorMessage = null) }
        viewModelScope.launch {
            when (val result = alertRepository.getAlerts()) {
                is DataResult.Success -> _uiState.update {
                    // Primero las no vistas; dentro de cada grupo, la más severa.
                    it.copy(
                        isLoading = false,
                        alerts = result.data.sortedWith(
                            compareBy<Alert> { a -> a.acknowledged }.thenByDescending { a -> a.severity.ordinal },
                        ),
                    )
                }
                is DataResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun acknowledge(alertId: String) {
        viewModelScope.launch {
            when (val result = alertRepository.acknowledge(alertId)) {
                is DataResult.Success -> {
                    // Se marca en la lista al instante (la tarjeta cambia y se reordena) y luego se recarga.
                    _uiState.update { state ->
                        state.copy(
                            alerts = state.alerts
                                .map { if (it.id == alertId) it.copy(acknowledged = true) else it }
                                .sortedWith(compareBy<Alert> { a -> a.acknowledged }.thenByDescending { a -> a.severity.ordinal }),
                            notice = Notice.ACKNOWLEDGED,
                        )
                    }
                    loadAlerts()
                }
                is DataResult.Error -> _uiState.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    fun openAction(alert: Alert) = _uiState.update { it.copy(actionFor = alert, actionError = null) }

    fun dismissAction() = _uiState.update { if (it.isSaving) it else it.copy(actionFor = null, actionError = null) }

    fun recordAction(type: CorrectiveActionType, notes: String) {
        val alert = _uiState.value.actionFor ?: return
        _uiState.update { it.copy(isSaving = true, actionError = null) }
        viewModelScope.launch {
            when (val result = alertRepository.recordCorrectiveAction(alert.id, type, notes)) {
                is DataResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            actionFor = null,
                            alerts = it.alerts.filterNot { a -> a.id == alert.id },
                            notice = Notice.ACTION_SAVED,
                        )
                    }
                    loadAlerts()
                }
                is DataResult.Error -> _uiState.update { it.copy(isSaving = false, actionError = result.message) }
            }
        }
    }

    fun noticeShown() = _uiState.update { it.copy(notice = null) }

    companion object {
        fun provideFactory(container: AppContainer) = viewModelFactory {
            initializer {
                AlertsViewModel(container.alertRepository)
            }
        }
    }
}
