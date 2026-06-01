package com.example.padavancontrol.data

import com.example.padavancontrol.data.models.LanClient
import com.example.padavancontrol.data.models.PortLink
import com.example.padavancontrol.data.models.SystemStatus
import com.example.padavancontrol.data.models.WanStatus
import com.example.padavancontrol.data.models.WirelessConfig
import com.example.padavancontrol.data.models.FirewallConfig
import com.example.padavancontrol.data.models.UsbShareConfig
import com.example.padavancontrol.data.models.ServiceFilterRule
import com.example.padavancontrol.data.models.MacFilterRule
import com.example.padavancontrol.data.models.AdminConfig
import com.example.padavancontrol.data.models.ScriptConfig
import com.example.padavancontrol.network.PadavanApiService
import com.example.padavancontrol.network.PadavanResponseParser
import com.example.padavancontrol.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.IOException

interface PadavanRepository {
    suspend fun checkLogin(ip: String, user: String, pass: String): Boolean
    fun getSystemStatus(): Flow<Result<SystemStatus>>
    fun getWanStatus(): Flow<Result<WanStatus>>
    fun getLanClients(): Flow<Result<List<LanClient>>>
    fun getTrafficStats(): Flow<Result<Map<String, Pair<Long, Long>>>>
    fun getLanLinks(): Flow<Result<List<PortLink>>>
    fun getSystemLogs(): Flow<Result<String>>
    suspend fun clearSystemLogs(): Boolean
    suspend fun executeCommand(cmd: String): String
    suspend fun commitFlash(): Boolean
    suspend fun rebootRouter(): Boolean
    suspend fun shutdownRouter(): Boolean
    suspend fun toggleWifi2G(enable: Boolean): Boolean
    suspend fun toggleWifi5G(enable: Boolean): Boolean
    fun getWirelessConfig(page: String, is5GHz: Boolean): Flow<Result<WirelessConfig>>
    suspend fun saveWirelessConfig(page: String, config: WirelessConfig, is5GHz: Boolean): Boolean
    fun getLanConfig(page: String): Flow<Result<com.example.padavancontrol.data.models.LanConfig>>
    suspend fun saveLanConfig(page: String, config: com.example.padavancontrol.data.models.LanConfig): Boolean
    fun getWanConfig(page: String): Flow<Result<com.example.padavancontrol.data.models.WanConfig>>
    suspend fun saveWanConfig(page: String, config: com.example.padavancontrol.data.models.WanConfig): Boolean
    suspend fun addStaticLease(mac: String, ip: String, name: String): Boolean
    suspend fun deleteStaticLease(index: Int): Boolean
    suspend fun addPortForwardRule(rule: com.example.padavancontrol.data.models.PortForwardRule): Boolean
    suspend fun deletePortForwardRule(index: Int): Boolean
    suspend fun sendWolPacket(mac: String): Boolean
    fun getFirewallConfig(page: String): Flow<Result<FirewallConfig>>
    suspend fun saveFirewallConfig(page: String, config: FirewallConfig): Boolean
    fun getUsbShareConfig(page: String): Flow<Result<UsbShareConfig>>
    suspend fun saveUsbShareConfig(page: String, config: UsbShareConfig): Boolean
    suspend fun addServiceFilterRule(rule: ServiceFilterRule): Boolean
    suspend fun deleteServiceFilterRule(index: Int): Boolean
    suspend fun addUrlKeyword(keyword: String): Boolean
    suspend fun deleteUrlKeyword(index: Int): Boolean
    suspend fun addMacFilterRule(rule: MacFilterRule): Boolean
    suspend fun deleteMacFilterRule(index: Int): Boolean
    fun getAdminConfig(page: String): Flow<Result<AdminConfig>>
    suspend fun saveAdminConfig(page: String, config: AdminConfig): Boolean
    fun getScriptConfig(page: String): Flow<Result<ScriptConfig>>
    suspend fun saveScriptConfig(page: String, config: ScriptConfig): Boolean
    fun scanWifiNetworks(is5GHz: Boolean): Flow<Result<List<com.example.padavancontrol.data.models.WifiNetwork>>>
}

