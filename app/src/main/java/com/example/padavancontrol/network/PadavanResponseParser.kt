package com.example.padavancontrol.network

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
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.util.regex.Pattern

object PadavanResponseParser {
    private val gson = Gson()

    fun parseSystemStatus(html: String): SystemStatus? {
        try {
            val pattern = Pattern.compile("var\\s+si_new\\s*=\\s*(\\{.*?\\});", Pattern.DOTALL)
            val matcher = pattern.matcher(html)
            if (matcher.find()) {
                val jsonStr = matcher.group(1)
                val json = gson.fromJson(jsonStr, JsonObject::class.java)

                val cpuObj = json.getAsJsonObject("cpu")
                val ramObj = json.getAsJsonObject("ram")
                val swapObj = json.getAsJsonObject("swap")
                val uptimeObj = json.getAsJsonObject("uptime")

                val cpuTotal = cpuObj?.get("total")?.asLong ?: 0L
                val cpuBusy = cpuObj?.get("busy")?.asLong ?: 0L

                val ramTotal = (ramObj?.get("total")?.asLong ?: 0L) * 1024L // KB to bytes
                val ramFree = (ramObj?.get("free")?.asLong ?: 0L) * 1024L
                val ramUsed = ramTotal - ramFree
                val ramCached = (ramObj?.get("cached")?.asLong ?: 0L) * 1024L
                val ramBuffers = (ramObj?.get("buffers")?.asLong ?: 0L) * 1024L

                val swapTotal = (swapObj?.get("total")?.asLong ?: 0L) * 1024L
                val swapUsed = (swapObj?.get("used")?.asLong ?: 0L) * 1024L

                val loadAvg = json.get("lavg")?.asString ?: "0.00 0.00 0.00"

                val days = uptimeObj?.get("days")?.asLong ?: 0L
                val hours = uptimeObj?.get("hours")?.asLong ?: 0L
                val minutes = uptimeObj?.get("minutes")?.asLong ?: 0L
                val seconds = uptimeObj?.get("seconds")?.asLong ?: 0L
                val uptimeSeconds = days * 86400 + hours * 3600 + minutes * 60 + seconds

                val cpuTemp =
                    json.get("cpu_temp")?.asString?.replace("°C", "")?.trim()?.toFloatOrNull()
                val wifiTemp =
                    json.get("wifi_temp")?.asString?.replace("°C", "")?.trim()?.toFloatOrNull()

                return SystemStatus(
                    cpuUsage = 0, // Will be computed at repository layer via delta checks
                    cpuTotal = cpuTotal,
                    cpuBusy = cpuBusy,
                    ramTotal = ramTotal,
                    ramUsed = ramUsed,
                    ramFree = ramFree,
                    ramCached = ramCached,
                    ramBuffers = ramBuffers,
                    swapTotal = swapTotal,
                    swapUsed = swapUsed,
                    loadAvg = loadAvg,
                    uptime = uptimeSeconds,
                    cpuTemp = cpuTemp,
                    wifiTemp = wifiTemp
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun parseLanClients(html: String): List<LanClient> {
        val clients = mutableListOf<LanClient>()
        try {
            val pattern =
                Pattern.compile("var\\s+ipmonitor_last\\s*=\\s*\\[(.*?)\\]\\s*;", Pattern.DOTALL)
            val matcher = pattern.matcher(html)
            if (matcher.find()) {
                val arrayContent = matcher.group(1)?.trim() ?: ""
                if (arrayContent.isEmpty()) return clients

                // Each client is defined as: ["ComputerName", "IPAddress", "MACAddress", rssi_type, rssi_val, device_type, has_http, status]
                val arrayPattern =
                    Pattern.compile("\\[\\s*\"([^\"]*)\"\\s*,\\s*\"([^\"]*)\"\\s*,\\s*\"([^\"]*)\"\\s*,\\s*([^,]*)\\s*,\\s*([^,]*)\\s*,\\s*\"([^\"]*)\"\\s*,\\s*\"([^\"]*)\"\\s*,\\s*\"([^\"]*)\"\\s*\\]")
                val arrayMatcher = arrayPattern.matcher(arrayContent)
                while (arrayMatcher.find()) {
                    val hostname = arrayMatcher.group(1) ?: ""
                    val ip = arrayMatcher.group(2) ?: ""
                    val mac = arrayMatcher.group(3) ?: ""
                    val connectionType = when (arrayMatcher.group(6)) {
                        "0" -> "Wired"
                        "1" -> "2.4G"
                        "2" -> "5G"
                        else -> "Wired"
                    }
                    val isOnline = arrayMatcher.group(8) == "u"
                    clients.add(
                        LanClient(
                            ipAddress = ip,
                            macAddress = mac,
                            hostname = if (hostname.isEmpty() || hostname == "*") "Unknown Device" else hostname,
                            isOnline = isOnline,
                            connectionType = connectionType
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return clients
    }

    fun parseWanStatus(html: String): WanStatus? {
        try {
            fun extractString(funcName: String): String {
                val pattern =
                    Pattern.compile("function\\s+$funcName\\(\\)\\s*\\{\\s*return\\s*'([^']*)';\\s*\\}")
                val matcher = pattern.matcher(html)
                if (matcher.find()) return matcher.group(1) ?: ""

                val patternDouble =
                    Pattern.compile("function\\s+$funcName\\(\\)\\s*\\{\\s*return\\s*\"([^\"]*)\";\\s*\\}")
                val matcherDouble = patternDouble.matcher(html)
                if (matcherDouble.find()) return matcherDouble.group(1) ?: ""

                return ""
            }

            fun extractLong(funcName: String): Long {
                val pattern =
                    Pattern.compile("function\\s+$funcName\\(\\)\\s*\\{\\s*return\\s*(\\d+);\\s*\\}")
                val matcher = pattern.matcher(html)
                if (matcher.find()) return matcher.group(1)?.toLongOrNull() ?: 0L
                return 0L
            }

            val statusVal = extractLong("wanlink_status")
            val isConnected = statusVal == 0L
            val connectionType = extractString("wanlink_type")
            val uptime = extractLong("wanlink_uptime")
            val wanIp = extractString("wanlink_ip4_wan")
            val gateway = extractString("wanlink_gw4_wan")
            val dnsString = extractString("wanlink_dns")
            val dnsParts = dnsString.split(" ").filter { it.isNotEmpty() }
            val dns1 = dnsParts.getOrNull(0) ?: "0.0.0.0"
            val dns2 = dnsParts.getOrNull(1) ?: "0.0.0.0"

            return WanStatus(
                isConnected = isConnected,
                wanIp = wanIp.ifEmpty { "0.0.0.0" },
                gateway = gateway.ifEmpty { "0.0.0.0" },
                dns1 = dns1,
                dns2 = dns2,
                connectionType = connectionType.ifEmpty { "DHCP" },
                uptime = uptime
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun parseTrafficStats(html: String): Map<String, Pair<Long, Long>> {
        val stats = mutableMapOf<String, Pair<Long, Long>>()
        try {
            val pattern = Pattern.compile("var\\s+netdevs\\s*=\\s*(\\{.*?\\});", Pattern.DOTALL)
            val matcher = pattern.matcher(html)
            if (matcher.find()) {
                val jsonStr = matcher.group(1)
                // JS allows unquoted keys or keys like wifi0: { ... } or "wifi0": { ... }
                // Let's parse it securely using custom regexes if it's not strictly JSON compliant, 
                // or standard Gson since it's very robust with relaxed JSON syntax.
                val json = gson.fromJson(jsonStr, JsonObject::class.java)
                for (key in json.keySet()) {
                    val ifObj = json.getAsJsonObject(key)
                    if (ifObj != null) {
                        val rx = ifObj.get("rx")?.asLong ?: ifObj.get("rx_bytes")?.asLong ?: 0L
                        val tx = ifObj.get("tx")?.asLong ?: ifObj.get("tx_bytes")?.asLong ?: 0L
                        stats[key] = Pair(rx, tx)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return stats
    }

    fun parseLanLinks(html: String): List<PortLink> {
        val ports = mutableListOf<PortLink>()
        val speedMap = mutableMapOf<Int, String>()
        try {
            // Matches returns inside switch or if cases in the JS code
            val pattern = Pattern.compile("(?:case\\s+(\\d+)|port\\s*==\\s*(\\d+)|idx\\s*==\\s*(\\d+)|idx\\s*===\\s*(\\d+))[^;]*?return\\s*['\"]([^'\"]*)['\"]", Pattern.DOTALL)
            val matcher = pattern.matcher(html)
            while (matcher.find()) {
                val idxStr = matcher.group(1) ?: matcher.group(2) ?: matcher.group(3) ?: matcher.group(4) ?: ""
                val speed = matcher.group(5) ?: "No link"
                val idx = idxStr.toIntOrNull()
                if (idx != null) {
                    speedMap[idx] = speed
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Define port names specifically for newifi D2 hardware mapping
        val portNames = listOf("WAN", "LAN 1", "LAN 2", "LAN 3", "LAN 4")
        for (i in 0..4) {
            val speed = speedMap[i] ?: "No link"
            val isConnected = speed.isNotEmpty() && !speed.contains("No link", ignoreCase = true)
            ports.add(PortLink(portNames[i], speed, isConnected))
        }
        return ports
    }

    fun extractValue(html: String, name: String): String {
        // 1. Try input with name and value
        var pattern = Pattern.compile("<input[^>]*?name=\"$name\"[^>]*?value=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
        var matcher = pattern.matcher(html)
        if (matcher.find()) return matcher.group(1) ?: ""

        // 2. Try input with value and name
        pattern = Pattern.compile("<input[^>]*?value=\"([^\"]*)\"[^>]*?name=\"$name\"", Pattern.CASE_INSENSITIVE)
        matcher = pattern.matcher(html)
        if (matcher.find()) return matcher.group(1) ?: ""

        // 3. Try checked radio button of this name
        pattern = Pattern.compile("<input[^>]*?name=\"$name\"[^>]*?value=\"([^\"]*)\"[^>]*?checked", Pattern.CASE_INSENSITIVE)
        matcher = pattern.matcher(html)
        if (matcher.find()) return matcher.group(1) ?: ""

        pattern = Pattern.compile("<input[^>]*?checked[^>]*?name=\"$name\"[^>]*?value=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
        matcher = pattern.matcher(html)
        if (matcher.find()) return matcher.group(1) ?: ""

        // 4. Try select dropdown
        pattern = Pattern.compile("<select[^>]*?name=\"$name\"[^>]*?>(.*?)</select>", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)
        matcher = pattern.matcher(html)
        if (matcher.find()) {
            val optionsHtml = matcher.group(1) ?: ""
            val optPattern = Pattern.compile("<option[^>]*?value=\"([^\"]*)\"[^>]*?selected", Pattern.CASE_INSENSITIVE)
            val optMatcher = optPattern.matcher(optionsHtml)
            if (optMatcher.find()) return optMatcher.group(1) ?: ""
        }

        // 5. Try JS variable
        pattern = Pattern.compile("var\\s+$name\\s*=\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
        matcher = pattern.matcher(html)
        if (matcher.find()) return matcher.group(1) ?: ""

        pattern = Pattern.compile("var\\s+$name\\s*=\\s*'([^']*)'", Pattern.CASE_INSENSITIVE)
        matcher = pattern.matcher(html)
        if (matcher.find()) return matcher.group(1) ?: ""

        return ""
    }

    private fun extractValueOrKeep(html: String, name: String, fallback: String): String {
        val containsName = html.contains("name=\"$name\"", ignoreCase = true) || 
                           html.contains("name='$name'", ignoreCase = true) || 
                           html.contains("name=$name", ignoreCase = true) || 
                           html.contains("var $name", ignoreCase = true) ||
                           html.contains("var\\s+$name\\s*=".toRegex(RegexOption.IGNORE_CASE))
        if (!containsName) return fallback
        val extracted = extractValue(html, name)
        return if (extracted.isEmpty() && !html.contains("value=\"\"", ignoreCase = true) && !html.contains("value=''", ignoreCase = true)) {
            fallback
        } else {
            extracted
        }
    }

    private fun extractIntOrKeep(html: String, name: String, fallback: Int): Int {
        val str = extractValueOrKeep(html, name, fallback.toString())
        return str.toIntOrNull() ?: fallback
    }

    private fun extractLongOrKeep(html: String, name: String, fallback: Long): Long {
        val str = extractValueOrKeep(html, name, fallback.toString())
        return str.toLongOrNull() ?: fallback
    }

    private fun extractBoolOrKeep(html: String, name: String, fallback: Boolean): Boolean {
        val nameWithoutX = if (name.endsWith("_x")) name.substring(0, name.length - 2) else name
        val containsName = html.contains("name=\"$name\"", ignoreCase = true) || 
                           html.contains("name='$name'", ignoreCase = true) || 
                           html.contains("name=$name", ignoreCase = true) || 
                           html.contains("var $name", ignoreCase = true) ||
                           html.contains("id=\"${name}_fake\"", ignoreCase = true) ||
                           html.contains("id=\"${nameWithoutX}_fake\"", ignoreCase = true) ||
                           html.contains("name=\"${name}_fake\"", ignoreCase = true) ||
                           html.contains("name=\"${nameWithoutX}_fake\"", ignoreCase = true) ||
                           html.contains("name=\"$nameWithoutX\"", ignoreCase = true)
        if (!containsName) return fallback
        
        val valStr = extractValue(html, name)
        if (valStr == "1") return true
        if (valStr == "0") return false
        
        if (html.contains("id=\"${name}_fake\" value=\"1 checked", ignoreCase = true) || 
            html.contains("id=\"${nameWithoutX}_fake\" value=\"1 checked", ignoreCase = true) || 
            html.contains("name=\"${name}\" value=\"1\" checked", ignoreCase = true) ||
            html.contains("name=\"${name}\" value=\"1 checked", ignoreCase = true) ||
            html.contains("name=\"${nameWithoutX}\" value=\"1 checked", ignoreCase = true) ||
            html.contains("name=\"${nameWithoutX}\" value=\"1\" checked", ignoreCase = true) ||
            html.contains("name=\"${name}_fake\" value=\"\" checked", ignoreCase = true) ||
            html.contains("name=\"${nameWithoutX}_fake\" value=\"\" checked", ignoreCase = true) ||
            html.contains("name=\"${name}_fake\" value=\"1 checked", ignoreCase = true) ||
            html.contains("name=\"${nameWithoutX}_fake\" value=\"1 checked", ignoreCase = true) ||
            html.contains("name=\"${name}_fake\" value=\"1\" checked", ignoreCase = true) ||
            html.contains("name=\"${nameWithoutX}_fake\" value=\"1\" checked", ignoreCase = true) ||
            html.contains("name=\"${name}_fake\" checked", ignoreCase = true) ||
            html.contains("name=\"${nameWithoutX}_fake\" checked", ignoreCase = true) ||
            html.contains("id=\"${name}_fake\" checked", ignoreCase = true) ||
            html.contains("id=\"${nameWithoutX}_fake\" checked", ignoreCase = true)) {
            return true
        }
        return false
    }

    fun parseWirelessConfig(html: String, is5GHz: Boolean, existingConfig: WirelessConfig = WirelessConfig()): WirelessConfig {
        val prefix = if (is5GHz) "wl_" else "rt_"

        // Radio Enable
        val isEnabled = extractBoolOrKeep(html, "${prefix}radio_x", existingConfig.isEnabled)

        // SSID
        var ssid = extractValueOrKeep(html, "${prefix}ssid2", "")
        if (ssid.isEmpty()) {
            ssid = extractValueOrKeep(html, "${prefix}ssid", existingConfig.ssid)
        }
        try {
            ssid = java.net.URLDecoder.decode(ssid, "UTF-8")
        } catch (e: Exception) {}

        // Broadcast SSID closed
        val isClosed = extractBoolOrKeep(html, "${prefix}closed", existingConfig.isClosed)

        // Wireless Mode
        val wirelessMode = extractValueOrKeep(html, "${prefix}gmode", existingConfig.wirelessMode)

        // Bandwidth
        val bandwidth = extractValueOrKeep(html, "${prefix}HT_BW", existingConfig.bandwidth)

        // Channel
        var channel = extractValueOrKeep(html, "${prefix}channel_orig", "")
        if (channel.isEmpty()) {
            channel = extractValueOrKeep(html, "${prefix}channel", existingConfig.channel)
        }

        // Auth Mode
        val authMode = extractValueOrKeep(html, "${prefix}auth_mode", existingConfig.authMode)
        val wpaMode = extractValueOrKeep(html, "${prefix}wpa_mode", existingConfig.wpaMode)

        // Encryption
        val crypto = extractValueOrKeep(html, "${prefix}crypto", existingConfig.crypto)

        // Key / Password
        var wpaPsk = extractValueOrKeep(html, "${prefix}wpa_psk_org", "")
        if (wpaPsk.isEmpty()) {
            wpaPsk = extractValueOrKeep(html, "${prefix}wpa_psk", existingConfig.wpaPsk)
        }
        try {
            wpaPsk = java.net.URLDecoder.decode(wpaPsk, "UTF-8")
        } catch (e: Exception) {}

        // Tx Power
        val txPower = extractIntOrKeep(html, "${prefix}TxPower", existingConfig.txPower)

        // Guest AP Settings
        val guestEnabled = extractBoolOrKeep(html, "${prefix}guest_enable", existingConfig.guestEnabled)
        var guestSsid = extractValueOrKeep(html, "${prefix}guest_ssid_org", "")
        if (guestSsid.isEmpty()) {
            guestSsid = extractValueOrKeep(html, "${prefix}guest_ssid", existingConfig.guestSsid)
        }
        try {
            guestSsid = java.net.URLDecoder.decode(guestSsid, "UTF-8")
        } catch (e: Exception) {}

        val guestClosed = extractBoolOrKeep(html, "${prefix}guest_closed", existingConfig.guestClosed)
        val guestLanIsolate = extractBoolOrKeep(html, "${prefix}guest_lan_isolate", existingConfig.guestLanIsolate)
        val guestApIsolate = extractBoolOrKeep(html, "${prefix}guest_ap_isolate", existingConfig.guestApIsolate)
        val guestAuthMode = extractValueOrKeep(html, "${prefix}guest_auth_mode", existingConfig.guestAuthMode)
        val guestWpaMode = extractValueOrKeep(html, "${prefix}guest_wpa_mode", existingConfig.guestWpaMode)
        val guestCrypto = extractValueOrKeep(html, "${prefix}guest_crypto", existingConfig.guestCrypto)
        var guestWpaPsk = extractValueOrKeep(html, "${prefix}guest_wpa_psk_org", "")
        if (guestWpaPsk.isEmpty()) {
            guestWpaPsk = extractValueOrKeep(html, "${prefix}guest_wpa_psk", existingConfig.guestWpaPsk)
        }
        try {
            guestWpaPsk = java.net.URLDecoder.decode(guestWpaPsk, "UTF-8")
        } catch (e: Exception) {}

        // Bridge / WDS Settings
        val modeX = extractValueOrKeep(html, "${prefix}mode_x", existingConfig.modeX)
        val staWisp = extractValueOrKeep(html, "${prefix}sta_wisp", existingConfig.staWisp)
        var staSsid = extractValueOrKeep(html, "${prefix}sta_ssid_org", "")
        if (staSsid.isEmpty()) {
            staSsid = extractValueOrKeep(html, "${prefix}sta_ssid", existingConfig.staSsid)
        }
        try {
            staSsid = java.net.URLDecoder.decode(staSsid, "UTF-8")
        } catch (e: Exception) {}

        val staAuthMode = extractValueOrKeep(html, "${prefix}sta_auth_mode", existingConfig.staAuthMode)
        val staWpaMode = extractValueOrKeep(html, "${prefix}sta_wpa_mode", existingConfig.staWpaMode)
        val staCrypto = extractValueOrKeep(html, "${prefix}sta_crypto", existingConfig.staCrypto)
        var staWpaPsk = extractValueOrKeep(html, "${prefix}sta_wpa_psk_org", "")
        if (staWpaPsk.isEmpty()) {
            staWpaPsk = extractValueOrKeep(html, "${prefix}sta_wpa_psk", existingConfig.staWpaPsk)
        }
        try {
            staWpaPsk = java.net.URLDecoder.decode(staWpaPsk, "UTF-8")
        } catch (e: Exception) {}

        val staAuto = extractValueOrKeep(html, "${prefix}sta_auto", existingConfig.staAuto)

        // Professional Settings
        val mcsMode = extractValueOrKeep(html, "${prefix}mcs_mode", existingConfig.mcsMode)
        val kickStaRssiLow = extractIntOrKeep(html, "${prefix}KickStaRssiLow", existingConfig.kickStaRssiLow)
        val assocReqRssiThres = extractIntOrKeep(html, "${prefix}AssocReqRssiThres", existingConfig.assocReqRssiThres)
        val countryCode = extractValueOrKeep(html, "${prefix}country_code", existingConfig.countryCode)

        return WirelessConfig(
            isEnabled = isEnabled,
            ssid = ssid,
            isClosed = isClosed,
            wirelessMode = wirelessMode,
            bandwidth = bandwidth,
            channel = channel,
            authMode = authMode,
            wpaMode = wpaMode,
            crypto = crypto,
            wpaPsk = wpaPsk,
            txPower = txPower,
            guestEnabled = guestEnabled,
            guestSsid = guestSsid,
            guestClosed = guestClosed,
            guestLanIsolate = guestLanIsolate,
            guestApIsolate = guestApIsolate,
            guestAuthMode = guestAuthMode,
            guestWpaMode = guestWpaMode,
            guestCrypto = guestCrypto,
            guestWpaPsk = guestWpaPsk,
            modeX = modeX,
            staWisp = staWisp,
            staSsid = staSsid,
            staAuthMode = staAuthMode,
            staWpaMode = staWpaMode,
            staCrypto = staCrypto,
            staWpaPsk = staWpaPsk,
            staAuto = staAuto,
            mcsMode = mcsMode,
            kickStaRssiLow = kickStaRssiLow,
            assocReqRssiThres = assocReqRssiThres,
            countryCode = countryCode
        )
    }

    fun parseLanConfig(html: String, existingConfig: com.example.padavancontrol.data.models.LanConfig = com.example.padavancontrol.data.models.LanConfig()): com.example.padavancontrol.data.models.LanConfig {
        // LAN IP settings
        val lanIpAddr = extractValueOrKeep(html, "lan_ipaddr", existingConfig.lanIpAddr)
        val lanNetmask = extractValueOrKeep(html, "lan_netmask", existingConfig.lanNetmask)
        val lanGateway = extractValueOrKeep(html, "lan_gateway", existingConfig.lanGateway)
        val lanStp = extractBoolOrKeep(html, "lan_stp", existingConfig.lanStp)

        // DHCP Server settings
        val dhcpEnabled = extractBoolOrKeep(html, "dhcp_enable_x", existingConfig.dhcpEnabled)
        val dhcpDomain = extractValueOrKeep(html, "lan_domain", existingConfig.dhcpDomain)
        val dhcpStart = extractValueOrKeep(html, "dhcp_start", existingConfig.dhcpStart)
        val dhcpEnd = extractValueOrKeep(html, "dhcp_end", existingConfig.dhcpEnd)
        val dhcpLease = extractLongOrKeep(html, "dhcp_lease", existingConfig.dhcpLease)
        val dhcpGateway = extractValueOrKeep(html, "dhcp_gateway_x", existingConfig.dhcpGateway)
        val dhcpDns1 = extractValueOrKeep(html, "dhcp_dns1_x", existingConfig.dhcpDns1)
        val dhcpDns2 = extractValueOrKeep(html, "dhcp_dns2_x", existingConfig.dhcpDns2)
        val dhcpDns3 = extractValueOrKeep(html, "dhcp_dns3_x", existingConfig.dhcpDns3)
        val dhcpWins = extractValueOrKeep(html, "dhcp_wins_x", existingConfig.dhcpWins)

        val dhcpStaticEnabled = extractBoolOrKeep(html, "dhcp_static_x", existingConfig.dhcpStaticEnabled)
        val dhcpStaticArp = extractBoolOrKeep(html, "dhcp_static_arp", existingConfig.dhcpStaticArp)

        // Parse Static Leases (ManualDHCPList)
        val staticLeases = mutableListOf<com.example.padavancontrol.data.models.StaticLease>()
        val mDhcpPattern = Pattern.compile("var\\s+m_dhcp\\s*=\\s*\\[(.*?)\\]\\s*;", Pattern.DOTALL)
        val mDhcpMatcher = mDhcpPattern.matcher(html)
        if (mDhcpMatcher.find()) {
            val arrayContent = mDhcpMatcher.group(1) ?: ""
            val entryPattern = Pattern.compile("\\[\\s*['\"]([^'\"]*)['\"]\\s*,\\s*['\"]([^'\"]*)['\"]\\s*,\\s*['\"]([^'\"]*)['\"]\\s*\\]")
            val entryMatcher = entryPattern.matcher(arrayContent)
            while (entryMatcher.find()) {
                val mac = entryMatcher.group(1) ?: ""
                val ip = entryMatcher.group(2) ?: ""
                val name = entryMatcher.group(3) ?: ""
                staticLeases.add(com.example.padavancontrol.data.models.StaticLease(mac, ip, name))
            }
        } else {
            if (!html.contains("var m_dhcp")) {
                staticLeases.addAll(existingConfig.staticLeases)
            }
        }

        // IPTV Settings
        val mrEnable = extractBoolOrKeep(html, "mr_enable_x", existingConfig.mrEnable)
        val igmpSnoop = extractBoolOrKeep(html, "ether_igmp", existingConfig.igmpSnoop)

        // Static Routes settings (RouterConfig / GWStaticList)
        val routeEnabled = extractBoolOrKeep(html, "sr_enable_x", existingConfig.routeEnabled)
        val staticRoutes = mutableListOf<com.example.padavancontrol.data.models.StaticRoute>()
        val gwStaticPattern = Pattern.compile("var\\s+GWStaticList\\s*=\\s*\\[(.*?)\\]\\s*;", Pattern.DOTALL)
        val gwStaticMatcher = gwStaticPattern.matcher(html)
        if (gwStaticMatcher.find()) {
            val arrayContent = gwStaticMatcher.group(1) ?: ""
            val entryPattern = Pattern.compile("\\[\\s*['\"]([^'\"]*)['\"]\\s*,\\s*['\"]([^'\"]*)['\"]\\s*,\\s*['\"]([^'\"]*)['\"]\\s*,\\s*['\"]([^'\"]*)['\"]\\s*,\\s*['\"]([^'\"]*)['\"]\\s*\\]")
            val entryMatcher = entryPattern.matcher(arrayContent)
            while (entryMatcher.find()) {
                val dest = entryMatcher.group(1) ?: ""
                val mask = entryMatcher.group(2) ?: ""
                val gw = entryMatcher.group(3) ?: ""
                val metric = entryMatcher.group(4)?.toIntOrNull() ?: 1
                val ifName = entryMatcher.group(5) ?: "LAN"
                staticRoutes.add(com.example.padavancontrol.data.models.StaticRoute(dest, mask, gw, metric, ifName))
            }
        } else {
            if (!html.contains("var GWStaticList")) {
                staticRoutes.addAll(existingConfig.staticRoutes)
            }
        }

        // Switch settings
        val greenEthernet = extractBoolOrKeep(html, "ether_green", existingConfig.greenEthernet)
        val eeeEnabled = extractBoolOrKeep(html, "ether_eee", existingConfig.eeeEnabled)

        return com.example.padavancontrol.data.models.LanConfig(
            lanIpAddr = lanIpAddr.ifEmpty { "192.168.2.1" },
            lanNetmask = lanNetmask.ifEmpty { "255.255.255.0" },
            lanGateway = lanGateway,
            lanStp = lanStp,
            dhcpEnabled = dhcpEnabled,
            dhcpDomain = dhcpDomain,
            dhcpStart = dhcpStart.ifEmpty { "192.168.2.2" },
            dhcpEnd = dhcpEnd.ifEmpty { "192.168.2.244" },
            dhcpLease = dhcpLease,
            dhcpGateway = dhcpGateway,
            dhcpDns1 = dhcpDns1,
            dhcpDns2 = dhcpDns2,
            dhcpDns3 = dhcpDns3,
            dhcpWins = dhcpWins,
            dhcpStaticEnabled = dhcpStaticEnabled,
            dhcpStaticArp = dhcpStaticArp,
            staticLeases = staticLeases,
            mrEnable = mrEnable,
            igmpSnoop = igmpSnoop,
            routeEnabled = routeEnabled,
            staticRoutes = staticRoutes,
            greenEthernet = greenEthernet,
            eeeEnabled = eeeEnabled
        )
    }

    fun parseWanConfig(
        html: String,
        existingConfig: com.example.padavancontrol.data.models.WanConfig = com.example.padavancontrol.data.models.WanConfig()
    ): com.example.padavancontrol.data.models.WanConfig {
        val wanProto = extractValueOrKeep(html, "wan_proto", existingConfig.wanProto)
        val wanIpAddr = extractValueOrKeep(html, "wan_ipaddr", existingConfig.wanIpAddr)
        val wanNetmask = extractValueOrKeep(html, "wan_netmask", existingConfig.wanNetmask)
        val wanGateway = extractValueOrKeep(html, "wan_gateway", existingConfig.wanGateway)
        
        val wanDnsEnable = extractBoolOrKeep(html, "wan_dnsenable_x", existingConfig.wanDnsEnable)
        val wanDns1 = extractValueOrKeep(html, "wan_dns1_x", existingConfig.wanDns1)
        val wanDns2 = extractValueOrKeep(html, "wan_dns2_x", existingConfig.wanDns2)

        val pppoeUser = extractValueOrKeep(html, "wan_pppoe_username", existingConfig.pppoeUser)
        val pppoePass = extractValueOrKeep(html, "wan_pppoe_passwd", existingConfig.pppoePass)
        val pppoeMru = extractIntOrKeep(html, "wan_pppoe_mru", existingConfig.pppoeMru)
        val pppoeMtu = extractIntOrKeep(html, "wan_pppoe_mtu", existingConfig.pppoeMtu)
        val pppoeService = extractValueOrKeep(html, "wan_pppoe_service", existingConfig.pppoeService)
        val pppoeAcName = extractValueOrKeep(html, "wan_pppoe_ac", existingConfig.pppoeAcName)

        // Port Forwarding / NAT Settings
        val upnpEnabled = extractBoolOrKeep(html, "upnp_enable_x", existingConfig.upnpEnabled)
        val upnpSecure = extractBoolOrKeep(html, "upnp_secure", existingConfig.upnpSecure)
        val portForwardEnabled = extractBoolOrKeep(html, "vts_enable_x", existingConfig.portForwardEnabled)

        val portForwardRules = mutableListOf<com.example.padavancontrol.data.models.PortForwardRule>()
        val vsListPattern = Pattern.compile("var\\s+VSList\\s*=\\s*\\[(.*?)\\]\\s*;", Pattern.DOTALL)
        val vsListMatcher = vsListPattern.matcher(html)
        if (vsListMatcher.find()) {
            val arrayContent = vsListMatcher.group(1) ?: ""
            val entryPattern = Pattern.compile("\\[\\s*['\"]([^'\"]*)['\"]\\s*,\\s*['\"]([^'\"]*)['\"]\\s*,\\s*['\"]([^'\"]*)['\"]\\s*,\\s*['\"]([^'\"]*)['\"]\\s*,\\s*['\"]([^'\"]*)['\"]\\s*,?\\s*['\"]?([^'\"]*)['\"]?\\s*\\]")
            val entryMatcher = entryPattern.matcher(arrayContent)
            while (entryMatcher.find()) {
                val name = entryMatcher.group(1) ?: ""
                val extPort = entryMatcher.group(2) ?: ""
                val intIp = entryMatcher.group(3) ?: ""
                val intPort = entryMatcher.group(4) ?: ""
                val protocol = entryMatcher.group(5) ?: ""
                val desc = entryMatcher.group(6) ?: ""
                portForwardRules.add(com.example.padavancontrol.data.models.PortForwardRule(name, extPort, intIp, intPort, protocol, desc))
            }
        } else {
            if (!html.contains("var VSList")) {
                portForwardRules.addAll(existingConfig.portForwardRules)
            }
        }

        // DMZ Settings
        val dmzEnabled = extractBoolOrKeep(html, "dmz_enable_x", existingConfig.dmzEnabled)
        val dmzIp = extractValueOrKeep(html, "dmz_ipaddr", existingConfig.dmzIp)

        // DDNS Settings
        val ddnsEnabled = extractBoolOrKeep(html, "ddns_enable_x", existingConfig.ddnsEnabled)
        val ddnsServer = extractValueOrKeep(html, "ddns_server_x", existingConfig.ddnsServer)
        val ddnsUser = extractValueOrKeep(html, "ddns_username_x", existingConfig.ddnsUser)
        val ddnsPass = extractValueOrKeep(html, "ddns_passwd_x", existingConfig.ddnsPass)
        val ddnsHostName = extractValueOrKeep(html, "ddns_hostname_x", existingConfig.ddnsHostName)

        // IPv6 Settings
        val ipv6Proto = extractValueOrKeep(html, "ip6_service", existingConfig.ipv6Proto)
        val ipv6Enabled = ipv6Proto.isNotEmpty() && ipv6Proto != "0"

        return com.example.padavancontrol.data.models.WanConfig(
            wanProto = wanProto.ifEmpty { "dhcp" },
            wanIpAddr = wanIpAddr,
            wanNetmask = wanNetmask,
            wanGateway = wanGateway,
            wanDnsEnable = wanDnsEnable,
            wanDns1 = wanDns1,
            wanDns2 = wanDns2,
            pppoeUser = pppoeUser,
            pppoePass = pppoePass,
            pppoeMru = pppoeMru,
            pppoeMtu = pppoeMtu,
            pppoeService = pppoeService,
            pppoeAcName = pppoeAcName,
            upnpEnabled = upnpEnabled,
            upnpSecure = upnpSecure,
            portForwardEnabled = portForwardEnabled,
            portForwardRules = portForwardRules,
            dmzEnabled = dmzEnabled,
            dmzIp = dmzIp,
            ddnsEnabled = ddnsEnabled,
            ddnsServer = ddnsServer,
            ddnsUser = ddnsUser,
            ddnsPass = ddnsPass,
            ddnsHostName = ddnsHostName,
            ipv6Enabled = ipv6Enabled,
            ipv6Proto = ipv6Proto
        )
    }

    fun parseFirewallConfig(
        html: String,
        existingConfig: FirewallConfig = FirewallConfig()
    ): FirewallConfig {
        // 1. General Security
        val fwEnabled = extractBoolOrKeep(html, "fw_enable_x", existingConfig.fwEnabled)
        val fwDosEnabled = extractBoolOrKeep(html, "fw_dos_x", existingConfig.fwDosEnabled)
        val fwSynCookEnabled = extractBoolOrKeep(html, "fw_syn_cook", existingConfig.fwSynCookEnabled)
        val fwLogMode = extractValueOrKeep(html, "fw_log_x", existingConfig.fwLogMode)
        val miscPingEnabled = extractBoolOrKeep(html, "misc_ping_x", existingConfig.miscPingEnabled)
        val miscHttpEnabled = extractBoolOrKeep(html, "misc_http_x", existingConfig.miscHttpEnabled)
        val miscHttpPort = extractValueOrKeep(html, "misc_httpport_x", existingConfig.miscHttpPort)
        val httpsWopen = extractBoolOrKeep(html, "https_wopen", existingConfig.httpsWopen)
        val httpsWport = extractValueOrKeep(html, "https_wport", existingConfig.httpsWport)
        val sshdWopen = extractBoolOrKeep(html, "sshd_wopen", existingConfig.sshdWopen)
        val sshdWport = extractValueOrKeep(html, "sshd_wport", existingConfig.sshdWport)
        val sshdWbfp = extractValueOrKeep(html, "sshd_wbfp", existingConfig.sshdWbfp)
        val ftpdWopen = extractBoolOrKeep(html, "ftpd_wopen", existingConfig.ftpdWopen)
        val ftpdWport = extractValueOrKeep(html, "ftpd_wport", existingConfig.ftpdWport)
        val udpxyWopen = extractBoolOrKeep(html, "udpxy_wopen", existingConfig.udpxyWopen)
        val udpxyWport = extractValueOrKeep(html, "udpxy_wport", existingConfig.udpxyWport)
        val trmdRopen = extractBoolOrKeep(html, "trmd_ropen", existingConfig.trmdRopen)
        val ariaRopen = extractBoolOrKeep(html, "aria_ropen", existingConfig.ariaRopen)

        // 2. Netfilter / ALGs
        val wanNatEnabled = extractBoolOrKeep(html, "wan_nat_x", existingConfig.wanNatEnabled)
        val nfMaxConn = extractValueOrKeep(html, "nf_max_conn", existingConfig.nfMaxConn)
        val nfNatType = extractValueOrKeep(html, "nf_nat_type", existingConfig.nfNatType)
        val nfNatLoopback = extractBoolOrKeep(html, "nf_nat_loop", existingConfig.nfNatLoopback)
        val fwPtPppoe = extractBoolOrKeep(html, "fw_pt_pppoe", existingConfig.fwPtPppoe)
        val nfAlgFtp0 = extractValueOrKeep(html, "nf_alg_ftp0", existingConfig.nfAlgFtp0)
        val nfAlgFtp1 = extractValueOrKeep(html, "nf_alg_ftp1", existingConfig.nfAlgFtp1)
        val nfAlgPptp = extractBoolOrKeep(html, "nf_alg_pptp", existingConfig.nfAlgPptp)
        val nfAlgRtsp = extractBoolOrKeep(html, "nf_alg_rtsp", existingConfig.nfAlgRtsp)
        val nfAlgH323 = extractBoolOrKeep(html, "nf_alg_h323", existingConfig.nfAlgH323)
        val nfAlgSip = extractBoolOrKeep(html, "nf_alg_sip", existingConfig.nfAlgSip)

        // 3. URL Filters
        val urlFilterEnabled = extractBoolOrKeep(html, "url_enable_x", existingConfig.urlFilterEnabled)
        val urlFilterDate = extractValueOrKeep(html, "url_date_x", existingConfig.urlFilterDate)
        val urlFilterTime = extractValueOrKeep(html, "url_time_x", existingConfig.urlFilterTime)
        val urlFilterMac = extractValueOrKeep(html, "url_mac_x", existingConfig.urlFilterMac)
        val urlFilterInvert = extractBoolOrKeep(html, "url_inv_x", existingConfig.urlFilterInvert)

        // Extract option values from the UrlList select list
        val urlKeywords = mutableListOf<String>()
        val urlSelectPattern = Pattern.compile("<select[^>]*?name=\"UrlList_s\"[^>]*?>(.*?)</select>", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)
        val urlSelectMatcher = urlSelectPattern.matcher(html)
        if (urlSelectMatcher.find()) {
            val options = urlSelectMatcher.group(1) ?: ""
            val optionPattern = Pattern.compile("<option[^>]*?value=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
            val optionMatcher = optionPattern.matcher(options)
            while (optionMatcher.find()) {
                val keyword = optionMatcher.group(1) ?: ""
                if (keyword.isNotEmpty()) urlKeywords.add(keyword)
            }
        } else {
            if (!html.contains("name=\"UrlList_s\"") && !html.contains("name='UrlList_s'")) {
                urlKeywords.addAll(existingConfig.urlKeywords)
            }
        }

        // 4. Hardware MAC Filters
        val macFilterMethod = extractValueOrKeep(html, "macfilter_enable_x", existingConfig.macFilterMethod)
        val fwMacDrop = extractBoolOrKeep(html, "fw_mac_drop", existingConfig.fwMacDrop)

        val macFilterRules = mutableListOf<MacFilterRule>()
        val macListPattern = Pattern.compile("var\\s+MACList\\s*=\\s*\\[(.*?)\\]\\s*;", Pattern.DOTALL)
        val macListMatcher = macListPattern.matcher(html)
        if (macListMatcher.find()) {
            val arrayContent = macListMatcher.group(1) ?: ""
            val entryPattern = Pattern.compile("\\[\\s*['\"]([^'\"]*)['\"]\\s*,\\s*['\"]([^'\"]*)['\"]\\s*,\\s*['\"]([^'\"]*)['\"]\\s*\\]")
            val entryMatcher = entryPattern.matcher(arrayContent)
            while (entryMatcher.find()) {
                val mac = entryMatcher.group(1) ?: ""
                val time = entryMatcher.group(2) ?: ""
                val date = entryMatcher.group(3) ?: ""
                macFilterRules.add(MacFilterRule(mac, time, date))
            }
        } else {
            if (!html.contains("var MACList")) {
                macFilterRules.addAll(existingConfig.macFilterRules)
            }
        }

        // 5. Network Services Filter (LWFilterList)
        val fwLwEnabled = extractBoolOrKeep(html, "fw_lw_enable_x", existingConfig.fwLwEnabled)
        val filterLwDefault = extractValueOrKeep(html, "filter_lw_default_x", existingConfig.filterLwDefault)
        val filterLwDate = extractValueOrKeep(html, "filter_lw_date_x", existingConfig.filterLwDate)
        val filterLwTime = extractValueOrKeep(html, "filter_lw_time_x", existingConfig.filterLwTime)
        val filterLwIcmp = extractValueOrKeep(html, "filter_lw_icmp_x", existingConfig.filterLwIcmp)

        val serviceFilterRules = mutableListOf<ServiceFilterRule>()
        val vsListPattern = Pattern.compile("var\\s+LWFilterList\\s*=\\s*\\[(.*?)\\]\\s*;", Pattern.DOTALL)
        val vsListMatcher = vsListPattern.matcher(html)
        if (vsListMatcher.find()) {
            val arrayContent = vsListMatcher.group(1) ?: ""
            val entryPattern = Pattern.compile("\\[\\s*['\"]([^'\"]*)['\"]\\s*,\\s*['\"]([^'\"]*)['\"]\\s*,\\s*['\"]([^'\"]*)['\"]\\s*,\\s*['\"]([^'\"]*)['\"]\\s*,\\s*['\"]([^'\"]*)['\"]\\s*,?\\s*['\"]?([^'\"]*)['\"]?\\s*\\]")
            val entryMatcher = entryPattern.matcher(arrayContent)
            while (entryMatcher.find()) {
                val srcIp = entryMatcher.group(1) ?: ""
                val srcPort = entryMatcher.group(2) ?: ""
                val dstIp = entryMatcher.group(3) ?: ""
                val dstPort = entryMatcher.group(4) ?: ""
                val proto = entryMatcher.group(5) ?: ""
                val protoNo = entryMatcher.group(6) ?: ""
                serviceFilterRules.add(ServiceFilterRule(srcIp, srcPort, dstIp, dstPort, proto, protoNo))
            }
        } else {
            if (!html.contains("var LWFilterList")) {
                serviceFilterRules.addAll(existingConfig.serviceFilterRules)
            }
        }

        return FirewallConfig(
            fwEnabled = fwEnabled,
            fwDosEnabled = fwDosEnabled,
            fwSynCookEnabled = fwSynCookEnabled,
            fwLogMode = fwLogMode,
            miscPingEnabled = miscPingEnabled,
            miscHttpEnabled = miscHttpEnabled,
            miscHttpPort = miscHttpPort,
            httpsWopen = httpsWopen,
            httpsWport = httpsWport,
            sshdWopen = sshdWopen,
            sshdWport = sshdWport,
            sshdWbfp = sshdWbfp,
            ftpdWopen = ftpdWopen,
            ftpdWport = ftpdWport,
            udpxyWopen = udpxyWopen,
            udpxyWport = udpxyWport,
            trmdRopen = trmdRopen,
            ariaRopen = ariaRopen,
            wanNatEnabled = wanNatEnabled,
            nfMaxConn = nfMaxConn,
            nfNatType = nfNatType,
            nfNatLoopback = nfNatLoopback,
            fwPtPppoe = fwPtPppoe,
            nfAlgFtp0 = nfAlgFtp0,
            nfAlgFtp1 = nfAlgFtp1,
            nfAlgPptp = nfAlgPptp,
            nfAlgRtsp = nfAlgRtsp,
            nfAlgH323 = nfAlgH323,
            nfAlgSip = nfAlgSip,
            urlFilterEnabled = urlFilterEnabled,
            urlFilterDate = urlFilterDate,
            urlFilterTime = urlFilterTime,
            urlFilterMac = urlFilterMac,
            urlFilterInvert = urlFilterInvert,
            urlKeywords = urlKeywords,
            macFilterMethod = macFilterMethod,
            fwMacDrop = fwMacDrop,
            macFilterRules = macFilterRules,
            fwLwEnabled = fwLwEnabled,
            filterLwDefault = filterLwDefault,
            filterLwDate = filterLwDate,
            filterLwTime = filterLwTime,
            filterLwIcmp = filterLwIcmp,
            serviceFilterRules = serviceFilterRules
        )
    }

    fun parseUsbShareConfig(
        html: String,
        existingConfig: UsbShareConfig = UsbShareConfig()
    ): UsbShareConfig {
        // 1. Common Settings
        val usb3Disable = extractBoolOrKeep(html, "usb3_disable", existingConfig.usb3Disable)
        val hddSpindown = extractValueOrKeep(html, "hdd_spindt", existingConfig.hddSpindown)
        val hddApmOff = extractBoolOrKeep(html, "hdd_apmoff", existingConfig.hddApmOff)
        val achkEnable = extractBoolOrKeep(html, "achk_enable", existingConfig.achkEnable)
        val pcacheReclaim = extractValueOrKeep(html, "pcache_reclaim", existingConfig.pcacheReclaim)
        val optwEnable = extractValueOrKeep(html, "optw_enable", existingConfig.optwEnable)
        val stMaxUser = extractValueOrKeep(html, "st_max_user", existingConfig.stMaxUser)

        // 2. Samba Server
        val enableSamba = extractBoolOrKeep(html, "enable_samba", existingConfig.enableSamba)
        val sambaWorkgroup = extractValueOrKeep(html, "st_samba_workgroup", existingConfig.sambaWorkgroup)
        val sambaMode = extractValueOrKeep(html, "st_samba_mode", existingConfig.sambaMode)
        val sambaLmb = extractValueOrKeep(html, "st_samba_lmb", existingConfig.sambaLmb)
        val sambaFp = extractValueOrKeep(html, "st_samba_fp", existingConfig.sambaFp)

        // 3. FTP Server
        val enableFtp = extractBoolOrKeep(html, "enable_ftp", existingConfig.enableFtp)
        val ftpMode = extractValueOrKeep(html, "st_ftp_mode", existingConfig.ftpMode)
        val ftpLog = extractBoolOrKeep(html, "st_ftp_log", existingConfig.ftpLog)
        val ftpPmin = extractValueOrKeep(html, "st_ftp_pmin", existingConfig.ftpPmin)
        val ftpPmax = extractValueOrKeep(html, "st_ftp_pmax", existingConfig.ftpPmax)
        val ftpAnmr = extractValueOrKeep(html, "st_ftp_anmr", existingConfig.ftpAnmr)

        // 4. LPR Printer Server
        val rawdEnable = extractValueOrKeep(html, "rawd_enable", existingConfig.rawdEnable)
        val lprdEnable = extractBoolOrKeep(html, "lprd_enable", existingConfig.lprdEnable)
        val u2ecEnable = extractBoolOrKeep(html, "u2ec_enable", existingConfig.u2ecEnable)

        // 5. 3G/4G USB Modem dials
        val modemRule = extractBoolOrKeep(html, "modem_rule", existingConfig.modemRule)
        val modemType = extractValueOrKeep(html, "modem_type", existingConfig.modemType)
        val modemCountry = extractValueOrKeep(html, "modem_country", existingConfig.modemCountry)
        val modemIsp = extractValueOrKeep(html, "modem_isp", existingConfig.modemIsp)
        val modemApn = extractValueOrKeep(html, "modem_apn", existingConfig.modemApn)
        val modemPin = extractValueOrKeep(html, "modem_pin", existingConfig.modemPin)
        val modemDialnum = extractValueOrKeep(html, "modem_dialnum", existingConfig.modemDialnum)
        val modemUser = extractValueOrKeep(html, "modem_user", existingConfig.modemUser)
        val modemPass = extractValueOrKeep(html, "modem_pass", existingConfig.modemPass)
        val modemNets = extractValueOrKeep(html, "modem_nets", existingConfig.modemNets)
        val modemMtu = extractIntOrKeep(html, "modem_mtu", existingConfig.modemMtu)
        val modemDnsAuto = extractBoolOrKeep(html, "modem_dnsa", existingConfig.modemDnsAuto)
        val wanDns1 = extractValueOrKeep(html, "wan_dns1_x", existingConfig.wanDns1)
        val wanDns2 = extractValueOrKeep(html, "wan_dns2_x", existingConfig.wanDns2)
        val wanDns3 = extractValueOrKeep(html, "wan_dns3_x", existingConfig.wanDns3)
        val modemNode = extractValueOrKeep(html, "modem_node", existingConfig.modemNode)
        val modemCmd = extractValueOrKeep(html, "modem_cmd", existingConfig.modemCmd)
        val modemZcd = extractValueOrKeep(html, "modem_zcd", existingConfig.modemZcd)

        return UsbShareConfig(
            usb3Disable = usb3Disable,
            hddSpindown = hddSpindown,
            hddApmOff = hddApmOff,
            achkEnable = achkEnable,
            pcacheReclaim = pcacheReclaim,
            optwEnable = optwEnable,
            stMaxUser = stMaxUser,
            enableSamba = enableSamba,
            sambaWorkgroup = sambaWorkgroup,
            sambaMode = sambaMode,
            sambaLmb = sambaLmb,
            sambaFp = sambaFp,
            enableFtp = enableFtp,
            ftpMode = ftpMode,
            ftpLog = ftpLog,
            ftpPmin = ftpPmin,
            ftpPmax = ftpPmax,
            ftpAnmr = ftpAnmr,
            rawdEnable = rawdEnable,
            lprdEnable = lprdEnable,
            u2ecEnable = u2ecEnable,
            modemRule = modemRule,
            modemType = modemType,
            modemCountry = modemCountry,
            modemIsp = modemIsp,
            modemApn = modemApn,
            modemPin = modemPin,
            modemDialnum = modemDialnum,
            modemUser = modemUser,
            modemPass = modemPass,
            modemNets = modemNets,
            modemMtu = modemMtu,
            modemDnsAuto = modemDnsAuto,
            wanDns1 = wanDns1,
            wanDns2 = wanDns2,
            wanDns3 = wanDns3,
            modemNode = modemNode,
            modemCmd = modemCmd,
            modemZcd = modemZcd
        )
    }

    fun parseAdminConfig(
        html: String,
        existingConfig: AdminConfig = AdminConfig()
    ): AdminConfig {
        val adminUser = extractValueOrKeep(html, "http_username", existingConfig.adminUser)
        val adminPass = extractValueOrKeep(html, "http_passwd2", existingConfig.adminPass)
        val timezone = extractValueOrKeep(html, "time_zone", existingConfig.timezone)
        val ntpServer1 = extractValueOrKeep(html, "ntp_server0", existingConfig.ntpServer1)
        val ntpServer2 = extractValueOrKeep(html, "ntp_server1", existingConfig.ntpServer2)
        
        val enableTelnet = extractBoolOrKeep(html, "telnetd", existingConfig.enableTelnet)
        val telnetPort = extractValueOrKeep(html, "telnetd_port", existingConfig.telnetPort)
        
        val enableSsh = if (html.contains("name=\"sshd_enable\"", ignoreCase = true) || html.contains("name='sshd_enable'", ignoreCase = true)) {
            extractValue(html, "sshd_enable") != "0"
        } else {
            existingConfig.enableSsh
        }
        val sshPort = extractValueOrKeep(html, "sshd_port", existingConfig.sshPort)
        val enableSftp = extractBoolOrKeep(html, "sshd_sftp", existingConfig.enableSftp)
        val enableWebdav = extractBoolOrKeep(html, "webdav_enable", existingConfig.enableWebdav)
        
        val opMode = extractValueOrKeep(html, "op_mode", existingConfig.opMode)
        val btnWpsMode = extractValueOrKeep(html, "btn_wps_mode", existingConfig.btnWpsMode)
        val ledPowerMode = extractValueOrKeep(html, "led_pwr_mode", existingConfig.ledPowerMode)
        
        var fwVer = existingConfig.firmwareVersion
        val verPattern = Pattern.compile("var\\s+firmware_version\\s*=\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
        val verMatcher = verPattern.matcher(html)
        if (verMatcher.find()) {
            fwVer = verMatcher.group(1) ?: ""
        }
        
        var buildD = existingConfig.buildDate
        val datePattern = Pattern.compile("var\\s+build_date\\s*=\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
        val dateMatcher = datePattern.matcher(html)
        if (dateMatcher.find()) {
            buildD = dateMatcher.group(1) ?: ""
        }
        
        return AdminConfig(
            adminUser = adminUser,
            adminPass = adminPass,
            timezone = timezone,
            ntpServer1 = ntpServer1,
            ntpServer2 = ntpServer2,
            enableTelnet = enableTelnet,
            telnetPort = telnetPort,
            enableSsh = enableSsh,
            sshPort = sshPort,
            enableSftp = enableSftp,
            enableWebdav = enableWebdav,
            opMode = opMode,
            btnWpsMode = btnWpsMode,
            ledPowerMode = ledPowerMode,
            firmwareVersion = fwVer,
            buildDate = buildD
        )
    }

    fun parseScriptConfig(
        html: String,
        existingConfig: ScriptConfig = ScriptConfig()
    ): ScriptConfig {
        val scriptStartup = extractTextareaOrKeep(html, "scripts.start_script.sh", existingConfig.scriptStartup)
        val scriptWanUp = extractTextareaOrKeep(html, "scripts.post_wan_script.sh", existingConfig.scriptWanUp)
        val scriptWanDown = extractTextareaOrKeep(html, "scripts.post_wandn_script.sh", existingConfig.scriptWanDown).ifEmpty {
            extractTextareaOrKeep(html, "scripts.ez_buttons_script.sh", existingConfig.scriptWanDown)
        }
        val scriptShutdown = extractTextareaOrKeep(html, "scripts.shutdown_script.sh", existingConfig.scriptShutdown)
        val scriptIpRules = extractTextareaOrKeep(html, "scripts.post_iptables_script.sh", existingConfig.scriptIpRules)
        
        val pingEnabled = extractBoolOrKeep(html, "di_poll_mode", existingConfig.pingEnabled)
        val pingHost1 = extractValueOrKeep(html, "di_addr0", existingConfig.pingHost1)
        val pingHost2 = extractValueOrKeep(html, "di_addr1", existingConfig.pingHost2)
        val pingPeriod = extractIntOrKeep(html, "di_time_done", existingConfig.pingPeriod)
        val pingAction = extractValueOrKeep(html, "di_lost_action", existingConfig.pingAction)
        
        return ScriptConfig(
            scriptStartup = scriptStartup,
            scriptWanUp = scriptWanUp,
            scriptWanDown = scriptWanDown,
            scriptShutdown = scriptShutdown,
            scriptIpRules = scriptIpRules,
            pingEnabled = pingEnabled,
            pingHost1 = pingHost1,
            pingHost2 = pingHost2,
            pingPeriod = pingPeriod,
            pingAction = pingAction
        )
    }

    private fun extractTextareaOrKeep(html: String, name: String, fallback: String): String {
        val containsName = html.contains("name=\"$name\"", ignoreCase = true) || 
                           html.contains("name='$name'", ignoreCase = true) || 
                           html.contains("name=$name", ignoreCase = true)
        if (!containsName) return fallback
        val extracted = extractTextarea(html, name)
        return if (extracted.isEmpty()) fallback else extracted
    }

    private fun extractTextarea(html: String, name: String): String {
        val pattern = Pattern.compile("<textarea[^>]*?name=\"$name\"[^>]*?>(.*?)</textarea>", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) {
            return matcher.group(1)?.trim() ?: ""
        }
        return ""
    }

    fun parseWifiScan(html: String): List<com.example.padavancontrol.data.models.WifiNetwork> {
        val networks = mutableListOf<com.example.padavancontrol.data.models.WifiNetwork>()
        try {
            val pattern = Pattern.compile("(?:apcli_scan_results|apcli_networks|networks|site_survey)\\s*=\\s*\\[(.*?)\\]\\s*;", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(html)
            var arrayContent = ""
            if (matcher.find()) {
                arrayContent = matcher.group(1) ?: ""
            } else {
                arrayContent = html
            }

            val entryPattern = Pattern.compile("\\[\\s*\"([^\"]*)\"\\s*,\\s*\"([^\"]*)\"\\s*,\\s*\"([^\"]*)\"\\s*,\\s*\"([^\"]*)\"\\s*,\\s*\"([^\"]*)\"[^\\]]*\\]", Pattern.CASE_INSENSITIVE)
            val entryMatcher = entryPattern.matcher(arrayContent)
            while (entryMatcher.find()) {
                var ssid = entryMatcher.group(1) ?: ""
                try {
                    ssid = java.net.URLDecoder.decode(ssid, "UTF-8")
                } catch (e: Exception) {}
                val bssid = entryMatcher.group(2) ?: ""
                val channel = entryMatcher.group(3) ?: ""
                val signalStr = entryMatcher.group(4) ?: "0"
                val signal = signalStr.replace("%", "").replace("dBm", "").trim().toIntOrNull() ?: 0
                val security = entryMatcher.group(5) ?: "Open"
                networks.add(com.example.padavancontrol.data.models.WifiNetwork(ssid, bssid, channel, signal, security))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return networks
    }
}
