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
}
