package com.example.padavancontrol.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.padavancontrol.data.PadavanRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LogViewerUiState(
    val rawLogs: String = "Fetching syslog messages from router...",
    val filterQuery: String = "",
    val isLoading: Boolean = false
)

sealed interface LogViewerEvent {
    data class ShowToast(val message: String) : LogViewerEvent
}

class LogViewerViewModel(
    private val repository: PadavanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogViewerUiState())
    val uiState: StateFlow<LogViewerUiState> = _uiState.asStateFlow()

    private val _events = Channel<LogViewerEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        fetchLogs()
    }

    fun updateFilterQuery(value: String) {
        _uiState.update { it.copy(filterQuery = value) }
    }

    fun fetchLogs() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.getSystemLogs().collect { result ->
                result.onSuccess { logs ->
                    _uiState.update { it.copy(rawLogs = logs.ifEmpty { "Syslog is currently empty." }, isLoading = false) }
                }.onFailure { err ->
                    _uiState.update { it.copy(rawLogs = "Failed to fetch logs: ${err.message}", isLoading = false) }
                }
            }
        }
    }

    fun clearLogs() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val success = repository.clearSystemLogs()
            if (success) {
                _events.send(LogViewerEvent.ShowToast("System syslog cleared"))
                _uiState.update { it.copy(rawLogs = "Syslog is currently empty.", isLoading = false) }
            } else {
                _events.send(LogViewerEvent.ShowToast("Failed to clear logs"))
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
