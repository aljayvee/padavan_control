package com.example.padavancontrol.data.models

data class MibCounters(
    val txGoodOctets: Long = 0,
    val txUcastFrames: Long = 0,
    val txMcastFrames: Long = 0,
    val txBcastFrames: Long = 0,
    val txDropFrames: Long = 0,
    val txPauseFrames: Long = 0,
    val txCollisions: Long = 0,
    val txCrcError: Long = 0,
    val rxGoodOctets: Long = 0,
    val rxUcastFrames: Long = 0,
    val rxMcastFrames: Long = 0,
    val rxBcastFrames: Long = 0,
    val rxDropFrames: Long = 0,
    val rxPauseFrames: Long = 0,
    val rxFilterFrames: Long = 0,
    val rxCrcError: Long = 0,
    val rxAlignmentError: Long = 0
)

data class EthernetPortInfo(
    val portName: String, // "WAN", "LAN 1", "LAN 2", "LAN 3", "LAN 4"
    val portLink: String, // "1000M" or "No Link", etc.
    val mibCounters: MibCounters
)

data class ApClientConnection(
    val bssid: String,
    val phyMode: String,
    val bw: String,
    val mcs: Int,
    val sgi: String,
    val ldpc: String,
    val stbc: String,
    val tRate: String,
    val rssi: Int
)

data class StationInfo(
    val mac: String,
    val aid: Int,
    val psm: String,
    val mimoPs: String,
    val mcs: Int,
    val bw: String,
    val sgi: String,
    val stbc: String,
    val tRate: String,
    val rssi: Int
)

data class WirelessSectionInfo(
    val band: String, // "2.4G" or "5G"
    val macApMain: String = "",
    val macApClient: String = "", // 5G only usually
    val operationMode: String = "",
    val wphyMode: String = "",
    val channelMain: Int = 0,
    val apClientConnection: ApClientConnection? = null,
    val mainStationsListRaw: String = "",
    val stations: List<StationInfo> = emptyList()
)

data class HardwareInfoData(
    val wireless2g: WirelessSectionInfo,
    val wireless5g: WirelessSectionInfo,
    val ports: List<EthernetPortInfo>
)
