package com.example.padavancontrol.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.padavancontrol.data.PadavanRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import kotlinx.coroutines.isActive

val INTERFACE_MAPPING = mapOf(
    "WAN" to "Wired: WAN",
    "LAN" to "Wired: LAN",
    "WLAN2_AP0" to "Wireless 2.4GHz: AP Main",
    "WLAN2_AP1" to "Wireless 2.4GHz: AP Guest",
    "WLAN2_APC" to "Wireless 2.4GHz: AP-Client",
    "WLAN2_WDS" to "Wireless 2.4GHz: WDS",
    "WLAN5_AP0" to "Wireless 5GHz: AP Main",
    "WLAN5_AP1" to "Wireless 5GHz: AP Guest",
    "WLAN5_APC" to "Wireless 5GHz: AP-Client",
    "WLAN5_WDS" to "Wireless 5GHz: WDS",
    "ESW_P0" to "Ethernet port: WAN",
    "ESW_P1" to "Ethernet port: LAN1",
    "ESW_P2" to "Ethernet port: LAN2",
    "ESW_P3" to "Ethernet port: LAN3",
    "ESW_P4" to "Ethernet port: LAN4"
)

data class InterfaceTrafficState(
    val interfaceKey: String,
    val displayName: String,
    val rxHistory: List<Float> = emptyList(),
    val txHistory: List<Float> = emptyList(),
    val currentRxSpeed: Double = 0.0, // in Mbps
    val currentTxSpeed: Double = 0.0, // in Mbps
    val averageRxSpeed: Double = 0.0, // in Mbps
    val averageTxSpeed: Double = 0.0, // in Mbps
    val maxRxSpeed: Double = 0.0,     // in Mbps
    val maxTxSpeed: Double = 0.0,     // in Mbps
    val totalRxBytes: Long = 0L,
    val totalTxBytes: Long = 0L
)

data class TrafficUiState(
    val downloadHistory: List<Float> = emptyList(),
    val uploadHistory: List<Float> = emptyList(),
    val currentDownloadSpeed: Double = 0.0,
    val currentUploadSpeed: Double = 0.0,
    val totalDownloadBytes: Long = 0L,
    val totalUploadBytes: Long = 0L,
    
    val availableInterfaces: List<String> = emptyList(),
    val selectedInterface: String = "WAN",
    val interfaceStates: Map<String, InterfaceTrafficState> = emptyMap(),
    val errorMessage: String? = null
)

class TrafficViewModel(
    private val repository: PadavanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrafficUiState())
    val uiState: StateFlow<TrafficUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var lastTimestamp = 0L

    init {
        startPolling()
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                repository.getTrafficStats().collect { result ->
                    result.onSuccess { stats ->
                        val updatedStates = _uiState.value.interfaceStates.toMutableMap()
                        val availableKeys = stats.keys.toList()
                        val now = System.currentTimeMillis()

                        var selectedIface = _uiState.value.selectedInterface
                        if (selectedIface == "WAN" && !availableKeys.contains("WAN") && availableKeys.isNotEmpty()) {
                            selectedIface = availableKeys.find {
                                it.equals("WAN", ignoreCase = true) ||
                                it.contains("wan", ignoreCase = true) ||
                                it.contains("apcli", ignoreCase = true)
                            } ?: availableKeys.first()
                        }

                        for ((key, pair) in stats) {
                            val rx = pair.first
                            val tx = pair.second
                            
                            val prevState = updatedStates[key]
                            val prevRx = prevState?.totalRxBytes ?: 0L
                            val prevTx = prevState?.totalTxBytes ?: 0L
                            
                            var dlSpeed = 0.0
                            var ulSpeed = 0.0
                            
                            if (lastTimestamp > 0 && now > lastTimestamp) {
                                val timeDiffSec = (now - lastTimestamp) / 1000.0
                                if (rx >= prevRx && tx >= prevTx && prevRx > 0L) {
                                    val rxDiff = rx - prevRx
                                    val txDiff = tx - prevTx
                                    dlSpeed = ((rxDiff * 8.0) / (1000.0 * 1000.0)) / timeDiffSec
                                    ulSpeed = ((txDiff * 8.0) / (1000.0 * 1000.0)) / timeDiffSec
                                }
                            }
                            
                            val dlHistory = (prevState?.rxHistory ?: emptyList()).toMutableList()
                            val ulHistory = (prevState?.txHistory ?: emptyList()).toMutableList()
                            
                            if (lastTimestamp > 0) {
                                dlHistory.add(dlSpeed.toFloat())
                                ulHistory.add(ulSpeed.toFloat())
                                if (dlHistory.size > 20) dlHistory.removeAt(0)
                                if (ulHistory.size > 20) ulHistory.removeAt(0)
                            } else {
                                dlHistory.add(0f)
                                ulHistory.add(0f)
                            }
                            
                            val avgRx = if (dlHistory.isNotEmpty()) dlHistory.average() else 0.0
                            val avgTx = if (ulHistory.isNotEmpty()) ulHistory.average() else 0.0
                            
                            val maxRx = maxOf(prevState?.maxRxSpeed ?: 0.0, dlSpeed)
                            val maxTx = maxOf(prevState?.maxTxSpeed ?: 0.0, ulSpeed)
                            
                            val displayName = INTERFACE_MAPPING[key] ?: key
                            
                            updatedStates[key] = InterfaceTrafficState(
                                interfaceKey = key,
                                displayName = displayName,
                                rxHistory = dlHistory,
                                txHistory = ulHistory,
                                currentRxSpeed = dlSpeed,
                                currentTxSpeed = ulSpeed,
                                averageRxSpeed = avgRx,
                                averageTxSpeed = avgTx,
                                maxRxSpeed = maxRx,
                                maxTxSpeed = maxTx,
                                totalRxBytes = rx,
                                totalTxBytes = tx
                            )
                        }

                        lastTimestamp = now

                        val activeState = updatedStates[selectedIface]
                        
                        _uiState.update { state ->
                            state.copy(
                                availableInterfaces = availableKeys,
                                selectedInterface = selectedIface,
                                interfaceStates = updatedStates,
                                downloadHistory = activeState?.rxHistory ?: emptyList(),
                                uploadHistory = activeState?.txHistory ?: emptyList(),
                                currentDownloadSpeed = activeState?.currentRxSpeed ?: 0.0,
                                currentUploadSpeed = activeState?.currentTxSpeed ?: 0.0,
                                totalDownloadBytes = activeState?.totalRxBytes ?: 0L,
                                totalUploadBytes = activeState?.totalTxBytes ?: 0L,
                                errorMessage = null
                            )
                        }
                    }
                    result.onFailure { error ->
                        _uiState.update { state ->
                            state.copy(
                                currentDownloadSpeed = 0.0,
                                currentUploadSpeed = 0.0,
                                errorMessage = error.message ?: "Failed to fetch traffic stats"
                            )
                        }
                    }
                }
                delay(2000)
            }
        }
    }

    fun selectInterface(interfaceKey: String) {
        _uiState.update { state ->
            val activeState = state.interfaceStates[interfaceKey]
            state.copy(
                selectedInterface = interfaceKey,
                downloadHistory = activeState?.rxHistory ?: emptyList(),
                uploadHistory = activeState?.txHistory ?: emptyList(),
                currentDownloadSpeed = activeState?.currentRxSpeed ?: 0.0,
                currentUploadSpeed = activeState?.currentTxSpeed ?: 0.0,
                totalDownloadBytes = activeState?.totalRxBytes ?: 0L,
                totalUploadBytes = activeState?.totalTxBytes ?: 0L
            )
        }
    }

    public override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
