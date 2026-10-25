package com.osoterra.mobile.ui.auth.register

import androidx.lifecycle.ViewModel
import com.osoterra.mobile.R
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.di.AppContainer
import com.osoterra.mobile.domain.model.RegisterData
import com.osoterra.mobile.domain.model.UserRole
import com.osoterra.mobile.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val role: UserRole = UserRole.PRODUCER,
    val licenseNumber: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** Error de validación propio de la pantalla, como recurso para mostrarlo en el idioma de la app. */
    val errorRes: Int? = null,
    val success: Boolean = false,
)

class RegisterViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChange(v: String) = _uiState.update { it.copy(fullName = v, errorMessage = null, errorRes = null) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, errorMessage = null, errorRes = null) }
    fun onPasswordChange(v: String) = _uiState.update { it.copy(password = v, errorMessage = null, errorRes = null) }
    fun onLicenseChange(v: String) = _uiState.update { it.copy(licenseNumber = v, errorMessage = null, errorRes = null) }
    fun onRoleChange(role: UserRole) = _uiState.update { it.copy(role = role, errorMessage = null, errorRes = null) }

    fun register() {
        val s = _uiState.value
        if (s.isLoading) return
        if (s.fullName.isBlank() || s.email.isBlank() || s.password.isBlank()) {
            _uiState.update { it.copy(errorRes = R.string.register_error_required) }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null, errorRes = null) }
        viewModelScope.launch {
            val data = RegisterData(
                fullName = s.fullName.trim(),
                email = s.email.trim(),
                password = s.password,
                role = s.role,
                licenseNumber = s.licenseNumber.trim().ifBlank { null },
            )
            when (val result = authRepository.register(data)) {
                is DataResult.Success -> _uiState.update { it.copy(isLoading = false, success = true) }
                is DataResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    companion object {
        fun provideFactory(container: AppContainer) = viewModelFactory {
            initializer { RegisterViewModel(container.authRepository) }
        }
    }
}
