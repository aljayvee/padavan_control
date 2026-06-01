package com.example.padavancontrol.data.models

data class WirelessMacFilterRule(
    val mac: String,
    val desc: String = ""
)

data class WirelessConfig(
    // Common/General Settings
    val isEnabled: Boolean = true,
    val ssid: String = "",
    val isClosed: Boolean = false,
    val wirelessMode: String = "6", // gmode
    val bandwidth: String = "1", // HT_BW
    val channel: String = "0", // channel (0 = Auto)
    val extChannel: String = "0", // Extension Channel
    val authMode: String = "psk",
    val wpaMode: String = "2", // 1=WPA, 2=WPA2, 0=WPA-Auto
    val crypto: String = "aes",
    val wpaPsk: String = "",
    val wpaGtkRekey: Int = 3600, // Network Key Rotation Interval
    val txPower: Int = 100,

    // Schedules
    val radioDate: String = "1111111", // Date to Enable Radio (Sun-Sat)
    val radioTimeWorkweek: String = "00002359", // Time of Day (workweek)
    val radioTimeWeekend: String = "00002359", // Time of Day (weekend)

    // Guest AP Settings
    val guestEnabled: Boolean = false,
    val guestSsid: String = "",
    val guestClosed: Boolean = false,
    val guestLanIsolate: Boolean = true,
    val guestApIsolate: Boolean = false,
    val guestMcsMode: String = "0", // Guest fixed TX rate link mode
    val guestAuthMode: String = "psk",
    val guestWpaMode: String = "2",
    val guestCrypto: String = "aes",
    val guestWpaPsk: String = "",
    val guestMacRule: Boolean = false, // Use MAC Address Filter Rules?
    val guestDate: String = "1111111", // Date to active Guest AP
    val guestTimeWorkweek: String = "00002359", // Time to active Guest AP (workweek)
    val guestTimeWeekend: String = "00002359", // Time to active Guest AP (weekend)

    // Bridge / WDS Settings
    val modeX: String = "0", // AP Mode: 0=AP Only, 1=WDS Lazy, 2=WDS Bridge, 3=AP Client, 4=AP Client + AP
    val staWisp: String = "0", // 0=LAN bridge, 1=WAN (WISP)
    val staSsid: String = "",
    val staAuthMode: String = "open",
    val staWpaMode: String = "2",
    val staCrypto: String = "aes",
    val staWpaPsk: String = "",
    val staAuto: String = "0", // Auto seek channel: 0 or 1

    // Wireless MAC Filter
    val macFilterMode: String = "disabled", // disabled, accept, reject
    val macFilterRules: List<WirelessMacFilterRule> = emptyList(),

    // RADIUS Settings
    val radiusIp: String = "",
    val radiusPort: Int = 1812,
    val radiusKey: String = "",

    // Professional Settings
    val mcsMode: String = "0",
    val kickStaRssiLow: Int = 0,
    val assocReqRssiThres: Int = 0,
    val countryCode: String = "US",
    val igmpSnooping: Boolean = false,
    val wmmCapable: Boolean = true,
    val apIsolate: Boolean = false,
    val bcnInterval: Int = 100,
    val dtimInterval: Int = 1,
    val fragThresh: Int = 2346,
    val rtsThresh: Int = 2347,
    val txBurst: Boolean = true,
    val greenAp: Boolean = false,

    // Additional Professional settings
    val streamTx: String = "2", // HT Spatial Streams TX
    val streamRx: String = "2", // HT Spatial Streams RX
    val preamble: String = "1", // Preamble Type (0=Long, 1=Short)
    val pktAggregate: Boolean = true, // Enable Packet Aggregation
    val htRdg: Boolean = false, // Enable Reverse Direction Grant
    val htAutoBA: Boolean = true, // Enable Auto Block Acknowledgement
    val htAmsdu: Boolean = false, // Enable AMSDU
    val wmmApsd: Boolean = false // Enable WMM APSD
)

