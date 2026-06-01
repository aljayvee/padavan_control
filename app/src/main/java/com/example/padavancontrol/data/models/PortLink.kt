package com.example.padavancontrol.data.models

data class PortLink(
    val name: String,         // "WAN", "LAN 1", "LAN 2", etc.
    val speed: String,        // "1000M", "100M", "10M", "No link"
    val isConnected: Boolean
)
