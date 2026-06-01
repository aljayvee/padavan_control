package com.example.padavancontrol.data.models

data class WanStatus(
    val isConnected: Boolean,
    val wanIp: String,
    val gateway: String,
    val dns1: String,
    val dns2: String,
    val connectionType: String,  // "PPPoE", "DHCP", "Static"
    val uptime: Long
)
