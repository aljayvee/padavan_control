package com.example.padavancontrol.data.models

data class LanClient(
    val ipAddress: String,
    val macAddress: String,
    val hostname: String,
    val isOnline: Boolean,
    val connectionType: String, // "2.4G", "5G", "Wired"
    val rssi: String = "",
    val blockIndex: Int = -1
)
