package com.example.padavancontrol.network

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response

class BasicAuthInterceptor(
    private val usernameProvider: () -> String,
    private val passwordProvider: () -> String,
    private val targetHostProvider: () -> String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val targetHost = targetHostProvider()
        val requestHost = originalRequest.url.host

        val username = usernameProvider()
        val password = passwordProvider()

        if (username.isEmpty() && password.isEmpty()) {
            return chain.proceed(originalRequest)
        }

        // Only add authorization header if requestHost matches targetHost
        if (targetHost.isNotEmpty() && requestHost.equals(targetHost, ignoreCase = true)) {
            val credential = Credentials.basic(username, password, java.nio.charset.StandardCharsets.UTF_8)
            val authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", credential)
                .build()
            
            val response = chain.proceed(authenticatedRequest)
            if (response.code == 401) {
                RetrofitClient.notifyUnauthorized()
            }
            return response
        }

        return chain.proceed(originalRequest)
    }
}
