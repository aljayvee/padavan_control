@file:Suppress("DEPRECATION")

package com.example.padavancontrol.discovery

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.net.DhcpInfo
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*

class RouterDiscoveryServiceTest {

    @Test
    fun testIsPrivateIp_validPrivateIps_returnsTrue() {
        assertTrue(RouterDiscoveryService.isPrivateIp("192.168.1.1"))
        assertTrue(RouterDiscoveryService.isPrivateIp("10.0.0.1"))
        assertTrue(RouterDiscoveryService.isPrivateIp("172.16.2.3"))
        assertTrue(RouterDiscoveryService.isPrivateIp("127.0.0.1"))
        assertTrue(RouterDiscoveryService.isPrivateIp("http://192.168.2.1/index.asp"))
        assertTrue(RouterDiscoveryService.isPrivateIp("https://10.20.30.40:8443/"))
    }

    @Test
    fun testIsPrivateIp_publicIps_returnsFalse() {
        assertFalse(RouterDiscoveryService.isPrivateIp("8.8.8.8"))
        assertFalse(RouterDiscoveryService.isPrivateIp("199.199.199.199"))
        assertFalse(RouterDiscoveryService.isPrivateIp("http://google.com/"))
        assertFalse(RouterDiscoveryService.isPrivateIp("https://github.com:443/"))
    }

    @Test
    fun testIsVpnActive_whenVpnIsActive_returnsTrue() {
        val context = mock(Context::class.java)
        val connectivityManager = mock(ConnectivityManager::class.java)
        val activeNetwork = mock(Network::class.java)
        val capabilities = mock(NetworkCapabilities::class.java)

        `when`(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager)
        `when`(connectivityManager.activeNetwork).thenReturn(activeNetwork)
        `when`(connectivityManager.getNetworkCapabilities(activeNetwork)).thenReturn(capabilities)
        `when`(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)).thenReturn(true)

        assertTrue(RouterDiscoveryService.isVpnActive(context))
    }

    @Test
    fun testIsVpnActive_whenVpnIsInactive_returnsFalse() {
        val context = mock(Context::class.java)
        val connectivityManager = mock(ConnectivityManager::class.java)
        val activeNetwork = mock(Network::class.java)
        val capabilities = mock(NetworkCapabilities::class.java)

        `when`(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager)
        `when`(connectivityManager.activeNetwork).thenReturn(activeNetwork)
        `when`(connectivityManager.getNetworkCapabilities(activeNetwork)).thenReturn(capabilities)
        `when`(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)).thenReturn(false)

        assertFalse(RouterDiscoveryService.isVpnActive(context))
    }

    @Test
    fun testGetWifiGatewayIp_whenOnWifi_returnsCorrectIp() {
        val context = mock(Context::class.java)
        val appContext = mock(Context::class.java)
        val connectivityManager = mock(ConnectivityManager::class.java)
        val wifiManager = mock(WifiManager::class.java)
        val activeNetwork = mock(Network::class.java)
        val capabilities = mock(NetworkCapabilities::class.java)
        val dhcpInfo = DhcpInfo()
        
        // 192.168.2.1 represented as Int (little endian format in dhcpInfo.gateway)
        dhcpInfo.gateway = 0x0102A8C0 // C0=192, A8=168, 02=2, 01=1

        `when`(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager)
        `when`(context.applicationContext).thenReturn(appContext)
        `when`(appContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(wifiManager)
        `when`(connectivityManager.activeNetwork).thenReturn(activeNetwork)
        `when`(connectivityManager.getNetworkCapabilities(activeNetwork)).thenReturn(capabilities)
        `when`(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(true)
        `when`(wifiManager.dhcpInfo).thenReturn(dhcpInfo)

        val gatewayIp = RouterDiscoveryService.getWifiGatewayIp(context)
        assertEquals("192.168.2.1", gatewayIp)
    }

    @Test
    fun testGetWifiGatewayIp_whenNotOnWifi_returnsNull() {
        val context = mock(Context::class.java)
        val connectivityManager = mock(ConnectivityManager::class.java)
        val activeNetwork = mock(Network::class.java)
        val capabilities = mock(NetworkCapabilities::class.java)

        `when`(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager)
        `when`(connectivityManager.activeNetwork).thenReturn(activeNetwork)
        `when`(connectivityManager.getNetworkCapabilities(activeNetwork)).thenReturn(capabilities)
        `when`(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(false)

        val gatewayIp = RouterDiscoveryService.getWifiGatewayIp(context)
        assertNull(gatewayIp)
    }

    @Test
    fun testGetWifiGatewayIp_whenSecurityExceptionThrown_returnsNull() {
        val context = mock(Context::class.java)
        val appContext = mock(Context::class.java)
        val connectivityManager = mock(ConnectivityManager::class.java)
        val wifiManager = mock(WifiManager::class.java)
        val activeNetwork = mock(Network::class.java)
        val capabilities = mock(NetworkCapabilities::class.java)

        `when`(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager)
        `when`(context.applicationContext).thenReturn(appContext)
        `when`(appContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(wifiManager)
        `when`(connectivityManager.activeNetwork).thenReturn(activeNetwork)
        `when`(connectivityManager.getNetworkCapabilities(activeNetwork)).thenReturn(capabilities)
        `when`(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(true)
        `when`(wifiManager.dhcpInfo).thenThrow(SecurityException("Permission denied"))

        val gatewayIp = RouterDiscoveryService.getWifiGatewayIp(context)
        assertNull(gatewayIp)
    }
}
