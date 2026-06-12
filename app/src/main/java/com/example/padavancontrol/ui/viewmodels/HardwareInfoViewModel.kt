package com.example.padavancontrol.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.padavancontrol.data.PadavanRepository
import com.example.padavancontrol.data.models.HardwareInfoData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import kotlinx.coroutines.isActive

data class HardwareInfoUiState(
    val hardwareInfo: HardwareInfoData? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class HardwareInfoViewModel(
    private val repository: PadavanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HardwareInfoUiState())
    val uiState: StateFlow<HardwareInfoUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        startPolling()
    }

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                repository.getHardwareInfo().collect { result ->
                    result.onSuccess { data ->
                        _uiState.update {
                            it.copy(hardwareInfo = data, isLoading = false, errorMessage = null)
                        }
                    }
                    result.onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Failed to fetch hardware status"
                            )
                        }
                    }
                }
                delay(3000)
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        startPolling()
    }

    public override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
