package com.example.padavancontrol.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate
import okhttp3.ConnectionSpec
import okhttp3.TlsVersion

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.channels.BufferOverflow

/**
 * Thread-safe singleton managing OkHttp + Retrofit instances.
 *
 * Uses an idempotent [initialize] that only rebuilds when the IP address
 * actually changes, and a [forceReinitialize] escape-hatch for credential
 * rotations (e.g., after the user edits Settings → Router IP).
 */
object RetrofitClient {
    private val _unauthorizedEvents = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val unauthorizedEvents: SharedFlow<Unit> = _unauthorizedEvents.asSharedFlow()

    fun notifyUnauthorized() {
        _unauthorizedEvents.tryEmit(Unit)
    }

    private var currentIp: String? = null

    @Volatile
    private var usernameProvider: () -> String = { "" }

    @Volatile
    private var passwordProvider: () -> String = { "" }

    @Volatile
    private var retrofitInstance: Retrofit? = null

    @Volatile
    private var apiServiceInstance: PadavanApiService? = null

    @Volatile
    private var activeClient: OkHttpClient? = null

    /**
     * Idempotent initialization. Only rebuilds the HTTP stack when the
     * resolved base URL differs from the one already in use.
     */
    fun initialize(
        ipAddress: String,
        usernameProvider: () -> String,
        passwordProvider: () -> String
    ) {
        val formattedIp = formatIp(ipAddress)

        // Lock-free fast path for already initialized state
        if (formattedIp == currentIp && apiServiceInstance != null) {
            this.usernameProvider = usernameProvider
            this.passwordProvider = passwordProvider
            return
        }

        synchronized(this) {
            // Double-check under lock
            if (formattedIp == currentIp && apiServiceInstance != null) {
                this.usernameProvider = usernameProvider
                this.passwordProvider = passwordProvider
                return
            }

            this.currentIp = formattedIp
            this.usernameProvider = usernameProvider
            this.passwordProvider = passwordProvider
            rebuildClient()
        }
    }

    /**
     * Force a full rebuild regardless of whether the IP changed.
     * Use when credentials have been modified in SettingsScreen.
     */
    @Synchronized
    fun forceReinitialize(
        ipAddress: String,
        usernameProvider: () -> String,
        passwordProvider: () -> String
    ) {
        this.currentIp = formatIp(ipAddress)
        this.usernameProvider = usernameProvider
        this.passwordProvider = passwordProvider
        rebuildClient()
    }

    fun getApiService(): PadavanApiService {
        return apiServiceInstance
            ?: throw IllegalStateException("RetrofitClient.initialize() must be called before getApiService()")
    }

    /** Returns true if a valid API service has been built. */
    fun isInitialized(): Boolean = apiServiceInstance != null

    internal fun formatIp(ipAddress: String): String {
        val trimmed = ipAddress.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        }
        val isHttpsPort = trimmed.endsWith(":443") || trimmed.contains(":443/") ||
                          trimmed.endsWith(":8443") || trimmed.contains(":8443/")
        val scheme = if (isHttpsPort) "https://" else "http://"
        return if (trimmed.endsWith("/")) "$scheme$trimmed" else "$scheme$trimmed/"
    }

    private val allowedCipherSuites = listOf(
        // DH+AESGCM
        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",

        // DH+AES256
        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",

        // DH+AES
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",

        // DH+3DES
        "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
        "TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA",

        // RSA+AES
        "TLS_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_RSA_WITH_AES_256_CBC_SHA256",
        "TLS_RSA_WITH_AES_128_CBC_SHA",
        "TLS_RSA_WITH_AES_256_CBC_SHA",

        // RSA+3DES
        "TLS_RSA_WITH_3DES_EDE_CBC_SHA"
    )

    val customConnectionSpec: ConnectionSpec by lazy {
        ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)
            .cipherSuites(*allowedCipherSuites.toTypedArray())
            .build()
    }

    private fun rebuildClient() {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val authInterceptor = BasicAuthInterceptor(
            usernameProvider = { this.usernameProvider() },
            passwordProvider = { this.passwordProvider() },
            targetHostProvider = {
                try {
                    java.net.URI(currentIp ?: "").host ?: ""
                } catch (e: Exception) {
                    ""
                }
            }
        )

        val timeoutInterceptor = okhttp3.Interceptor { chain ->
            val request = chain.request()
            val timeoutHeader = request.header("Custom-Read-Timeout")
            if (timeoutHeader != null) {
                val timeoutMs = timeoutHeader.toLongOrNull() ?: 5000L
                chain.withReadTimeout(timeoutMs.toInt(), TimeUnit.MILLISECONDS)
                    .proceed(request.newBuilder().removeHeader("Custom-Read-Timeout").build())
            } else {
                chain.proceed(request)
            }
        }

        val authenticator = BasicAuthenticator(
            usernameProvider = { this.usernameProvider() },
            passwordProvider = { this.passwordProvider() }
        )

        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(authenticator)
            .addInterceptor(timeoutInterceptor)
            .addInterceptor(logging)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .connectionSpecs(listOf(customConnectionSpec, ConnectionSpec.CLEARTEXT))

        val isHttps = currentIp?.startsWith("https://") == true
        if (isHttps) {
            try {
                val trustAllCerts = arrayOf<TrustManager>(
                    object : X509TrustManager {
                        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                    }
                )

                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllCerts, java.security.SecureRandom())
                
                clientBuilder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                clientBuilder.hostnameVerifier { _, _ -> true }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val client = clientBuilder.build()

        activeClient?.let { oldClient ->
            try {
                oldClient.dispatcher.executorService.shutdown()
                oldClient.connectionPool.evictAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        activeClient = client

        val retrofit = Retrofit.Builder()
            .baseUrl(currentIp!!)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()

        retrofitInstance = retrofit
        apiServiceInstance = retrofit.create(PadavanApiService::class.java)
    }
}
