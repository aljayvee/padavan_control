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
            <input type="hidden" name="dhcp_dns3_x" value="2.2.2.2">
            <input type="hidden" name="dhcp_wins_x" value="192.168.2.250">
            <input type="hidden" name="dhcp_dnsv6_x" value="2001:4860:4860::8888">
            <input type="hidden" name="dhcp_verbose" value="2">
            
            <textarea name="dnsmasq.dnsmasq.conf">
            no-resolv
            server=8.8.8.8
            </textarea>
            <textarea name="dnsmasq.dhcp.conf">
            dhcp-option=option:router,192.168.2.1
            </textarea>
            <textarea name="dnsmasq.hosts">
            192.168.2.5 myhost
            </textarea>

            <input type="hidden" name="dhcp_static_x" value="1">
            <input type="hidden" name="dhcp_static_arp" value="0">

            <script>
            var ipmonitor = [["192.168.2.100", "00:11:22:33:44:55", "MyPhone", "0", "1", "0"], ["192.168.2.102", "33:44:55:66:77:88", "WiredDevice", "0", "1", "0"]];
            var m_dhcp = [["00:11:22:33:44:55", "192.168.2.100", "MyPhone"], ["aa:bb:cc:dd:ee:ff", "192.168.2.101", "MyPC"]];
            var GWStaticList = [["10.0.0.0", "255.255.255.0", "192.168.2.10", "1", "LAN"], ["172.16.0.0", "255.240.0.0", "192.168.2.20", "2", "WAN"]];
            </script>

            <input type="hidden" name="mr_enable_x" value="1">
            <input type="hidden" name="force_igmp" value="2">
            <input type="hidden" name="udpxy_enable_x" value="4022">
            <input type="hidden" name="xupnpd_enable_x" value="4044">
            <input type="hidden" name="xupnpd_udpxy" value="1">
            <input type="hidden" name="ether_igmp" value="0">
            <input type="hidden" name="ether_m2u" value="1">
            <input type="hidden" name="rt_IgmpSnEnable" value="0">
            <input type="hidden" name="wl_IgmpSnEnable" value="1">
            <input type="hidden" name="controlrate_broadcast" value="10">

            <input type="hidden" name="dr_enable_x" value="1">
            <input type="hidden" name="sr_enable_x" value="1">

            <input type="hidden" name="ether_green" value="1">
            <input type="hidden" name="ether_eee" value="0">
            <input type="hidden" name="ether_jumbo" value="1">
            
            <input type="hidden" name="ether_flow_wan" value="1">
            <input type="hidden" name="ether_link_wan" value="2">
            <input type="hidden" name="ether_flow_lan1" value="0">
            <input type="hidden" name="ether_link_lan1" value="0">
            <input type="hidden" name="ether_flow_lan2" value="2">
            <input type="hidden" name="ether_link_lan2" value="15">
            <input type="hidden" name="ether_flow_lan3" value="0">
            <input type="hidden" name="ether_link_lan3" value="0">
            <input type="hidden" name="ether_flow_lan4" value="0">
            <input type="hidden" name="ether_link_lan4" value="0">

            <input type="hidden" name="wol_mac" value="00:11:22:33:44:55">
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
        assertEquals("2.2.2.2", config.dhcpDns3)
        assertEquals("192.168.2.250", config.dhcpWins)
        assertEquals("2001:4860:4860::8888", config.dhcpDnsv6)
        assertEquals(2, config.dhcpVerbose)
        
        assertEquals("no-resolv\nserver=8.8.8.8", config.dnsmasqDnsmasqConf)
        assertEquals("dhcp-option=option:router,192.168.2.1", config.dnsmasqDhcpConf)
        assertEquals("192.168.2.5 myhost", config.dnsmasqHosts)

        assertTrue(config.dhcpStaticEnabled)
        assertFalse(config.dhcpStaticArp)

        assertEquals(2, config.staticLeases.size)
        assertEquals("00:11:22:33:44:55", config.staticLeases[0].mac)
        assertEquals("192.168.2.100", config.staticLeases[0].ip)
        assertEquals("MyPhone", config.staticLeases[0].name)

        assertEquals("aa:bb:cc:dd:ee:ff", config.staticLeases[1].mac)
        assertEquals("192.168.2.101", config.staticLeases[1].ip)
        assertEquals("MyPC", config.staticLeases[1].name)

        // IPTV assertions
        assertTrue(config.mrEnable)
        assertEquals(2, config.forceIgmp)
        assertEquals(4022, config.udpxyPort)
        assertEquals(4044, config.xupnpdPort)
        assertTrue(config.xupnpdUdpxy)
        assertFalse(config.igmpSnoop)
        assertEquals(1, config.etherM2u)
        assertEquals(0, config.rtIgmpSnEnable)
        assertEquals(1, config.wlIgmpSnEnable)
        assertEquals(10, config.controlrateBroadcast)

        // Routing assertions
        assertTrue(config.useDhcpRoutes)
        assertTrue(config.routeEnabled)
        assertEquals(2, config.staticRoutes.size)
        assertEquals("10.0.0.0", config.staticRoutes[0].destIp)
        assertEquals("255.255.255.0", config.staticRoutes[0].netmask)
        assertEquals("192.168.2.10", config.staticRoutes[0].gateway)
        assertEquals(1, config.staticRoutes[0].metric)
        assertEquals("LAN", config.staticRoutes[0].interfaceName)

        // Switch assertions
        assertTrue(config.greenEthernet)
        assertFalse(config.eeeEnabled)
        assertEquals(1, config.etherJumbo)

        // Port configs
        assertEquals(5, config.portsConfig.size)
        val wanPort = config.portsConfig.find { it.portName == "WAN" }
        assertNotNull(wanPort)
        assertEquals(1, wanPort!!.flowControl)
        assertEquals(2, wanPort.speedDuplex)

        val lan2Port = config.portsConfig.find { it.portName == "LAN2" }
        assertNotNull(lan2Port)
        assertEquals(2, lan2Port!!.flowControl)
        assertEquals(15, lan2Port.speedDuplex)

        // WOL assertions
        assertEquals("00:11:22:33:44:55", config.wolMac)
        assertEquals(3, config.wolDevices.size)
        
        val myPhone = config.wolDevices.find { it.mac.equals("00:11:22:33:44:55", ignoreCase = true) }
        assertNotNull(myPhone)
        assertEquals("MyPhone", myPhone!!.name)
        
        val myPc = config.wolDevices.find { it.mac.equals("aa:bb:cc:dd:ee:ff", ignoreCase = true) }
        assertNotNull(myPc)
        assertEquals("MyPC", myPc!!.name)

        val wiredDev = config.wolDevices.find { it.mac.equals("33:44:55:66:77:88", ignoreCase = true) }
        assertNotNull(wiredDev)
        assertEquals("WiredDevice", wiredDev!!.name)
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
            
            <input type="hidden" name="hw_nat_mode" value="1">
            <input type="hidden" name="sfe_enable" value="0">
            <input type="hidden" name="gw_arp_ping" value="1">
            <input type="hidden" name="wan_auth_mode" value="0">
            <input type="hidden" name="wan_hostname" value="myrouter">
            <input type="hidden" name="wan_vci" value="myvci">
            <input type="hidden" name="wan_hwaddr_x" value="11:22:33:44:55:66">
            <input type="hidden" name="wan_ttl_fix" value="1">
            <input type="hidden" name="wan_ttl_value" value="64">
            <input type="hidden" name="wan_stb_x" value="3">
            <input type="hidden" name="wan_stb_iso" value="1">
            <input type="hidden" name="vlan_filter" value="1">
            <input type="hidden" name="vlan_vid_cpu" value="10">
            <input type="hidden" name="vlan_pri_cpu" value="0">
            <input type="hidden" name="vlan_vid_iptv" value="20">
            <input type="hidden" name="vlan_pri_iptv" value="4">

            <input type="hidden" name="wan_pppoe_username" value="myuser">
            <input type="hidden" name="wan_pppoe_passwd" value="mypass">
            <input type="hidden" name="wan_pppoe_mru" value="1480">
            <input type="hidden" name="wan_pppoe_mtu" value="1480">

            <input type="hidden" name="upnp_enable_x" value="1">
            <input type="hidden" name="upnp_proto" value="1">
            <input type="hidden" name="upnp_secure" value="1">
            <input type="hidden" name="upnp_eport_min" value="1024">
            <input type="hidden" name="upnp_eport_max" value="2048">
            <input type="hidden" name="upnp_iport_min" value="1024">
            <input type="hidden" name="upnp_iport_max" value="2048">
            <input type="hidden" name="upnp_clean_int" value="3600">
            <input type="hidden" name="upnp_clean_min" value="5">
            <input type="hidden" name="vts_enable_x" value="1">

            <script>
            var VSList = [["Web Server", "80", "192.168.2.50", "80", "TCP", "HTTP Forward"], ["SSH", "2222", "192.168.2.60", "22", "BOTH", "SSH Link"]];
            </script>

            <input type="hidden" name="dmz_enable_x" value="1">
            <input type="hidden" name="dmz_ipaddr" value="192.168.2.99">
            <input type="hidden" name="sp_battle_ips" value="1">

            <input type="hidden" name="ddns_enable_x" value="1">
            <select name="ddns_server_x"><option value="WWW.NO-IP.COM" selected>no-ip.com</option></select>
            <input type="hidden" name="ddns_username_x" value="ddnsuser">
            <input type="hidden" name="ddns_passwd_x" value="ddnspass">
            <input type="hidden" name="ddns_hostname_x" value="myhome.ddns.net">
            <input type="hidden" name="ddns_hostname2_x" value="myhome2.ddns.net">
            <input type="hidden" name="ddns_hostname3_x" value="myhome3.ddns.net">
            <input type="hidden" name="ddns_ssl" value="1">
            <input type="hidden" name="ddns_wildcard_x" value="1">
            <input type="hidden" name="ddns2_server" value="WWW.DYNDNS.ORG">
            <input type="hidden" name="ddns2_hname" value="myhome2.dyndns.org">
            <input type="hidden" name="ddns2_user" value="ddns2user">
            <input type="hidden" name="ddns2_pass" value="ddns2pass">
            <input type="hidden" name="ddns2_ssl" value="1">
            <input type="hidden" name="ddns2_wildcard_x" value="0">
            <input type="hidden" name="ddns_source" value="0">
            <input type="hidden" name="ddns_checkip" value="5">
            <input type="hidden" name="ddns2_checkip" value="0">
            <input type="hidden" name="ddns_period" value="24">
            <input type="hidden" name="ddns_forced" value="10">
            <input type="hidden" name="ddns_ipv6" value="1">
            <input type="hidden" name="ddns_verbose" value="1">

            <select name="ip6_service"><option value="dhcp6" selected>Native DHCPv6</option></select>
            <input type="hidden" name="ip6_wan_dhcp" value="1">
            <input type="hidden" name="ip6_dns_auto" value="0">
            <input type="hidden" name="ip6_dns1" value="2001:4860:4860::8888">
            <input type="hidden" name="ip6_dns2" value="2001:4860:4860::8844">
            <input type="hidden" name="ip6_dns3" value="">
            <input type="hidden" name="ip6_sit_mtu" value="1450">
            <input type="hidden" name="ip6_6to4_relay" value="192.88.99.1">
            <input type="hidden" name="ip6_wan_addr" value="2001:db8::">
            <input type="hidden" name="ip6_wan_size" value="32">
            <input type="hidden" name="ip6_6rd_relay" value="192.0.2.1">
            <input type="hidden" name="ip6_6rd_size" value="0">
            <input type="hidden" name="ip6_lan_auto" value="1">
            <input type="hidden" name="ip6_lan_radv" value="1">
            <input type="hidden" name="ip6_lan_dhcp" value="2">
            <input type="hidden" name="ip6_lan_sfps" value="2">
            <input type="hidden" name="ip6_lan_sfpe" value="254">
            <input type="hidden" name="ip6_lan_sflt" value="86400">
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

        assertEquals("1", config.hwNatMode)
        assertEquals("0", config.sfeEnable)
        assertTrue(config.gwArpPing)
        assertEquals("0", config.wanAuthMode)
        assertEquals("myrouter", config.wanHostname)
        assertEquals("myvci", config.wanVci)
        assertEquals("11:22:33:44:55:66", config.wanHwaddr)
        assertTrue(config.wanTtlFix)
        assertEquals("64", config.wanTtlValue)
        assertEquals("3", config.wanStbPort)
        assertEquals("1", config.wanStbIso)
        assertTrue(config.vlanFilter)
        assertEquals("10", config.vlanVidCpu)
        assertEquals("0", config.vlanPriCpu)
        assertEquals("20", config.vlanVidIptv)
        assertEquals("4", config.vlanPriIptv)

        assertTrue(config.upnpEnabled)
        assertEquals("1", config.upnpProto)
        assertTrue(config.upnpSecure)
        assertEquals("1024", config.upnpEportMin)
        assertEquals("2048", config.upnpEportMax)
        assertEquals("1024", config.upnpIportMin)
        assertEquals("2048", config.upnpIportMax)
        assertEquals("3600", config.upnpCleanInt)
        assertEquals("5", config.upnpCleanMin)
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
        assertTrue(config.dmzSpBattle)

        assertTrue(config.ddnsEnabled)
        assertEquals("WWW.NO-IP.COM", config.ddnsServer)
        assertEquals("ddnsuser", config.ddnsUser)
        assertEquals("ddnspass", config.ddnsPass)
        assertEquals("myhome.ddns.net", config.ddnsHostName)
        assertEquals("myhome2.ddns.net", config.ddnsHostName2)
        assertEquals("myhome3.ddns.net", config.ddnsHostName3)
        assertEquals("1", config.ddnsSsl)
        assertEquals("1", config.ddnsWildcard)
        assertEquals("WWW.DYNDNS.ORG", config.ddns2Server)
        assertEquals("myhome2.dyndns.org", config.ddns2HostName)
        assertEquals("ddns2user", config.ddns2User)
        assertEquals("ddns2pass", config.ddns2Pass)
        assertEquals("1", config.ddns2Ssl)
        assertEquals("0", config.ddns2Wildcard)
        assertEquals("0", config.ddnsSource)
        assertEquals("5", config.ddnsCheckIp)
        assertEquals("0", config.ddns2CheckIp)
        assertEquals("24", config.ddnsPeriod)
        assertEquals("10", config.ddnsForced)
        assertEquals("1", config.ddnsIpv6)
        assertEquals("1", config.ddnsVerbose)

        assertTrue(config.ipv6Enabled)
        assertEquals("dhcp6", config.ipv6Proto)
        assertEquals("1", config.ipv6WanDhcp)
        assertFalse(config.ipv6DnsAuto)
        assertEquals("2001:4860:4860::8888", config.ipv6Dns1)
        assertEquals("2001:4860:4860::8844", config.ipv6Dns2)
        assertEquals("", config.ipv6Dns3)
        assertEquals(1450, config.ipv6Mtu)
        assertEquals("192.88.99.1", config.ipv66to4Relay)
        assertEquals("2001:db8::", config.ipv66rdPrefix)
        assertEquals(32, config.ipv66rdPrefixLen)
        assertEquals("192.0.2.1", config.ipv66rdRouter)
        assertEquals(0, config.ipv66rdIp4Mtu)
        assertTrue(config.ipv6LanAuto)
        assertTrue(config.ipv6LanRadv)
        assertEquals("2", config.ipv6LanDhcp)
        assertEquals("2", config.ipv6LanSfps)
        assertEquals("254", config.ipv6LanSfpe)
        assertEquals("86400", config.ipv6LanSflt)
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

    @Test
    fun testParseWanConfigFallbackKeys() {
        val mockHtml = getMockHtml("""
            <input type="hidden" name="ipv6_service" value="6to4">
            <input type="hidden" name="ip6_wan_dhcp" value="1">
            <input type="hidden" name="ip6_dns_auto" value="0">
            <input type="hidden" name="ipv6_dns1_x" value="2001:4860:4860::8888">
            <input type="hidden" name="ipv6_dns2_x" value="2001:4860:4860::8844">
            <input type="hidden" name="ipv6_dns3_x" value="">
            <input type="hidden" name="ipv6_tun_mtu" value="1450">
            <input type="hidden" name="ipv6_relay" value="192.88.99.1">
            <input type="hidden" name="ipv6_tun_v4mtu" value="1420">
            <input type="hidden" name="ipv6_6rd_prefix" value="2001:db8::">
            <input type="hidden" name="ipv6_6rd_prefixlen" value="32">
            <input type="hidden" name="ipv6_6rd_router" value="192.0.2.1">
            <input type="hidden" name="ipv6_6rd_ip4size" value="0">
        """.trimIndent())

        val config = PadavanResponseParser.parseWanConfig(mockHtml)

        assertNotNull(config)
        assertEquals("6to4", config.ipv6Proto)
        assertEquals("1", config.ipv6WanDhcp)
        assertFalse(config.ipv6DnsAuto)
        assertEquals("2001:4860:4860::8888", config.ipv6Dns1)
        assertEquals("2001:4860:4860::8844", config.ipv6Dns2)
        assertEquals("", config.ipv6Dns3)
        assertEquals(1450, config.ipv6Mtu)
        assertEquals("192.88.99.1", config.ipv66to4Relay)
        assertEquals(1420, config.ipv66to4Mtu)
        assertEquals("2001:db8::", config.ipv66rdPrefix)
        assertEquals(32, config.ipv66rdPrefixLen)
        assertEquals("192.0.2.1", config.ipv66rdRouter)
        assertEquals(0, config.ipv66rdIp4Mtu)
    }
}
