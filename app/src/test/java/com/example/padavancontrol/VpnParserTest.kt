package com.example.padavancontrol

import com.example.padavancontrol.data.models.VpnConfig
import com.example.padavancontrol.network.PadavanResponseParser
import com.example.padavancontrol.network.RetrofitClient
import okhttp3.ConnectionSpec
import org.junit.Assert.*
import org.junit.Test

class VpnParserTest : ParserTestBase() {

    @Test
    fun testParseVpnConfig_pptp() {
        val html = """
            <input type="radio" name="vpnc_enable" value="1" checked>
            <select name="vpnc_type"><option value="0" selected>PPTP</option></select>
            <input type="text" name="vpnc_peer" value="vpn.example.com">
            <input type="text" name="vpnc_user" value="my_user">
            <input type="password" name="vpnc_pass" value="my_pass">
            <select name="vpnc_auth"><option value="1" selected>MS-CHAPv2</option></select>
            <select name="vpnc_mppe"><option value="1" selected>MPPE-128</option></select>
            <input type="text" name="vpnc_mtu" value="1400">
            <input type="text" name="vpnc_mru" value="1400">
            <input type="text" name="vpnc_pppd" value="debug noccp">
            <select name="vpnc_sfw"><option value="1" selected>Yes</option></select>
            <select name="vpnc_pdns"><option value="1" selected>Yes</option></select>
            <input type="radio" name="vpnc_dgw" value="1" checked>
            <input type="text" name="vpnc_rnet" value="">
            <input type="text" name="vpnc_rmsk" value="">
        """.trimIndent()

        val config = PadavanResponseParser.parseVpnConfig(html)
        assertTrue(config.enable)
        assertEquals("0", config.type)
        assertEquals("vpn.example.com", config.peer)
        assertEquals("my_user", config.username)
        assertEquals("my_pass", config.password)
        assertEquals("1", config.authType)
        assertEquals("1", config.mppe)
        assertEquals(1400, config.mtu)
        assertEquals(1400, config.mru)
        assertEquals("debug noccp", config.pppdOptions)
        assertEquals("1", config.firewall)
        assertEquals("1", config.peerDns)
        assertTrue(config.defaultGateway)
    }

    @Test
    fun testParseVpnConfig_openvpn() {
        val html = """
            <input type="radio" name="vpnc_enable" value="1" checked>
            <select name="vpnc_type"><option value="2" selected>OpenVPN</option></select>
            <input type="text" name="vpnc_peer" value="ovpn.example.com">
            <input type="text" name="vpnc_ov_port" value="1195">
            <select name="vpnc_ov_prot"><option value="1" selected>TCP</option></select>
            <select name="vpnc_ov_mode"><option value="1" selected>TUN</option></select>
            <select name="vpnc_ov_auth"><option value="0" selected>TLS Certs</option></select>
            <select name="vpnc_ov_mdig"><option value="3" selected>SHA256</option></select>
            <select name="vpnc_ov_ciph"><option value="14" selected>AES-256-GCM</option></select>
            <select name="vpnc_ov_clzo"><option value="0" selected>Disabled</option></select>
            <select name="vpnc_ov_atls"><option value="1" selected>Yes</option></select>
            <textarea name="ovpncli.client.conf">
            client
            dev tun
            </textarea>
            <textarea name="ovpncli.ca.crt">
            -----BEGIN CERTIFICATE-----
            MOCK_CA_CERT
            -----END CERTIFICATE-----
            </textarea>
            <textarea name="ovpncli.client.crt">
            -----BEGIN CERTIFICATE-----
            MOCK_CLIENT_CERT
            -----END CERTIFICATE-----
            </textarea>
            <textarea name="ovpncli.client.key">
            -----BEGIN PRIVATE KEY-----
            MOCK_CLIENT_KEY
            -----END PRIVATE KEY-----
            </textarea>
            <textarea name="ovpncli.ta.key">
            -----BEGIN OpenVPN Static key V1-----
            MOCK_TA_KEY
            -----END OpenVPN Static key V1-----
            </textarea>
            <textarea name="scripts.vpnc_server_script.sh">
            echo "Connected"
            </textarea>
        """.trimIndent()

        val config = PadavanResponseParser.parseVpnConfig(html)
        assertTrue(config.enable)
        assertEquals("2", config.type)
        assertEquals("ovpn.example.com", config.peer)
        assertEquals(1195, config.ovpnPort)
        assertEquals("1", config.ovpnProtocol)
        assertEquals("1", config.ovpnMode)
        assertEquals("0", config.ovpnAuthType)
        assertEquals("3", config.ovpnDigest)
        assertEquals("14", config.ovpnCipher)
        assertEquals("0", config.ovpnLzo)
        assertEquals("1", config.ovpnHmacSign)
        assertEquals("client\ndev tun", config.ovpnCustomConfig)
        assertEquals("-----BEGIN CERTIFICATE-----\nMOCK_CA_CERT\n-----END CERTIFICATE-----", config.caCert)
        assertEquals("-----BEGIN CERTIFICATE-----\nMOCK_CLIENT_CERT\n-----END CERTIFICATE-----", config.clientCert)
        assertEquals("-----BEGIN PRIVATE KEY-----\nMOCK_CLIENT_KEY\n-----END PRIVATE KEY-----", config.clientKey)
        assertEquals("-----BEGIN OpenVPN Static key V1-----\nMOCK_TA_KEY\n-----END OpenVPN Static key V1-----", config.tlsAuthKey)
        assertEquals("echo \"Connected\"", config.postScript)
    }

    @Test
    fun testParseVpnConfig_defaults_fallback() {
        val html = ""
        val existing = VpnConfig(peer = "existing.com", mtu = 1420)
        val config = PadavanResponseParser.parseVpnConfig(html, existing)
        assertFalse(config.enable)
        assertEquals("existing.com", config.peer)
        assertEquals(1420, config.mtu)
    }

    @Test
    fun testTlsCipherSuitesRestriction() {
        val spec = RetrofitClient.customConnectionSpec
        assertNotNull(spec)
        assertTrue(spec.isTls)

        val cipherSuites = spec.cipherSuites
        assertNotNull(cipherSuites)
        assertTrue(cipherSuites!!.isNotEmpty())

        for (cipher in cipherSuites) {
            val name = cipher.javaName
            // Ensure no Anonymous Diffie-Hellman (ADH)
            assertFalse("Cipher $name must not be anonymous", name.contains("_anon_"))
            // Ensure no MD5
            assertFalse("Cipher $name must not use MD5", name.contains("_MD5"))
            // Ensure no DSS
            assertFalse("Cipher $name must not use DSS", name.contains("_DSS"))
            
            // Ensure only allowed key exchanges (RSA, DHE/ECDHE) or TLS 1.3 ciphers
            val isValidExchange = name.startsWith("TLS_ECDHE_") || name.startsWith("TLS_DHE_") || name.startsWith("TLS_RSA_") ||
                    name.startsWith("SSL_ECDHE_") || name.startsWith("SSL_DHE_") || name.startsWith("SSL_RSA_") ||
                    name.startsWith("TLS_AES_") || name.startsWith("TLS_CHACHA20_")
            assertTrue("Cipher $name has invalid key exchange", isValidExchange)
        }
    }
}
