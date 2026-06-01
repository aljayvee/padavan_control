package com.example.padavancontrol

import com.example.padavancontrol.network.PadavanResponseParser
import org.junit.Assert.*
import org.junit.Test

class FirewallUsbParserTest : ParserTestBase() {

    @Test
    fun testParseFirewallConfig() {
        val mockHtml = getMockHtml("""
            <input type="checkbox" id="fw_enable_x_fake" value="1 checked">
            <input type="checkbox" id="fw_dos_x_fake" value="0">
            <input type="checkbox" id="fw_syn_cook_fake" value="1 checked">
            <select name="fw_log_x"><option value="drop" selected>Dropped</option></select>
            <input type="checkbox" id="misc_ping_x_fake" value="1 checked">
            <input type="checkbox" id="misc_http_x_fake" value="0">
            <input type="hidden" name="misc_httpport_x" value="8080">
            <input type="checkbox" id="https_wopen_fake" value="1 checked">
            <input type="hidden" name="https_wport" value="8443">
            <input type="checkbox" id="sshd_wopen_fake" value="1 checked">
            <input type="hidden" name="sshd_wport" value="2222">
            <select name="sshd_wbfp"><option value="3" selected>Max 3 tries / 10 min</option></select>
            <input type="checkbox" id="ftpd_wopen_fake" value="0">
            <input type="hidden" name="ftpd_wport" value="21">
            <input type="checkbox" id="udpxy_wopen_fake" value="1 checked">
            <input type="hidden" name="udpxy_wport" value="4000">
            <input type="checkbox" id="trmd_ropen_fake" value="0">
            <input type="checkbox" id="aria_ropen_fake" value="1 checked">

            <input type="checkbox" id="wan_nat_x_fake" value="1 checked">
            <select name="nf_max_conn"><option value="65536" selected>65536</option></select>
            <select name="nf_nat_type"><option value="1" selected>Full Cone NAT</option></select>
            <input type="checkbox" id="nf_nat_loop_fake" value="1 checked">
            <input type="checkbox" id="fw_pt_pppoe_fake" value="0">
            <input type="hidden" name="nf_alg_ftp0" value="21">
            <input type="hidden" name="nf_alg_ftp1" value="2121">
            <input type="checkbox" id="nf_alg_pptp_fake" value="1 checked">
            <input type="checkbox" id="nf_alg_rtsp_fake" value="0">
            <input type="checkbox" id="nf_alg_h323_fake" value="1 checked">
            <input type="checkbox" id="nf_alg_sip_fake" value="1 checked">

            <input type="checkbox" id="url_enable_fake" value="1 checked">
            <input type="hidden" name="url_date_x" value="1111100">
            <input type="hidden" name="url_time_x" value="08001800">
            <input type="hidden" name="url_mac_x" value="AA:BB:CC:DD:EE:FF">
            <input type="checkbox" name="url_inv_fake" value="" checked>
            <select name="UrlList_s" multiple="true">
                <option value="facebook.com">facebook.com</option>
                <option value="youtube.com">youtube.com</option>
            </select>

            <select name="macfilter_enable_x"><option value="2" selected>Reject</option></select>
            <input type="checkbox" id="fw_mac_drop_fake" value="1 checked">
            
            <script>
            var MACList = [["11:22:33:44:55:66", "09001700", "1111111"], ["22:33:44:55:66:77", "00002359", "0000011"]];
            var LWFilterList = [["192.168.2.50", "80", "192.168.2.100", "8080", "TCP", ""], ["*", "", "1.1.1.1", "53", "UDP", ""]];
            </script>

            <input type="checkbox" id="fw_lw_enable_fake" value="1 checked">
            <select name="filter_lw_default_x"><option value="DROP" selected>Blacklist</option></select>
            <input type="hidden" name="filter_lw_date_x" value="1010101">
            <input type="hidden" name="filter_lw_time_x" value="01002300">
            <input type="hidden" name="filter_lw_icmp_x" value="8,0">
        """.trimIndent())

        val config = PadavanResponseParser.parseFirewallConfig(mockHtml)

        assertNotNull(config)
        assertTrue(config.fwEnabled)
        assertFalse(config.fwDosEnabled)
        assertTrue(config.fwSynCookEnabled)
        assertEquals("drop", config.fwLogMode)
        assertTrue(config.miscPingEnabled)
        assertFalse(config.miscHttpEnabled)
        assertEquals("8080", config.miscHttpPort)
        assertTrue(config.httpsWopen)
        assertEquals("8443", config.httpsWport)
        assertTrue(config.sshdWopen)
        assertEquals("2222", config.sshdWport)
        assertEquals("3", config.sshdWbfp)
        assertFalse(config.ftpdWopen)
        assertEquals("21", config.ftpdWport)
        assertTrue(config.udpxyWopen)
        assertEquals("4000", config.udpxyWport)
        assertFalse(config.trmdRopen)
        assertTrue(config.ariaRopen)

        // Netfilter
        assertTrue(config.wanNatEnabled)
        assertEquals("65536", config.nfMaxConn)
        assertEquals("1", config.nfNatType)
        assertTrue(config.nfNatLoopback)
        assertFalse(config.fwPtPppoe)
        assertEquals("21", config.nfAlgFtp0)
        assertEquals("2121", config.nfAlgFtp1)
        assertTrue(config.nfAlgPptp)
        assertFalse(config.nfAlgRtsp)
        assertTrue(config.nfAlgH323)
        assertTrue(config.nfAlgSip)

        // URL Filter
        assertTrue(config.urlFilterEnabled)
        assertEquals("1111100", config.urlFilterDate)
        assertEquals("08001800", config.urlFilterTime)
        assertEquals("AA:BB:CC:DD:EE:FF", config.urlFilterMac)
        assertTrue(config.urlFilterInvert)
        assertEquals(2, config.urlKeywords.size)
        assertEquals("facebook.com", config.urlKeywords[0])
        assertEquals("youtube.com", config.urlKeywords[1])

        // MAC Filter Rules
        assertEquals("2", config.macFilterMethod)
        assertTrue(config.fwMacDrop)
        assertEquals(2, config.macFilterRules.size)
        assertEquals("11:22:33:44:55:66", config.macFilterRules[0].mac)
        assertEquals("09001700", config.macFilterRules[0].time)
        assertEquals("1111111", config.macFilterRules[0].date)
        assertEquals("22:33:44:55:66:77", config.macFilterRules[1].mac)
        assertEquals("00002359", config.macFilterRules[1].time)
        assertEquals("0000011", config.macFilterRules[1].date)

        // Services Filter (LWFilterList)
        assertTrue(config.fwLwEnabled)
        assertEquals("DROP", config.filterLwDefault)
        assertEquals("1010101", config.filterLwDate)
        assertEquals("01002300", config.filterLwTime)
        assertEquals("8,0", config.filterLwIcmp)
        assertEquals(2, config.serviceFilterRules.size)
        assertEquals("192.168.2.50", config.serviceFilterRules[0].srcIp)
        assertEquals("80", config.serviceFilterRules[0].srcPort)
        assertEquals("192.168.2.100", config.serviceFilterRules[0].dstIp)
        assertEquals("8080", config.serviceFilterRules[0].dstPort)
        assertEquals("TCP", config.serviceFilterRules[0].proto)
        assertEquals("", config.serviceFilterRules[0].protoNo)

        assertEquals("*", config.serviceFilterRules[1].srcIp)
        assertEquals("", config.serviceFilterRules[1].srcPort)
        assertEquals("1.1.1.1", config.serviceFilterRules[1].dstIp)
        assertEquals("53", config.serviceFilterRules[1].dstPort)
        assertEquals("UDP", config.serviceFilterRules[1].proto)
    }

