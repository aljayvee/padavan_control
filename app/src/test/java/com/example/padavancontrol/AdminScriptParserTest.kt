package com.example.padavancontrol

import com.example.padavancontrol.network.PadavanResponseParser
import com.example.padavancontrol.data.models.AdminConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class AdminScriptParserTest : ParserTestBase() {

    @Test
    fun testParseAdminConfig() {
        val mockJs = """
            var http_username = "custom_admin";
            var http_passwd2 = "secure_password";
            var time_zone = "GMT-5";
            var ntp_server0 = "ntp.mycompany.org";
            var ntp_server1 = "time.cloudflare.com";
            var telnetd = "0";
            var telnetd_port = "2323";
            var sshd_enable = "1";
            var sshd_port = "2222";
            var sshd_sftp = "1";
            var webdav_enable = "0";
            var op_mode = "2";
            var firmware_version = "3.4.3.9-100";
            var build_date = "2025-10-10";
        """.trimIndent()

        val html = getMockHtml(mockJs)
        val config = PadavanResponseParser.parseAdminConfig(html)

        assertNotNull(config)
        assertEquals("custom_admin", config.adminUser)
        assertEquals("secure_password", config.adminPass)
        assertEquals("GMT-5", config.timezone)
        assertEquals("ntp.mycompany.org", config.ntpServer1)
        assertEquals("time.cloudflare.com", config.ntpServer2)
        assertFalse(config.enableTelnet)
        assertEquals("2323", config.telnetPort)
        assertEquals("1", config.sshdEnable)
        assertTrue(config.enableSsh)
        assertEquals("2222", config.sshPort)
        assertTrue(config.enableSftp)
        assertFalse(config.enableWebdav)
        assertEquals("2", config.opMode)
        assertEquals("3.4.3.9-100", config.firmwareVersion)
        assertEquals("2025-10-10", config.buildDate)
    }

    @Test
    fun testParseAdminConfigPhase3() {
        val mockJs = """
            var computer_name = "Newifi3-Router";
            var ntp_period = "12";
            var log_ipaddr = "192.168.2.250";
            var log_port = "514";
            var log_float_ui = "2";
            var select_lang = "CN";
            var help_enable = "0";
            var nvram_manual = "1";
            var rstats_stored = "2";
            var stime_stored = "0";
            var productid = "Newifi3";
            var ez_action_short = "1";
            var ez_action_long = "3";
            var front_led_all = "0";
            var front_led_wan = "1";
            var front_led_usb = "2";
            var front_led_wif = "0";
            var front_led_pwr = "2";
            var front_led_lan = "1";
        """.trimIndent()

        val html = getMockHtml(mockJs)
        val config = PadavanResponseParser.parseAdminConfig(html)

        assertNotNull(config)
        assertEquals("Newifi3-Router", config.deviceName)
        assertEquals("12", config.ntpPeriod)
        assertEquals("192.168.2.250", config.logIpAddr)
        assertEquals("514", config.logPort)
        assertEquals("2", config.logFloatUi)
        assertEquals("CN", config.selectLang)
        assertFalse(config.helpEnable)
        assertEquals("1", config.nvramManual)
        assertEquals("2", config.rstatsStored)
        assertEquals("0", config.stimeStored)
        assertEquals("Newifi3", config.productId)
        assertEquals("1", config.btnWpsShort)
        assertEquals("3", config.btnWpsLong)
        assertFalse(config.ledEnable)
        assertEquals("1", config.ledInternet)
        assertEquals("2", config.ledUsb)
        assertEquals("0", config.ledWifi)
        assertEquals("2", config.ledPower)
        assertEquals("1", config.ledEthernet)
    }

    @Test
    fun testAdminConfigDefaultValues() {
        val config = AdminConfig()
        assertEquals("Router", config.deviceName)
        assertEquals("24", config.ntpPeriod)
        assertEquals("", config.logIpAddr)
        assertEquals("514", config.logPort)
        assertEquals("1", config.logFloatUi)
        assertEquals("EN", config.selectLang)
        assertTrue(config.helpEnable)
        assertEquals("0", config.nvramManual)
        assertEquals("1", config.rstatsStored)
        assertEquals("1", config.stimeStored)
        assertEquals("0", config.btnWpsShort)
        assertEquals("0", config.btnWpsLong)
        assertTrue(config.ledEnable)
        assertEquals("0", config.ledInternet)
        assertEquals("0", config.ledUsb)
        assertEquals("0", config.ledWifi)
        assertEquals("0", config.ledPower)
        assertEquals("0", config.ledEthernet)
    }

    @Test
    fun testParseScriptConfig() {
        val mockHtml = """
            <!DOCTYPE html>
            <html>
            <body>
            <textarea name="scripts.init_script.sh">
            #!/bin/sh
            logger "Init script!"
            </textarea>
            <textarea name="scripts.start_script.sh">
            #!/bin/sh
            echo "Starting startup scripts..."
            logger "Boot completed successfully!"
            </textarea>
            <textarea name="scripts.post_wan_script.sh">
            #!/bin/sh
            logger "WAN up!"
            </textarea>
            <textarea name="scripts.post_wandn_script.sh">
            #!/bin/sh
            logger "WAN down!"
            </textarea>
            <textarea name="scripts.ez_buttons_script.sh">
            #!/bin/sh
            logger "Buttons action!"
            </textarea>
            <textarea name="scripts.shutdown_script.sh">
            #!/bin/sh
            logger "Shutdown started!"
            </textarea>
            <textarea name="scripts.post_iptables_script.sh">
            #!/bin/sh
            iptables -I INPUT -p tcp --dport 80 -j ACCEPT
            </textarea>
            
            <input name="di_poll_mode" value="2">
            <input name="di_addr0" value="1.1.1.1">
            <input name="di_addr1" value="8.8.4.4">
            <input name="di_addr2" value="9.9.9.9">
            <input name="di_addr3" value="8.8.8.8">
            <input name="di_addr4" value="1.0.0.1">
            <input name="di_addr5" value="4.2.2.2">
            <input name="di_time_done" value="60">
            <input name="di_time_fail" value="15">
            <input name="di_time_timeout" value="8">
            <input name="di_lost_action" value="1">
            </body>
            </html>
        """.trimIndent()

        val config = PadavanResponseParser.parseScriptConfig(mockHtml)

        assertNotNull(config)
        assertTrue(config.scriptInit.contains("logger \"Init script!\""))
        assertTrue(config.scriptStart.contains("logger \"Boot completed successfully!\""))
        assertTrue(config.scriptWanUp.contains("logger \"WAN up!\""))
        assertTrue(config.scriptWanDown.contains("logger \"WAN down!\""))
        assertTrue(config.scriptEzButton.contains("logger \"Buttons action!\""))
        assertTrue(config.scriptShutdown.contains("logger \"Shutdown started!\""))
        assertTrue(config.scriptIpRules.contains("iptables -I INPUT"))
        
        assertTrue(config.pingEnabled)
        assertEquals("2", config.pingPollMode)
        assertEquals("1.1.1.1", config.pingHost1)
        assertEquals("8.8.4.4", config.pingHost2)
        assertEquals("9.9.9.9", config.pingHost3)
        assertEquals("8.8.8.8", config.pingHost4)
        assertEquals("1.0.0.1", config.pingHost5)
        assertEquals("4.2.2.2", config.pingHost6)
        assertEquals(60, config.pingIntervalSuccess)
        assertEquals(15, config.pingIntervalFail)
        assertEquals(8, config.pingTimeout)
        assertEquals("1", config.pingAction)
    }
}
