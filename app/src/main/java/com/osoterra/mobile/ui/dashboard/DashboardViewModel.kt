package com.osoterra.mobile.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.di.AppContainer
import com.osoterra.mobile.domain.model.AdvisoryRequest
import com.osoterra.mobile.domain.model.Plot
import com.osoterra.mobile.domain.model.SalinityLevel
import com.osoterra.mobile.domain.model.UserRole
import com.osoterra.mobile.domain.repository.AdvisoryLinkRepository
import com.osoterra.mobile.domain.repository.AlertRepository
import com.osoterra.mobile.domain.repository.AuthRepository
import com.osoterra.mobile.domain.repository.PlotRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val firstName: String = "",
    val plots: List<Plot> = emptyList(),
    val activeAlerts: Int = 0,
    val errorMessage: String? = null,
    /** Solicitudes de asesoría pendientes (solo para el productor). */
    val requests: List<AdvisoryRequest> = emptyList(),
    /** Respuesta recién dada a una solicitud, para confirmarla en pantalla. */
    val requestOutcome: RequestOutcome? = null,
    val respondingTo: String? = null,
) {
    /**
     * Parcelas que necesitan atención: solo las de nivel Alto o Muy alto, la misma regla que
     * sus píldoras y que la Web App. Las que están En vigilancia se cuentan aparte.
     */
    val needingAttention: List<Plot>
        get() = plots.filter { it.salinityLevel in ATTENTION }

    /** Parcelas En vigilancia: sobre el umbral, pero aún cerca de él. */
    val onWatch: List<Plot>
        get() = plots.filter { it.salinityLevel == SalinityLevel.WATCH }

    val sortedPlots: List<Plot>
        get() = plots.sortedWith(compareByDescending<Plot> { it.salinityLevel.urgency() }.thenBy { it.name })

    val devicesOnline: Int get() = plots.count { it.deviceOnline }
    val totalHectares: Double get() = plots.sumOf { it.areaHectares }

    private companion object {
        val ATTENTION = setOf(SalinityLevel.HIGH, SalinityLevel.CRITICAL)
    }
}

data class RequestOutcome(val advisorName: String, val accepted: Boolean)

private fun SalinityLevel.urgency(): Int = when (this) {
    SalinityLevel.CRITICAL -> 4
    SalinityLevel.HIGH -> 3
    SalinityLevel.WATCH -> 2
    SalinityLevel.NORMAL -> 1
    SalinityLevel.NO_DATA -> 0
}

class DashboardViewModel(
    private val authRepository: AuthRepository,
    private val plotRepository: PlotRepository,
    private val alertRepository: AlertRepository,
    private val advisoryLinkRepository: AdvisoryLinkRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        load(refreshing = false)
    }

    fun refresh() = load(refreshing = true)

    fun loadPlots() = load(refreshing = false)

    private fun load(refreshing: Boolean) {
        _uiState.update {
            it.copy(isLoading = !refreshing && it.plots.isEmpty(), isRefreshing = refreshing, errorMessage = null)
        }
        viewModelScope.launch {
            val user = async { authRepository.currentUser() }
            val plots = async { plotRepository.getPlots() }
            val alerts = async { alertRepository.getAlerts() }
            val currentUser = user.await()
            val firstName = currentUser?.fullName?.trim()?.substringBefore(' ').orEmpty()
            val requests = if (currentUser?.role == UserRole.PRODUCER) {
                (advisoryLinkRepository.getPendingRequests() as? DataResult.Success)?.data.orEmpty()
            } else {
                emptyList()
            }
            val activeAlerts = (alerts.await() as? DataResult.Success)?.data?.count { !it.acknowledged } ?: 0
            when (val result = plots.await()) {
                is DataResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        firstName = firstName,
                        plots = result.data,
                        activeAlerts = activeAlerts,
                        requests = requests,
                    )
                }
                is DataResult.Error -> _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, firstName = firstName, errorMessage = result.message)
                }
            }
        }
    }

    fun acceptRequest(request: AdvisoryRequest) = respond(request, accepted = true)

    fun declineRequest(request: AdvisoryRequest) = respond(request, accepted = false)

    private fun respond(request: AdvisoryRequest, accepted: Boolean) {
        if (_uiState.value.respondingTo != null) return
        _uiState.update { it.copy(respondingTo = request.id) }
        viewModelScope.launch {
            val result = if (accepted) advisoryLinkRepository.accept(request.id)
            else advisoryLinkRepository.decline(request.id)
            _uiState.update { state ->
                when (result) {
                    is DataResult.Success -> state.copy(
                        respondingTo = null,
                        requests = state.requests.filterNot { it.id == request.id },
                        requestOutcome = RequestOutcome(request.advisorName, accepted),
                    )
                    is DataResult.Error -> state.copy(respondingTo = null, errorMessage = result.message)
                }
            }
        }
    }

    companion object {
        fun provideFactory(container: AppContainer) = viewModelFactory {
            initializer {
                DashboardViewModel(
                    container.authRepository,
                    container.plotRepository,
                    container.alertRepository,
                    container.advisoryLinkRepository,
                )
            }
        }
    }
}
