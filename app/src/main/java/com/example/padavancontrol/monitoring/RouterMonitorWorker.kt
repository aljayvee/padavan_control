package com.example.padavancontrol.monitoring

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.padavancontrol.data.CredentialStore
import com.example.padavancontrol.data.DefaultPadavanRepository
import com.example.padavancontrol.data.SettingsDataStore
import kotlinx.coroutines.flow.first

class RouterMonitorWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val settingsDataStore = SettingsDataStore(context)

        // Check if monitoring is enabled
        val isEnabled = settingsDataStore.isMonitoringEnabled().first()
        if (!isEnabled) {
            return Result.success()
        }

        val credentialStore = CredentialStore(context)
        val repository = DefaultPadavanRepository(credentialStore)
        val notificationHelper = NotificationHelper(context)

        try {
            // 1. Fetch system status (for temperature alerts)
            repository.getSystemStatus().collect { statusResult ->
                statusResult.onSuccess { status ->
                    val shouldNotifyTemp = settingsDataStore.isNotifyHighTemp().first()
                    val threshold = settingsDataStore.getHighTempThreshold().first()
                    val cpuTemp = status.cpuTemp ?: 0f

                    if (shouldNotifyTemp && cpuTemp > threshold) {
                        notificationHelper.showNotification(
                            "High CPU Temperature Alert 🌡️",
                            "Your router CPU temperature is extremely high: ${status.cpuTemp} (Threshold: $threshold°C)"
                        )
                    }
                }
            }

            // 2. Fetch WAN status (for connection alerts)
            repository.getWanStatus().collect { wanResult ->
                wanResult.onSuccess { wan ->
                    val lastWanIp = settingsDataStore.getLastKnownWanIp().first()
                    val currentWanIp = wan.wanIp

                    val shouldNotifyWan = settingsDataStore.isNotifyWanDisconnect().first()

                    if (shouldNotifyWan) {
                        if (currentWanIp == "0.0.0.0" || currentWanIp.isEmpty()) {
                            // WAN is disconnected
                            notificationHelper.showNotification(
                                "WAN Connection Disconnected 🔴",
                                "The internet gateway connection was lost. External IP is empty."
                            )
                        } else if (lastWanIp != null && lastWanIp != currentWanIp && lastWanIp != "0.0.0.0") {
                            // IP changed
                            notificationHelper.showNotification(
                                "WAN Connection Reassigned 🌐",
                                "The internet connection is restored! External IP: $currentWanIp"
                            )
                        }
                    }

                    // Persist current WAN IP
                    settingsDataStore.saveLastKnownWanIp(currentWanIp)
                }
            }

            // 3. Fetch active LAN clients (for new device alerts)
            repository.getLanClients().collect { clientsResult ->
                clientsResult.onSuccess { clients ->
                    val lastDeviceMacs = settingsDataStore.getLastKnownDeviceMacs().first()
                    val currentDeviceMacs = clients.map { it.macAddress }.toSet()

                    val shouldNotifyNewDevice = settingsDataStore.isNotifyNewDevice().first()

                    if (shouldNotifyNewDevice && lastDeviceMacs.isNotEmpty()) {
                        val newDevices = clients.filter { it.macAddress !in lastDeviceMacs }
                        for (dev in newDevices) {
                            val name = dev.hostname.ifEmpty { dev.ipAddress }
                            notificationHelper.showNotification(
                                "New Device Connected 📱",
                                "Device '$name' (${dev.macAddress}) has joined the network."
                            )
                        }
                    }

                    // Persist current devices MACs
                    settingsDataStore.saveLastKnownDeviceMacs(currentDeviceMacs)
                }
            }

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}
