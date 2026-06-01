package com.example.padavancontrol.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.padavancontrol.data.PadavanRepository
import com.example.padavancontrol.data.models.WanConfig
import com.example.padavancontrol.data.models.PortForwardRule
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdvancedWanUiState(
    val config: WanConfig = WanConfig(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false
)

class AdvancedWanViewModel(
    private val repository: PadavanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdvancedWanUiState())
    val uiState: StateFlow<AdvancedWanUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    fun loadConfig(page: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, saveSuccess = false) }
        viewModelScope.launch {
            repository.getWanConfig(page).collect { result ->
                result.onSuccess { config ->
                    _uiState.update { it.copy(config = config, isLoading = false) }
                }
                result.onFailure { exception ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = exception.message ?: "Failed to load WAN configuration"
                        ) 
                    }
                }
            }
        }
    }

    fun updateConfig(config: WanConfig) {
        _uiState.update { it.copy(config = config) }
    }

    fun saveConfig(page: String) {
        _uiState.update { it.copy(isSaving = true, errorMessage = null, saveSuccess = false) }
        viewModelScope.launch {
            val success = repository.saveWanConfig(page, _uiState.value.config)
            if (success) {
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                _toastMessage.emit("Settings applied successfully.")
            } else {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Failed to apply WAN settings") }
                _toastMessage.emit("Failed to apply settings.")
            }
        }
    }

    fun addPortForwardRule(rule: PortForwardRule, page: String) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val success = repository.addPortForwardRule(rule)
            _uiState.update { it.copy(isSaving = false) }
            if (success) {
                _toastMessage.emit("Port forward rule added successfully.")
                loadConfig(page)
            } else {
                _toastMessage.emit("Failed to add port forward rule.")
            }
        }
    }

    fun deletePortForwardRule(index: Int, page: String) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val success = repository.deletePortForwardRule(index)
            _uiState.update { it.copy(isSaving = false) }
            if (success) {
                _toastMessage.emit("Port forward rule deleted.")
                loadConfig(page)
            } else {
                _toastMessage.emit("Failed to delete port forward rule.")
            }
        }
    }
}
