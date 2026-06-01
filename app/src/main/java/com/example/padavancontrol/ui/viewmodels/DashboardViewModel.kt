package com.example.padavancontrol.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.padavancontrol.data.CredentialStore
import com.example.padavancontrol.data.PadavanRepository
import com.example.padavancontrol.data.models.PortLink
import com.example.padavancontrol.data.models.SystemStatus
import com.example.padavancontrol.data.models.WanStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val systemStatus: SystemStatus? = null,
    val wanStatus: WanStatus? = null,
    val lanLinks: List<PortLink> = emptyList(),
    val isRefreshing: Boolean = false,
    val showRebootDialog: Boolean = false,
    val showShutdownDialog: Boolean = false,
    val wifi2GEnabled: Boolean = true,
    val wifi5GEnabled: Boolean = true,
    val routerIp: String = "",
    val hardwareWarning: String? = null
)

sealed interface DashboardEvent {
    data class ShowToast(val message: String) : DashboardEvent
}

class DashboardViewModel(
    private val repository: PadavanRepository,
    private val credentialStore: CredentialStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _events = Channel<DashboardEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var pollingJob: Job? = null

    private var isPollingActive = true

    init {
        val model = credentialStore.getHardwareModel()
        val warning = if (model.isNotEmpty() && model != "UNKNOWN" && model != "NEWIFI3") {
            "Warning: $model detected. Port links mapping is designed for Newifi D2."
        } else {
            null
        }
        _uiState.update { 
            it.copy(
                routerIp = credentialStore.getRouterIp(),
                hardwareWarning = warning
            ) 
        }
        startPolling()
    }

    fun setPollingEnabled(enabled: Boolean) {
        isPollingActive = enabled
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                if (isPollingActive) {
                    fetchDashboardData(showLoadingIndicator = false)
                }
                delay(1500)
            }
        }
    }

    private suspend fun fetchDashboardData(showLoadingIndicator: Boolean) {
        if (showLoadingIndicator) {
            _uiState.update { it.copy(isRefreshing = true) }
        }

        val systemJob = viewModelScope.launch {
            repository.getSystemStatus().collect { result ->
                result.onSuccess { status ->
                    _uiState.update { it.copy(systemStatus = status) }
                }
            }
        }

        val wanJob = viewModelScope.launch {
            repository.getWanStatus().collect { result ->
                result.onSuccess { status ->
                    _uiState.update { it.copy(wanStatus = status) }
                }
            }
        }

        val lanJob = viewModelScope.launch {
            repository.getLanLinks().collect { result ->
                result.onSuccess { links ->
                    _uiState.update { it.copy(lanLinks = links) }
                }
            }
        }

        systemJob.join()
        wanJob.join()
        lanJob.join()

        if (showLoadingIndicator) {
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            fetchDashboardData(showLoadingIndicator = true)
        }
    }

    fun toggleWifi2G(enable: Boolean) {
        _uiState.update { it.copy(wifi2GEnabled = enable) }
        viewModelScope.launch {
            val success = repository.toggleWifi2G(enable)
            val message = if (success) {
                "2.4G Wi-Fi toggled successfully"
            } else {
                "Failed to toggle 2.4G Wi-Fi"
            }
            _events.send(DashboardEvent.ShowToast(message))
        }
    }

    fun toggleWifi5G(enable: Boolean) {
        _uiState.update { it.copy(wifi5GEnabled = enable) }
        viewModelScope.launch {
            val success = repository.toggleWifi5G(enable)
            val message = if (success) {
                "5G Wi-Fi toggled successfully"
            } else {
                "Failed to toggle 5G Wi-Fi"
            }
            _events.send(DashboardEvent.ShowToast(message))
        }
    }

    fun commitFlash() {
        viewModelScope.launch {
            val success = repository.commitFlash()
            val message = if (success) {
                "NVRAM configuration saved permanently"
            } else {
                "Failed to save NVRAM"
            }
            _events.send(DashboardEvent.ShowToast(message))
        }
    }

    fun showRebootDialog() {
        _uiState.update { it.copy(showRebootDialog = true) }
    }

    fun dismissRebootDialog() {
        _uiState.update { it.copy(showRebootDialog = false) }
    }

    fun confirmReboot() {
        _uiState.update { it.copy(showRebootDialog = false) }
        viewModelScope.launch {
            repository.rebootRouter()
            _events.send(DashboardEvent.ShowToast("Rebooting router..."))
        }
    }

    fun showShutdownDialog() {
        _uiState.update { it.copy(showShutdownDialog = true) }
    }

    fun dismissShutdownDialog() {
        _uiState.update { it.copy(showShutdownDialog = false) }
    }

    fun confirmShutdown() {
        _uiState.update { it.copy(showShutdownDialog = false) }
        viewModelScope.launch {
            repository.shutdownRouter()
            _events.send(DashboardEvent.ShowToast("Shutting down router..."))
        }
    }

    public override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
