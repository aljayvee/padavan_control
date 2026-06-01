package com.example.padavancontrol

import com.example.padavancontrol.network.PadavanResponseParser
import org.junit.Test
import org.junit.Assert.*

class PadavanResponseParserTest {
    @Test
    fun testParseTrafficStats() {
        val html = """
            netdevs={
            'eth2':{rx:0x00000000,tx:0x00000000,rx_bytes:0x0000000000000000,tx_bytes:0x0000000000000000},
            'eth3':{rx:0x00000000,tx:0x00000000,rx_bytes:0x0000000000000000,tx_bytes:0x0000000000000000},
            'br0':{rx:0x000005C8,tx:0x0000021A,rx_bytes:0x00000000000005C8,tx_bytes:0x000000000000021A}
            };
        """.trimIndent()

        val stats = PadavanResponseParser.parseTrafficStats(html)
        println(stats)
        assertTrue(stats.isNotEmpty())
    }

    @Test
    fun testParseSystemStatus() {
        val html = """
            var si_new = {
                "cpu": {
                    "total": 10000,
                    "busy": 1000,
                    "user": 500,
                    "sys": 300,
                    "nice": 100,
                    "idle": 9000,
                    "irq": 50,
                    "sirq": 50
                },
                "ram": {
                    "total": 512000,
                    "free": 256000,
                    "cached": 128000,
                    "buffers": 64000
                },
                "swap": {
                    "total": 256000,
                    "used": 0
                },
                "lavg": "0.05 0.12 0.08",
                "uptime": {
                    "days": 1,
                    "hours": 2,
                    "minutes": 3,
                    "seconds": 4
                },
                "cpu_temp": "52°C",
                "wifi_temp": "43°C"
            };
        """.trimIndent()

        val status = PadavanResponseParser.parseSystemStatus(html)
        assertNotNull(status)
        assertEquals(10000L, status!!.cpuTotal)
        assertEquals(1000L, status.cpuBusy)
        assertEquals(500L, status.cpuUserTicks)
        assertEquals(300L, status.cpuSysTicks)
        assertEquals(100L, status.cpuNiceTicks)
        assertEquals(9000L, status.cpuIdleTicks)
        assertEquals(50L, status.cpuIrqTicks)
        assertEquals(50L, status.cpuSirqTicks)
        assertEquals(512000L * 1024, status.ramTotal)
        assertEquals(256000L * 1024, status.ramFree)
        assertEquals(128000L * 1024, status.ramCached)
        assertEquals(64000L * 1024, status.ramBuffers)
        assertEquals(256000L * 1024, status.swapTotal)
        assertEquals(0L, status.swapUsed)
        assertEquals("0.05 0.12 0.08", status.loadAvg)
        val expectedUptime = 1L * 86400 + 2L * 3600 + 3L * 60 + 4L
        assertEquals(expectedUptime, status.uptime)
        assertEquals(52f, status.cpuTemp!!, 0.001f)
        assertEquals(43f, status.wifiTemp!!, 0.001f)
    }

    @Test
    fun testParseLanClientsClassification() {
        val html = """
            var ipmonitor = [
                ['192.168.2.10', 'AA:BB:CC:DD:EE:FF', 'iPhone-5G', '1', '0', '0'],
                ['192.168.2.11', '11:22:33:44:55:66', 'Android-2G', '1', '0', '0'],
                ['192.168.2.12', '55:66:77:88:99:AA', 'Desktop-Wired', '1', '0', '0'],
                ['192.168.2.13', 'FF:EE:DD:CC:BB:AA', 'Offline-Device', '1', '0', '1']
            ];
            var wireless = {};
            var list_of_BlockedClient = [];
        """.trimIndent()

        val w2gHtml = """
            11:22:33:44:55:66  associated  signal -55dBm
        """.trimIndent()

        val w5gHtml = """
            AA:BB:CC:DD:EE:FF  associated  signal -40dBm
        """.trimIndent()

        val clients = PadavanResponseParser.parseLanClients(html, w2gHtml, w5gHtml)
        assertEquals(4, clients.size)

        val client5G = clients.find { it.macAddress == "AA:BB:CC:DD:EE:FF" }
        assertNotNull(client5G)
        assertEquals("5G", client5G!!.connectionType)

        val client2G = clients.find { it.macAddress == "11:22:33:44:55:66" }
        assertNotNull(client2G)
        assertEquals("2.4G", client2G!!.connectionType)

        val clientWired = clients.find { it.macAddress == "55:66:77:88:99:AA" }
        assertNotNull(clientWired)
        assertEquals("Wired", clientWired!!.connectionType)

        val clientOffline = clients.find { it.macAddress == "FF:EE:DD:CC:BB:AA" }
        assertNotNull(clientOffline)
        assertFalse(clientOffline!!.isOnline)
    }

    @Test
    fun testParseHardwareModel_variousFormats() {
        val htmlDoubleQuotes = """
            var product_id = "NEWIFI3";
            var some_other_val = "123";
        """.trimIndent()
        assertEquals("NEWIFI3", PadavanResponseParser.parseHardwareModel(htmlDoubleQuotes))

        val htmlSingleQuotes = """
            var product_id = 'MI-R3G';
        """.trimIndent()
        assertEquals("MI-R3G", PadavanResponseParser.parseHardwareModel(htmlSingleQuotes))

        val htmlVaryingSpaces = """
            var   product_id   =   "RT-N56U"  ;
        """.trimIndent()
        assertEquals("RT-N56U", PadavanResponseParser.parseHardwareModel(htmlVaryingSpaces))

        val htmlNoVarKeyword = """
            product_id = "K2P"
        """.trimIndent()
        assertEquals("K2P", PadavanResponseParser.parseHardwareModel(htmlNoVarKeyword))

        val htmlModelNameFallback = """
            var model_name = "Xiaomi-R3G";
        """.trimIndent()
        assertEquals("Xiaomi-R3G", PadavanResponseParser.parseHardwareModel(htmlModelNameFallback))

        val htmlProductidFallback = """
            var productid = "RT-N56U_B1";
        """.trimIndent()
        assertEquals("RT-N56U_B1", PadavanResponseParser.parseHardwareModel(htmlProductidFallback))

        val htmlNone = """
            var something_else = "NEWIFI3";
        """.trimIndent()
        assertNull(PadavanResponseParser.parseHardwareModel(htmlNone))
    }
}
