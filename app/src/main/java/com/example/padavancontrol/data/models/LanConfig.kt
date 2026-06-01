package com.example.padavancontrol.data.models

data class StaticLease(
    val mac: String,
    val ip: String,
    val name: String
)

data class StaticRoute(
    val destIp: String,
    val netmask: String,
    val gateway: String,
    val metric: Int,
    val interfaceName: String
)

data class LanConfig(
    // LAN IP settings (Advanced_LAN_Content.asp)
    val lanIpAddr: String = "192.168.2.1",
    val lanNetmask: String = "255.255.255.0",
    val lanGateway: String = "",
    val lanStp: Boolean = false,
    
    // DHCP Server settings (Advanced_DHCP_Content.asp)
    val dhcpEnabled: Boolean = true,
    val dhcpDomain: String = "",
    val dhcpStart: String = "192.168.2.2",
    val dhcpEnd: String = "192.168.2.244",
    val dhcpLease: Long = 86400L,
    val dhcpGateway: String = "",
    val dhcpDns1: String = "",
    val dhcpDns2: String = "",
    val dhcpDns3: String = "",
    val dhcpWins: String = "",
    val dhcpStaticEnabled: Boolean = false,
    val dhcpStaticArp: Boolean = false,
    val staticLeases: List<StaticLease> = emptyList(),
    
    // IPTV (Advanced_IPTV_Content.asp)
    val mrEnable: Boolean = false,
    val igmpSnoop: Boolean = false,
    val mrUnsafe: Boolean = false,
    
    // Static Routes
    val routeEnabled: Boolean = false,
    val staticRoutes: List<StaticRoute> = emptyList(),
    
    // Switch
    val greenEthernet: Boolean = false,
    val eeeEnabled: Boolean = false
)
