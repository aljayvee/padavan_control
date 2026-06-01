package com.example.padavancontrol.network

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response

class BasicAuthInterceptor(
    private val usernameProvider: () -> String,
    private val passwordProvider: () -> String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val username = usernameProvider()
        val password = passwordProvider()

        if (username.isEmpty() && password.isEmpty()) {
            return chain.proceed(originalRequest)
        }

        val credential = Credentials.basic(username, password)
        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", credential)
            .build()
        return chain.proceed(authenticatedRequest)
    }
}
