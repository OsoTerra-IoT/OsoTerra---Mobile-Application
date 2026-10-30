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
import com.osoterra.mobile.domain.model.Reading
import com.osoterra.mobile.domain.model.SupervisedPlot
import com.osoterra.mobile.domain.repository.AdvisorRepository
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
    /** ECe de las últimas lecturas de cada parcela, para su mini gráfico de tendencia. */
    val trends: Map<String, List<Double>> = emptyMap(),
    /** Días que abarca cada tendencia (de la primera a la última lectura); null si no se sabe. */
    val trendDays: Map<String, Int> = emptyMap(),
    /** true si quien entra es asesor: el inicio habla de las parcelas de sus clientes. */
    val isAdvisor: Boolean = false,
    /** Asesor: nombre corto de la parcela y su cliente, por id de parcela. */
    val plotTitles: Map<String, String> = emptyMap(),
    val clients: Map<String, String> = emptyMap(),
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

    /** La parcela más urgente (la primera del orden por gravedad); va en la tarjeta grande. */
    val focusPlot: Plot? get() = sortedPlots.firstOrNull()

    /** El resto de parcelas, en mosaicos. */
    val otherPlots: List<Plot> get() = sortedPlots.drop(1)

    fun titleOf(plot: Plot): String = plotTitles[plot.id] ?: plot.name

    val devicesOnline: Int get() = plots.count { it.deviceOnline }
    val totalHectares: Double get() = plots.sumOf { it.areaHectares }

    private companion object {
        val ATTENTION = setOf(SalinityLevel.HIGH, SalinityLevel.CRITICAL)
    }
}

/** Parcela de un cliente cuando el Backend no la devuelve en la lista propia del asesor. */
private fun SupervisedPlot.asPlot() = Plot(
    id = id,
    name = plotName,
    farmName = "",
    areaHectares = 0.0,
    crop = null,
    salinityLevel = salinityLevel,
    lastEcDsPerM = lastEcDsPerM,
    lastReadingAt = null,
    deviceOnline = true,
)

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
    private val advisorRepository: AdvisorRepository? = null,
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
            val isAdvisor = currentUser?.role == UserRole.ADVISOR
            // Asesor: las parcelas de sus clientes, para nombrar al cliente en cada tarjeta.
            val supervised = if (isAdvisor) {
                (advisorRepository?.getSupervisedPlots() as? DataResult.Success)?.data.orEmpty()
            } else {
                emptyList()
            }
            val activeAlerts = (alerts.await() as? DataResult.Success)?.data?.count { !it.acknowledged } ?: 0
            when (val result = plots.await()) {
                is DataResult.Success -> {
                    val list = result.data.ifEmpty { supervised.map { it.asPlot() } }
                    val byId = supervised.associateBy { it.id }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            firstName = firstName,
                            plots = list,
                            activeAlerts = activeAlerts,
                            requests = requests,
                            isAdvisor = isAdvisor,
                            plotTitles = byId.mapValues { (_, s) -> s.plotName },
                            clients = byId.mapValues { (_, s) -> s.producerName }.filterValues { it.isNotBlank() },
                        )
                    }
                    loadTrends(list)
                }
                is DataResult.Error -> _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, firstName = firstName, errorMessage = result.message)
                }
            }
        }
    }

    /**
     * Tendencias en segundo plano: la lista aparece primero y cada mini gráfico se dibuja cuando
     * llegan sus lecturas. Si una parcela falla, simplemente queda sin gráfico.
     */
    private fun loadTrends(plots: List<Plot>) {
        viewModelScope.launch {
            val series = plots
                .filter { it.lastEcDsPerM != null }
                .map { plot -> plot.id to async { plotRepository.getReadings(plot.id) } }
                .mapNotNull { (id, call) ->
                    (call.await() as? DataResult.Success)?.data
                        ?.takeLast(TREND_POINTS)
                        ?.takeIf { it.size >= 2 }
                        ?.let { id to it }
                }
            val trends = series.associate { (id, readings) -> id to readings.map { it.ecDsPerM } }
            val days = series.mapNotNull { (id, readings) -> daySpan(readings)?.let { id to it } }.toMap()
            _uiState.update { it.copy(trends = trends, trendDays = days) }
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
        /**
         * Días entre la primera y la última lectura, según su fecha ISO (AAAA-MM-DD…). Si alguna
         * fecha no se conoce, null: la tarjeta dice «en N lecturas» en lugar de inventar días.
         */
        fun daySpan(readings: List<Reading>): Int? {
            val first = readings.firstOrNull()?.capturedAt?.take(10) ?: return null
            val last = readings.lastOrNull()?.capturedAt?.take(10) ?: return null
            return try {
                val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val millis = format.parse(last)!!.time - format.parse(first)!!.time
                (millis / 86_400_000L).toInt().takeIf { it > 0 }?.let { it + 1 }
            } catch (_: Exception) {
                null
            }
        }

        /** Puntos del mini gráfico: un mes de lecturas diarias. */
        const val TREND_POINTS = 30

        fun provideFactory(container: AppContainer) = viewModelFactory {
            initializer {
                DashboardViewModel(
                    container.authRepository,
                    container.plotRepository,
                    container.alertRepository,
                    container.advisoryLinkRepository,
                    container.advisorRepository,
                )
            }
        }
    }
}
