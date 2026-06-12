package com.example.padavancontrol.network

import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class BasicAuthenticator(
    private val usernameProvider: () -> String,
    private val passwordProvider: () -> String
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        // If we already failed with these credentials, don't retry to avoid infinite loop
        if (response.request.header("Authorization") != null) {
            RetrofitClient.notifyUnauthorized()
            return null
        }

        val username = usernameProvider()
        val password = passwordProvider()

        if (username.isEmpty() && password.isEmpty()) {
            return null
        }

        val credential = Credentials.basic(username, password, java.nio.charset.StandardCharsets.UTF_8)
        return response.request.newBuilder()
            .header("Authorization", credential)
            .build()
    }
}
