package com.example.padavancontrol.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.padavancontrol.data.CredentialStore
import com.example.padavancontrol.data.PadavanRepository
import com.example.padavancontrol.discovery.RouterDiscoveryService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LoginStep {
    SCANNING,
    CREDENTIALS,
    MANUAL_IP
}

data class LoginUiState(
    val ipAddress: String = "",
    val username: String = "",
    val password: String = "",
    val rememberMe: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val passwordVisible: Boolean = false,
    val isScanning: Boolean = false,
    val scanStatusMessage: String? = null,
    val currentStep: LoginStep = LoginStep.SCANNING,
    val discoveredRouters: List<String> = emptyList()
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
        val isOnboarded = credentialStore.isOnboardingCompleted()
        _uiState.update {
            it.copy(
                ipAddress = credentialStore.getRouterIp(),
                username = credentialStore.getUsername(),
                password = credentialStore.getPassword(),
                rememberMe = credentialStore.isRememberCredentials(),
                currentStep = if (isOnboarded) LoginStep.CREDENTIALS else LoginStep.SCANNING
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
            try {
                val success = repository.checkLogin(
                    state.ipAddress,
                    state.username,
                    state.password
                )

                _uiState.update { it.copy(isLoading = false) }

                if (success) {
                    _events.send(LoginEvent.LoginSuccess)
                } else {
                    _uiState.update { it.copy(errorMessage = "Invalid IP, username, or password.") }
                    _events.send(LoginEvent.ShowError("Invalid IP, username, or password."))
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                if (e is IllegalStateException) {
                    _uiState.update { it.copy(errorMessage = e.message) }
                    _events.send(LoginEvent.ShowError(e.message ?: "Another device is currently logged in."))
                } else {
                    _uiState.update { it.copy(errorMessage = "Connection error. Please try again.") }
                    _events.send(LoginEvent.ShowError("Connection error. Please try again."))
                }
            }
        }
    }

    fun discoverRouter(context: Context) {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isScanning = true, 
                    scanStatusMessage = "Checking network...", 
                    errorMessage = null,
                    discoveredRouters = emptyList()
                ) 
            }

            try {
                // VPN check
                if (RouterDiscoveryService.isVpnActive(context)) {
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            errorMessage = "VPN active. Local router scans may fail. Please pause your VPN to scan."
                        )
                    }
                    return@launch
                }

                val discovered = mutableListOf<String>()

                // 1. Probe the DHCP Gateway IP first
                val gatewayIp = RouterDiscoveryService.getWifiGatewayIp(context)
                if (gatewayIp != null) {
                    _uiState.update { it.copy(scanStatusMessage = "Probing gateway: $gatewayIp...") }
                    val isPadavan = RouterDiscoveryService.probeIp(gatewayIp)
                    if (isPadavan) {
                        discovered.add(gatewayIp)
                        _uiState.update {
                            it.copy(
                                discoveredRouters = discovered.toList(),
                                scanStatusMessage = "Padavan Router found at $gatewayIp!"
                            )
                        }
                    }
                }
                
                // 2. Active scan via SSDP multicast fallback
                _uiState.update { it.copy(scanStatusMessage = "Searching network via SSDP...") }
                val ssdpIps = RouterDiscoveryService.discoverViaSsdp()
                for (ip in ssdpIps) {
                    if (!discovered.contains(ip)) {
                        discovered.add(ip)
                    }
                }

                _uiState.update { 
                    it.copy(
                        isScanning = false,
                        discoveredRouters = discovered.toList(),
                        scanStatusMessage = if (discovered.isNotEmpty()) {
                            "Scan complete. Found ${discovered.size} router(s)."
                        } else {
                            "No Padavan Router detected on this Wi-Fi."
                        },
                        errorMessage = if (discovered.isEmpty()) "No Padavan Router detected on this Wi-Fi. Please enter IP manually." else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        errorMessage = "Scan failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun selectRouter(ip: String) {
        credentialStore.saveRouterIp(ip)
        credentialStore.setOnboardingCompleted(true)
        _uiState.update {
            it.copy(
                ipAddress = ip,
                currentStep = LoginStep.CREDENTIALS,
                errorMessage = null
            )
        }
    }

    fun updateStep(step: LoginStep) {
        _uiState.update { it.copy(currentStep = step, errorMessage = null) }
    }

    fun testManualIp(ip: String) {
        if (ip.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "IP Address is required") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val isPadavan = RouterDiscoveryService.probeIp(ip)
                if (isPadavan) {
                    credentialStore.saveRouterIp(ip)
                    credentialStore.setOnboardingCompleted(true)
                    _uiState.update {
                        it.copy(
                            ipAddress = ip,
                            isLoading = false,
                            currentStep = LoginStep.CREDENTIALS
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "The IP address entered does not appear to run Padavan firmware."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Connection test failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }
}
