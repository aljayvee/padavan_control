package com.example.padavancontrol.data.models

data class UsbShareConfig(
    // 1. Common Settings (Advanced_AiDisk_others.asp)
    val usb3Disable: Boolean = false,
    val hddSpindown: String = "0", // Spindown mode
    val hddApmOff: Boolean = false,
    val achkEnable: Boolean = true,
    val pcacheReclaim: String = "0",
    val optwEnable: String = "2", // 0: No, 1: Optware, 2: Entware
    val stMaxUser: String = "10",

    // 2. Samba Server Settings
    val enableSamba: Boolean = true,
    val sambaWorkgroup: String = "WORKGROUP",
    val sambaMode: String = "1", // Share modes
    val sambaLmb: String = "1",
    val sambaFp: String = "1",

    // 3. FTP Server Settings
    val enableFtp: Boolean = false,
    val ftpMode: String = "1",
    val ftpLog: Boolean = false,
    val ftpPmin: String = "1024",
    val ftpPmax: String = "65535",
    val ftpAnmr: String = "0", // Anonymous speed limit

    // 4. LPR Printer Server (Advanced_Printer_others.asp)
    val rawdEnable: String = "0", // 0: No, 1: Yes, 2: Bidirectional
    val lprdEnable: Boolean = true,
    val u2ecEnable: Boolean = false,

    // 5. 3G/4G USB Modem dials (Advanced_Modem_others.asp)
    val modemRule: Boolean = false,
    val modemType: String = "3", // NDIS LTE
    val modemCountry: String = "",
    val modemIsp: String = "",
    val modemApn: String = "",
    val modemPin: String = "",
    val modemDialnum: String = "*99#",
    val modemUser: String = "",
    val modemPass: String = "",
    val modemNets: String = "0", // Auto
    val modemMtu: Int = 1500,
    val modemDnsAuto: Boolean = true,
    val wanDns1: String = "",
    val wanDns2: String = "",
    val wanDns3: String = "",
    val modemNode: String = "0",
    val modemCmd: String = "",
    val modemZcd: String = "0"
)
