@file:Suppress("DEPRECATION")

package com.example.padavancontrol.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class CredentialStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = try {
        createEncryptedSharedPreferences(context, masterKey)
    } catch (e: Exception) {
        e.printStackTrace()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.deleteSharedPreferences("secure_padavan_prefs")
            } else {
                val sharedPrefsFile = java.io.File(context.filesDir.parent, "shared_prefs/secure_padavan_prefs.xml")
                if (sharedPrefsFile.exists()) {
                    sharedPrefsFile.delete()
                }
            }
        } catch (delEx: Exception) {
            delEx.printStackTrace()
        }
        createEncryptedSharedPreferences(context, masterKey)
    }

    private fun createEncryptedSharedPreferences(context: Context, key: MasterKey): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            "secure_padavan_prefs",
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveCredentials(
        username: String,
        ipAddress: String,
        password: String,
        remember: Boolean,
        useBiometric: Boolean
    ) {
        sharedPreferences.edit().apply {
            putString("username", username)
            putString("router_ip", ipAddress)
            putString("password", password)
            putBoolean("remember_credentials", remember)
            putBoolean("use_biometric", useBiometric)
            apply()
        }
    }

    fun getUsername(): String = sharedPreferences.getString("username", "admin") ?: "admin"
    fun getRouterIp(): String =
        sharedPreferences.getString("router_ip", "192.168.2.2") ?: "192.168.2.2"

    fun getPassword(): String = sharedPreferences.getString("password", "") ?: ""
    fun isRememberCredentials(): Boolean =
        sharedPreferences.getBoolean("remember_credentials", false)

    fun isUseBiometric(): Boolean = sharedPreferences.getBoolean("use_biometric", false)

    fun clearCredentials() {
        sharedPreferences.edit().apply {
            remove("username")
            remove("password")
            remove("remember_credentials")
            remove("use_biometric")
            apply()
        }
    }

    fun saveRouterIp(ipAddress: String) {
        sharedPreferences.edit().putString("router_ip", ipAddress).apply()
    }

    fun saveHardwareModel(productId: String) {
        sharedPreferences.edit().putString("hardware_model", productId).apply()
    }

    fun getHardwareModel(): String = sharedPreferences.getString("hardware_model", "") ?: ""

    fun isOnboardingCompleted(): Boolean = sharedPreferences.getBoolean("onboarding_completed", false)

    fun setOnboardingCompleted(completed: Boolean) {
        sharedPreferences.edit().putBoolean("onboarding_completed", completed).apply()
    }
}
