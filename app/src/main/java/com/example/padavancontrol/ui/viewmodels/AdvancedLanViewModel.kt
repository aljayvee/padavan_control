package com.example.padavancontrol.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.padavancontrol.data.PadavanRepository
import com.example.padavancontrol.data.models.LanConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdvancedLanUiState(
    val config: LanConfig = LanConfig(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false,
    val loadProgress: Float = 0f,
    val loadStatus: String = ""
)

class AdvancedLanViewModel(
    private val repository: PadavanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdvancedLanUiState())
    val uiState: StateFlow<AdvancedLanUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    fun loadConfig(page: String) {
        _uiState.update { it.copy(isLoading = true, loadProgress = 0.2f, loadStatus = "Fetching LAN configuration...", errorMessage = null, saveSuccess = false) }
        viewModelScope.launch {
            repository.getLanConfig(page).collect { result ->
                result.onSuccess { config ->
                    _uiState.update { it.copy(config = config, loadProgress = 1.0f, loadStatus = "LAN settings loaded.", isLoading = false) }
                }
                result.onFailure { exception ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            loadProgress = 0f,
                            errorMessage = exception.message ?: "Failed to load LAN configuration"
                        ) 
                    }
                }
            }
        }
    }

    fun updateConfig(config: LanConfig) {
        _uiState.update { it.copy(config = config) }
    }

    fun saveConfig(page: String) {
        _uiState.update { it.copy(isSaving = true, errorMessage = null, saveSuccess = false) }
        viewModelScope.launch {
            val success = repository.saveLanConfig(page, _uiState.value.config)
            if (success) {
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                _toastMessage.emit("Settings applied successfully.")
            } else {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Failed to apply LAN settings") }
                _toastMessage.emit("Failed to apply settings.")
            }
        }
    }

    fun addStaticLease(mac: String, ip: String, name: String, page: String) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val success = repository.addStaticLease(mac, ip, name)
            _uiState.update { it.copy(isSaving = false) }
            if (success) {
                _toastMessage.emit("Static lease added successfully.")
                loadConfig(page)
            } else {
                _toastMessage.emit("Failed to add static lease.")
            }
        }
    }

    fun deleteStaticLease(index: Int, page: String) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val success = repository.deleteStaticLease(index)
            _uiState.update { it.copy(isSaving = false) }
            if (success) {
                _toastMessage.emit("Static lease deleted.")
                loadConfig(page)
            } else {
                _toastMessage.emit("Failed to delete static lease.")
            }
        }
    }

    fun sendWolPacket(mac: String) {
        viewModelScope.launch {
            val success = repository.sendWolPacket(mac)
            if (success) {
                _toastMessage.emit("WOL magic packet sent to $mac.")
            } else {
                _toastMessage.emit("Failed to send WOL magic packet.")
            }
        }
    }

    fun addStaticRoute(dest: String, mask: String, gw: String, metric: Int, iface: String, page: String) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val success = repository.addStaticRoute(dest, mask, gw, metric, iface)
            _uiState.update { it.copy(isSaving = false) }
            if (success) {
                _toastMessage.emit("Static route added successfully.")
                loadConfig(page)
            } else {
                _toastMessage.emit("Failed to add static route.")
            }
        }
    }

    fun deleteStaticRoute(index: Int, page: String) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val success = repository.deleteStaticRoute(index)
            _uiState.update { it.copy(isSaving = false) }
            if (success) {
                _toastMessage.emit("Static route deleted.")
                loadConfig(page)
            } else {
                _toastMessage.emit("Failed to delete static route.")
            }
        }
    }
}
