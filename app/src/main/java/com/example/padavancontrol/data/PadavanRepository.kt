package com.example.padavancontrol.data

import com.example.padavancontrol.data.models.LanClient
import com.example.padavancontrol.data.models.PortLink
import com.example.padavancontrol.data.models.SystemStatus
import com.example.padavancontrol.data.models.WanStatus
import com.example.padavancontrol.data.models.WirelessConfig
import com.example.padavancontrol.data.models.FirewallConfig
import com.example.padavancontrol.data.models.UsbShareConfig
import com.example.padavancontrol.data.models.FolderPermission
import com.example.padavancontrol.data.models.ShareNode
import com.example.padavancontrol.data.models.ServiceFilterRule
import com.example.padavancontrol.data.models.MacFilterRule
import com.example.padavancontrol.data.models.AdminConfig
import com.example.padavancontrol.data.models.ScriptConfig
import com.example.padavancontrol.network.PadavanApiService
import com.example.padavancontrol.network.PadavanResponseParser
import com.example.padavancontrol.network.RetrofitClient
import okhttp3.ResponseBody
import okhttp3.MultipartBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

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
    suspend fun blockClient(macAddress: String): Boolean
    suspend fun unblockClient(index: Int): Boolean
    fun getWirelessConfig(page: String, is5GHz: Boolean): Flow<Result<WirelessConfig>>
    suspend fun saveWirelessConfig(page: String, config: WirelessConfig, is5GHz: Boolean): Boolean
    fun getLanConfig(page: String): Flow<Result<com.example.padavancontrol.data.models.LanConfig>>
    suspend fun saveLanConfig(page: String, config: com.example.padavancontrol.data.models.LanConfig): Boolean
    fun getWanConfig(page: String, forceRefresh: Boolean = false): Flow<Result<com.example.padavancontrol.data.models.WanConfig>>
    suspend fun saveWanConfig(page: String, config: com.example.padavancontrol.data.models.WanConfig): Boolean
    suspend fun addStaticLease(mac: String, ip: String, name: String): Boolean
    suspend fun deleteStaticLease(index: Int): Boolean
    suspend fun addStaticRoute(dest: String, mask: String, gw: String, metric: Int, iface: String): Boolean
    suspend fun deleteStaticRoute(index: Int): Boolean
    suspend fun addPortForwardRule(rule: com.example.padavancontrol.data.models.PortForwardRule): Boolean
    suspend fun deletePortForwardRule(index: Int): Boolean
    suspend fun sendWolPacket(mac: String): Boolean
    fun getFirewallConfig(page: String): Flow<Result<FirewallConfig>>
    suspend fun saveFirewallConfig(page: String, config: FirewallConfig): Boolean
    fun getUsbShareConfig(page: String): Flow<Result<UsbShareConfig>>
    suspend fun saveUsbShareConfig(page: String, config: UsbShareConfig): Boolean
    fun getShareTree(layerOrder: String): Flow<Result<List<ShareNode>>>
    suspend fun createUsbAccount(account: String, pass: String): Boolean
    suspend fun deleteUsbAccount(account: String): Boolean
    suspend fun modifyUsbAccount(account: String, newAccount: String, newPass: String): Boolean
    suspend fun createUsbFolder(pool: String, folder: String): Boolean
    suspend fun deleteUsbFolder(pool: String, folder: String): Boolean
    suspend fun modifyUsbFolder(pool: String, folder: String, newFolder: String): Boolean
    suspend fun setUsbPermission(account: String, pool: String, folder: String, protocol: String, permission: String): Boolean
    suspend fun addServiceFilterRule(rule: ServiceFilterRule): Boolean
    suspend fun deleteServiceFilterRule(index: Int): Boolean
    suspend fun addUrlKeyword(keyword: String): Boolean
    suspend fun deleteUrlKeyword(index: Int): Boolean
    suspend fun addMacFilterRule(rule: MacFilterRule): Boolean
    suspend fun deleteMacFilterRule(index: Int): Boolean
    fun getRouterIp(): String
    suspend fun commitFlash(action: String): Boolean
    suspend fun restoreNvramDefaults(): Boolean
    suspend fun restoreStorageDefaults(): Boolean
    suspend fun downloadSettingsBackup(model: String): okhttp3.ResponseBody?
    suspend fun downloadStorageBackup(model: String): okhttp3.ResponseBody?
    suspend fun uploadSettingsBackup(file: okhttp3.MultipartBody.Part): Boolean
    suspend fun uploadStorageBackup(file: okhttp3.MultipartBody.Part): Boolean
    suspend fun uploadFirmwareUpgrade(file: okhttp3.MultipartBody.Part): Boolean
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

    private val loadedWanPages = Collections.synchronizedSet(mutableSetOf<String>())
    private val activeFetches = ConcurrentHashMap<String, Deferred<com.example.padavancontrol.data.models.WanConfig?>>()
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val wanPages = listOf(
        "Advanced_WAN_Content.asp",
        "Advanced_IPv6_Content.asp",
        "Advanced_VirtualServer_Content.asp",
        "Advanced_Exposed_Content.asp",
        "Advanced_DDNS_Content.asp"
    )

    private fun clearAllCaches() {
        cachedWireless2g = WirelessConfig()
        cachedWireless5g = WirelessConfig()
        cachedLan = com.example.padavancontrol.data.models.LanConfig()
        cachedWan = com.example.padavancontrol.data.models.WanConfig()
        cachedFirewall = FirewallConfig()
        cachedUsbShare = UsbShareConfig()
        cachedAdmin = AdminConfig()
        cachedScript = ScriptConfig()
        loadedWanPages.clear()
        activeFetches.forEach { (_, deferred) -> deferred.cancel() }
        activeFetches.clear()
    }

    private var prevCpuTotal = 0L
    private var prevCpuBusy = 0L
    private var prevCpuUserTicks = 0L
    private var prevCpuSysTicks = 0L
    private var prevCpuNiceTicks = 0L
    private var prevCpuIdleTicks = 0L
    private var prevCpuIrqTicks = 0L
    private var prevCpuSirqTicks = 0L

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
                val bodyStr = response.body() ?: ""
                if (bodyStr.contains("You cannot Login unless logout another user first", ignoreCase = true)) {
                    throw IllegalStateException("Another device is currently logged in. Please log out from the other device first.")
                }
                
                // Parse and save hardware model product_id
                val hardwareModel = PadavanResponseParser.parseHardwareModel(bodyStr) ?: "UNKNOWN"
                credentialStore.saveHardwareModel(hardwareModel)

                credentialStore.saveCredentials(
                    user,
                    ip,
                    pass,
                    true,
                    credentialStore.isUseBiometric()
                )
                clearAllCaches()
                return true
            }
        } catch (e: Exception) {
            if (e is IllegalStateException) {
                throw e // Re-throw to be caught by ViewModel
            }
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
                    val deltaUser = status.cpuUserTicks - prevCpuUserTicks
                    val deltaSys = status.cpuSysTicks - prevCpuSysTicks
                    val deltaNice = status.cpuNiceTicks - prevCpuNiceTicks
                    val deltaIdle = status.cpuIdleTicks - prevCpuIdleTicks
                    val deltaIrq = status.cpuIrqTicks - prevCpuIrqTicks
                    val deltaSirq = status.cpuSirqTicks - prevCpuSirqTicks

                    fun calcPercent(deltaVal: Long): Int {
                        return if (prevCpuTotal > 0L && deltaTotal > 0L) {
                            (deltaVal * 100 / deltaTotal).coerceIn(0, 100).toInt()
                        } else {
                            0
                        }
                    }

                    val finalCpuUsage = calcPercent(deltaBusy)
                    val userPercent = calcPercent(deltaUser)
                    val sysPercent = calcPercent(deltaSys)
                    val nicePercent = calcPercent(deltaNice)
                    val idlePercent = if (prevCpuTotal > 0L && deltaTotal > 0L) {
                        calcPercent(deltaIdle)
                    } else {
                        100
                    }
                    val irqPercent = calcPercent(deltaIrq)
                    val sirqPercent = calcPercent(deltaSirq)

                    prevCpuTotal = status.cpuTotal
                    prevCpuBusy = status.cpuBusy
                    prevCpuUserTicks = status.cpuUserTicks
                    prevCpuSysTicks = status.cpuSysTicks
                    prevCpuNiceTicks = status.cpuNiceTicks
                    prevCpuIdleTicks = status.cpuIdleTicks
                    prevCpuIrqTicks = status.cpuIrqTicks
                    prevCpuSirqTicks = status.cpuSirqTicks

                    val updatedStatus = status.copy(
                        cpuUsage = finalCpuUsage,
                        cpuBusyPercent = finalCpuUsage,
                        cpuUserPercent = userPercent,
                        cpuSysPercent = sysPercent,
                        cpuNicePercent = nicePercent,
                        cpuIdlePercent = idlePercent,
                        cpuIrqPercent = irqPercent,
                        cpuSirqPercent = sirqPercent
                    )
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
            coroutineScope {
                val clientsDeferred = async { getService().getLanClients() }
                val w2gDeferred = async {
                    try { getService().getPageContent("Main_WStatus2g_Content.asp") } catch (e: Exception) { null }
                }
                val w5gDeferred = async {
                    try { getService().getPageContent("Main_WStatus_Content.asp") } catch (e: Exception) { null }
                }

                val clientsResponse = clientsDeferred.await()
                val w2gResponse = w2gDeferred.await()
                val w5gResponse = w5gDeferred.await()

                if (clientsResponse.isSuccessful && clientsResponse.body() != null) {
                    val clientsHtml = clientsResponse.body()!!
                    val w2gHtml = w2gResponse?.body() ?: ""
                    val w5gHtml = w5gResponse?.body() ?: ""
                    val clients = PadavanResponseParser.parseLanClients(clientsHtml, w2gHtml, w5gHtml)
                    emit(Result.success(clients))
                } else {
                    emit(Result.failure(IOException("HTTP Error: ${clientsResponse.code()}")))
                }
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

    override fun getRouterIp(): String = credentialStore.getRouterIp()

    override suspend fun commitFlash(): Boolean = commitFlash("commit_nvram")

    override suspend fun commitFlash(action: String): Boolean {
        return try {
            getService().commitFlash(nvramAction = action).isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun restoreNvramDefaults(): Boolean {
        return try {
            val fields = mapOf(
                "action_mode" to " RestoreNVRAM ",
                "current_page" to "Advanced_SettingBackup_Content.asp",
                "next_page" to "Advanced_SettingBackup_Content.asp"
            )
            getService().applySettings(fields).isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun restoreStorageDefaults(): Boolean {
        return try {
            val fields = mapOf(
                "action_mode" to " RestoreStorage ",
                "current_page" to "Advanced_SettingBackup_Content.asp",
                "next_page" to "Advanced_SettingBackup_Content.asp"
            )
            getService().applySettings(fields).isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun downloadSettingsBackup(model: String): ResponseBody? {
        return try {
            val response = getService().downloadSettings(model)
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun downloadStorageBackup(model: String): ResponseBody? {
        return try {
            val response = getService().downloadStorage(model)
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun uploadSettingsBackup(file: MultipartBody.Part): Boolean {
        return try {
            getService().uploadSettingsBackup(file).isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun uploadStorageBackup(file: MultipartBody.Part): Boolean {
        return try {
            getService().uploadStorageBackup(file).isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun uploadFirmwareUpgrade(file: MultipartBody.Part): Boolean {
        return try {
            getService().uploadFirmwareUpgrade(file).isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun blockClient(macAddress: String): Boolean {
        val fields = mapOf(
            "current_page" to "/device-map/clients.asp",
            "next_page" to "/device-map/clients.asp",
            "modified" to "0",
            "action_mode" to " Add ",
            "action_wait" to "",
            "action_script" to "",
            "macfilter_enable_x" to "1",
            "macfilter_list_x_0" to macAddress,
            "macfilter_time_x_0" to "00002359",
            "macfilter_date_x_0" to "1111111"
        )
        return try {
            getService().applySettings(fields).isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun unblockClient(index: Int): Boolean {
        val fields = mapOf(
            "current_page" to "/device-map/clients.asp",
            "next_page" to "/device-map/clients.asp",
            "modified" to "0",
            "action_mode" to " Del ",
            "action_wait" to "",
            "action_script" to "",
            "macfilter_enable_x" to "1",
            "MFList_s" to index.toString()
        )
        return try {
            getService().applySettings(fields).isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
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
            fields["current_page"] = page
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
                fields["${prefix}guest_date_x"] = config.guestDate
                fields["${prefix}guest_time_x"] = config.guestTimeWorkweek
                fields["${prefix}guest_time2_x"] = config.guestTimeWeekend
                fields["${prefix}guest_mcs_mode"] = config.guestMcsMode
                fields["${prefix}guest_macrule"] = if (config.guestMacRule) "1" else "0"
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
            } else if (page.contains("ACL", ignoreCase = true) || page.contains("MACFilter", ignoreCase = true)) {
                fields["${prefix}macmode"] = config.macFilterMode
                val macListStr = config.macFilterRules.joinToString("&#62") { "<${it.mac}<${it.desc}" }
                fields["${prefix}maclist_x"] = macListStr
            } else if (page.contains("WSecurity", ignoreCase = true) || page.contains("RADIUS", ignoreCase = true)) {
                fields["${prefix}radius_ipaddr"] = config.radiusIp
                fields["${prefix}radius_port"] = config.radiusPort.toString()
                fields["${prefix}radius_key"] = config.radiusKey
            } else if (page.contains("WAdvanced", ignoreCase = true) || page.contains("Professional", ignoreCase = true)) {
                fields["${prefix}IGMPSnoop"] = if (config.igmpSnooping) "1" else "0"
                fields["${prefix}wme"] = if (config.wmmCapable) "1" else "0"
                fields["${prefix}ap_isolate"] = if (config.apIsolate) "1" else "0"
                fields["${prefix}bcn"] = config.bcnInterval.toString()
                fields["${prefix}dtim"] = config.dtimInterval.toString()
                fields["${prefix}frag"] = config.fragThresh.toString()
                fields["${prefix}rts"] = config.rtsThresh.toString()
                fields["${prefix}TxBurst"] = if (config.txBurst) "1" else "0"
                fields["${prefix}GreenAP"] = if (config.greenAp) "1" else "0"
                fields["${prefix}TxPower"] = config.txPower.toString()
                fields["${prefix}mcs_mode"] = config.mcsMode
                fields["${prefix}KickStaRssiLow"] = config.kickStaRssiLow.toString()
                fields["${prefix}AssocReqRssiThres"] = config.assocReqRssiThres.toString()
                fields["${prefix}country_code"] = config.countryCode
                fields["${prefix}stream_tx"] = config.streamTx
                fields["${prefix}stream_rx"] = config.streamRx
                fields["${prefix}preamble"] = config.preamble
                fields["${prefix}PktAggregate"] = if (config.pktAggregate) "1" else "0"
                fields["${prefix}HT_RDG"] = if (config.htRdg) "1" else "0"
                fields["${prefix}HT_AutoBA"] = if (config.htAutoBA) "1" else "0"
                fields["${prefix}HT_AMSDU"] = if (config.htAmsdu) "1" else "0"
                fields["${prefix}APSDCapable"] = if (config.wmmApsd) "1" else "0"
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
                fields["${prefix}radio_date_x"] = config.radioDate
                fields["${prefix}radio_time_x"] = config.radioTimeWorkweek
                fields["${prefix}radio_time2_x"] = config.radioTimeWeekend
                fields["${prefix}HT_EXTCHA"] = config.extChannel
                fields["${prefix}wpa_gtk_rekey"] = config.wpaGtkRekey.toString()
                fields["${prefix}mcs_mode"] = config.mcsMode
                fields["${prefix}KickStaRssiLow"] = config.kickStaRssiLow.toString()
                fields["${prefix}AssocReqRssiThres"] = config.assocReqRssiThres.toString()
                fields["${prefix}country_code"] = config.countryCode
                fields["${prefix}TxPower"] = config.txPower.toString()
            }

            val response = applyAndCommitSettings(fields)
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
            fields["current_page"] = page
            fields["next_page"] = ""
            
            if (page.contains("LAN", ignoreCase = true)) {
                fields["action_mode"] = " Apply "
                fields["sid_list"] = "LANHostConfig;"
                fields["lan_ipaddr"] = config.lanIpAddr
                fields["lan_netmask"] = config.lanNetmask
                fields["lan_stp"] = if (config.lanStp) "1" else "0"
                fields["dhcp_start"] = config.dhcpStart
                fields["dhcp_end"] = config.dhcpEnd
            } else if (page.contains("DHCP", ignoreCase = true)) {
                fields["action_mode"] = " Apply "
                fields["sid_list"] = "LANHostConfig;"
                fields["next_page"] = "Advanced_GWStaticRoute_Content.asp"
                fields["group_id"] = "ManualDHCPList"
                fields["lan_ipaddr"] = config.lanIpAddr
                fields["lan_netmask"] = config.lanNetmask
                fields["dhcp_enable_x"] = if (config.dhcpEnabled) "1" else "0"
                fields["lan_domain"] = config.dhcpDomain
                fields["dhcp_start"] = config.dhcpStart
                fields["dhcp_end"] = config.dhcpEnd
                fields["dhcp_lease"] = config.dhcpLease.toString()
                fields["dhcp_gateway_x"] = config.dhcpGateway
                fields["dhcp_dns1_x"] = config.dhcpDns1
                fields["dhcp_dns2_x"] = config.dhcpDns2
                fields["dhcp_dns3_x"] = config.dhcpDns3
                fields["dhcp_dnsv6_x"] = config.dhcpDnsv6
                fields["dhcp_wins_x"] = config.dhcpWins
                fields["dhcp_verbose"] = config.dhcpVerbose.toString()
                fields["dnsmasq.dnsmasq.conf"] = config.dnsmasqDnsmasqConf
                fields["dnsmasq.dhcp.conf"] = config.dnsmasqDhcpConf
                fields["dnsmasq.hosts"] = config.dnsmasqHosts
                fields["dhcp_static_x"] = if (config.dhcpStaticEnabled) "1" else "0"
                fields["dhcp_static_arp"] = if (config.dhcpStaticArp) "1" else "0"
                fields["dhcp_staticnum_x_0"] = "0"
            } else if (page.contains("IPTV", ignoreCase = true)) {
                fields["action_mode"] = " Apply "
                fields["sid_list"] = "RouterConfig;LANHostConfig;WLANConfig11a;WLANConfig11b;"
                fields["mr_enable_x"] = if (config.mrEnable) "1" else "0"
                fields["force_igmp"] = config.forceIgmp.toString()
                fields["udpxy_enable_x"] = config.udpxyPort.toString()
                fields["xupnpd_enable_x"] = config.xupnpdPort.toString()
                fields["xupnpd_udpxy"] = if (config.xupnpdUdpxy) "1" else "0"
                fields["ether_igmp"] = if (config.igmpSnoop) "1" else "0"
                fields["ether_m2u"] = config.etherM2u.toString()
                fields["rt_IgmpSnEnable"] = config.rtIgmpSnEnable.toString()
                fields["wl_IgmpSnEnable"] = config.wlIgmpSnEnable.toString()
                fields["controlrate_broadcast"] = config.controlrateBroadcast.toString()
            } else if (page.contains("Route", ignoreCase = true)) {
                fields["action_mode"] = if (config.routeEnabled) " Restart " else " Apply "
                fields["sid_list"] = "RouterConfig;"
                fields["group_id"] = "GWStatic"
                fields["dr_enable_x"] = if (config.useDhcpRoutes) "1" else "0"
                fields["sr_enable_x"] = if (config.routeEnabled) "1" else "0"
                fields["sr_num_x_0"] = "0"
            } else if (page.contains("Switch", ignoreCase = true)) {
                fields["action_mode"] = " Apply "
                fields["sid_list"] = "LANHostConfig;"
                fields["ether_green"] = if (config.greenEthernet) "1" else "0"
                fields["ether_eee"] = if (config.eeeEnabled) "1" else "0"
                fields["ether_jumbo"] = config.etherJumbo.toString()
                
                config.portsConfig.forEach { portConfig ->
                    val suffix = portConfig.portName.lowercase()
                    fields["ether_flow_$suffix"] = portConfig.flowControl.toString()
                    fields["ether_link_$suffix"] = portConfig.speedDuplex.toString()
                }
            }
            
            val response = applyAndCommitSettings(fields)
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
            fields["current_page"] = "Advanced_DHCP_Content.asp"
            fields["next_page"] = ""
            fields["sid_list"] = "LANHostConfig;"
            fields["dhcp_staticmac_x_0"] = mac
            fields["dhcp_staticip_x_0"] = ip
            fields["dhcp_staticname_x_0"] = name
            
            val response = applyAndCommitSettings(fields)
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
            fields["current_page"] = "Advanced_DHCP_Content.asp"
            fields["next_page"] = ""
            fields["sid_list"] = "LANHostConfig;"
            fields["ManualDHCPList"] = ""
            fields["ManualDHCPList_s"] = index.toString()
            
            val response = applyAndCommitSettings(fields)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun addStaticRoute(dest: String, mask: String, gw: String, metric: Int, iface: String): Boolean {
        return try {
            val fields = mutableMapOf<String, String>()
            fields["action_mode"] = " Add "
            fields["current_page"] = "Advanced_GWStaticRoute_Content.asp"
            fields["next_page"] = ""
            fields["sid_list"] = "RouterConfig;"
            fields["group_id"] = "GWStatic"
            fields["sr_ipaddr_x_0"] = dest
            fields["sr_netmask_x_0"] = mask
            fields["sr_gateway_x_0"] = gw
            fields["sr_matric_x_0"] = metric.toString()
            fields["sr_if_x_0"] = iface
            
            val response = getService().applySettings(fields)
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun deleteStaticRoute(index: Int): Boolean {
        return try {
            val fields = mutableMapOf<String, String>()
            fields["action_mode"] = " Del "
            fields["current_page"] = "Advanced_GWStaticRoute_Content.asp"
            fields["next_page"] = ""
            fields["sid_list"] = "RouterConfig;"
            fields["group_id"] = "GWStatic"
            fields["GWStatic"] = ""
            fields["GWStatic_s"] = index.toString()
            
            val response = getService().applySettings(fields)
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun getOrFetchPage(page: String, forceRefresh: Boolean): com.example.padavancontrol.data.models.WanConfig? {
        if (!forceRefresh && loadedWanPages.contains(page)) {
            return cachedWan
        }

        var deferred = activeFetches[page]
        if (deferred == null || forceRefresh) {
            if (forceRefresh) {
                deferred?.cancel()
            }

            val newDeferred = repositoryScope.async {
                try {
                    val response = getService().getPageContent(page)
                    if (response.isSuccessful && response.body() != null) {
                        synchronized(this@DefaultPadavanRepository) {
                            val config = PadavanResponseParser.parseWanConfig(response.body()!!, cachedWan)
                            cachedWan = config
                            loadedWanPages.add(page)
                            config
                        }
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                } finally {
                    activeFetches.remove(page)
                }
            }
            activeFetches[page] = newDeferred
            deferred = newDeferred
        }

        return try {
            deferred.await()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun triggerBackgroundPreFetch(currentPage: String, forceRefresh: Boolean) {
        repositoryScope.launch {
            val remainingPages = wanPages.filter { it != currentPage && (forceRefresh || !loadedWanPages.contains(it)) }
            remainingPages.forEach { page ->
                launch {
                    getOrFetchPage(page, forceRefresh = false)
                }
            }
        }
    }

    override fun getWanConfig(page: String, forceRefresh: Boolean): Flow<Result<com.example.padavancontrol.data.models.WanConfig>> = flow {
        if (!forceRefresh && loadedWanPages.contains(page)) {
            emit(Result.success(cachedWan.copy(loadProgress = 1.0f, loadStatus = "Loaded from cache")))
            repositoryScope.launch {
                getOrFetchPage(page, forceRefresh = true)
            }
            return@flow
        }

        emit(Result.success(cachedWan.copy(loadProgress = 0.1f, loadStatus = "Connecting to router gateway...")))

        emit(Result.success(cachedWan.copy(loadProgress = 0.2f, loadStatus = "Fetching active page settings...")))
        val config = getOrFetchPage(page, forceRefresh)
        if (config == null) {
            emit(Result.failure(IOException("Failed to fetch WAN configuration for $page")))
            return@flow
        }

        emit(Result.success(cachedWan.copy(loadProgress = 0.4f, loadStatus = "Active page loaded. Fetching remaining settings...")))

        val remainingPages = wanPages.filter { it != page }
        coroutineScope {
            val channel = Channel<Pair<String, Boolean>>()
            remainingPages.forEach { otherPage ->
                launch {
                    val success = getOrFetchPage(otherPage, forceRefresh) != null
                    channel.send(Pair(otherPage, success))
                }
            }

            repeat(remainingPages.size) { index ->
                val (completedPage, success) = channel.receive()
                val count = index + 1
                val nextProgress = 0.4f + (count * 0.15f)
                val progressVal = if (nextProgress > 1.0f) 1.0f else nextProgress
                val statusText = when (completedPage) {
                    "Advanced_WAN_Content.asp" -> "Internet Connection loaded..."
                    "Advanced_IPv6_Content.asp" -> "IPv6 Protocol settings loaded..."
                    "Advanced_VirtualServer_Content.asp" -> "Port Forwarding rules loaded..."
                    "Advanced_Exposed_Content.asp" -> "DMZ Host settings loaded..."
                    "Advanced_DDNS_Content.asp" -> "DDNS profile settings loaded..."
                    else -> "Settings page loaded..."
                }
                emit(Result.success(cachedWan.copy(loadProgress = progressVal, loadStatus = statusText)))
            }
            channel.close()
        }

        emit(Result.success(cachedWan.copy(loadProgress = 1.0f, loadStatus = "WAN Configuration successfully merged.")))
    }.flowOn(Dispatchers.IO)

    override suspend fun saveWanConfig(page: String, config: com.example.padavancontrol.data.models.WanConfig): Boolean {
        return try {
            val fields = mutableMapOf<String, String>()
            fields["action_mode"] = " Apply "
            fields["current_page"] = page
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
                fields["hw_nat_mode"] = config.hwNatMode
                fields["sfe_enable"] = config.sfeEnable
                fields["gw_arp_ping"] = if (config.gwArpPing) "1" else "0"
                fields["wan_auth_mode"] = config.wanAuthMode
                fields["wan_hostname"] = config.wanHostname
                fields["wan_vci"] = config.wanVci
                fields["wan_hwaddr_x"] = config.wanHwaddr
                fields["wan_ttl_fix"] = if (config.wanTtlFix) "1" else "0"
                fields["wan_ttl_value"] = config.wanTtlValue
                fields["wan_stb_x"] = config.wanStbPort
                fields["wan_stb_iso"] = config.wanStbIso
                fields["vlan_filter"] = if (config.vlanFilter) "1" else "0"
                fields["vlan_vid_cpu"] = config.vlanVidCpu
                fields["vlan_pri_cpu"] = config.vlanPriCpu
                fields["vlan_vid_iptv"] = config.vlanVidIptv
                fields["vlan_pri_iptv"] = config.vlanPriIptv
                
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
                fields["upnp_proto"] = config.upnpProto
                fields["upnp_secure"] = if (config.upnpSecure) "1" else "0"
                fields["upnp_eport_min"] = config.upnpEportMin
                fields["upnp_eport_max"] = config.upnpEportMax
                fields["upnp_iport_min"] = config.upnpIportMin
                fields["upnp_iport_max"] = config.upnpIportMax
                fields["upnp_clean_int"] = config.upnpCleanInt
                fields["upnp_clean_min"] = config.upnpCleanMin
                fields["vts_enable_x"] = if (config.portForwardEnabled) "1" else "0"
            } else if (page.contains("Exposed", ignoreCase = true)) {
                fields["sid_list"] = "IPConnection;PPPConnection;"
                fields["dmz_ip"] = config.dmzIp
                fields["sp_battle_ips"] = if (config.dmzSpBattle) "1" else "0"
            } else if (page.contains("DDNS", ignoreCase = true)) {
                fields["sid_list"] = "LANHostConfig;"
                fields["ddns_enable_x"] = if (config.ddnsEnabled) "1" else "0"
                fields["ddns_server_x"] = config.ddnsServer
                fields["ddns_username_x"] = config.ddnsUser
                fields["ddns_passwd_x"] = config.ddnsPass
                fields["ddns_hostname_x"] = config.ddnsHostName
                fields["ddns_hostname2_x"] = config.ddnsHostName2
                fields["ddns_hostname3_x"] = config.ddnsHostName3
                fields["ddns_ssl"] = config.ddnsSsl
                fields["ddns_wildcard_x"] = config.ddnsWildcard
                fields["ddns2_server"] = config.ddns2Server
                fields["ddns2_hname"] = config.ddns2HostName
                fields["ddns2_user"] = config.ddns2User
                fields["ddns2_pass"] = config.ddns2Pass
                fields["ddns2_ssl"] = config.ddns2Ssl
                fields["ddns2_wildcard_x"] = config.ddns2Wildcard
                fields["ddns_source"] = config.ddnsSource
                fields["ddns_checkip"] = config.ddnsCheckIp
                fields["ddns2_checkip"] = config.ddns2CheckIp
                fields["ddns_period"] = config.ddnsPeriod
                fields["ddns_forced"] = config.ddnsForced
                fields["ddns_ipv6"] = config.ddnsIpv6
                fields["ddns_verbose"] = config.ddnsVerbose
            } else if (page.contains("IPv6", ignoreCase = true)) {
                fields["sid_list"] = "IP6Connection;"
                
                // Service Type
                fields["ip6_service"] = config.ipv6Proto
                fields["ipv6_service"] = config.ipv6Proto
                
                fields["ip6_wan_dhcp"] = config.ipv6WanDhcp
                fields["ip6_dns_auto"] = if (config.ipv6DnsAuto) "1" else "0"
                
                // DNS
                fields["ip6_dns1"] = config.ipv6Dns1
                fields["ipv6_dns1_x"] = config.ipv6Dns1
                fields["ip6_dns2"] = config.ipv6Dns2
                fields["ipv6_dns2_x"] = config.ipv6Dns2
                fields["ip6_dns3"] = config.ipv6Dns3
                fields["ipv6_dns3_x"] = config.ipv6Dns3
                
                // MTU & Tunnel Settings
                fields["ip6_sit_mtu"] = config.ipv6Mtu.toString()
                fields["ipv6_tun_mtu"] = config.ipv6Mtu.toString()
                
                fields["ip6_6to4_relay"] = config.ipv66to4Relay
                fields["ipv6_relay"] = config.ipv66to4Relay
                
                if (config.ipv6Proto == "6to4") {
                    fields["ip6_sit_mtu"] = config.ipv66to4Mtu.toString()
                    fields["ipv6_tun_v4mtu"] = config.ipv66to4Mtu.toString()
                }
                
                // 6rd settings
                fields["ip6_wan_addr"] = config.ipv66rdPrefix
                fields["ipv6_6rd_prefix"] = config.ipv66rdPrefix
                fields["ip6_wan_size"] = config.ipv66rdPrefixLen.toString()
                fields["ipv6_6rd_prefixlen"] = config.ipv66rdPrefixLen.toString()
                fields["ip6_6rd_relay"] = config.ipv66rdRouter
                fields["ipv6_6rd_router"] = config.ipv66rdRouter
                fields["ip6_6rd_size"] = config.ipv66rdIp4Mtu.toString()
                fields["ipv6_6rd_ip4size"] = config.ipv66rdIp4Mtu.toString()
                
                // LAN settings
                fields["ip6_lan_auto"] = if (config.ipv6LanAuto) "1" else "0"
                fields["ip6_lan_radv"] = if (config.ipv6LanRadv) "1" else "0"
                fields["ip6_lan_dhcp"] = config.ipv6LanDhcp
                fields["ip6_lan_sfps"] = config.ipv6LanSfps
                fields["ip6_lan_sfpe"] = config.ipv6LanSfpe
                fields["ip6_lan_sflt"] = config.ipv6LanSflt
            }
            
            val response = applyAndCommitSettings(fields)
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
            fields["current_page"] = "Advanced_VirtualServer_Content.asp"
            fields["next_page"] = ""
            fields["sid_list"] = "IPConnection;"
            fields["vts_name_x_0"] = rule.name
            fields["vts_port_x_0"] = rule.extPort
            fields["vts_ipaddr_x_0"] = rule.intIp
            fields["vts_lport_x_0"] = rule.intPort
            fields["vts_proto_x_0"] = rule.protocol
            fields["vts_desc_x_0"] = rule.desc
            
            val response = applyAndCommitSettings(fields)
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
            fields["current_page"] = "Advanced_VirtualServer_Content.asp"
            fields["next_page"] = ""
            fields["sid_list"] = "IPConnection;"
            fields["VSList"] = ""
            fields["VSList_s"] = index.toString()
            
            val response = applyAndCommitSettings(fields)
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
            fields["current_page"] = page
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

            val response = applyAndCommitSettings(fields)
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
            fields["current_page"] = page
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

                // Torrent Transmission
                fields["trmd_enable"] = if (config.trmdEnable) "1" else "0"
                fields["trmd_pport"] = config.trmdPport
                fields["trmd_rport"] = config.trmdRport

                // Download manager Aria2
                fields["aria_enable"] = if (config.ariaEnable) "1" else "0"
                fields["aria_pport"] = config.ariaPport
                fields["aria_rport"] = config.ariaRport
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

            val response = applyAndCommitSettings(fields)
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

    override fun getShareTree(layerOrder: String): Flow<Result<List<ShareNode>>> = flow {
        try {
            val fields = mapOf(
                "layer_order" to layerOrder,
                "motion" to "gettree"
            )
            val response = getService().postGeneric("aidisk/getsharearray.asp", fields)
            if (response.isSuccessful && response.body() != null) {
                val nodes = PadavanResponseParser.parseShareTreeList(response.body()!!)
                emit(Result.success(nodes))
            } else {
                emit(Result.failure(IOException("HTTP Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun createUsbAccount(account: String, pass: String): Boolean {
        return try {
            val fields = mapOf(
                "account" to account,
                "password" to pass,
                "confirm_password" to pass
            )
            getService().postGeneric("aidisk/create_account.asp", fields).isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun deleteUsbAccount(account: String): Boolean {
        return try {
            val fields = mapOf(
                "account" to account
            )
            getService().postGeneric("aidisk/delete_account.asp", fields).isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun modifyUsbAccount(account: String, newAccount: String, newPass: String): Boolean {
        return try {
            val fields = mapOf(
                "account" to account,
                "new_account" to newAccount,
                "new_password" to newPass,
                "confirm_password" to newPass
            )
            getService().postGeneric("aidisk/modify_account.asp", fields).isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun createUsbFolder(pool: String, folder: String): Boolean {
        return try {
            val fields = mapOf(
                "pool" to pool,
                "folder" to folder
            )
            getService().postGeneric("aidisk/create_sharedfolder.asp", fields).isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun deleteUsbFolder(pool: String, folder: String): Boolean {
        return try {
            val fields = mapOf(
                "pool" to pool,
                "folder" to folder
            )
            getService().postGeneric("aidisk/delete_sharedfolder.asp", fields).isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun modifyUsbFolder(pool: String, folder: String, newFolder: String): Boolean {
        return try {
            val fields = mapOf(
                "pool" to pool,
                "folder" to folder,
                "new_folder" to newFolder
            )
            getService().postGeneric("aidisk/modify_sharedfolder.asp", fields).isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun setUsbPermission(
        account: String,
        pool: String,
        folder: String,
        protocol: String,
        permission: String
    ): Boolean {
        return try {
            val fields = mapOf(
                "account" to account,
                "pool" to pool,
                "folder" to folder,
                "protocol" to protocol,
                "permission" to permission
            )
            getService().postGeneric("aidisk/set_account_permission.asp", fields).isSuccessful
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
            fields["current_page"] = "Advanced_Firewall_Content.asp"
            fields["next_page"] = ""
            fields["sid_list"] = "FirewallConfig;"
            fields["filter_lw_srcip_x_0"] = rule.srcIp
            fields["filter_lw_srcport_x_0"] = rule.srcPort
            fields["filter_lw_dstip_x_0"] = rule.dstIp
            fields["filter_lw_dstport_x_0"] = rule.dstPort
            fields["filter_lw_proto_x_0"] = rule.proto
            fields["filter_lw_protono_x_0"] = rule.protoNo

            applyAndCommitSettings(fields).isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteServiceFilterRule(index: Int): Boolean {
        return try {
            val fields = mutableMapOf<String, String>()
            fields["action_mode"] = " Del "
            fields["group_id"] = "LWFilterList"
            fields["current_page"] = "Advanced_Firewall_Content.asp"
            fields["next_page"] = ""
            fields["sid_list"] = "FirewallConfig;"
            fields["LWFilterList"] = ""
            fields["LWFilterList_s"] = index.toString()

            applyAndCommitSettings(fields).isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun addUrlKeyword(keyword: String): Boolean {
        return try {
            val fields = mutableMapOf<String, String>()
            fields["action_mode"] = " Add "
            fields["group_id"] = "UrlList"
            fields["current_page"] = "Advanced_URLFilter_Content.asp"
            fields["next_page"] = ""
            fields["sid_list"] = "FirewallConfig;"
            fields["url_keyword_x_0"] = keyword

            applyAndCommitSettings(fields).isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteUrlKeyword(index: Int): Boolean {
        return try {
            val fields = mutableMapOf<String, String>()
            fields["action_mode"] = " Del "
            fields["group_id"] = "UrlList"
            fields["current_page"] = "Advanced_URLFilter_Content.asp"
            fields["next_page"] = ""
            fields["sid_list"] = "FirewallConfig;"
            fields["UrlList"] = ""
            fields["UrlList_s"] = index.toString()

            applyAndCommitSettings(fields).isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun addMacFilterRule(rule: MacFilterRule): Boolean {
        return try {
            val fields = mutableMapOf<String, String>()
            fields["action_mode"] = " Add "
            fields["group_id"] = "MFList"
            fields["current_page"] = "Advanced_MACFilter_Content.asp"
            fields["next_page"] = ""
            fields["sid_list"] = "FirewallConfig;"
            fields["macfilter_list_x_0"] = rule.mac
            fields["macfilter_time_x_0"] = rule.time
            fields["macfilter_date_x_0"] = rule.date

            applyAndCommitSettings(fields).isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteMacFilterRule(index: Int): Boolean {
        return try {
            val fields = mutableMapOf<String, String>()
            fields["action_mode"] = " Del "
            fields["group_id"] = "MFList"
            fields["current_page"] = "Advanced_MACFilter_Content.asp"
            fields["next_page"] = ""
            fields["sid_list"] = "FirewallConfig;"
            fields["MFList"] = ""
            fields["MFList_s"] = index.toString()

            applyAndCommitSettings(fields).isSuccessful
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
            fields["current_page"] = if (!page.startsWith("/")) "/$page" else page
            fields["next_page"] = ""

            if (page.contains("System", ignoreCase = true)) {
                fields["sid_list"] = "LANHostConfig;General;Storage;"
                fields["computer_name"] = config.deviceName
                fields["http_username"] = config.adminUser
                if (config.adminPass.isNotEmpty()) {
                    fields["http_passwd"] = config.adminPass
                    fields["http_passwd2"] = config.adminPass
                    fields["v_password2"] = config.adminPass
                }
                fields["time_zone"] = config.timezone
                fields["ntp_period"] = config.ntpPeriod
                fields["ntp_server0"] = config.ntpServer1
                fields["ntp_server1"] = config.ntpServer2
                fields["log_ipaddr"] = config.logIpAddr
                fields["log_port"] = config.logPort
                fields["log_float_ui"] = config.logFloatUi
                fields["select_lang"] = config.selectLang
                fields["help_enable"] = if (config.helpEnable) "1" else "0"
                fields["action_script"] = "restart_httpd"
            } else if (page.contains("Services", ignoreCase = true)) {
                fields["sid_list"] = "LANHostConfig;General;Storage;"
                fields["telnetd"] = if (config.enableTelnet) "1" else "0"
                fields["sshd_enable"] = config.sshdEnable
                fields["sshd_port"] = config.sshPort
                fields["sshd_sftp"] = if (config.enableSftp) "1" else "0"
                fields["webdav_enable"] = if (config.enableWebdav) "1" else "0"
                fields["http_proto"] = config.httpProto
                fields["http_lanport"] = config.httpLanPort
                fields["https_lport"] = config.httpsLPort
                fields["http_access"] = config.httpAccess
                fields["wins_enable"] = if (config.winsEnable) "1" else "0"
                fields["st_samba_workgroup"] = config.stSambaWorkgroup
                fields["st_samba_lmb"] = config.stSambaLmb
                fields["ttyd_enable"] = if (config.ttydEnable) "1" else "0"
                fields["ttyd_port"] = config.ttydPort
                fields["vlmcsd_enable"] = if (config.vlmcsdEnable) "1" else "0"
                fields["napt66_enable"] = if (config.napt66Enable) "1" else "0"
                fields["lltd_enable"] = if (config.lltdEnable) "1" else "0"
                fields["adsc_enable"] = if (config.adscEnable) "1" else "0"
                fields["crond_enable"] = if (config.crondEnable) "1" else "0"
                fields["crontab.login"] = config.crontabLogin
                fields["watchdog_cpu"] = if (config.watchdogCpu) "1" else "0"
            } else if (page.contains("OperationMode", ignoreCase = true) || page.contains("OpMode", ignoreCase = true)) {
                fields["action_mode"] = " Restart "
                fields["sid_list"] = "General;"
                fields["op_mode"] = config.opMode
                fields["sw_mode"] = config.opMode
            } else if (page.contains("Buttons", ignoreCase = true) || page.contains("Tweaks", ignoreCase = true)) {
                fields["sid_list"] = "General;"
                fields["btn_wps_mode"] = config.btnWpsMode
                fields["led_pwr_mode"] = config.ledPowerMode
                fields["btn_wps_s"] = config.btnWpsShort
                fields["btn_wps_l"] = config.btnWpsLong
                fields["led_enable"] = if (config.ledEnable) "1" else "0"
                fields["led_internet"] = config.ledInternet
                fields["led_usb"] = config.ledUsb
                fields["led_wifi"] = config.ledWifi
                fields["led_power"] = config.ledPower
                fields["led_ethernet"] = config.ledEthernet
            } else if (page.contains("Backup", ignoreCase = true)) {
                fields["sid_list"] = "General;"
                fields["nvram_manual"] = config.nvramManual
                fields["rstats_stored"] = config.rstatsStored
                fields["stime_stored"] = config.stimeStored
                fields["mtd_rwfs_mount"] = config.mtdRwfsMount
            }

            val response = applyAndCommitSettings(fields)
            if (response.isSuccessful) {
                cachedAdmin = config
                
                // Update CredentialStore if username or password was changed
                if (page.contains("System", ignoreCase = true) && (config.adminPass.isNotEmpty() || config.adminUser != credentialStore.getUsername())) {
                    val newPass = if (config.adminPass.isNotEmpty()) config.adminPass else credentialStore.getPassword()
                    val newUser = config.adminUser
                    
                    credentialStore.saveCredentials(
                        newUser,
                        credentialStore.getRouterIp(),
                        newPass,
                        credentialStore.isRememberCredentials(),
                        credentialStore.isUseBiometric()
                    )
                    
                    // Force Retrofit to update its credentials for any subsequent calls
                    RetrofitClient.forceReinitialize(
                        credentialStore.getRouterIp(),
                        { newUser },
                        { newPass }
                    )
                }
                
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
            fields["current_page"] = page
            fields["next_page"] = ""

            if (page.contains("Scripts", ignoreCase = true)) {
                fields["sid_list"] = "General;"
                fields["scripts.init_script.sh"] = config.scriptInit
                fields["scripts.start_script.sh"] = config.scriptStart
                fields["scripts.post_wan_script.sh"] = config.scriptWanUp
                fields["scripts.post_wandn_script.sh"] = config.scriptWanDown
                fields["scripts.ez_buttons_script.sh"] = config.scriptEzButton
                fields["scripts.shutdown_script.sh"] = config.scriptShutdown
                fields["scripts.post_iptables_script.sh"] = config.scriptIpRules
            } else if (page.contains("InetDetect", ignoreCase = true) || page.contains("Detector", ignoreCase = true)) {
                fields["sid_list"] = "General;"
                fields["di_poll_mode"] = config.pingPollMode
                fields["di_addr0"] = config.pingHost1
                fields["di_addr1"] = config.pingHost2
                fields["di_addr2"] = config.pingHost3
                fields["di_addr3"] = config.pingHost4
                fields["di_addr4"] = config.pingHost5
                fields["di_addr5"] = config.pingHost6
                fields["di_time_done"] = config.pingIntervalSuccess.toString()
                fields["di_time_fail"] = config.pingIntervalFail.toString()
                fields["di_time_timeout"] = config.pingTimeout.toString()
                fields["di_lost_action"] = config.pingAction
            }

            val response = applyAndCommitSettings(fields)
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
            val page = if (is5GHz) "wds_aplist.asp" else "wds_aplist_2g.asp"
            val response = getService().getPageContent(page, "15000")
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

    private suspend fun applyAndCommitSettings(fields: Map<String, String>): retrofit2.Response<String> {
        val response = getService().applySettings(fields)
        if (response.isSuccessful) {
            try {
                getService().commitFlash()
            } catch (e: Exception) {
                // Ignore exceptions here, as some apply actions (like restart_httpd) 
                // can cause the router to drop the connection during commitFlash.
                e.printStackTrace()
            }
        }
        return response
    }
}
