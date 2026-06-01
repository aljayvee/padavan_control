package com.example.padavancontrol.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Thread-safe singleton managing OkHttp + Retrofit instances.
 *
 * Uses an idempotent [initialize] that only rebuilds when the IP address
 * actually changes, and a [forceReinitialize] escape-hatch for credential
 * rotations (e.g., after the user edits Settings → Router IP).
 */
object RetrofitClient {
    private var currentIp: String? = null

    @Volatile
    private var usernameProvider: () -> String = { "" }

    @Volatile
    private var passwordProvider: () -> String = { "" }

    @Volatile
    private var retrofitInstance: Retrofit? = null

    @Volatile
    private var apiServiceInstance: PadavanApiService? = null

    /**
     * Idempotent initialization. Only rebuilds the HTTP stack when the
     * resolved base URL differs from the one already in use.
     */
    @Synchronized
    fun initialize(
        ipAddress: String,
        usernameProvider: () -> String,
        passwordProvider: () -> String
    ) {
        val formattedIp = formatIp(ipAddress)

        // Skip rebuild when the base URL hasn't changed
        if (formattedIp == currentIp && apiServiceInstance != null) {
            // Still update credential providers (they're cheap lambdas)
            this.usernameProvider = usernameProvider
            this.passwordProvider = passwordProvider
            return
        }

        this.currentIp = formattedIp
        this.usernameProvider = usernameProvider
        this.passwordProvider = passwordProvider
        rebuildClient()
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

    private fun formatIp(ipAddress: String): String {
        return if (ipAddress.startsWith("http://") || ipAddress.startsWith("https://")) {
            if (ipAddress.endsWith("/")) ipAddress else "$ipAddress/"
        } else {
            "http://$ipAddress/"
        }
    }

    private fun rebuildClient() {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val authInterceptor = BasicAuthInterceptor(
            usernameProvider = { this.usernameProvider() },
            passwordProvider = { this.passwordProvider() }
        )

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(currentIp!!)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()

        retrofitInstance = retrofit
        apiServiceInstance = retrofit.create(PadavanApiService::class.java)
    }
}
