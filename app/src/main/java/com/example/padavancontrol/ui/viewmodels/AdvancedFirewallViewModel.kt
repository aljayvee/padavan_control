package com.example.padavancontrol.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.padavancontrol.data.PadavanRepository
import com.example.padavancontrol.data.models.FirewallConfig
import com.example.padavancontrol.data.models.ServiceFilterRule
import com.example.padavancontrol.data.models.MacFilterRule
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdvancedFirewallUiState(
    val config: FirewallConfig = FirewallConfig(),
    val blockedDomains: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false,
    val loadProgress: Float = 0f,
    val loadStatus: String = "",
    val blockPublicDoh: Boolean = false
)

class AdvancedFirewallViewModel(
    private val repository: PadavanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdvancedFirewallUiState())
    val uiState: StateFlow<AdvancedFirewallUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    fun loadConfig(page: String) {
        _uiState.update { it.copy(isLoading = true, loadProgress = 0.2f, loadStatus = "Fetching configuration...", errorMessage = null, saveSuccess = false) }
        viewModelScope.launch {
            if (page.contains("DHCP", ignoreCase = true)) {
                repository.getLanConfig(page).collect { result ->
                    result.onSuccess { lanConfig ->
                        val domains = parseDomainsFromDnsmasq(lanConfig.dnsmasqDnsmasqConf)
                        var blockDoh = false
                        repository.getScriptConfig("Advanced_Scripts_Content.asp").collect { scriptResult ->
                            scriptResult.onSuccess { scriptConfig ->
                                blockDoh = scriptConfig.scriptIpRules.contains("# --- Padavan Control App DoH Block Start ---")
                            }
                        }
                        _uiState.update { it.copy(blockedDomains = domains, blockPublicDoh = blockDoh, loadProgress = 1.0f, loadStatus = "Domains loaded.", isLoading = false) }
                    }
                    result.onFailure { exception ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loadProgress = 0f,
                                errorMessage = exception.message ?: "Failed to load Domains configuration"
                            )
                        }
                    }
                }
            } else {
                repository.getFirewallConfig(page).collect { result ->
                    result.onSuccess { config ->
                        _uiState.update { it.copy(config = config, loadProgress = 1.0f, loadStatus = "Firewall settings loaded.", isLoading = false) }
                    }
                    result.onFailure { exception ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loadProgress = 0f,
                                errorMessage = exception.message ?: "Failed to load Firewall configuration"
                            )
                        }
                    }
                }
            }
        }
    }

    fun updateConfig(config: FirewallConfig) {
        _uiState.update { it.copy(config = config) }
    }

    fun saveConfig(page: String) {
        _uiState.update { it.copy(isSaving = true, errorMessage = null, saveSuccess = false) }
        viewModelScope.launch {
            val success = repository.saveFirewallConfig(page, _uiState.value.config)
            if (success) {
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                _toastMessage.emit("Settings applied successfully.")
            } else {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Failed to apply Firewall settings") }
                _toastMessage.emit("Failed to apply settings.")
            }
        }
    }

    fun addServiceFilterRule(rule: ServiceFilterRule, page: String) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val success = repository.addServiceFilterRule(rule)
            _uiState.update { it.copy(isSaving = false) }
            if (success) {
                _toastMessage.emit("Service filter rule added successfully.")
                loadConfig(page)
            } else {
                _toastMessage.emit("Failed to add service filter rule.")
            }
        }
    }

    fun deleteServiceFilterRule(index: Int, page: String) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val success = repository.deleteServiceFilterRule(index)
            _uiState.update { it.copy(isSaving = false) }
            if (success) {
                _toastMessage.emit("Service filter rule deleted.")
                loadConfig(page)
            } else {
                _toastMessage.emit("Failed to delete service filter rule.")
            }
        }
    }

    fun addUrlKeyword(keyword: String, page: String) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val success = repository.addUrlKeyword(keyword)
            _uiState.update { it.copy(isSaving = false) }
            if (success) {
                _toastMessage.emit("URL keyword filter added.")
                loadConfig(page)
            } else {
                _toastMessage.emit("Failed to add URL keyword.")
            }
        }
    }

    fun deleteUrlKeyword(index: Int, page: String) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val success = repository.deleteUrlKeyword(index)
            _uiState.update { it.copy(isSaving = false) }
            if (success) {
                _toastMessage.emit("URL keyword filter deleted.")
                loadConfig(page)
            } else {
                _toastMessage.emit("Failed to delete URL keyword.")
            }
        }
    }

    fun addMacFilterRule(rule: MacFilterRule, page: String) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val success = repository.addMacFilterRule(rule)
            _uiState.update { it.copy(isSaving = false) }
            if (success) {
                _toastMessage.emit("MAC filter rule added.")
                loadConfig(page)
            } else {
                _toastMessage.emit("Failed to add MAC filter rule.")
            }
        }
    }

    fun deleteMacFilterRule(index: Int, page: String) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val success = repository.deleteMacFilterRule(index)
            _uiState.update { it.copy(isSaving = false) }
            if (success) {
                _toastMessage.emit("MAC filter rule deleted.")
                loadConfig(page)
            } else {
                _toastMessage.emit("Failed to delete MAC filter rule.")
            }
        }
    }

    fun addDomainBlock(domain: String, page: String) {
        val currentDomains = _uiState.value.blockedDomains.toMutableList()
        if (currentDomains.contains(domain)) return
        currentDomains.add(domain)
        saveDnsIpsetConfig(currentDomains, page)
    }

    fun deleteDomainBlock(domain: String, page: String) {
        val currentDomains = _uiState.value.blockedDomains.toMutableList()
        currentDomains.remove(domain)
        saveDnsIpsetConfig(currentDomains, page)
    }

    fun toggleBlockPublicDoh(enabled: Boolean, page: String) {
        if (_uiState.value.blockPublicDoh == enabled) return
        _uiState.update { it.copy(blockPublicDoh = enabled) }
        saveDnsIpsetConfig(_uiState.value.blockedDomains, page)
    }

    private fun saveDnsIpsetConfig(domains: List<String>, page: String) {
        _uiState.update { it.copy(isSaving = true, errorMessage = null, saveSuccess = false) }
        viewModelScope.launch {
            try {
                var currentLanConfig: com.example.padavancontrol.data.models.LanConfig? = null
                repository.getLanConfig(page).collect { result ->
                    currentLanConfig = result.getOrNull()
                }

                if (currentLanConfig == null) {
                    _uiState.update { it.copy(isSaving = false, errorMessage = "Failed to fetch LAN configuration for updating.") }
                    _toastMessage.emit("Failed to save domains.")
                    return@launch
                }

                val updatedDnsmasq = updateDnsmasqConf(currentLanConfig!!.dnsmasqDnsmasqConf, domains)
                val updatedLanConfig = currentLanConfig!!.copy(dnsmasqDnsmasqConf = updatedDnsmasq)

                val saveLanSuccess = repository.saveLanConfig(page, updatedLanConfig)
                if (!saveLanSuccess) {
                    _uiState.update { it.copy(isSaving = false, errorMessage = "Failed to save dnsmasq configuration to router.") }
                    _toastMessage.emit("Failed to save domains.")
                    return@launch
                }

                var currentScriptConfig: com.example.padavancontrol.data.models.ScriptConfig? = null
                repository.getScriptConfig("Advanced_Scripts_Content.asp").collect { result ->
                    currentScriptConfig = result.getOrNull()
                }

                if (currentScriptConfig == null) {
                    _uiState.update { it.copy(isSaving = false, errorMessage = "Failed to fetch Script configuration.") }
                    _toastMessage.emit("Failed to save domains.")
                    return@launch
                }

                val updatedIpRules = updateScriptIpRules(currentScriptConfig!!.scriptIpRules, _uiState.value.blockPublicDoh)
                val updatedScriptConfig = currentScriptConfig!!.copy(scriptIpRules = updatedIpRules)

                val saveScriptSuccess = repository.saveScriptConfig("Advanced_Scripts_Content.asp", updatedScriptConfig)
                if (!saveScriptSuccess) {
                    _uiState.update { it.copy(isSaving = false, errorMessage = "Failed to save startup scripts to router.") }
                    _toastMessage.emit("Failed to save domains.")
                    return@launch
                }

                repository.commitFlash()
                repository.executeCommand("restart_firewall; restart_dhcpd")

                _uiState.update { it.copy(blockedDomains = domains, isSaving = false, saveSuccess = true) }
                _toastMessage.emit("Settings applied successfully.")
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message ?: "An unexpected error occurred.") }
                _toastMessage.emit("Failed to apply settings.")
            }
        }
    }

    private fun parseDomainsFromDnsmasq(conf: String): List<String> {
        val domains = mutableListOf<String>()
        val startMarker = "# --- Padavan Control App Domains Start ---"
        val endMarker = "# --- Padavan Control App Domains End ---"
        val lines = conf.lines()
        var inBlock = false
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed == startMarker) {
                inBlock = true
                continue
            }
            if (trimmed == endMarker) {
                inBlock = false
                break
            }
            if (inBlock) {
                if (trimmed.startsWith("address=/") && trimmed.endsWith("/0.0.0.0")) {
                    val domain = trimmed.substringAfter("address=/").substringBefore("/0.0.0.0").trim()
                    if (domain.isNotEmpty() && !domains.contains(domain)) {
                        domains.add(domain)
                    }
                }
            }
        }
        return domains
    }

    private fun updateDnsmasqConf(conf: String, domains: List<String>): String {
        val startMarker = "# --- Padavan Control App Domains Start ---"
        val endMarker = "# --- Padavan Control App Domains End ---"
        val lines = conf.lines()
        val outLines = mutableListOf<String>()
        var inBlock = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed == startMarker) {
                inBlock = true
                continue
            }
            if (trimmed == endMarker) {
                inBlock = false
                continue
            }
            if (!inBlock) {
                outLines.add(line)
            }
        }

        val sb = StringBuilder()
        for (line in outLines) {
            sb.append(line).append("\n")
        }
        sb.append(startMarker).append("\n")
        for (domain in domains) {
            sb.append("address=/").append(domain).append("/0.0.0.0\n")
            sb.append("address=/").append(domain).append("/::\n")
        }
        // Force browsers to disable DNS-over-HTTPS by blocking the canary domain
        sb.append("local=/use-application-dns.net/\n")
        sb.append(endMarker)
        return sb.toString()
    }

    private fun updateScriptIpRules(rules: String, blockDoh: Boolean): String {
        val startMarker = "# --- Padavan Control App IPSET Block Start ---"
        val endMarker = "# --- Padavan Control App IPSET Block End ---"
        val dohStartMarker = "# --- Padavan Control App DoH Block Start ---"
        val dohEndMarker = "# --- Padavan Control App DoH Block End ---"

        val lines = rules.lines()
        val outLines = mutableListOf<String>()
        var inBlock = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed == startMarker || trimmed == dohStartMarker) {
                inBlock = true
                continue
            }
            if (trimmed == endMarker || trimmed == dohEndMarker) {
                inBlock = false
                continue
            }
            if (!inBlock) {
                outLines.add(line)
            }
        }

        val baseRules = outLines.joinToString("\n").trimEnd()

        val dohBlock = if (blockDoh) {
            """
            
            $dohStartMarker
            # Drop DNS-over-TLS (port 853)
            iptables -C FORWARD -p tcp --dport 853 -j DROP 2>/dev/null || \
            iptables -I FORWARD -p tcp --dport 853 -j DROP
            iptables -C FORWARD -p udp --dport 853 -j DROP 2>/dev/null || \
            iptables -I FORWARD -p udp --dport 853 -j DROP
            
            # Drop DNS-over-HTTPS for Known Providers (port 443)
            # Google
            iptables -C FORWARD -d 8.8.8.8 -p tcp --dport 443 -j DROP 2>/dev/null || \
            iptables -I FORWARD -d 8.8.8.8 -p tcp --dport 443 -j DROP
            iptables -C FORWARD -d 8.8.4.4 -p tcp --dport 443 -j DROP 2>/dev/null || \
            iptables -I FORWARD -d 8.8.4.4 -p tcp --dport 443 -j DROP
            # Cloudflare
            iptables -C FORWARD -d 1.1.1.1 -p tcp --dport 443 -j DROP 2>/dev/null || \
            iptables -I FORWARD -d 1.1.1.1 -p tcp --dport 443 -j DROP
            iptables -C FORWARD -d 1.0.0.1 -p tcp --dport 443 -j DROP 2>/dev/null || \
            iptables -I FORWARD -d 1.0.0.1 -p tcp --dport 443 -j DROP
            # Quad9
            iptables -C FORWARD -d 9.9.9.9 -p tcp --dport 443 -j DROP 2>/dev/null || \
            iptables -I FORWARD -d 9.9.9.9 -p tcp --dport 443 -j DROP
            iptables -C FORWARD -d 149.112.112.112 -p tcp --dport 443 -j DROP 2>/dev/null || \
            iptables -I FORWARD -d 149.112.112.112 -p tcp --dport 443 -j DROP
            # AdGuard
            iptables -C FORWARD -d 94.140.14.14 -p tcp --dport 443 -j DROP 2>/dev/null || \
            iptables -I FORWARD -d 94.140.14.14 -p tcp --dport 443 -j DROP
            iptables -C FORWARD -d 94.140.15.15 -p tcp --dport 443 -j DROP 2>/dev/null || \
            iptables -I FORWARD -d 94.140.15.15 -p tcp --dport 443 -j DROP
            $dohEndMarker
            
            """.trimIndent()
        } else {
            ""
        }

        return if (blockDoh) {
            "$baseRules\n$dohBlock\n"
        } else {
            if (baseRules.isEmpty()) "" else "$baseRules\n"
        }
    }
}
