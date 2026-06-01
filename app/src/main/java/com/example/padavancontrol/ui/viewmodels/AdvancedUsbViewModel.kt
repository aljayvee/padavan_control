package com.example.padavancontrol.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.padavancontrol.data.PadavanRepository
import com.example.padavancontrol.data.models.UsbShareConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdvancedUsbUiState(
    val config: UsbShareConfig = UsbShareConfig(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false,
    val loadProgress: Float = 0f,
    val loadStatus: String = ""
)

class AdvancedUsbViewModel(
    private val repository: PadavanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdvancedUsbUiState())
    val uiState: StateFlow<AdvancedUsbUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    fun loadConfig(page: String) {
        _uiState.update { it.copy(isLoading = true, loadProgress = 0.2f, loadStatus = "Fetching USB configuration...", errorMessage = null, saveSuccess = false) }
        viewModelScope.launch {
            repository.getUsbShareConfig(page).collect { result ->
                result.onSuccess { config ->
                    _uiState.update { it.copy(config = config, loadProgress = 1.0f, loadStatus = "USB settings loaded.", isLoading = false) }
                }
                result.onFailure { exception ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            loadProgress = 0f,
                            errorMessage = exception.message ?: "Failed to load USB configuration"
                        ) 
                    }
                }
            }
        }
    }

    fun updateConfig(config: UsbShareConfig) {
        _uiState.update { it.copy(config = config) }
    }

    fun saveConfig(page: String) {
        _uiState.update { it.copy(isSaving = true, errorMessage = null, saveSuccess = false) }
        viewModelScope.launch {
            val success = repository.saveUsbShareConfig(page, _uiState.value.config)
            if (success) {
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                _toastMessage.emit("Settings applied successfully.")
            } else {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Failed to apply USB settings") }
                _toastMessage.emit("Failed to apply settings.")
            }
        }
    }
}
