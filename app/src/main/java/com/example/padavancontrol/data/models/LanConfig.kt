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

data class SwitchPortConfig(
    val portName: String, // "WAN", "LAN1", "LAN2", "LAN3", "LAN4"
    val flowControl: Int = 0, // 0: TX/RX, 1: TX (Asymmetric Pause), 2: Disabled
    val speedDuplex: Int = 0, // 0: Auto, 1: 1000M FD, 2: 100M FD, etc.
    val linkState: String = "No link"
)

data class WolDevice(
    val mac: String,
    val name: String,
    val vendor: String = ""
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
    val dhcpDnsv6: String = "",
    val dhcpVerbose: Int = 0,
    val dnsmasqDnsmasqConf: String = "",
    val dnsmasqDhcpConf: String = "",
    val dnsmasqHosts: String = "",
    val dhcpStaticEnabled: Boolean = false,
    val dhcpStaticArp: Boolean = false,
    val staticLeases: List<StaticLease> = emptyList(),
    
    // IPTV (Advanced_IPTV_Content.asp)
    val mrEnable: Boolean = false,
    val forceIgmp: Int = 0,
    val udpxyPort: Int = 0,
    val xupnpdPort: Int = 0,
    val xupnpdUdpxy: Boolean = false,
    val igmpSnoop: Boolean = false,
    val etherM2u: Int = 2,
    val rtIgmpSnEnable: Int = 1,
    val wlIgmpSnEnable: Int = 1,
    val controlrateBroadcast: Int = 0,
    val mrUnsafe: Boolean = false,
    
    // Static Routes
    val useDhcpRoutes: Boolean = false,
    val routeEnabled: Boolean = false,
    val staticRoutes: List<StaticRoute> = emptyList(),
    
    // Switch
    val greenEthernet: Boolean = false,
    val eeeEnabled: Boolean = false,
    val etherJumbo: Int = 0,
    val portsConfig: List<SwitchPortConfig> = listOf(
        SwitchPortConfig("WAN", 0, 0),
        SwitchPortConfig("LAN1", 0, 0),
        SwitchPortConfig("LAN2", 0, 0),
        SwitchPortConfig("LAN3", 0, 0),
        SwitchPortConfig("LAN4", 0, 0)
    ),
    
    // Wake-on-LAN
    val wolMac: String = "",
    val wolDevices: List<WolDevice> = emptyList()
)
