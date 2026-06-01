package com.example.padavancontrol.data.models

data class AdminConfig(
    // 1. System General Settings (Advanced_System_Content.asp)
    val adminUser: String = "admin",
    val adminPass: String = "",
    val timezone: String = "GMT+8",
    val ntpServer1: String = "pool.ntp.org",
    val ntpServer2: String = "time.nist.gov",
    
    // 2. Services Configuration (Advanced_Services_Content.asp)
    val enableTelnet: Boolean = true,
    val telnetPort: String = "23",
    val enableSsh: Boolean = true,
    val sshPort: String = "22",
    val enableSftp: Boolean = true,
    val enableWebdav: Boolean = false,
    
    // 3. Operation Mode (Advanced_OperationMode_Content.asp)
    val opMode: String = "1", // 1: Router, 2: AP, 3: WDS / Bridge
    
    // 4. Hardware buttons / LED (Advanced_Buttons_LED_Content.asp)
    val btnWpsMode: String = "0", // 0: WPS, 1: Toggle Radio, 2: Toggle LED
    val ledPowerMode: String = "0", // 0: Normal, 1: Always Off
    
    // 5. Firmware version info
    val firmwareVersion: String = "3.4.3.9-099",
    val buildDate: String = "2024-05-15"
)