    @Test
    fun testParseUsbShareConfig() {
        val mockHtml = getMockHtml("""
            <select name="usb3_disable"><option value="1" selected>Yes</option></select>
            <select name="hdd_spindt"><option value="3" selected>1h:00m</option></select>
            <select name="hdd_apmoff"><option value="0" selected>No</option></select>
            <select name="achk_enable"><option value="1" selected>Yes</option></select>
            <select name="pcache_reclaim"><option value="2" selected>50% RAM</option></select>
            <select name="optw_enable"><option value="2" selected>Entware</option></select>
            <input type="text" name="st_max_user" value="15">

            <input type="checkbox" id="enable_samba_fake" value="1 checked">
            <input type="text" name="st_samba_workgroup" value="MYGROUP">
            <select name="st_samba_mode"><option value="3" selected>Samba Mode 3</option></select>
            <select name="st_samba_lmb"><option value="2" selected>Local & Domain MB</option></select>
            <select name="st_samba_fp"><option value="0" selected>No</option></select>

            <input type="checkbox" id="enable_ftp_fake" value="0">
            <select name="st_ftp_mode"><option value="2" selected>FTP Mode 2</option></select>
            <select name="st_ftp_log"><option value="1" selected>Yes</option></select>
            <input type="text" name="st_ftp_pmin" value="2000">
            <input type="text" name="st_ftp_pmax" value="4000">
            <input type="text" name="st_ftp_anmr" value="512">

            <select name="rawd_enable"><option value="2" selected>Yes bidirectional</option></select>
            <input type="checkbox" id="lprd_enable_fake" value="1 checked">
            <input type="checkbox" id="u2ec_enable_fake" value="0">

            <input type="checkbox" id="modem_rule_fake" value="1 checked">
            <select name="modem_type"><option value="3" selected>NDIS</option></select>
            <input type="hidden" name="modem_country" value="USA">
            <input type="hidden" name="modem_isp" value="T-Mobile">
            <input id="modem_apn" name="modem_apn" value="fast.t-mobile.com">
            <input id="modem_pin" name="modem_pin" value="1234">
            <input id="modem_dialnum" name="modem_dialnum" value="*99***1#">
            <input id="modem_user" name="modem_user" value="tmobile">
            <input id="modem_pass" name="modem_pass" value="pass123">
            <select name="modem_nets"><option value="1" selected>LTE Only</option></select>
            <input name="modem_mtu" value="1420">
            <input type="checkbox" id="modem_dnsa_fake" value="0">
            <input type="text" name="wan_dns1_x" value="8.8.8.8">
            <input type="text" name="wan_dns2_x" value="8.8.4.4">
            <input type="text" name="wan_dns3_x" value="1.1.1.1">
            <select name="modem_node"><option value="2" selected>ttyUSB1</option></select>
            <input name="modem_cmd" value="AT+CFUN=1">
            <select name="modem_zcd"><option value="1" selected>legacy eject</option></select>
        """.trimIndent())

        val config = PadavanResponseParser.parseUsbShareConfig(mockHtml)

        assertNotNull(config)
        assertTrue(config.usb3Disable)
        assertEquals("3", config.hddSpindown)
        assertFalse(config.hddApmOff)
        assertTrue(config.achkEnable)
        assertEquals("2", config.pcacheReclaim)
        assertEquals("2", config.optwEnable)
        assertEquals("15", config.stMaxUser)

        // Samba
        assertTrue(config.enableSamba)
        assertEquals("MYGROUP", config.sambaWorkgroup)
        assertEquals("3", config.sambaMode)
        assertEquals("2", config.sambaLmb)
        assertEquals("0", config.sambaFp)

        // FTP
        assertFalse(config.enableFtp)
        assertEquals("2", config.ftpMode)
        assertTrue(config.ftpLog)
        assertEquals("2000", config.ftpPmin)
        assertEquals("4000", config.ftpPmax)
        assertEquals("512", config.ftpAnmr)

        // LPR Printer
        assertEquals("2", config.rawdEnable)
        assertTrue(config.lprdEnable)
        assertFalse(config.u2ecEnable)

        // Modem
        assertTrue(config.modemRule)
        assertEquals("3", config.modemType)
        assertEquals("USA", config.modemCountry)
        assertEquals("T-Mobile", config.modemIsp)
        assertEquals("fast.t-mobile.com", config.modemApn)
        assertEquals("1234", config.modemPin)
        assertEquals("*99***1#", config.modemDialnum)
        assertEquals("tmobile", config.modemUser)
        assertEquals("pass123", config.modemPass)
        assertEquals("1", config.modemNets)
        assertEquals(1420, config.modemMtu)
        assertFalse(config.modemDnsAuto)
        assertEquals("8.8.8.8", config.wanDns1)
        assertEquals("8.8.4.4", config.wanDns2)
        assertEquals("1.1.1.1", config.wanDns3)
        assertEquals("2", config.modemNode)
        assertEquals("AT+CFUN=1", config.modemCmd)
        assertEquals("1", config.modemZcd)
    }
}
