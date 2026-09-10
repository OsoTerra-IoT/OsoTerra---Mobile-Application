package com.osoterra.mobile.ui.farms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.di.AppContainer
import com.osoterra.mobile.domain.model.Farm
import com.osoterra.mobile.domain.repository.FarmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FarmsUiState(
    val isLoading: Boolean = true,
    val farms: List<Farm> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

class FarmsViewModel(
    private val farmRepository: FarmRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FarmsUiState())
    val uiState: StateFlow<FarmsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = farmRepository.getFarms()) {
                is DataResult.Success -> _uiState.update { it.copy(isLoading = false, farms = result.data) }
                is DataResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    fun createFarm(
        name: String,
        department: String,
        province: String,
        district: String,
        onDone: () -> Unit,
    ) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = farmRepository.createFarm(name, department, province, district)) {
                is DataResult.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    load()
                    onDone()
                }
                is DataResult.Error -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.message)
                }
            }
        }
    }

    companion object {
        fun provideFactory(container: AppContainer) = viewModelFactory {
            initializer { FarmsViewModel(container.farmRepository) }
        }
    }
}
