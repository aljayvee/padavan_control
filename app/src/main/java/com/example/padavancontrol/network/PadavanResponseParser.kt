package com.example.padavancontrol.network

import com.example.padavancontrol.data.models.LanClient
import com.example.padavancontrol.data.models.PortLink
import com.example.padavancontrol.data.models.SystemStatus
import com.example.padavancontrol.data.models.WanStatus
import com.example.padavancontrol.data.models.WirelessConfig
import com.example.padavancontrol.data.models.WirelessMacFilterRule
import com.example.padavancontrol.data.models.FirewallConfig
import com.example.padavancontrol.data.models.UsbShareConfig
import com.example.padavancontrol.data.models.FolderPermission
import com.example.padavancontrol.data.models.ShareNode
import com.example.padavancontrol.data.models.ServiceFilterRule
import com.example.padavancontrol.data.models.MacFilterRule
import com.example.padavancontrol.data.models.AdminConfig
import com.example.padavancontrol.data.models.ScriptConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.util.regex.Pattern

object PadavanResponseParser {
    @Suppress("DEPRECATION")
    private val gson = com.google.gson.GsonBuilder().setLenient().create()

    fun parseSystemStatus(html: String): SystemStatus? {
        try {
            val pattern = Pattern.compile("var\\s+si_new\\s*=\\s*(\\{.*?\\});", Pattern.DOTALL)
            val matcher = pattern.matcher(html)
            if (matcher.find()) {
                var jsonStr = matcher.group(1) ?: "{}"
                
                // Padavan firmware outputs hex numbers (e.g., 0x4a) unquoted, which breaks Gson.
                // We wrap them in quotes so Gson parses them as Strings.
                jsonStr = jsonStr.replace(Regex(":\\s*(0x[0-9a-fA-F]+)"), ": \"$1\"")

                val json = gson.fromJson(jsonStr, JsonObject::class.java)

                val cpuObj = json.getAsJsonObject("cpu")
                val ramObj = json.getAsJsonObject("ram")
                val swapObj = json.getAsJsonObject("swap")
                val uptimeObj = json.getAsJsonObject("uptime")

                fun parseLongSafe(element: com.google.gson.JsonElement?): Long {
                    if (element == null || element.isJsonNull) return 0L
                    val str = element.asString.trim()
                    return if (str.startsWith("0x", ignoreCase = true)) {
                        str.removePrefix("0x").removePrefix("0X").toLongOrNull(16) ?: 0L
                    } else {
                        str.toLongOrNull() ?: 0L
                    }
                }

                val cpuTotal = parseLongSafe(cpuObj?.get("total"))
                val cpuBusy = parseLongSafe(cpuObj?.get("busy"))
                val cpuUserTicks = parseLongSafe(cpuObj?.get("user"))
                val cpuSysTicks = parseLongSafe(cpuObj?.get("sys") ?: cpuObj?.get("system"))
                val cpuNiceTicks = parseLongSafe(cpuObj?.get("nice"))
                val cpuIdleTicks = parseLongSafe(cpuObj?.get("idle"))
                val cpuIrqTicks = parseLongSafe(cpuObj?.get("irq"))
                val cpuSirqTicks = parseLongSafe(cpuObj?.get("sirq"))

                val ramTotal = parseLongSafe(ramObj?.get("total")) * 1024L // KB to bytes
                val ramFree = parseLongSafe(ramObj?.get("free")) * 1024L
                val ramUsed = ramTotal - ramFree
                val ramCached = parseLongSafe(ramObj?.get("cached")) * 1024L
                val ramBuffers = parseLongSafe(ramObj?.get("buffers")) * 1024L

                val swapTotal = parseLongSafe(swapObj?.get("total")) * 1024L
                val swapUsed = parseLongSafe(swapObj?.get("used")) * 1024L

                val loadAvg = json.get("lavg")?.asString ?: "0.00 0.00 0.00"

                val days = parseLongSafe(uptimeObj?.get("days"))
                val hours = parseLongSafe(uptimeObj?.get("hours"))
                val minutes = parseLongSafe(uptimeObj?.get("minutes"))
                val seconds = parseLongSafe(uptimeObj?.get("seconds"))
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
                    wifiTemp = wifiTemp,
                    cpuUserTicks = cpuUserTicks,
                    cpuSysTicks = cpuSysTicks,
                    cpuNiceTicks = cpuNiceTicks,
                    cpuIdleTicks = cpuIdleTicks,
                    cpuIrqTicks = cpuIrqTicks,
                    cpuSirqTicks = cpuSirqTicks
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun parseLanClients(html: String, w2gHtml: String = "", w5gHtml: String = ""): List<LanClient> {
        val clients = mutableListOf<LanClient>()
        try {
            // Extract MACs helper
            fun extractMacs(txt: String): Set<String> {
                if (txt.isEmpty()) return emptySet()
                val set = mutableSetOf<String>()
                val matcher = Pattern.compile("([0-9a-fA-F]{2}[:-]){5}([0-9a-fA-F]{2})").matcher(txt)
                while (matcher.find()) {
                    set.add(matcher.group().uppercase().replace("-", ":"))
                }
                return set
            }

            val w2gMacs = extractMacs(w2gHtml)
            val w5gMacs = extractMacs(w5gHtml)

            // Parse wireless map for RSSI
            val wirelessMap = mutableMapOf<String, String>()
            val wirelessPattern = Pattern.compile("var\\s+wireless\\s*=\\s*\\{(.*?)\\}\\s*;", Pattern.DOTALL)
            val wirelessMatcher = wirelessPattern.matcher(html)
            if (wirelessMatcher.find()) {
                val wirelessContent = wirelessMatcher.group(1)?.trim() ?: ""
                if (wirelessContent.isNotEmpty()) {
                    try {
                        val jsonStr = "{ $wirelessContent }"
                        val jsonObject = gson.fromJson(jsonStr, com.google.gson.JsonObject::class.java)
                        for (key in jsonObject.keySet()) {
                            wirelessMap[key] = jsonObject.get(key).asString
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // Parse blocked clients
            val blockedMap = mutableMapOf<String, Int>()
            val blockedPattern = Pattern.compile("var\\s+list_of_BlockedClient\\s*=\\s*\\[(.*?)\\]\\s*;", Pattern.DOTALL)
            val blockedMatcher = blockedPattern.matcher(html)
            if (blockedMatcher.find()) {
                val blockedContent = blockedMatcher.group(1)?.trim() ?: ""
                if (blockedContent.isNotEmpty()) {
                    try {
                        val jsonStr = "[$blockedContent]"
                        val jsonArray = gson.fromJson(jsonStr, com.google.gson.JsonArray::class.java)
                        for (i in 0 until jsonArray.size()) {
                            val item = jsonArray[i]
                            if (item.isJsonArray) {
                                val arr = item.asJsonArray
                                if (arr.size() > 0 && !arr[0].isJsonNull) {
                                    blockedMap[arr[0].asString] = i
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val pattern =
                Pattern.compile("var\\s+ipmonitor(?:_last)?\\s*=\\s*\\[(.*?)\\]\\s*;", Pattern.DOTALL)
            val matcher = pattern.matcher(html)
            if (matcher.find()) {
                val arrayContent = matcher.group(1)?.trim() ?: ""
                if (arrayContent.isEmpty()) return clients

                // Padavan output format is: [IP, MAC, DeviceName, Type, http, staled]
                val jsonStr = "[$arrayContent]"
                val jsonArray = gson.fromJson(jsonStr, com.google.gson.JsonArray::class.java)

                for (item in jsonArray) {
                    if (!item.isJsonArray) continue
                    val arr = item.asJsonArray
                    
                    val ip = if (arr.size() > 0 && !arr[0].isJsonNull) arr[0].asString else ""
                    val mac = if (arr.size() > 1 && !arr[1].isJsonNull) arr[1].asString else ""
                    val hostname = if (arr.size() > 2 && !arr[2].isJsonNull) arr[2].asString else ""
                    
                    val rssi = wirelessMap[mac] ?: ""
                    
                    val clientMacUpper = mac.uppercase().replace("-", ":")
                    val connectionType = when {
                        w5gMacs.contains(clientMacUpper) -> "5G"
                        w2gMacs.contains(clientMacUpper) -> "2.4G"
                        rssi.isNotEmpty() -> "2.4G"
                        else -> "Wired"
                    }
                    
                    val isOnline = if (arr.size() > 5 && !arr[5].isJsonNull) arr[5].asString == "0" else true
                    val blockIndex = blockedMap[mac] ?: -1

                    clients.add(
                        LanClient(
                            ipAddress = ip,
                            macAddress = mac,
                            hostname = if (hostname.isEmpty() || hostname == "*") "Unknown Device" else hostname,
                            isOnline = isOnline,
                            connectionType = connectionType,
                            rssi = rssi,
                            blockIndex = blockIndex
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
            var startIdx = html.indexOf("netdevs")
            if (startIdx != -1) {
                startIdx = html.indexOf("{", startIdx)
                val endIdx = html.indexOf("};", startIdx).takeIf { it != -1 } ?: html.indexOf("}", startIdx)
                if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                    val jsonStr = html.substring(startIdx, endIdx + 1)
                    
                    // Match each interface block: 'eth3':{rx:...,tx:...} or "eth3":{...}
                    val blockPattern = Pattern.compile("['\"]([^'\"]+)['\"]\\s*:\\s*\\{([^}]+)\\}")
                    val blockMatcher = blockPattern.matcher(jsonStr)
                    
                    // Match individual keys inside the block: rx:0x123 or rx:123
                    val valuePattern = Pattern.compile("([a-zA-Z_]+)\\s*:\\s*(0x[0-9a-fA-F]+|\\d+)", Pattern.CASE_INSENSITIVE)

                    while (blockMatcher.find()) {
                        val iface = blockMatcher.group(1) ?: continue
                        val innerBlock = blockMatcher.group(2) ?: continue
                        
                        val innerMatcher = valuePattern.matcher(innerBlock)
                        val values = mutableMapOf<String, Long>()
                        
                        while (innerMatcher.find()) {
                            val key = innerMatcher.group(1)?.lowercase() ?: continue
                            val rawVal = innerMatcher.group(2) ?: continue
                            
                            val parsedVal = if (rawVal.startsWith("0x", ignoreCase = true)) {
                                rawVal.removePrefix("0x").removePrefix("0X").toLongOrNull(16) ?: 0L
                            } else {
                                rawVal.toLongOrNull() ?: 0L
                            }
                            values[key] = parsedVal
                        }
                        
                        val rx = values["rx"] ?: 0L
                        val tx = values["tx"] ?: 0L
                        val rxBytes = if (rx == 0L) (values["rx_bytes"] ?: 0L) else rx
                        val txBytes = if (tx == 0L) (values["tx_bytes"] ?: 0L) else tx
                        
                        stats[iface] = Pair(rxBytes, txBytes)
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
        if (name.isEmpty()) return ""
        // Optimized linear parsing
        var searchIndex = 0
        while (true) {
            val idx = html.indexOf(name, searchIndex, ignoreCase = true)
            if (idx == -1) break
            
            // Check if this is an input/select field like name="varname" or id="varname"
            val prefix = html.substring(maxOf(0, idx - 10), idx)
            if (prefix.endsWith("name=\"") || prefix.endsWith("name='") || prefix.endsWith("name=") || prefix.endsWith("id=\"")) {
                val tagStart = html.lastIndexOf("<", idx)
                val tagEnd = html.indexOf(">", idx)
                if (tagStart != -1 && tagEnd != -1 && tagStart < tagEnd) {
                    val tagStr = html.substring(tagStart, tagEnd + 1)
                    
                    if (tagStr.startsWith("<select", ignoreCase = true)) {
                        // Find the matching </select>
                        val selectEnd = html.indexOf("</select>", tagEnd, ignoreCase = true)
                        if (selectEnd != -1) {
                            val selectContent = html.substring(tagEnd + 1, selectEnd)
                            val optPattern = Pattern.compile("<option[^>]*?value=[\"']?([^\"'>\\s]*)[\"']?[^>]*?selected", Pattern.CASE_INSENSITIVE)
                            val optMatcher = optPattern.matcher(selectContent)
                            if (optMatcher.find()) return optMatcher.group(1) ?: ""
                            
                            val firstOptPattern = Pattern.compile("<option[^>]*?value=[\"']?([^\"'>\\s]*)", Pattern.CASE_INSENSITIVE)
                            val firstOptMatcher = firstOptPattern.matcher(selectContent)
                            if (firstOptMatcher.find()) return firstOptMatcher.group(1) ?: ""
                        }
                    } else {
                        val valueMatch = Pattern.compile("value=[\"']?([^\"'>\\s]*)", Pattern.CASE_INSENSITIVE).matcher(tagStr)
                        if (valueMatch.find()) {
                            return valueMatch.group(1) ?: ""
                        }
                    }
                }
            }
            
            // Check JS assignment: document.form.varname.value = '123'
            val jsPrefix = html.substring(maxOf(0, idx - 15), idx)
            if (jsPrefix.contains("document.form.")) {
                val lineEnd = html.indexOf("\n", idx).takeIf { it != -1 } ?: html.length
                val lineStr = html.substring(idx, lineEnd)
                val jsMatch = Pattern.compile("\\.value\\s*=\\s*['\"](.*?)['\"]", Pattern.CASE_INSENSITIVE).matcher(lineStr)
                if (jsMatch.find()) {
                    return jsMatch.group(1) ?: ""
                }
            }
            
            // Check JS var assignment: var varname = '123'
            val jsVarPrefix = html.substring(maxOf(0, idx - 10), idx)
            if (jsVarPrefix.matches(Regex("(?s).*var\\s+$", RegexOption.IGNORE_CASE))) {
                val lineEnd = html.indexOf("\n", idx).takeIf { it != -1 } ?: html.length
                val lineStr = html.substring(idx, lineEnd)
                val jsMatch = Pattern.compile("=\\s*['\"](.*?)['\"]", Pattern.CASE_INSENSITIVE).matcher(lineStr)
                if (jsMatch.find()) {
                    return jsMatch.group(1) ?: ""
                }
            }
            
            searchIndex = maxOf(searchIndex + 1, idx + name.length)
        }
        return ""
    }

    private fun extractValueOrKeep(html: String, name: String, fallback: String): String {
        val extracted = extractValue(html, name)
        if (extracted.isEmpty()) {
            val exists = html.contains("name=\"$name\"", ignoreCase = true) || 
                         html.contains("name='$name'", ignoreCase = true) || 
                         html.contains("name=$name", ignoreCase = true) || 
                         html.contains("document.form.$name", ignoreCase = true) ||
                         html.contains("var $name", ignoreCase = true)
            if (!exists) return fallback
        }
        return extracted
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
        if (name.isEmpty()) return fallback
        val nameWithoutX = if (name.endsWith("_x")) name.substring(0, name.length - 2) else name
        val namesToSearch = listOf(name, "${name}_fake", nameWithoutX, "${nameWithoutX}_fake")
        
        var foundCheckbox = false
        var checkboxChecked = false
        var radioValue: String? = null
        var foundGenericValue = false
        var genericValue = ""
        var jsVarValue: String? = null

        for (searchName in namesToSearch) {
            if (searchName.isEmpty()) continue
            var searchIndex = 0
            while (true) {
                val idx = html.indexOf(searchName, searchIndex, ignoreCase = true)
                if (idx == -1) break
                
                val prefix = html.substring(maxOf(0, idx - 10), idx)
                if (prefix.endsWith("name=\"") || prefix.endsWith("name='") || prefix.endsWith("name=") || prefix.endsWith("id=\"")) {
                    val tagStart = html.lastIndexOf("<", idx)
                    val tagEnd = html.indexOf(">", idx)
                    if (tagStart != -1 && tagEnd != -1 && tagStart < tagEnd) {
                        val tagStr = html.substring(tagStart, tagEnd + 1).replace("'", "\"")
                        
                        val isChecked = tagStr.contains("checked", ignoreCase = true) || tagStr.contains("value=\"1 checked\"", ignoreCase = true)
                        val valMatcher = Pattern.compile("value=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE).matcher(tagStr)
                        val thisValue = if (valMatcher.find()) valMatcher.group(1) ?: "" else ""
                        val typeMatcher = Pattern.compile("type=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE).matcher(tagStr)
                        val type = if (typeMatcher.find()) typeMatcher.group(1)?.lowercase() else ""
                        
                        if (type == "radio") {
                            if (isChecked) radioValue = thisValue
                        } else if (type == "checkbox") {
                            foundCheckbox = true
                            if (isChecked) checkboxChecked = true
                        } else {
                            if (thisValue.isNotEmpty() && !foundGenericValue) {
                                genericValue = thisValue
                                foundGenericValue = true
                            }
                        }
                    }
                }
                
                // Check JS var assignment for boolean (1/0 or '1'/'0')
                val jsVarPrefix = html.substring(maxOf(0, idx - 10), idx)
                if (jsVarPrefix.matches(Regex("(?s).*var\\s+$", RegexOption.IGNORE_CASE))) {
                    val lineEnd = html.indexOf("\n", idx).takeIf { it != -1 } ?: html.length
                    val lineStr = html.substring(idx, lineEnd)
                    val jsMatch = Pattern.compile("=\\s*['\"]?(1|0|true|false)['\"]?", Pattern.CASE_INSENSITIVE).matcher(lineStr)
                    if (jsMatch.find()) {
                        jsVarValue = jsMatch.group(1)
                    }
                }
                
                searchIndex = maxOf(searchIndex + 1, idx + searchName.length)
            }
        }
        
        if (radioValue != null) return radioValue == "1"
        if (foundCheckbox) return checkboxChecked
        if (jsVarValue != null) return jsVarValue == "1" || jsVarValue.equals("true", ignoreCase = true)
        if (foundGenericValue) return genericValue == "1"
        
        // Fallback to extractValue if input tag loop didn't yield a definitive boolean
        var valStr = extractValue(html, name)
        if (valStr.isEmpty()) valStr = extractValue(html, "${name}_fake")
        if (valStr.isEmpty() && nameWithoutX.isNotEmpty()) valStr = extractValue(html, nameWithoutX)
        
        if (valStr.equals("1", ignoreCase = true) || valStr.equals("true", ignoreCase = true)) return true
        if (valStr.equals("0", ignoreCase = true) || valStr.equals("false", ignoreCase = true)) return false

        return fallback
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

        // Extension Channel
        val extChannel = extractValueOrKeep(html, "${prefix}HT_EXTCHA", existingConfig.extChannel)

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

        // Network Key Rotation
        val wpaGtkRekey = extractIntOrKeep(html, "${prefix}wpa_gtk_rekey", existingConfig.wpaGtkRekey)

        // Tx Power
        val txPower = extractIntOrKeep(html, "${prefix}TxPower", existingConfig.txPower)

        // Schedules
        val radioDate = extractValueOrKeep(html, "${prefix}radio_date_x", existingConfig.radioDate)
        val radioTimeWorkweek = extractValueOrKeep(html, "${prefix}radio_time_x", existingConfig.radioTimeWorkweek)
        val radioTimeWeekend = extractValueOrKeep(html, "${prefix}radio_time2_x", existingConfig.radioTimeWeekend)

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
        val guestMcsMode = extractValueOrKeep(html, "${prefix}guest_mcs_mode", existingConfig.guestMcsMode)
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
        val guestMacRule = extractBoolOrKeep(html, "${prefix}guest_macrule", existingConfig.guestMacRule)
        val guestDate = extractValueOrKeep(html, "${prefix}guest_date_x", existingConfig.guestDate)
        val guestTimeWorkweek = extractValueOrKeep(html, "${prefix}guest_time_x", existingConfig.guestTimeWorkweek)
        val guestTimeWeekend = extractValueOrKeep(html, "${prefix}guest_time2_x", existingConfig.guestTimeWeekend)

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
        val igmpSnooping = extractBoolOrKeep(html, "${prefix}IGMPSnoop", existingConfig.igmpSnooping)
        val wmmCapable = extractBoolOrKeep(html, "${prefix}wme", existingConfig.wmmCapable)
        val apIsolate = extractBoolOrKeep(html, "${prefix}ap_isolate", existingConfig.apIsolate)
        val bcnInterval = extractIntOrKeep(html, "${prefix}bcn", existingConfig.bcnInterval)
        val dtimInterval = extractIntOrKeep(html, "${prefix}dtim", existingConfig.dtimInterval)
        val fragThresh = extractIntOrKeep(html, "${prefix}frag", existingConfig.fragThresh)
        val rtsThresh = extractIntOrKeep(html, "${prefix}rts", existingConfig.rtsThresh)
        val txBurst = extractBoolOrKeep(html, "${prefix}TxBurst", existingConfig.txBurst)
        val greenAp = extractBoolOrKeep(html, "${prefix}GreenAP", existingConfig.greenAp)

        // Additional Professional settings
        val streamTx = extractValueOrKeep(html, "${prefix}stream_tx", existingConfig.streamTx)
        val streamRx = extractValueOrKeep(html, "${prefix}stream_rx", existingConfig.streamRx)
        val preamble = extractValueOrKeep(html, "${prefix}preamble", existingConfig.preamble)
        val pktAggregate = extractBoolOrKeep(html, "${prefix}PktAggregate", existingConfig.pktAggregate)
        val htRdg = extractBoolOrKeep(html, "${prefix}HT_RDG", existingConfig.htRdg)
        val htAutoBA = extractBoolOrKeep(html, "${prefix}HT_AutoBA", existingConfig.htAutoBA)
        val htAmsdu = extractBoolOrKeep(html, "${prefix}HT_AMSDU", existingConfig.htAmsdu)
        val wmmApsd = extractBoolOrKeep(html, "${prefix}APSDCapable", existingConfig.wmmApsd)

        // Wireless MAC Filter
        val macFilterMode = extractValueOrKeep(html, "${prefix}macmode", existingConfig.macFilterMode)
        val macFilterRules = mutableListOf<WirelessMacFilterRule>()
        val macListPattern = Pattern.compile("var\\s+ACLList\\s*=\\s*\\[(.*?)\\]\\s*;", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)
        val macListMatcher = macListPattern.matcher(html)
        if (macListMatcher.find()) {
            val content = macListMatcher.group(1) ?: ""
            val entryPattern = Pattern.compile("\\[\\s*['\"]([^'\"]*)['\"]\\s*,\\s*['\"]([^'\"]*)['\"]\\s*\\]")
            val entryMatcher = entryPattern.matcher(content)
            while (entryMatcher.find()) {
                val mac = entryMatcher.group(1) ?: ""
                val desc = entryMatcher.group(2) ?: ""
                macFilterRules.add(WirelessMacFilterRule(mac, desc))
            }
        } else {
            macFilterRules.addAll(existingConfig.macFilterRules)
        }

        // RADIUS Settings
        val radiusIp = extractValueOrKeep(html, "${prefix}radius_ipaddr", existingConfig.radiusIp)
        val radiusPort = extractIntOrKeep(html, "${prefix}radius_port", existingConfig.radiusPort)
        val radiusKey = extractValueOrKeep(html, "${prefix}radius_key", existingConfig.radiusKey)

        return WirelessConfig(
            isEnabled = isEnabled,
            ssid = ssid,
            isClosed = isClosed,
            wirelessMode = wirelessMode,
            bandwidth = bandwidth,
            channel = channel,
            extChannel = extChannel,
            authMode = authMode,
            wpaMode = wpaMode,
            crypto = crypto,
            wpaPsk = wpaPsk,
            wpaGtkRekey = wpaGtkRekey,
            txPower = txPower,
            radioDate = radioDate,
            radioTimeWorkweek = radioTimeWorkweek,
            radioTimeWeekend = radioTimeWeekend,
            guestEnabled = guestEnabled,
            guestSsid = guestSsid,
            guestClosed = guestClosed,
            guestLanIsolate = guestLanIsolate,
            guestApIsolate = guestApIsolate,
            guestMcsMode = guestMcsMode,
            guestAuthMode = guestAuthMode,
            guestWpaMode = guestWpaMode,
            guestCrypto = guestCrypto,
            guestWpaPsk = guestWpaPsk,
            guestMacRule = guestMacRule,
            guestDate = guestDate,
            guestTimeWorkweek = guestTimeWorkweek,
            guestTimeWeekend = guestTimeWeekend,
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
            countryCode = countryCode,
            macFilterMode = macFilterMode,
            macFilterRules = macFilterRules,
            radiusIp = radiusIp,
            radiusPort = radiusPort,
            radiusKey = radiusKey,
            igmpSnooping = igmpSnooping,
            wmmCapable = wmmCapable,
            apIsolate = apIsolate,
            bcnInterval = bcnInterval,
            dtimInterval = dtimInterval,
            fragThresh = fragThresh,
            rtsThresh = rtsThresh,
            txBurst = txBurst,
            greenAp = greenAp,
            streamTx = streamTx,
            streamRx = streamRx,
            preamble = preamble,
            pktAggregate = pktAggregate,
            htRdg = htRdg,
            htAutoBA = htAutoBA,
            htAmsdu = htAmsdu,
            wmmApsd = wmmApsd
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
        val dhcpDnsv6 = extractValueOrKeep(html, "dhcp_dnsv6_x", existingConfig.dhcpDnsv6)
        val dhcpVerbose = extractIntOrKeep(html, "dhcp_verbose", existingConfig.dhcpVerbose)

        val dnsmasqDnsmasqConf = extractTextareaOrKeep(html, "dnsmasq.dnsmasq.conf", existingConfig.dnsmasqDnsmasqConf)
        val dnsmasqDhcpConf = extractTextareaOrKeep(html, "dnsmasq.dhcp.conf", existingConfig.dnsmasqDhcpConf)
        val dnsmasqHosts = extractTextareaOrKeep(html, "dnsmasq.hosts", existingConfig.dnsmasqHosts)

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
        val forceIgmp = extractIntOrKeep(html, "force_igmp", existingConfig.forceIgmp)
        val udpxyPort = extractIntOrKeep(html, "udpxy_enable_x", existingConfig.udpxyPort)
        val xupnpdPort = extractIntOrKeep(html, "xupnpd_enable_x", existingConfig.xupnpdPort)
        val xupnpdUdpxy = extractBoolOrKeep(html, "xupnpd_udpxy", existingConfig.xupnpdUdpxy)
        val igmpSnoop = extractBoolOrKeep(html, "ether_igmp", existingConfig.igmpSnoop)
        val etherM2u = extractIntOrKeep(html, "ether_m2u", existingConfig.etherM2u)
        val rtIgmpSnEnable = extractIntOrKeep(html, "rt_IgmpSnEnable", existingConfig.rtIgmpSnEnable)
        val wlIgmpSnEnable = extractIntOrKeep(html, "wl_IgmpSnEnable", existingConfig.wlIgmpSnEnable)
        val controlrateBroadcast = extractIntOrKeep(html, "controlrate_broadcast", existingConfig.controlrateBroadcast)

        // Static Routes settings (RouterConfig / GWStaticList)
        val useDhcpRoutes = extractBoolOrKeep(html, "dr_enable_x", existingConfig.useDhcpRoutes)
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
        val etherJumbo = extractIntOrKeep(html, "ether_jumbo", existingConfig.etherJumbo)

        val ports = listOf("WAN", "LAN1", "LAN2", "LAN3", "LAN4")
        val portsConfig = ports.map { port ->
            val nvramSuffix = port.lowercase()
            val flowVal = extractIntOrKeep(html, "ether_flow_$nvramSuffix", 0)
            val linkVal = extractIntOrKeep(html, "ether_link_$nvramSuffix", 0)
            val existingPort = existingConfig.portsConfig.find { it.portName == port }
            val linkState = existingPort?.linkState ?: "No link"
            com.example.padavancontrol.data.models.SwitchPortConfig(
                portName = port,
                flowControl = flowVal,
                speedDuplex = linkVal,
                linkState = linkState
            )
        }

        // Parse Wake-on-LAN devices
        val wolDevices = mutableListOf<com.example.padavancontrol.data.models.WolDevice>()
        if (html.contains("ipmonitor") || html.contains("m_dhcp")) {
            val devicesMap = mutableMapOf<String, String>() // MAC -> Name
            
            // 1. Parse ipmonitor
            val ipMonPattern = Pattern.compile("var\\s+ipmonitor(?:_last)?\\s*=\\s*\\[(.*?)\\]\\s*;", Pattern.DOTALL)
            val ipMonMatcher = ipMonPattern.matcher(html)
            if (ipMonMatcher.find()) {
                val arrayContent = ipMonMatcher.group(1)?.trim() ?: ""
                if (arrayContent.isNotEmpty()) {
                    try {
                        val jsonStr = "[$arrayContent]"
                        val jsonArray = gson.fromJson(jsonStr, com.google.gson.JsonArray::class.java)
                        for (item in jsonArray) {
                            if (!item.isJsonArray) continue
                            val arr = item.asJsonArray
                            val mac = if (arr.size() > 1 && !arr[1].isJsonNull) arr[1].asString else ""
                            val name = if (arr.size() > 2 && !arr[2].isJsonNull) arr[2].asString else ""
                            if (mac.isNotEmpty()) {
                                devicesMap[mac.uppercase()] = if (name.isNotEmpty() && name != "*") name else ""
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // 2. Parse m_dhcp
            mDhcpMatcher.reset()
            if (mDhcpMatcher.find()) {
                val arrayContent = mDhcpMatcher.group(1) ?: ""
                val entryPattern = Pattern.compile("\\[\\s*['\"]([^'\"]*)['\"]\\s*,\\s*['\"]([^'\"]*)['\"]\\s*,\\s*['\"]([^'\"]*)['\"]\\s*\\]")
                val entryMatcher = entryPattern.matcher(arrayContent)
                while (entryMatcher.find()) {
                    val macRaw = entryMatcher.group(1) ?: ""
                    val mac = if (!macRaw.contains(":")) {
                        val clean = macRaw.replace(":", "").replace("-", "")
                        if (clean.length == 12) {
                            val sb = StringBuilder()
                            for (i in 0 until 12 step 2) {
                                sb.append(clean.substring(i, i + 2))
                                if (i < 10) sb.append(":")
                            }
                            sb.toString()
                        } else {
                            macRaw
                        }
                    } else {
                        macRaw
                    }.uppercase()
                    val name = entryMatcher.group(3) ?: ""
                    if (mac.isNotEmpty()) {
                        devicesMap[mac] = name
                    }
                }
            }

            for ((mac, name) in devicesMap) {
                val existingDevice = existingConfig.wolDevices.find { it.mac.equals(mac, ignoreCase = true) }
                wolDevices.add(com.example.padavancontrol.data.models.WolDevice(
                    mac = mac,
                    name = name.ifEmpty { "Device" },
                    vendor = existingDevice?.vendor ?: ""
                ))
            }
        } else {
            wolDevices.addAll(existingConfig.wolDevices)
        }

        val wolMac = extractValueOrKeep(html, "wol_mac", existingConfig.wolMac)

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
            dhcpDnsv6 = dhcpDnsv6,
            dhcpVerbose = dhcpVerbose,
            dnsmasqDnsmasqConf = dnsmasqDnsmasqConf,
            dnsmasqDhcpConf = dnsmasqDhcpConf,
            dnsmasqHosts = dnsmasqHosts,
            dhcpStaticEnabled = dhcpStaticEnabled,
            dhcpStaticArp = dhcpStaticArp,
            staticLeases = staticLeases,
            mrEnable = mrEnable,
            forceIgmp = forceIgmp,
            udpxyPort = udpxyPort,
            xupnpdPort = xupnpdPort,
            xupnpdUdpxy = xupnpdUdpxy,
            igmpSnoop = igmpSnoop,
            etherM2u = etherM2u,
            rtIgmpSnEnable = rtIgmpSnEnable,
            wlIgmpSnEnable = wlIgmpSnEnable,
            controlrateBroadcast = controlrateBroadcast,
            useDhcpRoutes = useDhcpRoutes,
            routeEnabled = routeEnabled,
            staticRoutes = staticRoutes,
            greenEthernet = greenEthernet,
            eeeEnabled = eeeEnabled,
            etherJumbo = etherJumbo,
            portsConfig = portsConfig,
            wolMac = wolMac,
            wolDevices = wolDevices
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

        val hwNatMode = extractValueOrKeep(html, "hw_nat_mode", existingConfig.hwNatMode)
        val sfeEnable = extractValueOrKeep(html, "sfe_enable", existingConfig.sfeEnable)
        val gwArpPing = extractBoolOrKeep(html, "gw_arp_ping", existingConfig.gwArpPing)
        val wanAuthMode = extractValueOrKeep(html, "wan_auth_mode", existingConfig.wanAuthMode)
        val wanHostname = extractValueOrKeep(html, "wan_hostname", existingConfig.wanHostname)
        val wanVci = extractValueOrKeep(html, "wan_vci", existingConfig.wanVci)
        val wanHwaddr = extractValueOrKeep(html, "wan_hwaddr_x", existingConfig.wanHwaddr)
        val wanTtlFix = extractBoolOrKeep(html, "wan_ttl_fix", existingConfig.wanTtlFix)
        val wanTtlValue = extractValueOrKeep(html, "wan_ttl_value", existingConfig.wanTtlValue)
        val wanStbPort = extractValueOrKeep(html, "wan_stb_x", existingConfig.wanStbPort)
        val wanStbIso = extractValueOrKeep(html, "wan_stb_iso", existingConfig.wanStbIso)
        val vlanFilter = extractBoolOrKeep(html, "vlan_filter", existingConfig.vlanFilter)
        val vlanVidCpu = extractValueOrKeep(html, "vlan_vid_cpu", existingConfig.vlanVidCpu)
        val vlanPriCpu = extractValueOrKeep(html, "vlan_pri_cpu", existingConfig.vlanPriCpu)
        val vlanVidIptv = extractValueOrKeep(html, "vlan_vid_iptv", existingConfig.vlanVidIptv)
        val vlanPriIptv = extractValueOrKeep(html, "vlan_pri_iptv", existingConfig.vlanPriIptv)

        val pppoeUser = extractValueOrKeep(html, "wan_pppoe_username", existingConfig.pppoeUser)
        val pppoePass = extractValueOrKeep(html, "wan_pppoe_passwd", existingConfig.pppoePass)
        val pppoeMru = extractIntOrKeep(html, "wan_pppoe_mru", existingConfig.pppoeMru)
        val pppoeMtu = extractIntOrKeep(html, "wan_pppoe_mtu", existingConfig.pppoeMtu)
        val pppoeService = extractValueOrKeep(html, "wan_pppoe_service", existingConfig.pppoeService)
        val pppoeAcName = extractValueOrKeep(html, "wan_pppoe_ac", existingConfig.pppoeAcName)

        // Port Forwarding / NAT Settings
        val upnpEnabled = extractBoolOrKeep(html, "upnp_enable_x", existingConfig.upnpEnabled)
        val upnpProto = extractValueOrKeep(html, "upnp_proto", existingConfig.upnpProto)
        val upnpSecure = extractBoolOrKeep(html, "upnp_secure", existingConfig.upnpSecure)
        val upnpEportMin = extractValueOrKeep(html, "upnp_eport_min", existingConfig.upnpEportMin)
        val upnpEportMax = extractValueOrKeep(html, "upnp_eport_max", existingConfig.upnpEportMax)
        val upnpIportMin = extractValueOrKeep(html, "upnp_iport_min", existingConfig.upnpIportMin)
        val upnpIportMax = extractValueOrKeep(html, "upnp_iport_max", existingConfig.upnpIportMax)
        val upnpCleanInt = extractValueOrKeep(html, "upnp_clean_int", existingConfig.upnpCleanInt)
        val upnpCleanMin = extractValueOrKeep(html, "upnp_clean_min", existingConfig.upnpCleanMin)
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
        val dmzIp = if (html.contains("name=\"dmz_ip\"") || html.contains("name='dmz_ip'")) {
            extractValueOrKeep(html, "dmz_ip", existingConfig.dmzIp)
        } else {
            extractValueOrKeep(html, "dmz_ipaddr", existingConfig.dmzIp)
        }
        val dmzEnabled = if (html.contains("Advanced_Exposed_Content.asp") || html.contains("name=\"dmz_ip\"")) {
            dmzIp.isNotEmpty()
        } else {
            extractBoolOrKeep(html, "dmz_enable_x", existingConfig.dmzEnabled)
        }
        val dmzSpBattle = extractBoolOrKeep(html, "sp_battle_ips", existingConfig.dmzSpBattle)

        // DDNS Settings
        val ddnsEnabled = extractBoolOrKeep(html, "ddns_enable_x", existingConfig.ddnsEnabled)
        val ddnsServer = extractValueOrKeep(html, "ddns_server_x", existingConfig.ddnsServer)
        val ddnsUser = extractValueOrKeep(html, "ddns_username_x", existingConfig.ddnsUser)
        val ddnsPass = extractValueOrKeep(html, "ddns_passwd_x", existingConfig.ddnsPass)
        val ddnsHostName = extractValueOrKeep(html, "ddns_hostname_x", existingConfig.ddnsHostName)
        val ddnsHostName2 = extractValueOrKeep(html, "ddns_hostname2_x", existingConfig.ddnsHostName2)
        val ddnsHostName3 = extractValueOrKeep(html, "ddns_hostname3_x", existingConfig.ddnsHostName3)
        val ddnsSsl = extractValueOrKeep(html, "ddns_ssl", existingConfig.ddnsSsl)
        val ddnsWildcard = extractValueOrKeep(html, "ddns_wildcard_x", existingConfig.ddnsWildcard)
        val ddns2Server = extractValueOrKeep(html, "ddns2_server", existingConfig.ddns2Server)
        val ddns2HostName = extractValueOrKeep(html, "ddns2_hname", existingConfig.ddns2HostName)
        val ddns2User = extractValueOrKeep(html, "ddns2_user", existingConfig.ddns2User)
        val ddns2Pass = extractValueOrKeep(html, "ddns2_pass", existingConfig.ddns2Pass)
        val ddns2Ssl = extractValueOrKeep(html, "ddns2_ssl", existingConfig.ddns2Ssl)
        val ddns2Wildcard = extractValueOrKeep(html, "ddns2_wildcard_x", existingConfig.ddns2Wildcard)
        val ddnsSource = extractValueOrKeep(html, "ddns_source", existingConfig.ddnsSource)
        val ddnsCheckIp = extractValueOrKeep(html, "ddns_checkip", existingConfig.ddnsCheckIp)
        val ddns2CheckIp = extractValueOrKeep(html, "ddns2_checkip", existingConfig.ddns2CheckIp)
        val ddnsPeriod = extractValueOrKeep(html, "ddns_period", existingConfig.ddnsPeriod)
        val ddnsForced = extractValueOrKeep(html, "ddns_forced", existingConfig.ddnsForced)
        val ddnsIpv6 = extractValueOrKeep(html, "ddns_ipv6", existingConfig.ddnsIpv6)
        val ddnsVerbose = extractValueOrKeep(html, "ddns_verbose", existingConfig.ddnsVerbose)

        // IPv6 Settings
        val ipv6Proto = if (html.contains("ip6_service")) {
            extractValueOrKeep(html, "ip6_service", existingConfig.ipv6Proto)
        } else {
            extractValueOrKeep(html, "ipv6_service", existingConfig.ipv6Proto)
        }
        val ipv6Enabled = ipv6Proto.isNotEmpty() && ipv6Proto != "disabled"
        val ipv6WanDhcp = extractValueOrKeep(html, "ip6_wan_dhcp", existingConfig.ipv6WanDhcp)
        val ipv6DnsAuto = extractBoolOrKeep(html, "ip6_dns_auto", existingConfig.ipv6DnsAuto)
        val ipv6Dns1 = if (html.contains("ip6_dns1")) {
            extractValueOrKeep(html, "ip6_dns1", existingConfig.ipv6Dns1)
        } else {
            extractValueOrKeep(html, "ipv6_dns1_x", existingConfig.ipv6Dns1)
        }
        val ipv6Dns2 = if (html.contains("ip6_dns2")) {
            extractValueOrKeep(html, "ip6_dns2", existingConfig.ipv6Dns2)
        } else {
            extractValueOrKeep(html, "ipv6_dns2_x", existingConfig.ipv6Dns2)
        }
        val ipv6Dns3 = if (html.contains("ip6_dns3")) {
            extractValueOrKeep(html, "ip6_dns3", existingConfig.ipv6Dns3)
        } else {
            extractValueOrKeep(html, "ipv6_dns3_x", existingConfig.ipv6Dns3)
        }
        val ipv6Mtu = if (html.contains("ip6_sit_mtu")) {
            extractIntOrKeep(html, "ip6_sit_mtu", existingConfig.ipv6Mtu)
        } else {
            extractIntOrKeep(html, "ipv6_tun_mtu", existingConfig.ipv6Mtu)
        }
        val ipv66to4Relay = if (html.contains("ip6_6to4_relay")) {
            extractValueOrKeep(html, "ip6_6to4_relay", existingConfig.ipv66to4Relay)
        } else {
            extractValueOrKeep(html, "ipv6_relay", existingConfig.ipv66to4Relay)
        }
        val ipv66to4Mtu = if (html.contains("ip6_sit_mtu")) {
            extractIntOrKeep(html, "ip6_sit_mtu", existingConfig.ipv66to4Mtu)
        } else {
            extractIntOrKeep(html, "ipv6_tun_v4mtu", existingConfig.ipv66to4Mtu)
        }
        val ipv66rdPrefix = if (html.contains("ip6_wan_addr")) {
            extractValueOrKeep(html, "ip6_wan_addr", existingConfig.ipv66rdPrefix)
        } else {
            extractValueOrKeep(html, "ipv6_6rd_prefix", existingConfig.ipv66rdPrefix)
        }
        val ipv66rdPrefixLen = if (html.contains("ip6_wan_size")) {
            extractIntOrKeep(html, "ip6_wan_size", existingConfig.ipv66rdPrefixLen)
        } else {
            extractIntOrKeep(html, "ipv6_6rd_prefixlen", existingConfig.ipv66rdPrefixLen)
        }
        val ipv66rdRouter = if (html.contains("ip6_6rd_relay")) {
            extractValueOrKeep(html, "ip6_6rd_relay", existingConfig.ipv66rdRouter)
        } else {
            extractValueOrKeep(html, "ipv6_6rd_router", existingConfig.ipv66rdRouter)
        }
        val ipv66rdIp4Mtu = if (html.contains("ip6_6rd_size")) {
            extractIntOrKeep(html, "ip6_6rd_size", existingConfig.ipv66rdIp4Mtu)
        } else {
            extractIntOrKeep(html, "ipv6_6rd_ip4size", existingConfig.ipv66rdIp4Mtu)
        }
        val ipv6LanAuto = extractBoolOrKeep(html, "ip6_lan_auto", existingConfig.ipv6LanAuto)
        val ipv6LanRadv = extractBoolOrKeep(html, "ip6_lan_radv", existingConfig.ipv6LanRadv)
        val ipv6LanDhcp = extractValueOrKeep(html, "ip6_lan_dhcp", existingConfig.ipv6LanDhcp)
        val ipv6LanSfps = extractValueOrKeep(html, "ip6_lan_sfps", existingConfig.ipv6LanSfps)
        val ipv6LanSfpe = extractValueOrKeep(html, "ip6_lan_sfpe", existingConfig.ipv6LanSfpe)
        val ipv6LanSflt = extractValueOrKeep(html, "ip6_lan_sflt", existingConfig.ipv6LanSflt)

        return com.example.padavancontrol.data.models.WanConfig(
            wanProto = wanProto.ifEmpty { "dhcp" },
            wanIpAddr = wanIpAddr,
            wanNetmask = wanNetmask,
            wanGateway = wanGateway,
            wanDnsEnable = wanDnsEnable,
            wanDns1 = wanDns1,
            wanDns2 = wanDns2,
            hwNatMode = hwNatMode,
            sfeEnable = sfeEnable,
            gwArpPing = gwArpPing,
            wanAuthMode = wanAuthMode,
            wanHostname = wanHostname,
            wanVci = wanVci,
            wanHwaddr = wanHwaddr,
            wanTtlFix = wanTtlFix,
            wanTtlValue = wanTtlValue,
            wanStbPort = wanStbPort,
            wanStbIso = wanStbIso,
            vlanFilter = vlanFilter,
            vlanVidCpu = vlanVidCpu,
            vlanPriCpu = vlanPriCpu,
            vlanVidIptv = vlanVidIptv,
            vlanPriIptv = vlanPriIptv,
            pppoeUser = pppoeUser,
            pppoePass = pppoePass,
            pppoeMru = pppoeMru,
            pppoeMtu = pppoeMtu,
            pppoeService = pppoeService,
            pppoeAcName = pppoeAcName,
            upnpEnabled = upnpEnabled,
            upnpProto = upnpProto,
            upnpSecure = upnpSecure,
            upnpEportMin = upnpEportMin,
            upnpEportMax = upnpEportMax,
            upnpIportMin = upnpIportMin,
            upnpIportMax = upnpIportMax,
            upnpCleanInt = upnpCleanInt,
            upnpCleanMin = upnpCleanMin,
            portForwardEnabled = portForwardEnabled,
            portForwardRules = portForwardRules,
            dmzEnabled = dmzEnabled,
            dmzIp = dmzIp,
            dmzSpBattle = dmzSpBattle,
            ddnsEnabled = ddnsEnabled,
            ddnsServer = ddnsServer,
            ddnsUser = ddnsUser,
            ddnsPass = ddnsPass,
            ddnsHostName = ddnsHostName,
            ddnsHostName2 = ddnsHostName2,
            ddnsHostName3 = ddnsHostName3,
            ddnsSsl = ddnsSsl,
            ddnsWildcard = ddnsWildcard,
            ddns2Server = ddns2Server,
            ddns2HostName = ddns2HostName,
            ddns2User = ddns2User,
            ddns2Pass = ddns2Pass,
            ddns2Ssl = ddns2Ssl,
            ddns2Wildcard = ddns2Wildcard,
            ddnsSource = ddnsSource,
            ddnsCheckIp = ddnsCheckIp,
            ddns2CheckIp = ddns2CheckIp,
            ddnsPeriod = ddnsPeriod,
            ddnsForced = ddnsForced,
            ddnsIpv6 = ddnsIpv6,
            ddnsVerbose = ddnsVerbose,
            ipv6Enabled = ipv6Enabled,
            ipv6Proto = ipv6Proto,
            ipv6WanDhcp = ipv6WanDhcp,
            ipv6DnsAuto = ipv6DnsAuto,
            ipv6Dns1 = ipv6Dns1,
            ipv6Dns2 = ipv6Dns2,
            ipv6Dns3 = ipv6Dns3,
            ipv6Mtu = ipv6Mtu,
            ipv66to4Relay = ipv66to4Relay,
            ipv66to4Mtu = ipv66to4Mtu,
            ipv66rdPrefix = ipv66rdPrefix,
            ipv66rdPrefixLen = ipv66rdPrefixLen,
            ipv66rdRouter = ipv66rdRouter,
            ipv66rdIp4Mtu = ipv66rdIp4Mtu,
            ipv6LanAuto = ipv6LanAuto,
            ipv6LanRadv = ipv6LanRadv,
            ipv6LanDhcp = ipv6LanDhcp,
            ipv6LanSfps = ipv6LanSfps,
            ipv6LanSfpe = ipv6LanSfpe,
            ipv6LanSflt = ipv6LanSflt
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

        // 6. Torrent Transmission
        val trmdEnable = extractBoolOrKeep(html, "trmd_enable", existingConfig.trmdEnable)
        val trmdPport = extractValueOrKeep(html, "trmd_pport", existingConfig.trmdPport)
        val trmdRport = extractValueOrKeep(html, "trmd_rport", existingConfig.trmdRport)

        // 7. Download manager Aria2
        val ariaEnable = extractBoolOrKeep(html, "aria_enable", existingConfig.ariaEnable)
        val ariaPport = extractValueOrKeep(html, "aria_pport", existingConfig.ariaPport)
        val ariaRport = extractValueOrKeep(html, "aria_rport", existingConfig.ariaRport)

        // 8. Dynamic Accounts list
        val accountsList = mutableListOf<String>()
        val accountsPattern = Pattern.compile("var\\s+accounts\\s*=\\s*\\[(.*?)\\]\\s*;", Pattern.DOTALL)
        val accountsMatcher = accountsPattern.matcher(html)
        if (accountsMatcher.find()) {
            val accountsStr = accountsMatcher.group(1) ?: ""
            val itemPattern = Pattern.compile("['\"]([^'\"]*)['\"]")
            val itemMatcher = itemPattern.matcher(accountsStr)
            while (itemMatcher.find()) {
                val acc = itemMatcher.group(1) ?: ""
                if (acc.isNotEmpty()) {
                    accountsList.add(acc)
                }
            }
        } else {
            accountsList.addAll(existingConfig.accounts)
        }

        // 9. Permissions Map
        val permissionMap = mutableMapOf<String, List<FolderPermission>>()
        val ifPattern = Pattern.compile("if\\s*\\(\\s*account\\s*==\\s*['\"]([^'\"]+)['\"]\\s*&&\\s*pool\\s*==\\s*['\"]([^'\"]+)['\"]\\s*\\)\\s*return\\s*\\[(.*?)\\]\\s*;", Pattern.DOTALL)
        val ifMatcher = ifPattern.matcher(html)
        while (ifMatcher.find()) {
            val accountName = ifMatcher.group(1) ?: ""
            val poolName = ifMatcher.group(2) ?: ""
            val entriesText = ifMatcher.group(3) ?: ""
            
            val list = permissionMap[accountName]?.toMutableList() ?: mutableListOf()
            val entryPattern = Pattern.compile("\\[\\s*['\"]([^'\"]*)['\"]\\s*,\\s*['\"]([^'\"]*)['\"]\\s*,\\s*['\"]([^'\"]*)['\"]\\s*\\]")
            val entryMatcher = entryPattern.matcher(entriesText)
            while (entryMatcher.find()) {
                val folderName = entryMatcher.group(1) ?: ""
                val cifsPerm = entryMatcher.group(2) ?: ""
                val ftpPerm = entryMatcher.group(3) ?: ""
                list.add(FolderPermission(poolName, folderName, cifsPerm, ftpPerm))
            }
            permissionMap[accountName] = list
        }
        val finalPermissions = if (permissionMap.isNotEmpty()) permissionMap else existingConfig.permissions

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
            modemZcd = modemZcd,
            trmdEnable = trmdEnable,
            trmdPport = trmdPport,
            trmdRport = trmdRport,
            ariaEnable = ariaEnable,
            ariaPport = ariaPport,
            ariaRport = ariaRport,
            accounts = accountsList,
            permissions = finalPermissions
        )
    }

    fun parseShareTreeList(html: String): List<ShareNode> {
        val nodes = mutableListOf<ShareNode>()
        val resultPattern = Pattern.compile("var\\s+result\\s*=\\s*\\[(.*?)\\]\\s*;", Pattern.DOTALL)
        val resultMatcher = resultPattern.matcher(html)
        val content = if (resultMatcher.find()) (resultMatcher.group(1) ?: "") else ""
        
        val strPattern = Pattern.compile("['\"]([^'\"]*)['\"]")
        val strMatcher = strPattern.matcher(content)
        
        var layerOrder = extractValue(html, "layer_order")
        if (layerOrder.isEmpty()) {
            val layerPattern = Pattern.compile("var\\s+layer_order\\s*=\\s*['\"](.*?)['\"]")
            val layerMatcher = layerPattern.matcher(html)
            if (layerMatcher.find()) {
                layerOrder = layerMatcher.group(1) ?: ""
            }
        }
        
        while (strMatcher.find()) {
            val value = strMatcher.group(1) ?: ""
            val parts = value.split("#")
            if (parts.size >= 3) {
                val name = parts[0].trim()
                val id = parts[1].trim()
                val hasSub = parts[2].trim() == "1"
                nodes.add(ShareNode(name, id, hasSub, layerOrder))
            }
        }
        return nodes
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
        
        val deviceName = extractValueOrKeep(html, "computer_name", existingConfig.deviceName)
        val ntpPeriod = extractValueOrKeep(html, "ntp_period", existingConfig.ntpPeriod)
        val logIpAddr = extractValueOrKeep(html, "log_ipaddr", existingConfig.logIpAddr)
        val logPort = extractValueOrKeep(html, "log_port", existingConfig.logPort)
        val logFloatUi = extractValueOrKeep(html, "log_float_ui", existingConfig.logFloatUi)
        val selectLang = extractValueOrKeep(html, "select_lang", existingConfig.selectLang)
        val helpEnable = extractBoolOrKeep(html, "help_enable", existingConfig.helpEnable)
        
        val enableTelnet = extractBoolOrKeep(html, "telnetd", existingConfig.enableTelnet)
        val telnetPort = extractValueOrKeep(html, "telnetd_port", existingConfig.telnetPort)
        
        val sshdEnable = extractValueOrKeep(html, "sshd_enable", existingConfig.sshdEnable)
        val enableSsh = sshdEnable != "0"
        val sshPort = extractValueOrKeep(html, "sshd_port", existingConfig.sshPort)
        val enableSftp = extractBoolOrKeep(html, "sshd_sftp", existingConfig.enableSftp)
        val enableWebdav = extractBoolOrKeep(html, "webdav_enable", existingConfig.enableWebdav)
        
        val httpProto = extractValueOrKeep(html, "http_proto", existingConfig.httpProto)
        val httpLanPort = extractValueOrKeep(html, "http_lanport", existingConfig.httpLanPort)
        val httpsLPort = extractValueOrKeep(html, "https_lport", existingConfig.httpsLPort)
        val httpAccess = extractValueOrKeep(html, "http_access", existingConfig.httpAccess)
        
        val winsEnable = extractBoolOrKeep(html, "wins_enable", existingConfig.winsEnable)
        val stSambaWorkgroup = extractValueOrKeep(html, "st_samba_workgroup", existingConfig.stSambaWorkgroup)
        val stSambaLmb = extractValueOrKeep(html, "st_samba_lmb", existingConfig.stSambaLmb)
        
        val ttydEnable = extractBoolOrKeep(html, "ttyd_enable", existingConfig.ttydEnable)
        val ttydPort = extractValueOrKeep(html, "ttyd_port", existingConfig.ttydPort)
        
        val vlmcsdEnable = extractBoolOrKeep(html, "vlmcsd_enable", existingConfig.vlmcsdEnable)
        val napt66Enable = extractBoolOrKeep(html, "napt66_enable", existingConfig.napt66Enable)
        val lltdEnable = extractBoolOrKeep(html, "lltd_enable", existingConfig.lltdEnable)
        val adscEnable = extractBoolOrKeep(html, "adsc_enable", existingConfig.adscEnable)
        val crondEnable = extractBoolOrKeep(html, "crond_enable", existingConfig.crondEnable)
        val crontabLogin = extractTextareaOrKeep(html, "crontab.login", existingConfig.crontabLogin)
        val watchdogCpu = extractBoolOrKeep(html, "watchdog_cpu", existingConfig.watchdogCpu)
        
        val opMode = if (html.contains("sw_mode", ignoreCase = true)) {
            extractValueOrKeep(html, "sw_mode", existingConfig.opMode)
        } else {
            extractValueOrKeep(html, "op_mode", existingConfig.opMode)
        }
        val btnWpsMode = extractValueOrKeep(html, "btn_wps_mode", existingConfig.btnWpsMode)
        val ledPowerMode = extractValueOrKeep(html, "led_pwr_mode", existingConfig.ledPowerMode)
        val btnWpsShort = extractValueOrKeep(html, "ez_action_short", existingConfig.btnWpsShort)
        val btnWpsLong = extractValueOrKeep(html, "ez_action_long", existingConfig.btnWpsLong)
        val ledEnable = extractBoolOrKeep(html, "front_led_all", existingConfig.ledEnable)
        val ledInternet = extractValueOrKeep(html, "front_led_wan", existingConfig.ledInternet)
        val ledUsb = extractValueOrKeep(html, "front_led_usb", existingConfig.ledUsb)
        val ledWifi = extractValueOrKeep(html, "front_led_wif", existingConfig.ledWifi)
        val ledPower = extractValueOrKeep(html, "front_led_pwr", existingConfig.ledPower)
        val ledEthernet = extractValueOrKeep(html, "front_led_lan", existingConfig.ledEthernet)
        
        val nvramManual = extractValueOrKeep(html, "nvram_manual", existingConfig.nvramManual)
        val rstatsStored = extractValueOrKeep(html, "rstats_stored", existingConfig.rstatsStored)
        val stimeStored = extractValueOrKeep(html, "stime_stored", existingConfig.stimeStored)
        val mtdRwfsMount = extractValueOrKeep(html, "mtd_rwfs_mount", existingConfig.mtdRwfsMount)
        
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
        
        val productId = extractValueOrKeep(html, "productid", existingConfig.productId)
        
        return AdminConfig(
            adminUser = adminUser,
            adminPass = adminPass,
            timezone = timezone,
            ntpServer1 = ntpServer1,
            ntpServer2 = ntpServer2,
            deviceName = deviceName,
            ntpPeriod = ntpPeriod,
            logIpAddr = logIpAddr,
            logPort = logPort,
            logFloatUi = logFloatUi,
            selectLang = selectLang,
            helpEnable = helpEnable,
            enableTelnet = enableTelnet,
            telnetPort = telnetPort,
            enableSsh = enableSsh,
            sshdEnable = sshdEnable,
            sshPort = sshPort,
            enableSftp = enableSftp,
            enableWebdav = enableWebdav,
            httpProto = httpProto,
            httpLanPort = httpLanPort,
            httpsLPort = httpsLPort,
            httpAccess = httpAccess,
            winsEnable = winsEnable,
            stSambaWorkgroup = stSambaWorkgroup,
            stSambaLmb = stSambaLmb,
            ttydEnable = ttydEnable,
            ttydPort = ttydPort,
            vlmcsdEnable = vlmcsdEnable,
            napt66Enable = napt66Enable,
            lltdEnable = lltdEnable,
            adscEnable = adscEnable,
            crondEnable = crondEnable,
            crontabLogin = crontabLogin,
            watchdogCpu = watchdogCpu,
            opMode = opMode,
            btnWpsMode = btnWpsMode,
            ledPowerMode = ledPowerMode,
            btnWpsShort = btnWpsShort,
            btnWpsLong = btnWpsLong,
            ledEnable = ledEnable,
            ledInternet = ledInternet,
            ledUsb = ledUsb,
            ledWifi = ledWifi,
            ledPower = ledPower,
            ledEthernet = ledEthernet,
            nvramManual = nvramManual,
            rstatsStored = rstatsStored,
            stimeStored = stimeStored,
            mtdRwfsMount = mtdRwfsMount,
            firmwareVersion = fwVer,
            buildDate = buildD,
            productId = productId
        )
    }

    fun parseScriptConfig(
        html: String,
        existingConfig: ScriptConfig = ScriptConfig()
    ): ScriptConfig {
        val scriptInit = extractTextareaOrKeep(html, "scripts.init_script.sh", existingConfig.scriptInit)
        val scriptStart = extractTextareaOrKeep(html, "scripts.start_script.sh", existingConfig.scriptStart)
        val scriptStartup = extractTextareaOrKeep(html, "scripts.start_script.sh", existingConfig.scriptStartup)
        val scriptWanUp = extractTextareaOrKeep(html, "scripts.post_wan_script.sh", existingConfig.scriptWanUp)
        val scriptWanDown = extractTextareaOrKeep(html, "scripts.post_wandn_script.sh", existingConfig.scriptWanDown)
        val scriptShutdown = extractTextareaOrKeep(html, "scripts.shutdown_script.sh", existingConfig.scriptShutdown)
        val scriptIpRules = extractTextareaOrKeep(html, "scripts.post_iptables_script.sh", existingConfig.scriptIpRules)
        val scriptEzButton = extractTextareaOrKeep(html, "scripts.ez_buttons_script.sh", existingConfig.scriptEzButton)
        
        val pingPollMode = extractValueOrKeep(html, "di_poll_mode", existingConfig.pingPollMode)
        val pingEnabled = pingPollMode != "0"
        
        val pingHost1 = extractValueOrKeep(html, "di_addr0", existingConfig.pingHost1)
        val pingHost2 = extractValueOrKeep(html, "di_addr1", existingConfig.pingHost2)
        val pingHost3 = extractValueOrKeep(html, "di_addr2", existingConfig.pingHost3)
        val pingHost4 = extractValueOrKeep(html, "di_addr3", existingConfig.pingHost4)
        val pingHost5 = extractValueOrKeep(html, "di_addr4", existingConfig.pingHost5)
        val pingHost6 = extractValueOrKeep(html, "di_addr5", existingConfig.pingHost6)
        
        val pingPeriod = extractIntOrKeep(html, "di_time_done", existingConfig.pingPeriod)
        val pingIntervalSuccess = extractIntOrKeep(html, "di_time_done", existingConfig.pingIntervalSuccess)
        val pingIntervalFail = extractIntOrKeep(html, "di_time_fail", existingConfig.pingIntervalFail)
        val pingTimeout = extractIntOrKeep(html, "di_time_timeout", existingConfig.pingTimeout)
        val pingAction = extractValueOrKeep(html, "di_lost_action", existingConfig.pingAction)
        
        return ScriptConfig(
            scriptInit = scriptInit,
            scriptStart = scriptStart,
            scriptStartup = scriptStartup,
            scriptWanUp = scriptWanUp,
            scriptWanDown = scriptWanDown,
            scriptShutdown = scriptShutdown,
            scriptIpRules = scriptIpRules,
            scriptEzButton = scriptEzButton,
            pingEnabled = pingEnabled,
            pingPollMode = pingPollMode,
            pingHost1 = pingHost1,
            pingHost2 = pingHost2,
            pingHost3 = pingHost3,
            pingHost4 = pingHost4,
            pingHost5 = pingHost5,
            pingHost6 = pingHost6,
            pingPeriod = pingPeriod,
            pingIntervalSuccess = pingIntervalSuccess,
            pingIntervalFail = pingIntervalFail,
            pingTimeout = pingTimeout,
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
            val pattern = Pattern.compile("(apcli_scan_results|apcli_networks|networks|site_survey|wds_aplist)\\s*=\\s*\\[(.*?)\\]\\s*;", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(html)
            var varName = ""
            var arrayContent = ""
            if (matcher.find()) {
                varName = matcher.group(1)?.lowercase() ?: ""
                arrayContent = matcher.group(2) ?: ""
            } else {
                arrayContent = html
            }

            val entryPattern = Pattern.compile("\\[(.*?)\\]", Pattern.DOTALL)
            val entryMatcher = entryPattern.matcher(arrayContent)
            while (entryMatcher.find()) {
                val row = entryMatcher.group(1) ?: continue
                val items = row.split(",").map { it.trim().replace("'", "").replace("\"", "") }
                if (items.size >= 4) {
                    if (varName == "wds_aplist") {
                        val bssid = items.getOrNull(1) ?: ""
                        if (!bssid.matches(Regex("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})", RegexOption.IGNORE_CASE))) {
                            continue
                        }
                        var ssid = items.getOrNull(0) ?: ""
                        try {
                            ssid = java.net.URLDecoder.decode(ssid, "UTF-8")
                        } catch (e: Exception) {}
                        val channel = (items.getOrNull(2) ?: "0").filter { it.isDigit() }.takeIf { it.isNotEmpty() } ?: "0"
                        val sig = items.getOrNull(3) ?: "0"
                        val signal = sig.replace("%", "").replace("dBm", "").trim().toIntOrNull() ?: 0
                        val security = items.getOrNull(4) ?: "Open"
                        networks.add(com.example.padavancontrol.data.models.WifiNetwork(ssid, bssid, channel, signal, security))
                        continue
                    }

                    val bssidIndex = items.indexOfFirst { it.matches(Regex("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})", RegexOption.IGNORE_CASE)) }
                    if (bssidIndex == -1) continue
                    
                    var ssid = ""
                    var channel = "0"
                    var security = "Open"
                    var signal = 0
                    
                    if (bssidIndex == 1) {
                        ssid = items[0]
                        channel = items.getOrNull(2) ?: "0"
                        val type = items.getOrNull(3) ?: ""
                        val encrypt = items.getOrNull(4) ?: ""
                        security = if (encrypt.isNotBlank() && type.isNotBlank() && items.size > 5) "$type/$encrypt" else type
                        val sig = if (items.size > 5) items.getOrNull(5) ?: "0" else items.getOrNull(4) ?: "0"
                        signal = sig.replace("%", "").replace("dBm", "").trim().toIntOrNull() ?: 0
                    } else if (bssidIndex == 2) {
                        channel = items[0]
                        ssid = items[1]
                        security = items.getOrNull(3) ?: "Open"
                        val sig = items.getOrNull(4) ?: "0"
                        signal = sig.replace("%", "").replace("dBm", "").trim().toIntOrNull() ?: 0
                    } else {
                        ssid = if (bssidIndex > 0) items[bssidIndex - 1] else ""
                        channel = if (bssidIndex > 1) items[bssidIndex - 2] else items.getOrNull(2) ?: "0"
                        security = items.getOrNull(bssidIndex + 1) ?: "Open"
                        val sig = items.getOrNull(bssidIndex + 2) ?: "0"
                        signal = sig.replace("%", "").replace("dBm", "").trim().toIntOrNull() ?: 0
                    }
                    
                    try {
                        ssid = java.net.URLDecoder.decode(ssid, "UTF-8")
                    } catch (e: Exception) {}
                    
                    channel = channel.filter { it.isDigit() }.takeIf { it.isNotEmpty() } ?: "0"
                    
                    networks.add(com.example.padavancontrol.data.models.WifiNetwork(ssid, items[bssidIndex], channel, signal, security))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return networks
    }

    fun parseHardwareModel(html: String): String? {
        try {
            val patterns = listOf(
                Pattern.compile("var\\s+product_id\\s*=\\s*['\"](.*?)['\"];?", Pattern.CASE_INSENSITIVE),
                Pattern.compile("product_id\\s*=\\s*['\"](.*?)['\"]", Pattern.CASE_INSENSITIVE),
                Pattern.compile("var\\s+model_name\\s*=\\s*['\"](.*?)['\"];?", Pattern.CASE_INSENSITIVE),
                Pattern.compile("var\\s+productid\\s*=\\s*['\"](.*?)['\"];?", Pattern.CASE_INSENSITIVE)
            )
            for (pattern in patterns) {
                val matcher = pattern.matcher(html)
                if (matcher.find()) {
                    val model = matcher.group(1)?.trim() ?: ""
                    if (model.isNotEmpty()) {
                        return model
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
