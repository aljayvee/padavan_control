package com.example.padavancontrol

import com.example.padavancontrol.network.PadavanResponseParser
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
            var btn_wps_mode = "1";
            var led_pwr_mode = "1";
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
        assertTrue(config.enableSsh)
        assertEquals("2222", config.sshPort)
        assertTrue(config.enableSftp)
        assertFalse(config.enableWebdav)
        assertEquals("2", config.opMode)
        assertEquals("1", config.btnWpsMode)
        assertEquals("1", config.ledPowerMode)
        assertEquals("3.4.3.9-100", config.firmwareVersion)
        assertEquals("2025-10-10", config.buildDate)
    }

    @Test
    fun testParseScriptConfig() {
        val mockHtml = """
            <!DOCTYPE html>
            <html>
            <body>
            <textarea name="scripts.start_script.sh">
            #!/bin/sh
            echo "Starting startup scripts..."
            logger "Boot completed successfully!"
            </textarea>
            <textarea name="scripts.post_wan_script.sh">
            #!/bin/sh
            logger "WAN up!"
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
            
            <input name="di_poll_mode" value="1">
            <input name="di_addr0" value="1.1.1.1">
            <input name="di_addr1" value="8.8.4.4">
            <input name="di_time_done" value="60">
            <input name="di_lost_action" value="1">
            </body>
            </html>
        """.trimIndent()

        val config = PadavanResponseParser.parseScriptConfig(mockHtml)

        assertNotNull(config)
        assertTrue(config.scriptStartup.contains("logger \"Boot completed successfully!\""))
        assertTrue(config.scriptWanUp.contains("logger \"WAN up!\""))
        assertTrue(config.scriptWanDown.contains("logger \"Buttons action!\""))
        assertTrue(config.scriptShutdown.contains("logger \"Shutdown started!\""))
        assertTrue(config.scriptIpRules.contains("iptables -I INPUT"))
        
        assertTrue(config.pingEnabled)
        assertEquals("1.1.1.1", config.pingHost1)
        assertEquals("8.8.4.4", config.pingHost2)
        assertEquals(60, config.pingPeriod)
        assertEquals("1", config.pingAction)
    }
}
