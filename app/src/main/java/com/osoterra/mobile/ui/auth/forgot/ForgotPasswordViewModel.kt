package com.osoterra.mobile.ui.auth.forgot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.di.AppContainer
import com.osoterra.mobile.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForgotUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val sent: Boolean = false,
    val errorMessage: String? = null,
)

class ForgotPasswordViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotUiState())
    val uiState: StateFlow<ForgotUiState> = _uiState.asStateFlow()

    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, errorMessage = null) }

    fun submit() {
        val s = _uiState.value
        if (s.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = authRepository.requestPasswordReset(s.email.trim())) {
                is DataResult.Success -> _uiState.update { it.copy(isLoading = false, sent = true) }
                is DataResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    companion object {
        fun provideFactory(container: AppContainer) = viewModelFactory {
            initializer { ForgotPasswordViewModel(container.authRepository) }
        }
    }
}
