package com.example.padavancontrol.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Extension property creating a single DataStore instance per Context. */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "padavan_settings"
)

/**
 * Centralized DataStore wrapper for persisting user navigation state,
 * form drafts, monitoring preferences, and last-known router state
 * for background event detection.
 */
class SettingsDataStore(private val applicationContext: Context) {

    // ─── Per-Category Last-Visited Section ─────────────────────────

    private fun lastSectionKey(category: String) =
        stringPreferencesKey("last_section_$category")

    /** Save the last-visited section within an Advanced category. */
    suspend fun saveLastSection(category: String, section: String) {
        applicationContext.dataStore.edit { prefs ->
            prefs[lastSectionKey(category)] = section
        }
    }

    /** Observe the last-visited section for a given category. */
    fun getLastSection(category: String): Flow<String?> =
        applicationContext.dataStore.data.map { prefs ->
            prefs[lastSectionKey(category)]
        }

    // ─── Advanced Hub Expansion State ──────────────────────────────

    private val hubExpandedCategoriesKey = stringSetPreferencesKey("hub_expanded_categories")

    suspend fun saveHubExpansionState(expandedCategories: Set<String>) {
        applicationContext.dataStore.edit { prefs ->
            prefs[hubExpandedCategoriesKey] = expandedCategories
        }
    }

    fun getHubExpansionState(): Flow<Set<String>> =
        applicationContext.dataStore.data.map { prefs ->
            prefs[hubExpandedCategoriesKey] ?: emptySet()
        }

    // ─── Background Monitoring Preferences ─────────────────────────

    private val monitoringEnabledKey = booleanPreferencesKey("monitoring_enabled")
    private val monitorIntervalMinutesKey = intPreferencesKey("monitor_interval_minutes")

    suspend fun setMonitoringEnabled(enabled: Boolean) {
        applicationContext.dataStore.edit { prefs ->
            prefs[monitoringEnabledKey] = enabled
        }
    }

    fun isMonitoringEnabled(): Flow<Boolean> =
        applicationContext.dataStore.data.map { prefs ->
            prefs[monitoringEnabledKey] ?: false
        }

    suspend fun setMonitorIntervalMinutes(minutes: Int) {
        applicationContext.dataStore.edit { prefs ->
            prefs[monitorIntervalMinutesKey] = minutes
        }
    }

    fun getMonitorIntervalMinutes(): Flow<Int> =
        applicationContext.dataStore.data.map { prefs ->
            prefs[monitorIntervalMinutesKey] ?: 15
        }

    // ─── Last-Known Router State (for change detection) ────────────

    private val lastKnownWanIpKey = stringPreferencesKey("last_known_wan_ip")
    private val lastKnownDeviceMacsKey = stringSetPreferencesKey("last_known_device_macs")
    private val lastKnownDeviceCountKey = intPreferencesKey("last_known_device_count")
    private val lastKnownOnlineKey = booleanPreferencesKey("last_known_online")

    suspend fun saveLastKnownOnline(online: Boolean) {
        applicationContext.dataStore.edit { prefs ->
            prefs[lastKnownOnlineKey] = online
        }
    }

    fun getLastKnownOnline(): Flow<Boolean?> =
        applicationContext.dataStore.data.map { prefs ->
            prefs[lastKnownOnlineKey]
        }

    suspend fun saveLastKnownWanIp(ip: String) {
        applicationContext.dataStore.edit { prefs ->
            prefs[lastKnownWanIpKey] = ip
        }
    }

    fun getLastKnownWanIp(): Flow<String?> =
        applicationContext.dataStore.data.map { prefs ->
            prefs[lastKnownWanIpKey]
        }

    suspend fun saveLastKnownDeviceMacs(macs: Set<String>) {
        applicationContext.dataStore.edit { prefs ->
            prefs[lastKnownDeviceMacsKey] = macs
            prefs[lastKnownDeviceCountKey] = macs.size
        }
    }

    fun getLastKnownDeviceMacs(): Flow<Set<String>> =
        applicationContext.dataStore.data.map { prefs ->
            prefs[lastKnownDeviceMacsKey] ?: emptySet()
        }

    fun getLastKnownDeviceCount(): Flow<Int> =
        applicationContext.dataStore.data.map { prefs ->
            prefs[lastKnownDeviceCountKey] ?: 0
        }

    // ─── Notification Preferences ──────────────────────────────────

    private val notifyWanDisconnectKey = booleanPreferencesKey("notify_wan_disconnect")
    private val notifyNewDeviceKey = booleanPreferencesKey("notify_new_device")
    private val notifyHighTempKey = booleanPreferencesKey("notify_high_temp")
    private val highTempThresholdKey = intPreferencesKey("high_temp_threshold")

    suspend fun setNotifyWanDisconnect(enabled: Boolean) {
        applicationContext.dataStore.edit { prefs -> prefs[notifyWanDisconnectKey] = enabled }
    }

    fun isNotifyWanDisconnect(): Flow<Boolean> =
        applicationContext.dataStore.data.map { prefs -> prefs[notifyWanDisconnectKey] ?: true }

    suspend fun setNotifyNewDevice(enabled: Boolean) {
        applicationContext.dataStore.edit { prefs -> prefs[notifyNewDeviceKey] = enabled }
    }

    fun isNotifyNewDevice(): Flow<Boolean> =
        applicationContext.dataStore.data.map { prefs -> prefs[notifyNewDeviceKey] ?: true }

    suspend fun setNotifyHighTemp(enabled: Boolean) {
        applicationContext.dataStore.edit { prefs -> prefs[notifyHighTempKey] = enabled }
    }

    fun isNotifyHighTemp(): Flow<Boolean> =
        applicationContext.dataStore.data.map { prefs -> prefs[notifyHighTempKey] ?: true }

    suspend fun setHighTempThreshold(celsius: Int) {
        applicationContext.dataStore.edit { prefs -> prefs[highTempThresholdKey] = celsius }
    }

    fun getHighTempThreshold(): Flow<Int> =
        applicationContext.dataStore.data.map { prefs -> prefs[highTempThresholdKey] ?: 75 }

    // ─── App Language Mode ─────────────────────────────────────────

    private val appLanguageModeKey = stringPreferencesKey("app_language_mode")

    suspend fun saveAppLanguageMode(mode: String) {
        applicationContext.dataStore.edit { prefs ->
            prefs[appLanguageModeKey] = mode
        }
    }

    fun getAppLanguageMode(): Flow<String> =
        applicationContext.dataStore.data.map { prefs ->
            // Migrate legacy "EN" value to "EN_ALL"
            val stored = prefs[appLanguageModeKey] ?: "EN_ALL"
            if (stored == "EN") "EN_ALL" else stored
        }
}
