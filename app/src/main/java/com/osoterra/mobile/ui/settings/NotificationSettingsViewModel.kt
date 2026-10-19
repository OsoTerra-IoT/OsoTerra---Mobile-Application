package com.osoterra.mobile.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.di.AppContainer
import com.osoterra.mobile.domain.model.AlertSeverity
import com.osoterra.mobile.domain.model.NotificationPreferences
import com.osoterra.mobile.domain.repository.NotificationPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationSettingsUiState(
    val isLoading: Boolean = true,
    val preferences: NotificationPreferences = NotificationPreferences(),
    val errorMessage: String? = null,
    /** Se intentó apagar el único canal activo. */
    val needsOneChannel: Boolean = false,
    val saved: Boolean = false,
    /** Ya llegaron las preferencias del Backend al menos una vez. */
    val loaded: Boolean = false,
)

class NotificationSettingsViewModel(
    private val repository: NotificationPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.getPreferences()) {
                is DataResult.Success -> _uiState.update { it.copy(isLoading = false, loaded = true, preferences = result.data) }
                is DataResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    fun onMinSeverityChange(severity: AlertSeverity) = update { it.copy(minSeverity = severity) }
    fun onPushChange(enabled: Boolean) = update { it.copy(pushEnabled = enabled) }
    fun onEmailChange(enabled: Boolean) = update { it.copy(emailEnabled = enabled) }

    fun savedShown() = _uiState.update { it.copy(saved = false) }

    /** Guarda al instante; si el Backend lo rechaza, vuelve al valor anterior y lo dice. */
    private fun update(transform: (NotificationPreferences) -> NotificationPreferences) {
        val previous = _uiState.value.preferences
        val updated = transform(previous)
        if (!updated.pushEnabled && !updated.emailEnabled) {
            _uiState.update { it.copy(needsOneChannel = true) }
            return
        }
        _uiState.update { it.copy(preferences = updated, needsOneChannel = false, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.savePreferences(updated)) {
                is DataResult.Success -> _uiState.update { it.copy(saved = true) }
                is DataResult.Error -> _uiState.update {
                    it.copy(preferences = previous, errorMessage = result.message)
                }
            }
        }
    }

    companion object {
        fun provideFactory(container: AppContainer) = viewModelFactory {
            initializer { NotificationSettingsViewModel(container.notificationPreferencesRepository) }
        }
    }
}
