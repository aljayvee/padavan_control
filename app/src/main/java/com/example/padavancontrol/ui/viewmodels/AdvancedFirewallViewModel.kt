package com.example.padavancontrol.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.padavancontrol.data.PadavanRepository
import com.example.padavancontrol.data.models.FirewallConfig
import com.example.padavancontrol.data.models.ServiceFilterRule
import com.example.padavancontrol.data.models.MacFilterRule
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdvancedFirewallUiState(
    val config: FirewallConfig = FirewallConfig(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false,
    val loadProgress: Float = 0f,
    val loadStatus: String = ""
)

class AdvancedFirewallViewModel(
    private val repository: PadavanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdvancedFirewallUiState())
    val uiState: StateFlow<AdvancedFirewallUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    fun loadConfig(page: String) {
        _uiState.update { it.copy(isLoading = true, loadProgress = 0.2f, loadStatus = "Fetching Firewall configuration...", errorMessage = null, saveSuccess = false) }
        viewModelScope.launch {
            repository.getFirewallConfig(page).collect { result ->
                result.onSuccess { config ->
                    _uiState.update { it.copy(config = config, loadProgress = 1.0f, loadStatus = "Firewall settings loaded.", isLoading = false) }
                }
                result.onFailure { exception ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            loadProgress = 0f,
                            errorMessage = exception.message ?: "Failed to load Firewall configuration"
                        ) 
                    }
                }
            }
        }
    }

    fun updateConfig(config: FirewallConfig) {
        _uiState.update { it.copy(config = config) }
    }

    fun saveConfig(page: String) {
        _uiState.update { it.copy(isSaving = true, errorMessage = null, saveSuccess = false) }
        viewModelScope.launch {
            val success = repository.saveFirewallConfig(page, _uiState.value.config)
            if (success) {
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                _toastMessage.emit("Settings applied successfully.")
            } else {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Failed to apply Firewall settings") }
                _toastMessage.emit("Failed to apply settings.")
            }
        }
    }

    fun addServiceFilterRule(rule: ServiceFilterRule, page: String) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val success = repository.addServiceFilterRule(rule)
            _uiState.update { it.copy(isSaving = false) }
            if (success) {
                _toastMessage.emit("Service filter rule added successfully.")
                loadConfig(page)
            } else {
                _toastMessage.emit("Failed to add service filter rule.")
            }
        }
    }

    fun deleteServiceFilterRule(index: Int, page: String) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val success = repository.deleteServiceFilterRule(index)
            _uiState.update { it.copy(isSaving = false) }
            if (success) {
                _toastMessage.emit("Service filter rule deleted.")
                loadConfig(page)
            } else {
                _toastMessage.emit("Failed to delete service filter rule.")
            }
        }
    }

    fun addUrlKeyword(keyword: String, page: String) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val success = repository.addUrlKeyword(keyword)
            _uiState.update { it.copy(isSaving = false) }
            if (success) {
                _toastMessage.emit("URL keyword filter added.")
                loadConfig(page)
            } else {
                _toastMessage.emit("Failed to add URL keyword.")
            }
        }
    }

    fun deleteUrlKeyword(index: Int, page: String) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val success = repository.deleteUrlKeyword(index)
            _uiState.update { it.copy(isSaving = false) }
            if (success) {
                _toastMessage.emit("URL keyword filter deleted.")
                loadConfig(page)
            } else {
                _toastMessage.emit("Failed to delete URL keyword.")
            }
        }
    }

    fun addMacFilterRule(rule: MacFilterRule, page: String) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val success = repository.addMacFilterRule(rule)
            _uiState.update { it.copy(isSaving = false) }
            if (success) {
                _toastMessage.emit("MAC filter rule added.")
                loadConfig(page)
            } else {
                _toastMessage.emit("Failed to add MAC filter rule.")
            }
        }
    }

    fun deleteMacFilterRule(index: Int, page: String) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val success = repository.deleteMacFilterRule(index)
            _uiState.update { it.copy(isSaving = false) }
            if (success) {
                _toastMessage.emit("MAC filter rule deleted.")
                loadConfig(page)
            } else {
                _toastMessage.emit("Failed to delete MAC filter rule.")
            }
        }
    }
}
