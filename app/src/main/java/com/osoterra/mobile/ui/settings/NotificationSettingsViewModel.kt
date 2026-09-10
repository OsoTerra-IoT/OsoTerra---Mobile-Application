package com.osoterra.mobile.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
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
)

class NotificationSettingsViewModel(
    private val repository: NotificationPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = false, preferences = repository.getPreferences()) }
        }
    }

    fun onMinSeverityChange(severity: AlertSeverity) = update { it.copy(minSeverity = severity) }
    fun onPushChange(enabled: Boolean) = update { it.copy(pushEnabled = enabled) }
    fun onEmailChange(enabled: Boolean) = update { it.copy(emailEnabled = enabled) }

    private fun update(transform: (NotificationPreferences) -> NotificationPreferences) {
        val updated = transform(_uiState.value.preferences)
        _uiState.update { it.copy(preferences = updated) }
        viewModelScope.launch { repository.savePreferences(updated) }
    }

    companion object {
        fun provideFactory(container: AppContainer) = viewModelFactory {
            initializer { NotificationSettingsViewModel(container.notificationPreferencesRepository) }
        }
    }
}
