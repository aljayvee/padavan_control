package com.example.padavancontrol.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.padavancontrol.data.PadavanRepository
import com.example.padavancontrol.data.models.WirelessConfig
import com.example.padavancontrol.data.models.WifiNetwork
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdvancedWirelessUiState(
    val config: WirelessConfig = WirelessConfig(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isScanning: Boolean = false,
    val scanResults: List<WifiNetwork> = emptyList(),
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false
)

class AdvancedWirelessViewModel(
    private val repository: PadavanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdvancedWirelessUiState())
    val uiState: StateFlow<AdvancedWirelessUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    fun loadConfig(page: String, is5GHz: Boolean) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, saveSuccess = false) }
        viewModelScope.launch {
            repository.getWirelessConfig(page, is5GHz).collect { result ->
                result.onSuccess { config ->
                    _uiState.update { it.copy(config = config, isLoading = false) }
                }
                result.onFailure { exception ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = exception.message ?: "Failed to load wireless configuration"
                        ) 
                    }
                }
            }
        }
    }

    fun updateConfig(config: WirelessConfig) {
        _uiState.update { it.copy(config = config) }
    }

    fun saveConfig(page: String, is5GHz: Boolean) {
        _uiState.update { it.copy(isSaving = true, errorMessage = null, saveSuccess = false) }
        viewModelScope.launch {
            val success = repository.saveWirelessConfig(page, _uiState.value.config, is5GHz)
            if (success) {
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                _toastMessage.emit("Settings applied successfully.")
            } else {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Failed to apply wireless settings") }
                _toastMessage.emit("Failed to apply settings.")
            }
        }
    }

    fun scanNetworks(is5GHz: Boolean) {
        _uiState.update { it.copy(isScanning = true, errorMessage = null) }
        viewModelScope.launch {
            repository.scanWifiNetworks(is5GHz).collect { result ->
                result.onSuccess { list ->
                    _uiState.update { it.copy(isScanning = false, scanResults = list) }
                }
                result.onFailure { exception ->
                    _uiState.update { 
                        it.copy(
                            isScanning = false,
                            errorMessage = exception.message ?: "Failed to scan networks"
                        ) 
                    }
                    _toastMessage.emit("Scan failed: ${exception.message}")
                }
            }
        }
    }
}
