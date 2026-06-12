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
    val filteredLogLines: List<String> = emptyList(),
    val rawLogsLength: Int = 0,
    val filterQuery: String = "",
    val isLoading: Boolean = false
)

sealed interface LogViewerEvent {
    data class ShowToast(val message: String) : LogViewerEvent
}

class LogViewerViewModel(
    private val repository: PadavanRepository,
    private val defaultDispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogViewerUiState())
    val uiState: StateFlow<LogViewerUiState> = _uiState.asStateFlow()

    private val _events = Channel<LogViewerEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var allLogLines: List<String> = listOf("Fetching syslog messages from router...")

    init {
        fetchLogs()
    }

    fun updateFilterQuery(value: String) {
        _uiState.update { it.copy(filterQuery = value) }
        applyFilter()
    }

    private fun applyFilter() {
        val query = _uiState.value.filterQuery
        viewModelScope.launch(defaultDispatcher) {
            val filtered = if (query.trim().isEmpty()) {
                allLogLines
            } else {
                allLogLines.filter { it.contains(query, ignoreCase = true) }
            }
            _uiState.update { it.copy(filteredLogLines = filtered) }
        }
    }

    fun fetchLogs() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.getSystemLogs().collect { result ->
                result.onSuccess { logs ->
                    val raw = logs.ifEmpty { "Syslog is currently empty." }
                    viewModelScope.launch(defaultDispatcher) {
                        allLogLines = raw.split("\n")
                        _uiState.update { it.copy(rawLogsLength = raw.length, isLoading = false) }
                        applyFilter()
                    }
                }.onFailure { err ->
                    viewModelScope.launch(defaultDispatcher) {
                        allLogLines = listOf("Failed to fetch logs: ${err.message}")
                        _uiState.update { it.copy(isLoading = false) }
                        applyFilter()
                    }
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
                allLogLines = listOf("Syslog is currently empty.")
                _uiState.update { it.copy(rawLogsLength = 0, isLoading = false) }
                applyFilter()
            } else {
                _events.send(LogViewerEvent.ShowToast("Failed to clear logs"))
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
