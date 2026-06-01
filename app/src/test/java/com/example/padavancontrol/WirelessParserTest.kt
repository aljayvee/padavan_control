package com.example.padavancontrol

import com.example.padavancontrol.network.PadavanResponseParser
import org.junit.Assert.*
import org.junit.Test

class WirelessParserTest : ParserTestBase() {

    @Test
    fun testParseWirelessConfig2G_General() {
        val mockHtml = getMockHtml("""
            <input type="hidden" name="rt_radio_date_x" value="1111111">
            <input type="hidden" name="rt_ssid2" value="ASUS_2G_TEST">
            <input type="hidden" name="rt_closed" value="1">
            <input type="hidden" name="rt_gmode" value="6">
            <input type="hidden" name="rt_HT_BW" value="1">
            <input type="hidden" name="rt_channel_orig" value="6">
            <input type="hidden" name="rt_auth_mode" value="psk">
            <input type="hidden" name="rt_wpa_mode" value="2">
            <input type="hidden" name="rt_crypto" value="aes">
            <input type="hidden" name="rt_wpa_psk_org" value="mypassword123">
            <input type="hidden" name="rt_TxPower" value="80">
            <input type="checkbox" id="rt_radio_x_fake" value="1 checked">
        """.trimIndent())

        val config = PadavanResponseParser.parseWirelessConfig(mockHtml, is5GHz = false)

        assertNotNull(config)
        assertTrue(config.isEnabled)
        assertEquals("ASUS_2G_TEST", config.ssid)
        assertTrue(config.isClosed)
        assertEquals("6", config.wirelessMode)
        assertEquals("1", config.bandwidth)
        assertEquals("6", config.channel)
        assertEquals("psk", config.authMode)
        assertEquals("2", config.wpaMode)
        assertEquals("aes", config.crypto)
        assertEquals("mypassword123", config.wpaPsk)
        assertEquals(80, config.txPower)
    }

    @Test
    fun testParseWirelessConfig5G_General() {
        val mockHtml = getMockHtml("""
            <input type="hidden" name="wl_radio_x" value="1">
            <input type="hidden" name="wl_ssid2" value="ASUS_5G_TEST%20SPEED">
            <input type="hidden" name="wl_closed" value="0">
            <input type="hidden" name="wl_gmode" value="5">
            <input type="hidden" name="wl_HT_BW" value="2">
            <input type="hidden" name="wl_channel" value="36">
            <input type="hidden" name="wl_auth_mode" value="open">
            <input type="hidden" name="wl_wpa_psk_org" value="">
            <input type="hidden" name="wl_TxPower" value="100">
        """.trimIndent())

        val config = PadavanResponseParser.parseWirelessConfig(mockHtml, is5GHz = true)

        assertNotNull(config)
        assertTrue(config.isEnabled)
        assertEquals("ASUS_5G_TEST SPEED", config.ssid)
        assertFalse(config.isClosed)
        assertEquals("5", config.wirelessMode)
        assertEquals("2", config.bandwidth)
        assertEquals("36", config.channel)
        assertEquals("open", config.authMode)
        assertEquals("", config.wpaPsk)
        assertEquals(100, config.txPower)
    }

    @Test
    fun testParseWirelessConfig2G_Guest() {
        val mockHtml = getMockHtml("""
            <input type="hidden" name="rt_guest_enable" value="1">
            <input type="hidden" name="rt_guest_ssid_org" value="ASUS_GUEST_2G">
            <input type="hidden" name="rt_guest_closed" value="1">
            <input type="hidden" name="rt_guest_lan_isolate" value="1">
            <input type="hidden" name="rt_guest_ap_isolate" value="0">
            <input type="hidden" name="rt_guest_auth_mode" value="psk">
            <input type="hidden" name="rt_guest_wpa_mode" value="2">
            <input type="hidden" name="rt_guest_crypto" value="aes">
            <input type="hidden" name="rt_guest_wpa_psk_org" value="guestpass123">
        """.trimIndent())

        val config = PadavanResponseParser.parseWirelessConfig(mockHtml, is5GHz = false)

        assertNotNull(config)
        assertTrue(config.guestEnabled)
        assertEquals("ASUS_GUEST_2G", config.guestSsid)
        assertTrue(config.guestClosed)
        assertTrue(config.guestLanIsolate)
        assertFalse(config.guestApIsolate)
        assertEquals("psk", config.guestAuthMode)
        assertEquals("2", config.guestWpaMode)
        assertEquals("aes", config.guestCrypto)
        assertEquals("guestpass123", config.guestWpaPsk)
    }

    @Test
    fun testParseWirelessConfig2G_WDS_APClient() {
        val mockHtml = getMockHtml("""
            <input type="hidden" name="rt_mode_x" value="3">
            <input type="hidden" name="rt_sta_wisp" value="1">
            <input type="hidden" name="rt_sta_ssid_org" value="RemoteRouterSSID">
            <input type="hidden" name="rt_sta_auth_mode" value="psk">
            <input type="hidden" name="rt_sta_wpa_mode" value="2">
            <input type="hidden" name="rt_sta_crypto" value="aes">
            <input type="hidden" name="rt_sta_wpa_psk_org" value="remotepass">
            <input type="hidden" name="rt_sta_auto" value="1">
            <input type="hidden" name="rt_channel" value="11">
        """.trimIndent())

        val config = PadavanResponseParser.parseWirelessConfig(mockHtml, is5GHz = false)

        assertNotNull(config)
        assertEquals("3", config.modeX)
        assertEquals("1", config.staWisp)
        assertEquals("RemoteRouterSSID", config.staSsid)
        assertEquals("psk", config.staAuthMode)
        assertEquals("2", config.staWpaMode)
        assertEquals("aes", config.staCrypto)
        assertEquals("remotepass", config.staWpaPsk)
        assertEquals("1", config.staAuto)
        assertEquals("11", config.channel)
    }

