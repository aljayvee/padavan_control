package com.example.padavancontrol.data.models

data class ScriptConfig(
    // 1. Bash Startup & Shutdown Customization scripts (Advanced_Scripts_Content.asp)
    val scriptStartup: String = "",
    val scriptWanUp: String = "",
    val scriptWanDown: String = "",
    val scriptShutdown: String = "",
    val scriptIpRules: String = "",
    
    // 2. Internet Detector / Watchdog (Advanced_InetDetect_Content.asp)
    val pingEnabled: Boolean = false,
    val pingHost1: String = "8.8.8.8",
    val pingHost2: String = "114.114.114.114",
    val pingPeriod: Int = 30,
    val pingAction: String = "0" // 0: Restart WAN, 1: Reboot router, 2: Run Custom script
)
