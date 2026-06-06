package com.example.padavancontrol.data.models

data class ScriptConfig(
    // 1. Bash Startup & Shutdown Customization scripts (Advanced_Scripts_Content.asp)
    val scriptInit: String = "",
    val scriptStart: String = "",
    val scriptStartup: String = "", // Keep for compatibility
    val scriptWanUp: String = "",
    val scriptWanDown: String = "",
    val scriptShutdown: String = "",
    val scriptIpRules: String = "",
    val scriptEzButton: String = "",
    
    // 2. Internet Detector / Watchdog (Advanced_InetDetect_Content.asp)
    val pingEnabled: Boolean = false,
    val pingPollMode: String = "0", // 0: Disabled, 1: Ping (ICMP Echo), 2: TCP Connection
    val pingHost1: String = "8.8.8.8",
    val pingHost2: String = "114.114.114.114",
    val pingHost3: String = "",
    val pingHost4: String = "",
    val pingHost5: String = "",
    val pingHost6: String = "",
    val pingPeriod: Int = 30, // Keep for compatibility
    val pingIntervalSuccess: Int = 30,
    val pingIntervalFail: Int = 10,
    val pingTimeout: Int = 5,
    val pingAction: String = "0" // 0: Restart WAN, 1: Reboot router, 2: Run Custom script
)
