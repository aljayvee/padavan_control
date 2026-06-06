package com.example.padavancontrol.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.padavancontrol.data.PadavanRepository
import com.example.padavancontrol.data.models.UsbShareConfig
import com.example.padavancontrol.data.models.ShareNode
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

    fun fetchShareTree(layerOrder: String, onSuccess: (List<ShareNode>) -> Unit) {
        viewModelScope.launch {
            repository.getShareTree(layerOrder).collect { result ->
                result.onSuccess { nodes ->
                    onSuccess(nodes)
                }
                result.onFailure { exception ->
                    _toastMessage.emit("Failed to fetch directories: ${exception.message}")
                }
            }
        }
    }

    fun createAccount(account: String, pass: String, currentPage: String) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val success = repository.createUsbAccount(account, pass)
            _uiState.update { it.copy(isSaving = false) }
            if (success) {
                _toastMessage.emit("Account created successfully.")
                loadConfig(currentPage)
            } else {
                _toastMessage.emit("Failed to create account.")
            }
        }
    }

    fun deleteAccount(account: String, currentPage: String) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val success = repository.deleteUsbAccount(account)
            _uiState.update { it.copy(isSaving = false) }
            if (success) {
                _toastMessage.emit("Account deleted successfully.")
                loadConfig(currentPage)
            } else {
                _toastMessage.emit("Failed to delete account.")
            }
        }
    }

    fun modifyAccount(account: String, newAccount: String, newPass: String, currentPage: String) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val success = repository.modifyUsbAccount(account, newAccount, newPass)
            _uiState.update { it.copy(isSaving = false) }
            if (success) {
                _toastMessage.emit("Account modified successfully.")
                loadConfig(currentPage)
            } else {
                _toastMessage.emit("Failed to modify account.")
            }
        }
    }

    fun createFolder(pool: String, folder: String, currentPage: String) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val success = repository.createUsbFolder(pool, folder)
            _uiState.update { it.copy(isSaving = false) }
            if (success) {
                _toastMessage.emit("Folder shared successfully.")
                loadConfig(currentPage)
            } else {
                _toastMessage.emit("Failed to share folder.")
            }
        }
    }

    fun deleteFolder(pool: String, folder: String, currentPage: String) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val success = repository.deleteUsbFolder(pool, folder)
            _uiState.update { it.copy(isSaving = false) }
            if (success) {
                _toastMessage.emit("Folder deleted successfully.")
                loadConfig(currentPage)
            } else {
                _toastMessage.emit("Failed to delete folder.")
            }
        }
    }

    fun modifyFolder(pool: String, folder: String, newFolder: String, currentPage: String) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val success = repository.modifyUsbFolder(pool, folder, newFolder)
            _uiState.update { it.copy(isSaving = false) }
            if (success) {
                _toastMessage.emit("Folder renamed successfully.")
                loadConfig(currentPage)
            } else {
                _toastMessage.emit("Failed to rename folder.")
            }
        }
    }

    fun setPermission(account: String, pool: String, folder: String, protocol: String, permission: String, currentPage: String) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val success = repository.setUsbPermission(account, pool, folder, protocol, permission)
            _uiState.update { it.copy(isSaving = false) }
            if (success) {
                _toastMessage.emit("Permissions updated successfully.")
                loadConfig(currentPage)
            } else {
                _toastMessage.emit("Failed to update permissions.")
            }
        }
    }
}
