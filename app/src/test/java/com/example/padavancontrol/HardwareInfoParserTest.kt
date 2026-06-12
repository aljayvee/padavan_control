package com.example.padavancontrol

import com.example.padavancontrol.network.PadavanResponseParser
import org.junit.Assert.*
import org.junit.Test

class HardwareInfoParserTest : ParserTestBase() {

    @Test
    fun testParseEthernetPortsNoLink() {
        val mockMibHtml = """
- WAN:
Port Link			: No Link

MIB Counters
----------------------------------------
TxGoodOctets			: 0
TxUcastFrames			: 0
TxMcastFrames			: 0
TxBcastFrames			: 0
TxDropFrames			: 0
TxPauseFrames			: 0
TxCollisions			: 0
TxCRCError			: 0
RxGoodOctets			: 0
RxUcastFrames			: 0
RxMcastFrames			: 0
RxBcastFrames			: 0
RxDropFrames			: 0
RxPauseFrames			: 0
RxFilterFrames			: 0
RxCRCError			: 0
RxAligmentError			: 0

- LAN 1
Port Link			: No Link

MIB Counters
----------------------------------------
TxGoodOctets			: 10
TxUcastFrames			: 20
TxMcastFrames			: 30
TxBcastFrames			: 40
TxDropFrames			: 50
TxPauseFrames			: 60
TxCollisions			: 70
TxCRCError			: 80
RxGoodOctets			: 90
RxUcastFrames			: 100
RxMcastFrames			: 110
RxBcastFrames			: 120
RxDropFrames			: 130
RxPauseFrames			: 140
RxFilterFrames			: 150
RxCRCError			: 160
RxAligmentError			: 170

- LAN 2:
Port Link			: 100M Full

MIB Counters
----------------------------------------
TxGoodOctets			: 999
        """.trimIndent()

        val ports = PadavanResponseParser.parseEthernetPorts(mockMibHtml)
        assertEquals(3, ports.size)

        // WAN
        val wan = ports[0]
        assertEquals("WAN", wan.portName)
        assertEquals("No Link", wan.portLink)
        assertEquals(0L, wan.mibCounters.txGoodOctets)
        assertEquals(0L, wan.mibCounters.rxCrcError)

        // LAN 1
        val lan1 = ports[1]
        assertEquals("LAN 1", lan1.portName)
        assertEquals("No Link", lan1.portLink)
        assertEquals(10L, lan1.mibCounters.txGoodOctets)
        assertEquals(20L, lan1.mibCounters.txUcastFrames)
        assertEquals(30L, lan1.mibCounters.txMcastFrames)
        assertEquals(40L, lan1.mibCounters.txBcastFrames)
        assertEquals(50L, lan1.mibCounters.txDropFrames)
        assertEquals(60L, lan1.mibCounters.txPauseFrames)
        assertEquals(70L, lan1.mibCounters.txCollisions)
        assertEquals(80L, lan1.mibCounters.txCrcError)
        assertEquals(90L, lan1.mibCounters.rxGoodOctets)
        assertEquals(100L, lan1.mibCounters.rxUcastFrames)
        assertEquals(110L, lan1.mibCounters.rxMcastFrames)
        assertEquals(120L, lan1.mibCounters.rxBcastFrames)
        assertEquals(130L, lan1.mibCounters.rxDropFrames)
        assertEquals(140L, lan1.mibCounters.rxPauseFrames)
        assertEquals(150L, lan1.mibCounters.rxFilterFrames)
        assertEquals(160L, lan1.mibCounters.rxCrcError)
        assertEquals(170L, lan1.mibCounters.rxAlignmentError)

        // LAN 2
        val lan2 = ports[2]
        assertEquals("LAN 2", lan2.portName)
        assertEquals("100M Full", lan2.portLink)
        assertEquals(999L, lan2.mibCounters.txGoodOctets)
    }

    @Test
    fun testParseWirelessStatus2g() {
        val mockText = """
MAC (AP Main)	: 20:76:93:53:F0:AE
Operation Mode	: AP
WPHY Mode	: 11b/g/n
Channel Main	: 9

AP Main Stations List
MAC                AID PSM MimoPS MCS BW SGI STBC TRate RSSI
1E:95:FD:CB:03:E7    1  NO  YES     7 20M  NO  YES  72.2M  -45
        """.trimIndent()

        val info = PadavanResponseParser.parseWirelessStatusText(mockText, "2.4G")
        assertEquals("2.4G", info.band)
        assertEquals("20:76:93:53:F0:AE", info.macApMain)
        assertEquals("", info.macApClient)
        assertEquals("AP", info.operationMode)
        assertEquals("11b/g/n", info.wphyMode)
        assertEquals(9, info.channelMain)
        assertNull(info.apClientConnection)
        
        val stationsClean = info.mainStationsListRaw.trim()
        assertTrue(stationsClean.contains("1E:95:FD:CB:03:E7"))
        assertTrue(stationsClean.contains("-45"))
        
        assertEquals(1, info.stations.size)
        val station = info.stations[0]
        assertEquals("1E:95:FD:CB:03:E7", station.mac)
        assertEquals(1, station.aid)
        assertEquals("NO", station.psm)
        assertEquals("YES", station.mimoPs)
        assertEquals(7, station.mcs)
        assertEquals("20M", station.bw)
        assertEquals("NO", station.sgi)
        assertEquals("YES", station.stbc)
        assertEquals("72.2M", station.tRate)
        assertEquals(-45, station.rssi)
    }

    @Test
    fun testParseWirelessStatus5g() {
        val mockText = """
MAC (AP Main)	: 20:76:93:53:F0:B0
MAC (AP-Client)	: 22:76:93:51:F0:B0
Operation Mode	: AP-Client + AP
WPHY Mode	: 11a/n/ac
Channel Main	: 108

AP-Client Connection
----------------------------------------
BSSID              PhyMode  BW MCS SGI LDPC STBC TRate RSSI 
30:40:74:A4:39:28  VHT     80M   3  NO  YES   NO  243M  -86

AP Main Stations List
        """.trimIndent()

        val info = PadavanResponseParser.parseWirelessStatusText(mockText, "5G")
        assertEquals("5G", info.band)
        assertEquals("20:76:93:53:F0:B0", info.macApMain)
        assertEquals("22:76:93:51:F0:B0", info.macApClient)
        assertEquals("AP-Client + AP", info.operationMode)
        assertEquals("11a/n/ac", info.wphyMode)
        assertEquals(108, info.channelMain)
        
        val conn = info.apClientConnection
        assertNotNull(conn)
        assertEquals("30:40:74:A4:39:28", conn?.bssid)
        assertEquals("VHT", conn?.phyMode)
        assertEquals("80M", conn?.bw)
        assertEquals(3, conn?.mcs)
        assertEquals("NO", conn?.sgi)
        assertEquals("YES", conn?.ldpc)
        assertEquals("NO", conn?.stbc)
        assertEquals("243M", conn?.tRate)
        assertEquals(-86, conn?.rssi)
        
        assertEquals("", info.mainStationsListRaw.trim())
    }
}
