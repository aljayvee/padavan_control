package com.example.padavancontrol.network

import org.junit.Assert.assertEquals
import org.junit.Test

class RetrofitClientTest {

    @Test
    fun testFormatIp_standardHttp() {
        assertEquals("http://192.168.2.1/", RetrofitClient.formatIp("192.168.2.1"))
        assertEquals("http://192.168.2.1/", RetrofitClient.formatIp("http://192.168.2.1"))
        assertEquals("http://192.168.2.1/", RetrofitClient.formatIp("http://192.168.2.1/"))
    }

    @Test
    fun testFormatIp_standardHttps() {
        assertEquals("https://192.168.2.1/", RetrofitClient.formatIp("https://192.168.2.1"))
        assertEquals("https://192.168.2.1/", RetrofitClient.formatIp("https://192.168.2.1/"))
    }

    @Test
    fun testFormatIp_httpsPorts_defaultsToHttps() {
        assertEquals("https://192.168.2.1:443/", RetrofitClient.formatIp("192.168.2.1:443"))
        assertEquals("https://192.168.2.1:8443/", RetrofitClient.formatIp("192.168.2.1:8443"))
        assertEquals("https://192.168.2.1:443/", RetrofitClient.formatIp("192.168.2.1:443/"))
        assertEquals("https://192.168.2.1:8443/", RetrofitClient.formatIp("192.168.2.1:8443/"))
    }

    @Test
    fun testFormatIp_explicitHttpOnHttpsPort() {
        assertEquals("http://192.168.2.1:443/", RetrofitClient.formatIp("http://192.168.2.1:443"))
        assertEquals("http://192.168.2.1:8443/", RetrofitClient.formatIp("http://192.168.2.1:8443"))
    }
}
