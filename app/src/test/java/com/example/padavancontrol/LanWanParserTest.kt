package com.example.padavancontrol

import com.example.padavancontrol.network.PadavanResponseParser
import org.junit.Assert.*
import org.junit.Test

class LanWanParserTest : ParserTestBase() {

    @Test
    fun testParseLanConfig() {
        val mockHtml = getMockHtml("""
            <input type="hidden" name="lan_ipaddr" value="192.168.2.1">
            <input type="hidden" name="lan_netmask" value="255.255.255.0">
            <input type="hidden" name="lan_gateway" value="192.168.2.254">
            <input type="hidden" name="lan_stp" value="1">
            
            <input type="hidden" name="dhcp_enable_x" value="1">
            <input type="hidden" name="lan_domain" value="home.lan">
            <input type="hidden" name="dhcp_start" value="192.168.2.10">
            <input type="hidden" name="dhcp_end" value="192.168.2.200">
            <input type="hidden" name="dhcp_lease" value="86400">
            <input type="hidden" name="dhcp_gateway_x" value="192.168.2.1">
            <input type="hidden" name="dhcp_dns1_x" value="8.8.8.8">
            <input type="hidden" name="dhcp_dns2_x" value="1.1.1.1">
            <input type="hidden" name="dhcp_static_x" value="1">
            <input type="hidden" name="dhcp_static_arp" value="0">

            <script>
            var m_dhcp = [["00:11:22:33:44:55", "192.168.2.100", "MyPhone"], ["aa:bb:cc:dd:ee:ff", "192.168.2.101", "MyPC"]];
            var GWStaticList = [["10.0.0.0", "255.255.255.0", "192.168.2.10", "1", "LAN"], ["172.16.0.0", "255.240.0.0", "192.168.2.20", "2", "WAN"]];
            </script>

            <input type="hidden" name="mr_enable_x" value="1">
            <input type="hidden" name="ether_igmp" value="0">
            <input type="hidden" name="ether_green" value="1">
            <input type="hidden" name="ether_eee" value="0">
        """.trimIndent())

        val config = PadavanResponseParser.parseLanConfig(mockHtml)

        assertNotNull(config)
        assertEquals("192.168.2.1", config.lanIpAddr)
        assertEquals("255.255.255.0", config.lanNetmask)
        assertEquals("192.168.2.254", config.lanGateway)
        assertTrue(config.lanStp)

        assertTrue(config.dhcpEnabled)
        assertEquals("home.lan", config.dhcpDomain)
        assertEquals("192.168.2.10", config.dhcpStart)
        assertEquals("192.168.2.200", config.dhcpEnd)
        assertEquals(86400L, config.dhcpLease)
        assertEquals("192.168.2.1", config.dhcpGateway)
        assertEquals("8.8.8.8", config.dhcpDns1)
        assertEquals("1.1.1.1", config.dhcpDns2)
        assertTrue(config.dhcpStaticEnabled)
        assertFalse(config.dhcpStaticArp)

        assertEquals(2, config.staticLeases.size)
        assertEquals("00:11:22:33:44:55", config.staticLeases[0].mac)
        assertEquals("192.168.2.100", config.staticLeases[0].ip)
        assertEquals("MyPhone", config.staticLeases[0].name)

        assertEquals("aa:bb:cc:dd:ee:ff", config.staticLeases[1].mac)
        assertEquals("192.168.2.101", config.staticLeases[1].ip)
        assertEquals("MyPC", config.staticLeases[1].name)

        assertTrue(config.mrEnable)
        assertFalse(config.igmpSnoop)

        assertEquals(2, config.staticRoutes.size)
        assertEquals("10.0.0.0", config.staticRoutes[0].destIp)
        assertEquals("255.255.255.0", config.staticRoutes[0].netmask)
        assertEquals("192.168.2.10", config.staticRoutes[0].gateway)
        assertEquals(1, config.staticRoutes[0].metric)
        assertEquals("LAN", config.staticRoutes[0].interfaceName)

        assertTrue(config.greenEthernet)
        assertFalse(config.eeeEnabled)
    }

