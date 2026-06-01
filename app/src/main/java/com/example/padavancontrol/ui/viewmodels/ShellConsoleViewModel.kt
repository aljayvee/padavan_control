package com.example.padavancontrol.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.padavancontrol.data.PadavanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShellConsoleUiState(
    val commandInput: String = "",
    val terminalOutput: String = "root@newifi_d2:~# Type a command or select a shortcut chip below to begin diagnostics.\n",
    val isExecuting: Boolean = false,
    val shortcutChips: List<String> = listOf(
        "ping -c 3 1.1.1.1",
        "df -h",
        "free",
        "ifconfig",
        "netstat -l",
        "uptime"
    )
)

class ShellConsoleViewModel(
    private val repository: PadavanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShellConsoleUiState())
    val uiState: StateFlow<ShellConsoleUiState> = _uiState.asStateFlow()

    fun updateCommandInput(value: String) {
        _uiState.update { it.copy(commandInput = value) }
    }

    fun runCommand(cmd: String) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty() || _uiState.value.isExecuting) return

        _uiState.update {
            it.copy(
                isExecuting = true,
                terminalOutput = it.terminalOutput + "root@newifi_d2:~# $cmd\n",
                commandInput = ""
            )
        }

        viewModelScope.launch {
            val response = repository.executeCommand(trimmed)
            _uiState.update {
                it.copy(
                    isExecuting = false,
                    terminalOutput = it.terminalOutput + "$response\n\n"
                )
            }
        }
    }
}
