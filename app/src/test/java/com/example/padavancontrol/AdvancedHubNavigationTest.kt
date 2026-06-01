package com.example.padavancontrol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AdvancedHubNavigationTest : ParserTestBase() {

    @Test
    fun testAdvancedNavigationKeysDistinct() {
        val keys = listOf(
            AdvancedHub,
            Wifi2gGeneral, Wifi2gGuest, Wifi2gBridge, Wifi2gMacFilter, Wifi2gRadius, Wifi2gProfessional,
            Wifi5gGeneral, Wifi5gGuest, Wifi5gBridge, Wifi5gMacFilter, Wifi5gRadius, Wifi5gProfessional,
            LanIp, LanDhcp, LanRoute, LanIptv, LanSwitch, LanWol,
            WanConnection, WanIpv6, WanPortForward, WanDmz, WanDdns,
            FirewallGeneral, FirewallNetfilter, FirewallUrlFilter, FirewallMacFilter, FirewallServicesFilter,
            UsbCommon, UsbSamba, UsbFtp, UsbModem, UsbPrinter,
            AdminSystem, AdminServices, AdminOpMode, AdminFirmware, AdminSettingsBackup, AdminConsole, AdminButtonsLed,
            CustomScripts, CustomDetector
        )

        // Verify all 43 navigation keys are defined and unique
        assertEquals(43, keys.size)
        assertEquals(43, keys.distinct().size)
    }

    @Test
    fun testRouteDescriptionsMap() {
        val menuTitles = mapOf(
            Wifi2gGeneral to "Wireless 2.4GHz - General Settings",
            LanDhcp to "LAN - DHCP Server Config",
            WanConnection to "WAN - Internet Connection",
            CustomScripts to "Customization - Bash Startup Scripts"
        )

        // Verify type-safe matching maps correctly
        assertNotNull(menuTitles[Wifi2gGeneral])
        assertEquals("Wireless 2.4GHz - General Settings", menuTitles[Wifi2gGeneral])
        assertEquals("LAN - DHCP Server Config", menuTitles[LanDhcp])
        assertEquals("WAN - Internet Connection", menuTitles[WanConnection])
        assertEquals("Customization - Bash Startup Scripts", menuTitles[CustomScripts])
    }
}
