package com.example.padavancontrol.data.models

data class PortForwardRule(
    val name: String,
    val extPort: String,
    val intIp: String,
    val intPort: String,
    val protocol: String,
    val desc: String = ""
)

data class WanConfig(
    // WAN General settings (Advanced_WAN_Content.asp)
    val wanProto: String = "dhcp", // pppoe, dhcp, static, etc.
    val wanIpAddr: String = "",
    val wanNetmask: String = "",
    val wanGateway: String = "",
    val wanDnsEnable: Boolean = true,
    val wanDns1: String = "",
    val wanDns2: String = "",
    val hwNatMode: String = "1", // 0=TCP LAN, 1=TCP LAN/WLAN, 3=TCP/UDP LAN, 4=TCP/UDP LAN/WLAN, 2=Disable
    val sfeEnable: String = "0", // 0=Disable, 1=IPv4/IPv6, 2=IPv4/IPv6/WiFi
    val gwArpPing: Boolean = false,
    val wanAuthMode: String = "0",
    val wanHostname: String = "",
    val wanVci: String = "",
    val wanHwaddr: String = "",
    val wanTtlFix: Boolean = false,
    val wanTtlValue: String = "64",
    val wanStbPort: String = "0",
    val wanStbIso: String = "0",
    val vlanFilter: Boolean = false,
    val vlanVidCpu: String = "",
    val vlanPriCpu: String = "",
    val vlanVidIptv: String = "",
    val vlanPriIptv: String = "",
    
    // PPPoE Connection details
    val pppoeUser: String = "",
    val pppoePass: String = "",
    val pppoeMru: Int = 1492,
    val pppoeMtu: Int = 1492,
    val pppoeService: String = "",
    val pppoeAcName: String = "",
    
    // Port Forwarding / NAT settings (Advanced_VirtualServer_Content.asp)
    val upnpEnabled: Boolean = true,
    val upnpProto: String = "0",
    val upnpSecure: Boolean = true,
    val upnpEportMin: String = "1024",
    val upnpEportMax: String = "65535",
    val upnpIportMin: String = "1024",
    val upnpIportMax: String = "65535",
    val upnpCleanInt: String = "3600",
    val upnpCleanMin: String = "10",
    val portForwardEnabled: Boolean = false,
    val portForwardRules: List<PortForwardRule> = emptyList(),
    
    // DMZ host
    val dmzEnabled: Boolean = false,
    val dmzIp: String = "",
    val dmzSpBattle: Boolean = false,
    
    // DDNS
    val ddnsEnabled: Boolean = false,
    val ddnsServer: String = "",
    val ddnsUser: String = "",
    val ddnsPass: String = "",
    val ddnsHostName: String = "",
    val ddnsHostName2: String = "",
    val ddnsHostName3: String = "",
    val ddnsSsl: String = "0",
    val ddnsWildcard: String = "0",
    val ddns2Server: String = "",
    val ddns2HostName: String = "",
    val ddns2User: String = "",
    val ddns2Pass: String = "",
    val ddns2Ssl: String = "0",
    val ddns2Wildcard: String = "0",
    val ddnsSource: String = "0",
    val ddnsCheckIp: String = "",
    val ddns2CheckIp: String = "",
    val ddnsPeriod: String = "3600",
    val ddnsForced: String = "21600",
    val ddnsIpv6: String = "0",
    val ddnsVerbose: String = "0",
    
    // IPv6
    val ipv6Enabled: Boolean = false,
    val ipv6Proto: String = "dhcp6", // dhcp6, static6, 6to4, 6rd, etc.
    val ipv6WanDhcp: String = "0",
    val ipv6DnsAuto: Boolean = true,
    val ipv6Dns1: String = "",
    val ipv6Dns2: String = "",
    val ipv6Dns3: String = "",
    val ipv6Mtu: Int = 1500,
    val ipv66to4Relay: String = "192.88.99.1",
    val ipv66to4Mtu: Int = 1480,
    val ipv66rdPrefix: String = "",
    val ipv66rdPrefixLen: Int = 32,
    val ipv66rdRouter: String = "",
    val ipv66rdIp4Mtu: Int = 0,
    val ipv6LanAuto: Boolean = true,
    val ipv6LanRadv: Boolean = true,
    val ipv6LanDhcp: String = "0",
    val ipv6LanSfps: String = "1",
    val ipv6LanSfpe: String = "254",
    val ipv6LanSflt: String = "86400",
    
    // UI Loading state variables
    val loadProgress: Float = 0f,
    val loadStatus: String = ""
)
