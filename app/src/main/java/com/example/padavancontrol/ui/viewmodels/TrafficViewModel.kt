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

data class TrafficUiState(
    val downloadHistory: List<Float> = emptyList(),
    val uploadHistory: List<Float> = emptyList(),
    val currentDownloadSpeed: Double = 0.0,
    val currentUploadSpeed: Double = 0.0,
    val totalDownloadBytes: Long = 0L,
    val totalUploadBytes: Long = 0L
)

class TrafficViewModel(
    private val repository: PadavanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrafficUiState())
    val uiState: StateFlow<TrafficUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var lastTimestamp = 0L

    init {
        startPolling()
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                repository.getTrafficStats().collect { result ->
                    result.onSuccess { stats ->
                        val wanKey = stats.keys.find {
                            it.equals("WAN", ignoreCase = true) ||
                            it.equals("eth3", ignoreCase = true) ||
                            it.equals("eth2", ignoreCase = true) ||
                            it.contains("wan", ignoreCase = true) ||
                            it.contains("apcli", ignoreCase = true)
                        } ?: stats.keys.firstOrNull()

                        if (wanKey != null) {
                            val pair = stats[wanKey]!!
                            val rx = pair.first
                            val tx = pair.second
                            val now = System.currentTimeMillis()

                            var dlSpeed = 0.0
                            var ulSpeed = 0.0

                            if (lastTimestamp > 0 && now > lastTimestamp) {
                                val timeDiffSec = (now - lastTimestamp) / 1000.0
                                if (rx >= lastRxBytes && tx >= lastTxBytes) {
                                    val rxDiff = rx - lastRxBytes
                                    val txDiff = tx - lastTxBytes

                                    dlSpeed = ((rxDiff * 8.0) / (1024.0 * 1024.0)) / timeDiffSec
                                    ulSpeed = ((txDiff * 8.0) / (1024.0 * 1024.0)) / timeDiffSec
                                }
                            }

                            lastRxBytes = rx
                            lastTxBytes = tx
                            lastTimestamp = now

                            _uiState.update { state ->
                                val dlHistory = state.downloadHistory.toMutableList()
                                val ulHistory = state.uploadHistory.toMutableList()

                                if (lastTimestamp > 0) {
                                    dlHistory.add(dlSpeed.toFloat())
                                    ulHistory.add(ulSpeed.toFloat())
                                    if (dlHistory.size > 20) dlHistory.removeAt(0)
                                    if (ulHistory.size > 20) ulHistory.removeAt(0)
                                }

                                state.copy(
                                    downloadHistory = dlHistory,
                                    uploadHistory = ulHistory,
                                    currentDownloadSpeed = dlSpeed,
                                    currentUploadSpeed = ulSpeed,
                                    totalDownloadBytes = rx,
                                    totalUploadBytes = tx
                                )
                            }
                        }
                    }
                    result.onFailure { error ->
                        _uiState.update { state ->
                            state.copy(
                                currentDownloadSpeed = 0.0,
                                currentUploadSpeed = 0.0
                            )
                        }
                    }
                }
                delay(2000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
