package com.example.padavancontrol.data.models

data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val channel: String,
    val signal: Int,
    val security: String
)