    @Test
    fun testParseWirelessConfig_Schedules_Professional_MACList() {
        val mockHtml = getMockHtml("""
            <input type="hidden" name="rt_radio_date_x" value="1111100">
            <input type="hidden" name="rt_radio_time_x" value="09001800">
            <input type="hidden" name="rt_radio_time2_x" value="10001700">
            
            <input type="hidden" name="rt_guest_date_x" value="0011111">
            <input type="hidden" name="rt_guest_time_x" value="08002000">
            <input type="hidden" name="rt_guest_time2_x" value="12001500">
            <input type="hidden" name="rt_guest_mcs_mode" value="3">
            <input type="hidden" name="rt_guest_macrule" value="1">
            
            <input type="hidden" name="rt_stream_tx" value="3">
            <input type="hidden" name="rt_stream_rx" value="3">
            <input type="hidden" name="rt_preamble" value="0">
            <input type="hidden" name="rt_frag" value="2000">
            <input type="hidden" name="rt_rts" value="2001">
            <input type="hidden" name="rt_dtim" value="2">
            <input type="hidden" name="rt_bcn" value="200">
            <input type="hidden" name="rt_mcs_mode" value="4">
            <input type="hidden" name="rt_country_code" value="CN">
            
            <input type="hidden" name="rt_wme" value="0">
            <input type="hidden" name="rt_ap_isolate" value="1">
            <input type="hidden" name="rt_TxBurst" value="0">
            <input type="hidden" name="rt_GreenAP" value="1">
            <input type="hidden" name="rt_PktAggregate" value="0">
            <input type="hidden" name="rt_HT_RDG" value="1">
            <input type="hidden" name="rt_HT_AutoBA" value="0">
            <input type="hidden" name="rt_HT_AMSDU" value="1">
            <input type="hidden" name="rt_APSDCapable" value="1">
            
            <input type="hidden" name="rt_macmode" value="accept">
            <script>
                var ACLList = [
                    ['11:22:33:44:55:66', 'Work Laptop'],
                    ['AA:BB:CC:DD:EE:FF', 'Work Phone']
                ];
            </script>
        """.trimIndent())

        val config = PadavanResponseParser.parseWirelessConfig(mockHtml, is5GHz = false)

        assertNotNull(config)
        // Schedules
        assertEquals("1111100", config.radioDate)
        assertEquals("09001800", config.radioTimeWorkweek)
        assertEquals("10001700", config.radioTimeWeekend)
        
        // Guest Schedules & advanced fields
        assertEquals("0011111", config.guestDate)
        assertEquals("08002000", config.guestTimeWorkweek)
        assertEquals("12001500", config.guestTimeWeekend)
        assertEquals("3", config.guestMcsMode)
        assertTrue(config.guestMacRule)
        
        // Professional Settings
        assertEquals("3", config.streamTx)
        assertEquals("3", config.streamRx)
        assertEquals("0", config.preamble)
        assertEquals(2000, config.fragThresh)
        assertEquals(2001, config.rtsThresh)
        assertEquals(2, config.dtimInterval)
        assertEquals(200, config.bcnInterval)
        assertEquals("4", config.mcsMode)
        assertEquals("CN", config.countryCode)
        
        assertFalse(config.wmmCapable)
        assertTrue(config.apIsolate)
        assertFalse(config.txBurst)
        assertTrue(config.greenAp)
        assertFalse(config.pktAggregate)
        assertTrue(config.htRdg)
        assertFalse(config.htAutoBA)
        assertTrue(config.htAmsdu)
        assertTrue(config.wmmApsd)
        
        // MAC Filter
        assertEquals("accept", config.macFilterMode)
        assertEquals(2, config.macFilterRules.size)
        assertEquals("11:22:33:44:55:66", config.macFilterRules[0].mac)
        assertEquals("Work Laptop", config.macFilterRules[0].desc)
        assertEquals("AA:BB:CC:DD:EE:FF", config.macFilterRules[1].mac)
        assertEquals("Work Phone", config.macFilterRules[1].desc)
    }

    @Test
    fun testParseWifiScan_WdsAplist() {
        val mockHtml = """
            var wds_aplist = [
                ['ASUS_2G_TEST', '11:22:33:44:55:66', '6', '85%', 'WPA2-PSK'],
                ['HomeWifi', 'AA:BB:CC:DD:EE:FF', '11', '90', 'WPA-PSK/WPA2-PSK']
            ];
        """.trimIndent()

        val networks = PadavanResponseParser.parseWifiScan(mockHtml)

        assertNotNull(networks)
        assertEquals(2, networks.size)
        assertEquals("ASUS_2G_TEST", networks[0].ssid)
        assertEquals("11:22:33:44:55:66", networks[0].bssid)
        assertEquals("6", networks[0].channel)
        assertEquals(85, networks[0].signal)
        assertEquals("WPA2-PSK", networks[0].security)

        assertEquals("HomeWifi", networks[1].ssid)
        assertEquals("AA:BB:CC:DD:EE:FF", networks[1].bssid)
        assertEquals("11", networks[1].channel)
        assertEquals(90, networks[1].signal)
        assertEquals("WPA-PSK/WPA2-PSK", networks[1].security)
    }
}

