package com.osoterra.mobile.ui.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.di.AppContainer
import com.osoterra.mobile.domain.model.Subscription
import com.osoterra.mobile.domain.model.SubscriptionOverview
import com.osoterra.mobile.domain.model.SubscriptionPlan
import com.osoterra.mobile.domain.model.SubscriptionStatus
import com.osoterra.mobile.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Lo que la persona pidió y debe confirmar antes de llamar al API. */
sealed interface SubscriptionAction {
    /** Contratar o cambiar a [plan]; [replacesCurrent] es true si ya tiene un plan vigente. */
    data class Change(val plan: SubscriptionPlan, val replacesCurrent: Boolean) : SubscriptionAction

    data class Cancel(val subscription: Subscription) : SubscriptionAction
}

/** Resultado de una acción que se muestra hasta que la persona lo cierre. */
data class SubscriptionNotice(val kind: Kind, val planName: String) {
    enum class Kind { ACTIVATED, PENDING_PAYMENT, CANCELLED }
}

data class SubscriptionUiState(
    val isLoading: Boolean = true,
    val overview: SubscriptionOverview? = null,
    /** La consulta no se pudo hacer (red, 403…); se ofrece «Reintentar». */
    val loadError: String? = null,
    /** Acción esperando la confirmación de la persona. */
    val confirming: SubscriptionAction? = null,
    val isWorking: Boolean = false,
    /** La acción falló; [failedAction] permite reintentarla. */
    val actionError: String? = null,
    val failedAction: SubscriptionAction? = null,
    val notice: SubscriptionNotice? = null,
)

class SubscriptionViewModel(
    private val repository: SubscriptionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = it.overview == null, loadError = null) }
        viewModelScope.launch {
            when (val result = repository.getOverview()) {
                is DataResult.Success -> _uiState.update {
                    it.copy(isLoading = false, overview = result.data, loadError = null)
                }
                is DataResult.Error -> _uiState.update {
                    it.copy(isLoading = false, loadError = if (it.overview == null) result.message else null)
                }
            }
        }
    }

    fun askChange(plan: SubscriptionPlan) {
        val replaces = _uiState.value.overview?.current != null
        _uiState.update { it.copy(confirming = SubscriptionAction.Change(plan, replaces), actionError = null, notice = null) }
    }

    fun askCancel(subscription: Subscription) {
        _uiState.update { it.copy(confirming = SubscriptionAction.Cancel(subscription), actionError = null, notice = null) }
    }

    fun dismissConfirmation() = _uiState.update { it.copy(confirming = null) }

    fun dismissNotice() = _uiState.update { it.copy(notice = null) }

    fun dismissActionError() = _uiState.update { it.copy(actionError = null, failedAction = null) }

    /** La persona confirmó: llama al API. */
    fun confirm() {
        val action = _uiState.value.confirming ?: return
        _uiState.update { it.copy(confirming = null) }
        run(action)
    }

    /** Repite la acción que falló. */
    fun retryAction() {
        val action = _uiState.value.failedAction ?: return
        run(action)
    }

    private fun run(action: SubscriptionAction) {
        if (_uiState.value.isWorking) return
        _uiState.update { it.copy(isWorking = true, actionError = null, failedAction = null, notice = null) }
        viewModelScope.launch {
            val result = when (action) {
                is SubscriptionAction.Change -> repository.subscribe(action.plan.id)
                is SubscriptionAction.Cancel -> repository.cancel(action.subscription.id)
            }
            when (result) {
                is DataResult.Success -> {
                    val subscription = result.data
                    val kind = when {
                        action is SubscriptionAction.Cancel -> SubscriptionNotice.Kind.CANCELLED
                        subscription.status == SubscriptionStatus.ACTIVE -> SubscriptionNotice.Kind.ACTIVATED
                        else -> SubscriptionNotice.Kind.PENDING_PAYMENT
                    }
                    _uiState.update {
                        it.copy(isWorking = false, notice = SubscriptionNotice(kind, subscription.planName))
                    }
                    load()
                }
                is DataResult.Error -> _uiState.update {
                    it.copy(isWorking = false, actionError = result.message, failedAction = action)
                }
            }
        }
    }

    companion object {
        fun provideFactory(container: AppContainer) = viewModelFactory {
            initializer { SubscriptionViewModel(container.subscriptionRepository) }
        }
    }
}
