package com.example.padavancontrol.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.padavancontrol.data.PadavanRepository
import com.example.padavancontrol.data.models.VpnConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdvancedVpnUiState(
    val config: VpnConfig = VpnConfig(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false,
    val loadProgress: Float = 0f,
    val loadStatus: String = ""
)

class AdvancedVpnViewModel(
    private val repository: PadavanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdvancedVpnUiState())
    val uiState: StateFlow<AdvancedVpnUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    fun loadConfig() {
        _uiState.update { it.copy(isLoading = true, loadProgress = 0.2f, loadStatus = "Fetching VPN configurations...", errorMessage = null, saveSuccess = false) }
        viewModelScope.launch {
            repository.getVpnConfig().collect { result ->
                result.onSuccess { config ->
                    _uiState.update { it.copy(config = config, loadProgress = 1.0f, loadStatus = "VPN configurations loaded.", isLoading = false) }
                }
                result.onFailure { exception ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            loadProgress = 0f,
                            errorMessage = exception.message ?: "Failed to load VPN configuration"
                        ) 
                    }
                }
            }
        }
    }

    fun updateConfig(config: VpnConfig) {
        _uiState.update { it.copy(config = config) }
    }

    fun saveConfig() {
        _uiState.update { it.copy(isSaving = true, errorMessage = null, saveSuccess = false) }
        viewModelScope.launch {
            val success = repository.saveVpnConfig(_uiState.value.config)
            if (success) {
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                _toastMessage.emit("Settings applied successfully.")
            } else {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Failed to apply VPN configuration") }
                _toastMessage.emit("Failed to apply settings.")
            }
        }
    }
}
