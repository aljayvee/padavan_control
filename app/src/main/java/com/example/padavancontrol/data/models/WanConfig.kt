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
    
    // PPPoE Connection details
    val pppoeUser: String = "",
    val pppoePass: String = "",
    val pppoeMru: Int = 1492,
    val pppoeMtu: Int = 1492,
    val pppoeService: String = "",
    val pppoeAcName: String = "",
    
    // Port Forwarding / NAT settings (Advanced_VirtualServer_Content.asp)
    val upnpEnabled: Boolean = true,
    val upnpSecure: Boolean = true,
    val portForwardEnabled: Boolean = false,
    val portForwardRules: List<PortForwardRule> = emptyList(),
    
    // DMZ host
    val dmzEnabled: Boolean = false,
    val dmzIp: String = "",
    
    // DDNS
    val ddnsEnabled: Boolean = false,
    val ddnsServer: String = "",
    val ddnsUser: String = "",
    val ddnsPass: String = "",
    val ddnsHostName: String = "",
    
    // IPv6
    val ipv6Enabled: Boolean = false,
    val ipv6Proto: String = "dhcp6" // dhcp6, static6, 6to4, etc.
)
