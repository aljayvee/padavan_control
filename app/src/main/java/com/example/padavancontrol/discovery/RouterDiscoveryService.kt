package com.example.padavancontrol.discovery

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object RouterDiscoveryService {

    // A permissive OkHttpClient that bypasses SSL verification for local IPs
    private val permissiveClient: OkHttpClient by lazy {
        try {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            
            OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        } catch (e: Exception) {
            OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .build()
        }
    }

    // A standard OkHttpClient for non-private IP queries
    private val standardClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Determines if a given IP address is in a private network range.
     */
    fun isPrivateIp(ip: String): Boolean {
        val cleanIp = ip.replace(Regex("^(https?://)?"), "").split(":")[0].split("/")[0]
        val privateRegex = Regex("^(127\\.0\\.0\\.1|10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|172\\.(1[6-9]|2\\d|3[0-1])\\.\\d{1,3}\\.\\d{1,3}|192\\.168\\.\\d{1,3}\\.\\d{1,3})$")
        return privateRegex.matches(cleanIp)
    }

    /**
     * Checks if a VPN is currently active on the device.
     */
    fun isVpnActive(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }

    /**
     * Gets the connected Wi-Fi's DHCP Gateway IP Address.
     * Returns null if not on Wi-Fi or gateway is unrouteable.
     */
    @Suppress("DEPRECATION")
    fun getWifiGatewayIp(context: Context): String? {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return null

        val activeNetwork = connectivityManager.activeNetwork ?: return null
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return null

        // Ensure we are connected to Wi-Fi
        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return null
        }

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return null

        val dhcpInfo = try {
            wifiManager.dhcpInfo
        } catch (e: SecurityException) {
            null
        } ?: return null
        val gateway = dhcpInfo.gateway
        if (gateway == 0) return null

        // Convert Int IP to String
        return (gateway and 0xFF).toString() + "." +
                (gateway shr 8 and 0xFF) + "." +
                (gateway shr 16 and 0xFF) + "." +
                (gateway shr 24 and 0xFF)
    }

    /**
     * Probes an IP to check if it's running Padavan firmware.
     * Checks headers for Basic realm="RT-N56U" or HTML title/scripts.
     */
    suspend fun probeIp(ip: String): Boolean = withContext(Dispatchers.IO) {
        val formattedUrl = if (ip.startsWith("http://") || ip.startsWith("https://")) {
            ip.removeSuffix("/") + "/index.asp"
        } else {
            "http://$ip/index.asp"
        }

        val client = if (isPrivateIp(ip)) permissiveClient else standardClient

        try {
            val request = Request.Builder().url(formattedUrl).build()
            client.newCall(request).execute().use { response ->
                // Check header for Padavan realm (Auth challenge 401)
                val authHeader = response.header("WWW-Authenticate")
                if (authHeader != null && (authHeader.contains("RT-N56U", ignoreCase = true) || authHeader.contains("Padavan", ignoreCase = true))) {
                    return@withContext true
                }

                // Or check the body (Unauthenticated HTTP 200)
                if (response.isSuccessful) {
                    val body = response.body.string()
                    if (body.contains("<title>Padavan</title>", ignoreCase = true) ||
                        body.contains("/bootstrap.js", ignoreCase = true) ||
                        body.contains("login.js", ignoreCase = true)) {
                        return@withContext true
                    }
                }
            }
        } catch (e: Exception) {
            // Silently ignore connection errors during network discovery scans
        }
        return@withContext false
    }

    /**
     * Broadcasts SSDP M-SEARCH packet to discover local devices.
     * Listens for replies up to 2.0s and parses locations.
     */
    suspend fun discoverViaSsdp(): List<String> = withContext(Dispatchers.IO) {
        val discoveredIps = mutableListOf<String>()
        val multicastGroup = "239.255.255.250"
        val port = 1900
        val timeoutMs = 2000

        val mSearchRequest = """
            M-SEARCH * HTTP/1.1
            HOST: $multicastGroup:$port
            MAN: "ssdp:discover"
            MX: 2
            ST: ssdp:all
            
            
        """.trimIndent().replace("\n", "\r\n")

        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.soTimeout = timeoutMs

            val groupAddress = InetAddress.getByName(multicastGroup)
            val sendData = mSearchRequest.toByteArray()
            val sendPacket = DatagramPacket(sendData, sendData.size, groupAddress, port)

            // Send discovery packet
            socket.send(sendPacket)

            // Buffer for receiving replies
            val receiveData = ByteArray(1024)
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                val receivePacket = DatagramPacket(receiveData, receiveData.size)
                try {
                    socket.receive(receivePacket)
                    val response = String(receivePacket.data, 0, receivePacket.length)
                    val ip = receivePacket.address.hostAddress

                    if (ip != null && !discoveredIps.contains(ip)) {
                        // Scan headers of response for Location pointing to UPnP description
                        val lines = response.split("\r\n")
                        var locationUrl: String? = null
                        for (line in lines) {
                            if (line.startsWith("LOCATION:", ignoreCase = true)) {
                                locationUrl = line.substring(9).trim()
                                break
                            }
                        }

                        if (locationUrl != null) {
                            // Verify if it is Padavan
                            if (verifyUpnpDevice(locationUrl)) {
                                discoveredIps.add(ip)
                            }
                        } else {
                            // Fast check/probe if no LOCATION header (fallback)
                            if (probeIp(ip)) {
                                discoveredIps.add(ip)
                            }
                        }
                    }
                } catch (e: SocketTimeoutException) {
                    break
                } catch (e: Exception) {
                    // Ignore packet parsing errors
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            socket?.close()
        }
        return@withContext discoveredIps
    }

    private suspend fun verifyUpnpDevice(locationUrl: String): Boolean = withContext(Dispatchers.IO) {
        val client = if (isPrivateIp(locationUrl)) permissiveClient else standardClient
        try {
            val request = Request.Builder().url(locationUrl).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val xml = response.body.string()
                    if (xml.contains("RT-N56U", ignoreCase = true) ||
                        xml.contains("Padavan", ignoreCase = true) ||
                        xml.contains("newifi", ignoreCase = true)) {
                        return@withContext true
                    }
                }
            }
        } catch (e: Exception) {
            // Fail silently
        }
        return@withContext false
    }
}
