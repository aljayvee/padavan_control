package com.example.padavancontrol.data.models

data class AdminConfig(
    // 1. System General Settings (Advanced_System_Content.asp)
    val adminUser: String = "admin",
    val adminPass: String = "",
    val timezone: String = "GMT+8",
    val ntpServer1: String = "pool.ntp.org",
    val ntpServer2: String = "time.nist.gov",
    val deviceName: String = "Router",
    val ntpPeriod: String = "24",
    val logIpAddr: String = "",
    val logPort: String = "514",
    val logFloatUi: String = "1",
    val selectLang: String = "EN",
    val helpEnable: Boolean = true,
    
    // 2. Services Configuration (Advanced_Services_Content.asp)
    val enableTelnet: Boolean = true,
    val telnetPort: String = "23",
    val enableSsh: Boolean = true,
    val sshdEnable: String = "1",
    val sshPort: String = "22",
    val enableSftp: Boolean = true,
    val enableWebdav: Boolean = false,
    val httpProto: String = "0",
    val httpLanPort: String = "80",
    val httpsLPort: String = "443",
    val httpAccess: String = "0",
    val winsEnable: Boolean = false,
    val stSambaWorkgroup: String = "WORKGROUP",
    val stSambaLmb: String = "1",
    val ttydEnable: Boolean = false,
    val ttydPort: String = "7681",
    val vlmcsdEnable: Boolean = false,
    val napt66Enable: Boolean = false,
    val lltdEnable: Boolean = false,
    val adscEnable: Boolean = false,
    val crondEnable: Boolean = false,
    val crontabLogin: String = "",
    val watchdogCpu: Boolean = false,
    
    // 3. Operation Mode (Advanced_OperationMode_Content.asp)
    val opMode: String = "1", // 1: Router, 2: AP, 3: WDS / Bridge
    
    // 4. Hardware buttons / LED (Advanced_Buttons_LED_Content.asp)
    val btnWpsShort: String = "0",
    val btnWpsLong: String = "0",
    val ledEnable: Boolean = true,
    val ledInternet: String = "0",
    val ledUsb: String = "0",
    val ledWifi: String = "0",
    val ledPower: String = "0",
    val ledEthernet: String = "0",
    
    // 5. Settings & Storage (Advanced_SettingBackup_Content.asp)
    val nvramManual: String = "0",
    val rstatsStored: String = "1",
    val stimeStored: String = "1",
    val mtdRwfsMount: String = "0",
    
    // 6. Firmware version info
    val firmwareVersion: String = "3.4.3.9-099",
    val buildDate: String = "2024-05-15",
    val productId: String = ""
)
