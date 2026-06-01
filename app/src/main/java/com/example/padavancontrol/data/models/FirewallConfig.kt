package com.example.padavancontrol.data.models

data class ServiceFilterRule(
    val srcIp: String,
    val srcPort: String,
    val dstIp: String,
    val dstPort: String,
    val proto: String, // "TCP", "UDP", "OTHER", etc.
    val protoNo: String = ""
)

data class MacFilterRule(
    val mac: String,
    val time: String, // "00002359" format
    val date: String  // "1111111" format (Mon-Sun bitmask)
)

data class FirewallConfig(
    // 1. General Security (Advanced_BasicFirewall_Content.asp)
    val fwEnabled: Boolean = true,
    val fwDosEnabled: Boolean = false,
    val fwSynCookEnabled: Boolean = false,
    val fwLogMode: String = "none", // none, drop, accept, both
    val miscPingEnabled: Boolean = true,
    val miscHttpEnabled: Boolean = false,
    val miscHttpPort: String = "80",
    val httpsWopen: Boolean = false,
    val httpsWport: String = "443",
    val sshdWopen: Boolean = false,
    val sshdWport: String = "22",
    val sshdWbfp: String = "2", // brute force protection level
    val ftpdWopen: Boolean = false,
    val ftpdWport: String = "21",
    val udpxyWopen: Boolean = false,
    val udpxyWport: String = "1024",
    val trmdRopen: Boolean = false,
    val ariaRopen: Boolean = false,

    // 2. Netfilter Settings (Advanced_Netfilter_Content.asp)
    val wanNatEnabled: Boolean = true,
    val nfMaxConn: String = "32768",
    val nfNatType: String = "2", // 0: Cone, 1: Full Cone, 2: Hybrid
    val nfNatLoopback: Boolean = true,
    val fwPtPppoe: Boolean = false,
    val nfAlgFtp0: String = "21",
    val nfAlgFtp1: String = "",
    val nfAlgPptp: Boolean = true,
    val nfAlgRtsp: Boolean = true,
    val nfAlgH323: Boolean = true,
    val nfAlgSip: Boolean = true,

    // 3. URL Filter (Advanced_URLFilter_Content.asp)
    val urlFilterEnabled: Boolean = false,
    val urlFilterDate: String = "1111111",
    val urlFilterTime: String = "00002359",
    val urlFilterMac: String = "",
    val urlFilterInvert: Boolean = false,
    val urlKeywords: List<String> = emptyList(),

    // 4. Hardware MAC Filter (Advanced_MACFilter_Content.asp)
    val macFilterMethod: String = "0", // 0: Disabled, 1: Accept, 2: Reject
    val fwMacDrop: Boolean = true,
    val macFilterRules: List<MacFilterRule> = emptyList(),

    // 5. Network Services Filter (Advanced_Firewall_Content.asp)
    val fwLwEnabled: Boolean = false,
    val filterLwDefault: String = "ACCEPT", // ACCEPT: Blacklist, DROP: Whitelist
    val filterLwDate: String = "1111111",
    val filterLwTime: String = "00002359",
    val filterLwIcmp: String = "",
    val serviceFilterRules: List<ServiceFilterRule> = emptyList()
)
