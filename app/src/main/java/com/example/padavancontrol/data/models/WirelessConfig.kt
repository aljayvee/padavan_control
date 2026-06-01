package com.example.padavancontrol.data.models

data class WirelessConfig(
    // Common/General Settings
    val isEnabled: Boolean = true,
    val ssid: String = "",
    val isClosed: Boolean = false,
    val wirelessMode: String = "6", // gmode
    val bandwidth: String = "1", // HT_BW
    val channel: String = "0", // channel (0 = Auto)
    val authMode: String = "psk",
    val wpaMode: String = "2", // 1=WPA, 2=WPA2, 0=WPA-Auto
    val crypto: String = "aes",
    val wpaPsk: String = "",
    val txPower: Int = 100,

    // Guest AP Settings
    val guestEnabled: Boolean = false,
    val guestSsid: String = "",
    val guestClosed: Boolean = false,
    val guestLanIsolate: Boolean = true,
    val guestApIsolate: Boolean = false,
    val guestAuthMode: String = "psk",
    val guestWpaMode: String = "2",
    val guestCrypto: String = "aes",
    val guestWpaPsk: String = "",

    // Bridge / WDS Settings
    val modeX: String = "0", // AP Mode: 0=AP Only, 1=WDS Lazy, 2=WDS Bridge, 3=AP Client, 4=AP Client + AP
    val staWisp: String = "0", // 0=LAN bridge, 1=WAN (WISP)
    val staSsid: String = "",
    val staAuthMode: String = "open",
    val staWpaMode: String = "2",
    val staCrypto: String = "aes",
    val staWpaPsk: String = "",
    val staAuto: String = "0", // Auto seek channel: 0 or 1

    // Professional/RADIUS Settings
    val mcsMode: String = "0",
    val kickStaRssiLow: Int = 0,
    val assocReqRssiThres: Int = 0,
    val countryCode: String = "US"
)