    @Test
    fun testParseWanConfig() {
        val mockHtml = getMockHtml("""
            <input type="hidden" name="wan_proto" value="pppoe">
            <input type="hidden" name="wan_ipaddr" value="100.64.12.34">
            <input type="hidden" name="wan_netmask" value="255.255.255.255">
            <input type="hidden" name="wan_gateway" value="100.64.12.1">
            <input type="hidden" name="wan_dnsenable_x" value="0">
            <input type="hidden" name="wan_dns1_x" value="8.8.4.4">
            <input type="hidden" name="wan_dns2_x" value="9.9.9.9">

            <input type="hidden" name="wan_pppoe_username" value="myuser">
            <input type="hidden" name="wan_pppoe_passwd" value="mypass">
            <input type="hidden" name="wan_pppoe_mru" value="1480">
            <input type="hidden" name="wan_pppoe_mtu" value="1480">

            <input type="hidden" name="upnp_enable_x" value="1">
            <input type="hidden" name="upnp_secure" value="1">
            <input type="hidden" name="vts_enable_x" value="1">

            <script>
            var VSList = [["Web Server", "80", "192.168.2.50", "80", "TCP", "HTTP Forward"], ["SSH", "2222", "192.168.2.60", "22", "BOTH", "SSH Link"]];
            </script>

            <input type="hidden" name="dmz_enable_x" value="1">
            <input type="hidden" name="dmz_ipaddr" value="192.168.2.99">

            <input type="hidden" name="ddns_enable_x" value="1">
            <select name="ddns_server_x"><option value="WWW.NO-IP.COM" selected>no-ip.com</option></select>
            <input type="hidden" name="ddns_username_x" value="ddnsuser">
            <input type="hidden" name="ddns_passwd_x" value="ddnspass">
            <input type="hidden" name="ddns_hostname_x" value="myhome.ddns.net">

            <select name="ip6_service"><option value="dhcp6" selected>Native DHCPv6</option></select>
        """.trimIndent())

        val config = PadavanResponseParser.parseWanConfig(mockHtml)

        assertNotNull(config)
        assertEquals("pppoe", config.wanProto)
        assertEquals("100.64.12.34", config.wanIpAddr)
        assertEquals("255.255.255.255", config.wanNetmask)
        assertEquals("100.64.12.1", config.wanGateway)
        assertFalse(config.wanDnsEnable)
        assertEquals("8.8.4.4", config.wanDns1)
        assertEquals("9.9.9.9", config.wanDns2)

        assertEquals("myuser", config.pppoeUser)
        assertEquals("mypass", config.pppoePass)
        assertEquals(1480, config.pppoeMru)
        assertEquals(1480, config.pppoeMtu)

        assertTrue(config.upnpEnabled)
        assertTrue(config.upnpSecure)
        assertTrue(config.portForwardEnabled)

        assertEquals(2, config.portForwardRules.size)
        assertEquals("Web Server", config.portForwardRules[0].name)
        assertEquals("80", config.portForwardRules[0].extPort)
        assertEquals("192.168.2.50", config.portForwardRules[0].intIp)
        assertEquals("80", config.portForwardRules[0].intPort)
        assertEquals("TCP", config.portForwardRules[0].protocol)
        assertEquals("HTTP Forward", config.portForwardRules[0].desc)

        assertTrue(config.dmzEnabled)
        assertEquals("192.168.2.99", config.dmzIp)

        assertTrue(config.ddnsEnabled)
        assertEquals("WWW.NO-IP.COM", config.ddnsServer)
        assertEquals("ddnsuser", config.ddnsUser)
        assertEquals("ddnspass", config.ddnsPass)
        assertEquals("myhome.ddns.net", config.ddnsHostName)

        assertTrue(config.ipv6Enabled)
        assertEquals("dhcp6", config.ipv6Proto)
    }

    @Test
    fun testWanConfigPersistenceMerge() {
        // 1. Initial full WAN parsing
        val initialHtml = getMockHtml("""
            <input type="hidden" name="wan_proto" value="pppoe">
            <input type="hidden" name="wan_pppoe_username" value="original_user">
            <input type="hidden" name="wan_pppoe_passwd" value="original_pass">
            <input type="hidden" name="dmz_enable_x" value="0">
            <script>
            var VSList = [["SSH", "22", "192.168.2.10", "22", "TCP", "SSH Forward"]];
            </script>
        """.trimIndent())

        val initialConfig = PadavanResponseParser.parseWanConfig(initialHtml)
        assertEquals("pppoe", initialConfig.wanProto)
        assertEquals("original_user", initialConfig.pppoeUser)
        assertEquals("original_pass", initialConfig.pppoePass)
        assertFalse(initialConfig.dmzEnabled)
        assertEquals(1, initialConfig.portForwardRules.size)

        // 2. DMZ sub-page fetch where PPPoE credentials and port forwards are completely missing in HTML
        val subPageHtml = getMockHtml("""
            <input type="hidden" name="dmz_enable_x" value="1">
            <input type="hidden" name="dmz_ipaddr" value="192.168.2.99">
        """.trimIndent())

        // 3. Parse passing existingConfig as fallback
        val mergedConfig = PadavanResponseParser.parseWanConfig(subPageHtml, initialConfig)

        // DMZ details should update successfully
        assertTrue(mergedConfig.dmzEnabled)
        assertEquals("192.168.2.99", mergedConfig.dmzIp)

        // PPPoE and port forwards from initial config should be kept instead of wiped to default empty/null states
        assertEquals("pppoe", mergedConfig.wanProto)
        assertEquals("original_user", mergedConfig.pppoeUser)
        assertEquals("original_pass", mergedConfig.pppoePass)
        assertEquals(1, mergedConfig.portForwardRules.size)
        assertEquals("SSH", mergedConfig.portForwardRules[0].name)
    }
}
