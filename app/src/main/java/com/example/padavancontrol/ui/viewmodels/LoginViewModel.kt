package com.example.padavancontrol.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.padavancontrol.data.CredentialStore
import com.example.padavancontrol.data.PadavanRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val ipAddress: String = "",
    val username: String = "",
    val password: String = "",
    val rememberMe: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val passwordVisible: Boolean = false
)

sealed interface LoginEvent {
    data object LoginSuccess : LoginEvent
    data class ShowError(val message: String) : LoginEvent
}

class LoginViewModel(
    private val repository: PadavanRepository,
    private val credentialStore: CredentialStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        _uiState.update {
            it.copy(
                ipAddress = credentialStore.getRouterIp(),
                username = credentialStore.getUsername(),
                password = credentialStore.getPassword(),
                rememberMe = credentialStore.isRememberCredentials()
            )
        }
    }

    fun updateIpAddress(value: String) {
        _uiState.update { it.copy(ipAddress = value) }
    }

    fun updateUsername(value: String) {
        _uiState.update { it.copy(username = value) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun updateRememberMe(value: Boolean) {
        _uiState.update { it.copy(rememberMe = value) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun login() {
        val state = _uiState.value

        if (state.ipAddress.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "IP Address is required") }
            return
        }
        if (state.username.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Username is required") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val success = repository.checkLogin(
                state.ipAddress,
                state.username,
                state.password
            )

            _uiState.update { it.copy(isLoading = false) }

            if (success) {
                _events.send(LoginEvent.LoginSuccess)
            } else {
                _uiState.update {
                    it.copy(errorMessage = "Connection failed. Check IP & credentials.")
                }
            }
        }
    }
}