class DefaultPadavanRepository(
    private val credentialStore: CredentialStore
) : PadavanRepository {

    private var cachedWireless2g = WirelessConfig()
    private var cachedWireless5g = WirelessConfig()
    private var cachedLan = com.example.padavancontrol.data.models.LanConfig()
    private var cachedWan = com.example.padavancontrol.data.models.WanConfig()
    private var cachedFirewall = FirewallConfig()
    private var cachedUsbShare = UsbShareConfig()
    private var cachedAdmin = AdminConfig()
    private var cachedScript = ScriptConfig()

    private var prevCpuTotal = 0L
    private var prevCpuBusy = 0L

    private fun getService(): PadavanApiService {
        RetrofitClient.initialize(
            ipAddress = credentialStore.getRouterIp(),
            usernameProvider = { credentialStore.getUsername() },
            passwordProvider = { credentialStore.getPassword() }
        )
        return RetrofitClient.getApiService()
    }

    override suspend fun checkLogin(ip: String, user: String, pass: String): Boolean {
        try {
            RetrofitClient.initialize(
                ipAddress = ip,
                usernameProvider = { user },
                passwordProvider = { pass }
            )
            val service = RetrofitClient.getApiService()
            val response = service.checkLogin()
            if (response.isSuccessful) {
                credentialStore.saveCredentials(
                    user,
                    ip,
                    pass,
                    true,
                    credentialStore.isUseBiometric()
                )
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    override fun getSystemStatus(): Flow<Result<SystemStatus>> = flow {
        try {
            val response = getService().getSystemStatusData()
            if (response.isSuccessful && response.body() != null) {
                val status = PadavanResponseParser.parseSystemStatus(response.body()!!)
                if (status != null) {
                    val deltaTotal = status.cpuTotal - prevCpuTotal
                    val deltaBusy = status.cpuBusy - prevCpuBusy

                    val finalCpuUsage = if (prevCpuTotal > 0L && deltaTotal > 0L) {
                        (deltaBusy * 100 / deltaTotal).coerceIn(0, 100).toInt()
                    } else {
                        0
                    }

                    prevCpuTotal = status.cpuTotal
                    prevCpuBusy = status.cpuBusy

                    val updatedStatus = status.copy(cpuUsage = finalCpuUsage)
                    emit(Result.success(updatedStatus))
                } else {
                    emit(Result.failure(IOException("Failed to parse system status")))
                }
            } else {
                emit(Result.failure(IOException("HTTP Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override fun getWanStatus(): Flow<Result<WanStatus>> = flow {
        try {
            val response = getService().getWanLinkStatus()
            if (response.isSuccessful && response.body() != null) {
                val status = PadavanResponseParser.parseWanStatus(response.body()!!)
                if (status != null) {
                    emit(Result.success(status))
                } else {
                    emit(Result.failure(IOException("Failed to parse WAN status")))
                }
            } else {
                emit(Result.failure(IOException("HTTP Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override fun getLanClients(): Flow<Result<List<LanClient>>> = flow {
        try {
            val response = getService().getLanClients()
            if (response.isSuccessful && response.body() != null) {
                val clients = PadavanResponseParser.parseLanClients(response.body()!!)
                emit(Result.success(clients))
            } else {
                emit(Result.failure(IOException("HTTP Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override fun getTrafficStats(): Flow<Result<Map<String, Pair<Long, Long>>>> = flow {
        try {
            val response = getService().getTrafficStats()
            if (response.isSuccessful && response.body() != null) {
                val stats = PadavanResponseParser.parseTrafficStats(response.body()!!)
                emit(Result.success(stats))
            } else {
                emit(Result.failure(IOException("HTTP Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun rebootRouter(): Boolean {
        return try {
            val response = getService().performAction(actionMode = " Reboot ")
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun shutdownRouter(): Boolean {
        return try {
            val response = getService().performAction(actionMode = " Shutdown ")
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun toggleWifi2G(enable: Boolean): Boolean {
        return try {
            val radioVal = if (enable) "1" else "0"
            val response = getService().performAction(actionMode = " Apply ", rtRadioX = radioVal)
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun toggleWifi5G(enable: Boolean): Boolean {
        return try {
            val radioVal = if (enable) "1" else "0"
            val response = getService().performAction(actionMode = " Apply ", wlRadioX = radioVal)
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun getLanLinks(): Flow<Result<List<PortLink>>> = flow {
        try {
            val response = getService().getLanLinkStatus()
            if (response.isSuccessful && response.body() != null) {
                val links = PadavanResponseParser.parseLanLinks(response.body()!!)
                emit(Result.success(links))
            } else {
                emit(Result.failure(IOException("HTTP Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override fun getSystemLogs(): Flow<Result<String>> = flow {
        try {
            val response = getService().getSystemLogs()
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(IOException("HTTP Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun clearSystemLogs(): Boolean {
        return try {
            getService().clearLogs().isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun executeCommand(cmd: String): String {
        return try {
            // Post command to applying queue
            val applyResponse = getService().executeCommand(systemCmd = cmd)
            if (!applyResponse.isSuccessful) return "Error sending command: ${applyResponse.code()}"
            
            // Wait for C exec to output into file
            delay(500)
            
            // Fetch contents
            val response = getService().getConsoleResponse()
            if (response.isSuccessful) {
                response.body()?.trim() ?: "No output returned"
            } else {
                "Error reading command output: ${response.code()}"
            }
        } catch (e: Exception) {
            "Exception during command execution: ${e.message}"
        }
    }

    override suspend fun commitFlash(): Boolean {
        return try {
            getService().commitFlash().isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override fun getWirelessConfig(page: String, is5GHz: Boolean): Flow<Result<WirelessConfig>> = flow {
        try {
            val response = getService().getPageContent(page)
            if (response.isSuccessful && response.body() != null) {
                val existing = if (is5GHz) cachedWireless5g else cachedWireless2g
                val config = PadavanResponseParser.parseWirelessConfig(response.body()!!, is5GHz, existing)
                if (is5GHz) cachedWireless5g = config else cachedWireless2g = config
                emit(Result.success(config))
            } else {
                emit(Result.failure(IOException("HTTP Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun saveWirelessConfig(page: String, config: WirelessConfig, is5GHz: Boolean): Boolean {
        return try {
            val prefix = if (is5GHz) "wl_" else "rt_"
            val sidList = if (is5GHz) "WLANConfig11a;" else "WLANConfig11b;"
            val fields = mutableMapOf<String, String>()

            fields["action_mode"] = " Apply "
            fields["current_page"] = "/$page"
            fields["next_page"] = ""
            fields["sid_list"] = sidList

            if (page.contains("WGuest", ignoreCase = true)) {
                fields["${prefix}guest_enable"] = if (config.guestEnabled) "1" else "0"
                fields["${prefix}guest_ssid"] = config.guestSsid
                fields["${prefix}guest_closed"] = if (config.guestClosed) "1" else "0"
                fields["${prefix}guest_lan_isolate"] = if (config.guestLanIsolate) "1" else "0"
                fields["${prefix}guest_ap_isolate"] = if (config.guestApIsolate) "1" else "0"
                fields["${prefix}guest_auth_mode"] = config.guestAuthMode
                fields["${prefix}guest_wpa_mode"] = config.guestWpaMode
                fields["${prefix}guest_crypto"] = config.guestCrypto
                fields["${prefix}guest_wpa_psk"] = config.guestWpaPsk
            } else if (page.contains("WMode", ignoreCase = true)) {
                val mode = config.modeX
                fields["action_mode"] = if (mode == "1" || mode == "2") " Restart " else " Apply "
                fields["${prefix}mode_x"] = mode
                fields["${prefix}sta_wisp"] = config.staWisp
                fields["${prefix}sta_ssid"] = config.staSsid
                fields["${prefix}sta_auth_mode"] = config.staAuthMode
                fields["${prefix}sta_wpa_mode"] = config.staWpaMode
                fields["${prefix}sta_crypto"] = config.staCrypto
                fields["${prefix}sta_wpa_psk"] = config.staWpaPsk
                fields["${prefix}sta_auto"] = config.staAuto
                fields["${prefix}channel"] = config.channel
            } else {
                fields["${prefix}radio_x"] = if (config.isEnabled) "1" else "0"
                fields["${prefix}ssid"] = config.ssid
                fields["${prefix}closed"] = if (config.isClosed) "1" else "0"
                fields["${prefix}gmode"] = config.wirelessMode
                fields["${prefix}HT_BW"] = config.bandwidth
                fields["${prefix}channel"] = config.channel
                fields["${prefix}auth_mode"] = config.authMode
                fields["${prefix}wpa_mode"] = config.wpaMode
                fields["${prefix}crypto"] = config.crypto
                fields["${prefix}wpa_psk"] = config.wpaPsk
                fields["${prefix}TxPower"] = config.txPower.toString()
                fields["${prefix}mcs_mode"] = config.mcsMode
                fields["${prefix}KickStaRssiLow"] = config.kickStaRssiLow.toString()
                fields["${prefix}AssocReqRssiThres"] = config.assocReqRssiThres.toString()
                fields["${prefix}country_code"] = config.countryCode
            }

            val response = getService().applySettings(fields)
            if (response.isSuccessful) {
                if (is5GHz) cachedWireless5g = config else cachedWireless2g = config
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun getLanConfig(page: String): Flow<Result<com.example.padavancontrol.data.models.LanConfig>> = flow {
        try {
            val response = getService().getPageContent(page)
            if (response.isSuccessful && response.body() != null) {
                val config = PadavanResponseParser.parseLanConfig(response.body()!!, cachedLan)
                cachedLan = config
                emit(Result.success(config))
            } else {
                emit(Result.failure(IOException("HTTP Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun saveLanConfig(page: String, config: com.example.padavancontrol.data.models.LanConfig): Boolean {
        return try {
            val fields = mutableMapOf<String, String>()
            fields["action_mode"] = " Apply "
            fields["current_page"] = "/$page"
            fields["next_page"] = ""
            
            if (page.contains("LAN", ignoreCase = true)) {
                fields["action_mode"] = " Restart "
                fields["sid_list"] = "LANHostConfig;"
                fields["lan_ipaddr"] = config.lanIpAddr
                fields["lan_netmask"] = config.lanNetmask
                fields["lan_gateway"] = config.lanGateway
                fields["lan_stp"] = if (config.lanStp) "1" else "0"
            } else if (page.contains("DHCP", ignoreCase = true)) {
                fields["action_mode"] = " Restart "
                fields["sid_list"] = "LANHostConfig;"
                fields["dhcp_enable_x"] = if (config.dhcpEnabled) "1" else "0"
                fields["lan_domain"] = config.dhcpDomain
                fields["dhcp_start"] = config.dhcpStart
                fields["dhcp_end"] = config.dhcpEnd
                fields["dhcp_lease"] = config.dhcpLease.toString()
                fields["dhcp_gateway_x"] = config.dhcpGateway
                fields["dhcp_dns1_x"] = config.dhcpDns1
                fields["dhcp_dns2_x"] = config.dhcpDns2
                fields["dhcp_dns3_x"] = config.dhcpDns3
                fields["dhcp_wins_x"] = config.dhcpWins
                fields["dhcp_static_x"] = if (config.dhcpStaticEnabled) "1" else "0"
                fields["dhcp_static_arp"] = if (config.dhcpStaticArp) "1" else "0"
            } else if (page.contains("IPTV", ignoreCase = true)) {
                fields["sid_list"] = "RouterConfig;LANHostConfig;WLANConfig11a;WLANConfig11b;"
                fields["mr_enable_x"] = if (config.mrEnable) "1" else "0"
                fields["ether_igmp"] = if (config.igmpSnoop) "1" else "0"
            } else if (page.contains("Route", ignoreCase = true)) {
                fields["action_mode"] = if (config.routeEnabled) " Restart " else " Apply "
                fields["sid_list"] = "RouterConfig;"
                fields["sr_enable_x"] = if (config.routeEnabled) "1" else "0"
            } else if (page.contains("Switch", ignoreCase = true)) {
                fields["sid_list"] = "RouterConfig;"
                fields["ether_green"] = if (config.greenEthernet) "1" else "0"
                fields["ether_eee"] = if (config.eeeEnabled) "1" else "0"
            }
            
            val response = getService().applySettings(fields)
            if (response.isSuccessful) {
                cachedLan = config
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun addStaticLease(mac: String, ip: String, name: String): Boolean {
        return try {
            val fields = mutableMapOf<String, String>()
            fields["action_mode"] = " Add "
            fields["group_id"] = "ManualDHCPList"
            fields["current_page"] = "/Advanced_DHCP_Content.asp"
            fields["next_page"] = ""
            fields["sid_list"] = "LANHostConfig;"
            fields["dhcp_staticmac_x_0"] = mac
            fields["dhcp_staticip_x_0"] = ip
            fields["dhcp_staticname_x_0"] = name
            
            val response = getService().applySettings(fields)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteStaticLease(index: Int): Boolean {
        return try {
            val fields = mutableMapOf<String, String>()
            fields["action_mode"] = " Del "
            fields["group_id"] = "ManualDHCPList"
            fields["current_page"] = "/Advanced_DHCP_Content.asp"
            fields["next_page"] = ""
            fields["sid_list"] = "LANHostConfig;"
            fields["ManualDHCPList"] = ""
            fields["ManualDHCPList_s"] = index.toString()
            
            val response = getService().applySettings(fields)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override fun getWanConfig(page: String): Flow<Result<com.example.padavancontrol.data.models.WanConfig>> = flow {
        try {
            val response = getService().getPageContent(page)
            if (response.isSuccessful && response.body() != null) {
                val config = PadavanResponseParser.parseWanConfig(response.body()!!, cachedWan)
                cachedWan = config
                emit(Result.success(config))
            } else {
                emit(Result.failure(IOException("HTTP Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun saveWanConfig(page: String, config: com.example.padavancontrol.data.models.WanConfig): Boolean {
        return try {
            val fields = mutableMapOf<String, String>()
            fields["action_mode"] = " Apply "
            fields["current_page"] = "/$page"
            fields["next_page"] = ""
            
            if (page.contains("WAN", ignoreCase = true)) {
                fields["sid_list"] = "IPConnection;"
                fields["wan_proto"] = config.wanProto
                fields["wan_ipaddr"] = config.wanIpAddr
                fields["wan_netmask"] = config.wanNetmask
                fields["wan_gateway"] = config.wanGateway
                fields["wan_dnsenable_x"] = if (config.wanDnsEnable) "1" else "0"
                fields["wan_dns1_x"] = config.wanDns1
                fields["wan_dns2_x"] = config.wanDns2
                
                if (config.wanProto == "pppoe") {
                    fields["wan_pppoe_username"] = config.pppoeUser
                    fields["wan_pppoe_passwd"] = config.pppoePass
                    fields["wan_pppoe_mru"] = config.pppoeMru.toString()
                    fields["wan_pppoe_mtu"] = config.pppoeMtu.toString()
                    fields["wan_pppoe_service"] = config.pppoeService
                    fields["wan_pppoe_ac"] = config.pppoeAcName
                }
            } else if (page.contains("VirtualServer", ignoreCase = true)) {
                fields["action_mode"] = if (config.portForwardEnabled) " Restart " else " Apply "
                fields["sid_list"] = "IPConnection;"
                fields["upnp_enable_x"] = if (config.upnpEnabled) "1" else "0"
                fields["upnp_secure"] = if (config.upnpSecure) "1" else "0"
                fields["vts_enable_x"] = if (config.portForwardEnabled) "1" else "0"
                fields["dmz_enable_x"] = if (config.dmzEnabled) "1" else "0"
                fields["dmz_ipaddr"] = config.dmzIp
            } else if (page.contains("DDNS", ignoreCase = true)) {
                fields["sid_list"] = "LANHostConfig;"
                fields["ddns_enable_x"] = if (config.ddnsEnabled) "1" else "0"
                fields["ddns_server_x"] = config.ddnsServer
                fields["ddns_username_x"] = config.ddnsUser
                fields["ddns_passwd_x"] = config.ddnsPass
                fields["ddns_hostname_x"] = config.ddnsHostName
            } else if (page.contains("IPv6", ignoreCase = true)) {
                fields["sid_list"] = "IP6Connection;"
                fields["ip6_service"] = config.ipv6Proto
            }
            
            val response = getService().applySettings(fields)
            if (response.isSuccessful) {
                cachedWan = config
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun addPortForwardRule(rule: com.example.padavancontrol.data.models.PortForwardRule): Boolean {
        return try {
            val fields = mutableMapOf<String, String>()
            fields["action_mode"] = " Add "
            fields["group_id"] = "VSList"
            fields["current_page"] = "/Advanced_VirtualServer_Content.asp"
            fields["next_page"] = ""
            fields["sid_list"] = "IPConnection;"
            fields["vts_name_x_0"] = rule.name
            fields["vts_port_x_0"] = rule.extPort
            fields["vts_ipaddr_x_0"] = rule.intIp
            fields["vts_lport_x_0"] = rule.intPort
            fields["vts_proto_x_0"] = rule.protocol
            fields["vts_desc_x_0"] = rule.desc
            
            val response = getService().applySettings(fields)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deletePortForwardRule(index: Int): Boolean {
        return try {
            val fields = mutableMapOf<String, String>()
            fields["action_mode"] = " Del "
            fields["group_id"] = "VSList"
            fields["current_page"] = "/Advanced_VirtualServer_Content.asp"
            fields["next_page"] = ""
            fields["sid_list"] = "IPConnection;"
            fields["VSList"] = ""
            fields["VSList_s"] = index.toString()
            
            val response = getService().applySettings(fields)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun sendWolPacket(mac: String): Boolean {
        return try {
            val response = getService().getPageContent("wol_action.asp?dstmac=$mac")
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override fun getFirewallConfig(page: String): Flow<Result<FirewallConfig>> = flow {
        try {
            val response = getService().getPageContent(page)
            if (response.isSuccessful && response.body() != null) {
                val config = PadavanResponseParser.parseFirewallConfig(response.body()!!, cachedFirewall)
                cachedFirewall = config
                emit(Result.success(config))
            } else {
                emit(Result.failure(IOException("HTTP Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun saveFirewallConfig(page: String, config: FirewallConfig): Boolean {
        return try {
            val fields = mutableMapOf<String, String>()
            fields["action_mode"] = " Apply "
            fields["current_page"] = "/$page"
            fields["next_page"] = ""
            fields["sid_list"] = "FirewallConfig;"

            if (page.contains("BasicFirewall", ignoreCase = true)) {
                fields["fw_enable_x"] = if (config.fwEnabled) "1" else "0"
                fields["fw_dos_x"] = if (config.fwDosEnabled) "1" else "0"
                fields["fw_syn_cook"] = if (config.fwSynCookEnabled) "1" else "0"
                fields["fw_log_x"] = config.fwLogMode
                fields["misc_ping_x"] = if (config.miscPingEnabled) "1" else "0"
                fields["misc_http_x"] = if (config.miscHttpEnabled) "1" else "0"
                fields["misc_httpport_x"] = config.miscHttpPort
                fields["https_wopen"] = if (config.httpsWopen) "1" else "0"
                fields["https_wport"] = config.httpsWport
                fields["sshd_wopen"] = if (config.sshdWopen) "1" else "0"
                fields["sshd_wport"] = config.sshdWport
                fields["sshd_wbfp"] = config.sshdWbfp
                fields["ftpd_wopen"] = if (config.ftpdWopen) "1" else "0"
                fields["ftpd_wport"] = config.ftpdWport
                fields["udpxy_wopen"] = if (config.udpxyWopen) "1" else "0"
                fields["udpxy_wport"] = config.udpxyWport
                fields["trmd_ropen"] = if (config.trmdRopen) "1" else "0"
                fields["aria_ropen"] = if (config.ariaRopen) "1" else "0"
            } else if (page.contains("Netfilter", ignoreCase = true)) {
                fields["wan_nat_x"] = if (config.wanNatEnabled) "1" else "0"
                fields["nf_max_conn"] = config.nfMaxConn
                fields["nf_nat_type"] = config.nfNatType
                fields["nf_nat_loop"] = if (config.nfNatLoopback) "1" else "0"
                fields["fw_pt_pppoe"] = if (config.fwPtPppoe) "1" else "0"
                fields["nf_alg_ftp0"] = config.nfAlgFtp0
                fields["nf_alg_ftp1"] = config.nfAlgFtp1
                fields["nf_alg_pptp"] = if (config.nfAlgPptp) "1" else "0"
                fields["nf_alg_rtsp"] = if (config.nfAlgRtsp) "1" else "0"
                fields["nf_alg_h323"] = if (config.nfAlgH323) "1" else "0"
                fields["nf_alg_sip"] = if (config.nfAlgSip) "1" else "0"
            } else if (page.contains("URLFilter", ignoreCase = true)) {
                fields["action_mode"] = " Restart "
                fields["group_id"] = "UrlList"
                fields["url_enable_x"] = if (config.urlFilterEnabled) "1" else "0"
                fields["url_date_x"] = config.urlFilterDate
                fields["url_time_x"] = config.urlFilterTime
                fields["url_mac_x"] = config.urlFilterMac
                fields["url_inv_x"] = if (config.urlFilterInvert) "1" else "0"
            } else if (page.contains("MACFilter", ignoreCase = true)) {
                fields["action_mode"] = if (config.macFilterMethod == "0") " Apply " else " Restart "
                fields["group_id"] = "MFList"
                fields["macfilter_enable_x"] = config.macFilterMethod
                fields["fw_mac_drop"] = if (config.fwMacDrop) "1" else "0"
            } else if (page.contains("Advanced_Firewall", ignoreCase = true) || page.contains("Firewall_Content", ignoreCase = true)) {
                fields["action_mode"] = " Restart "
                fields["group_id"] = "LWFilterList"
                fields["fw_lw_enable_x"] = if (config.fwLwEnabled) "1" else "0"
                fields["filter_lw_default_x"] = config.filterLwDefault
                fields["filter_lw_date_x"] = config.filterLwDate
                fields["filter_lw_time_x"] = config.filterLwTime
                fields["filter_lw_icmp_x"] = config.filterLwIcmp
            }

            val response = getService().applySettings(fields)
            if (response.isSuccessful) {
                cachedFirewall = config
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun getUsbShareConfig(page: String): Flow<Result<UsbShareConfig>> = flow {
        try {
            val response = getService().getPageContent(page)
            if (response.isSuccessful && response.body() != null) {
                val config = PadavanResponseParser.parseUsbShareConfig(response.body()!!, cachedUsbShare)
                cachedUsbShare = config
                emit(Result.success(config))
            } else {
                emit(Result.failure(IOException("HTTP Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun saveUsbShareConfig(page: String, config: UsbShareConfig): Boolean {
        return try {
            val fields = mutableMapOf<String, String>()
            fields["action_mode"] = " Apply "
            fields["current_page"] = "/$page"
            fields["next_page"] = ""

            if (page.contains("samba", ignoreCase = true) || page.contains("others", ignoreCase = true)) {
                fields["sid_list"] = "Storage;LANHostConfig;"
                fields["usb3_disable"] = if (config.usb3Disable) "1" else "0"
                fields["hdd_spindt"] = config.hddSpindown
                fields["hdd_apmoff"] = if (config.hddApmOff) "1" else "0"
                fields["achk_enable"] = if (config.achkEnable) "1" else "0"
                fields["pcache_reclaim"] = config.pcacheReclaim
                fields["optw_enable"] = config.optwEnable
                fields["st_max_user"] = config.stMaxUser

                fields["enable_samba"] = if (config.enableSamba) "1" else "0"
                fields["st_samba_workgroup"] = config.sambaWorkgroup
                fields["st_samba_mode"] = config.sambaMode
                fields["st_samba_lmb"] = config.sambaLmb
                fields["st_samba_fp"] = config.sambaFp
            } else if (page.contains("ftp", ignoreCase = true)) {
                fields["sid_list"] = "Storage;"
                fields["enable_ftp"] = if (config.enableFtp) "1" else "0"
                fields["st_ftp_mode"] = config.ftpMode
                fields["st_ftp_log"] = if (config.ftpLog) "1" else "0"
                fields["st_ftp_pmin"] = config.ftpPmin
                fields["st_ftp_pmax"] = config.ftpPmax
                fields["st_ftp_anmr"] = config.ftpAnmr
            } else if (page.contains("Printer", ignoreCase = true)) {
                fields["sid_list"] = "General;"
                fields["rawd_enable"] = config.rawdEnable
                fields["lprd_enable"] = if (config.lprdEnable) "1" else "0"
                fields["u2ec_enable"] = if (config.u2ecEnable) "1" else "0"
            } else if (page.contains("Modem", ignoreCase = true)) {
                fields["sid_list"] = "General;Layer3Forwarding;IPConnection;"
                fields["modem_rule"] = if (config.modemRule) "1" else "0"
                fields["modem_type"] = config.modemType
                fields["modem_country"] = config.modemCountry
                fields["modem_isp"] = config.modemIsp
                fields["modem_apn"] = config.modemApn
                fields["modem_pin"] = config.modemPin
                fields["modem_dialnum"] = config.modemDialnum
                fields["modem_user"] = config.modemUser
                fields["modem_pass"] = config.modemPass
                fields["modem_nets"] = config.modemNets
                fields["modem_mtu"] = config.modemMtu.toString()
                fields["modem_dnsa"] = if (config.modemDnsAuto) "1" else "0"
                fields["wan_dns1_x"] = config.wanDns1
                fields["wan_dns2_x"] = config.wanDns2
                fields["wan_dns3_x"] = config.wanDns3
                fields["modem_node"] = config.modemNode
                fields["modem_cmd"] = config.modemCmd
                fields["modem_zcd"] = config.modemZcd
            }

            val response = getService().applySettings(fields)
            if (response.isSuccessful) {
                cachedUsbShare = config
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun addServiceFilterRule(rule: ServiceFilterRule): Boolean {
        return try {
            val fields = mutableMapOf<String, String>()
            fields["action_mode"] = " Add "
            fields["group_id"] = "LWFilterList"
            fields["current_page"] = "/Advanced_Firewall_Content.asp"
            fields["next_page"] = ""
            fields["sid_list"] = "FirewallConfig;"
            fields["filter_lw_srcip_x_0"] = rule.srcIp
            fields["filter_lw_srcport_x_0"] = rule.srcPort
            fields["filter_lw_dstip_x_0"] = rule.dstIp
            fields["filter_lw_dstport_x_0"] = rule.dstPort
            fields["filter_lw_proto_x_0"] = rule.proto
            fields["filter_lw_protono_x_0"] = rule.protoNo

            getService().applySettings(fields).isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteServiceFilterRule(index: Int): Boolean {
        return try {
            val fields = mutableMapOf<String, String>()
            fields["action_mode"] = " Del "
            fields["group_id"] = "LWFilterList"
            fields["current_page"] = "/Advanced_Firewall_Content.asp"
            fields["next_page"] = ""
            fields["sid_list"] = "FirewallConfig;"
            fields["LWFilterList"] = ""
            fields["LWFilterList_s"] = index.toString()

            getService().applySettings(fields).isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun addUrlKeyword(keyword: String): Boolean {
        return try {
            val fields = mutableMapOf<String, String>()
            fields["action_mode"] = " Add "
            fields["group_id"] = "UrlList"
            fields["current_page"] = "/Advanced_URLFilter_Content.asp"
            fields["next_page"] = ""
            fields["sid_list"] = "FirewallConfig;"
            fields["url_keyword_x_0"] = keyword

            getService().applySettings(fields).isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteUrlKeyword(index: Int): Boolean {
        return try {
            val fields = mutableMapOf<String, String>()
            fields["action_mode"] = " Del "
            fields["group_id"] = "UrlList"
            fields["current_page"] = "/Advanced_URLFilter_Content.asp"
            fields["next_page"] = ""
            fields["sid_list"] = "FirewallConfig;"
            fields["UrlList"] = ""
            fields["UrlList_s"] = index.toString()

            getService().applySettings(fields).isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun addMacFilterRule(rule: MacFilterRule): Boolean {
        return try {
            val fields = mutableMapOf<String, String>()
            fields["action_mode"] = " Add "
            fields["group_id"] = "MFList"
            fields["current_page"] = "/Advanced_MACFilter_Content.asp"
            fields["next_page"] = ""
            fields["sid_list"] = "FirewallConfig;"
            fields["macfilter_list_x_0"] = rule.mac
            fields["macfilter_time_x_0"] = rule.time
            fields["macfilter_date_x_0"] = rule.date

            getService().applySettings(fields).isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteMacFilterRule(index: Int): Boolean {
        return try {
            val fields = mutableMapOf<String, String>()
            fields["action_mode"] = " Del "
            fields["group_id"] = "MFList"
            fields["current_page"] = "/Advanced_MACFilter_Content.asp"
            fields["next_page"] = ""
            fields["sid_list"] = "FirewallConfig;"
            fields["MFList"] = ""
            fields["MFList_s"] = index.toString()

            getService().applySettings(fields).isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override fun getAdminConfig(page: String): Flow<Result<AdminConfig>> = flow {
        try {
            val response = getService().getPageContent(page)
            if (response.isSuccessful && response.body() != null) {
                val config = PadavanResponseParser.parseAdminConfig(response.body()!!, cachedAdmin)
                cachedAdmin = config
                emit(Result.success(config))
            } else {
                emit(Result.failure(IOException("HTTP Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun saveAdminConfig(page: String, config: AdminConfig): Boolean {
        return try {
            val fields = mutableMapOf<String, String>()
            fields["action_mode"] = " Apply "
            fields["current_page"] = "/$page"
            fields["next_page"] = ""

            if (page.contains("System", ignoreCase = true)) {
                fields["sid_list"] = "LANHostConfig;General;Storage;"
                fields["computer_name"] = "Router"
                fields["http_username"] = config.adminUser
                if (config.adminPass.isNotEmpty()) {
                    fields["http_passwd"] = config.adminPass
                    fields["http_passwd2"] = config.adminPass
                    fields["v_password2"] = config.adminPass
                }
                fields["time_zone"] = config.timezone
                fields["ntp_server0"] = config.ntpServer1
                fields["ntp_server1"] = config.ntpServer2
            } else if (page.contains("Services", ignoreCase = true)) {
                fields["sid_list"] = "LANHostConfig;General;Storage;"
                fields["telnetd"] = if (config.enableTelnet) "1" else "0"
                fields["sshd_enable"] = if (config.enableSsh) (if (config.enableSftp) "1" else "2") else "0"
                fields["sshd_port"] = config.sshPort
                fields["sshd_sftp"] = if (config.enableSftp) "1" else "0"
                fields["webdav_enable"] = if (config.enableWebdav) "1" else "0"
            } else if (page.contains("OperationMode", ignoreCase = true) || page.contains("OpMode", ignoreCase = true)) {
                fields["action_mode"] = " Restart "
                fields["sid_list"] = "General;"
                fields["op_mode"] = config.opMode
            } else if (page.contains("Buttons", ignoreCase = true) || page.contains("Tweaks", ignoreCase = true)) {
                fields["sid_list"] = "General;"
                fields["btn_wps_mode"] = config.btnWpsMode
                fields["led_pwr_mode"] = config.ledPowerMode
            }

            val response = getService().applySettings(fields)
            if (response.isSuccessful) {
                cachedAdmin = config
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun getScriptConfig(page: String): Flow<Result<ScriptConfig>> = flow {
        try {
            val response = getService().getPageContent(page)
            if (response.isSuccessful && response.body() != null) {
                val config = PadavanResponseParser.parseScriptConfig(response.body()!!, cachedScript)
                cachedScript = config
                emit(Result.success(config))
            } else {
                emit(Result.failure(IOException("HTTP Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun saveScriptConfig(page: String, config: ScriptConfig): Boolean {
        return try {
            val fields = mutableMapOf<String, String>()
            fields["action_mode"] = " Apply "
            fields["current_page"] = "/$page"
            fields["next_page"] = ""

            if (page.contains("Scripts", ignoreCase = true)) {
                fields["sid_list"] = "General;"
                fields["scripts.start_script.sh"] = config.scriptStartup
                fields["scripts.post_wan_script.sh"] = config.scriptWanUp
                fields["scripts.post_wandn_script.sh"] = config.scriptWanDown
                fields["scripts.ez_buttons_script.sh"] = config.scriptWanDown
                fields["scripts.shutdown_script.sh"] = config.scriptShutdown
                fields["scripts.post_iptables_script.sh"] = config.scriptIpRules
            } else if (page.contains("InetDetect", ignoreCase = true) || page.contains("Detector", ignoreCase = true)) {
                fields["sid_list"] = "General;"
                fields["di_poll_mode"] = if (config.pingEnabled) "1" else "0"
                fields["di_addr0"] = config.pingHost1
                fields["di_addr1"] = config.pingHost2
                fields["di_time_done"] = config.pingPeriod.toString()
                fields["di_lost_action"] = config.pingAction
            }

            val response = getService().applySettings(fields)
            if (response.isSuccessful) {
                cachedScript = config
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun scanWifiNetworks(is5GHz: Boolean): Flow<Result<List<com.example.padavancontrol.data.models.WifiNetwork>>> = flow {
        try {
            val page = if (is5GHz) "apcli_scan.asp" else "apcli_scan2g.asp"
            val response = getService().getPageContent(page)
            if (response.isSuccessful && response.body() != null) {
                val networks = PadavanResponseParser.parseWifiScan(response.body()!!)
                emit(Result.success(networks))
            } else {
                emit(Result.failure(IOException("HTTP Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
}
