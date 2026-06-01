package com.example.padavancontrol.data.models

/**
 * Sealed hierarchy representing discrete router events detected by
 * the background [RouterMonitorWorker]. Each variant maps to a
 * specific notification template in [NotificationHelper].
 */
sealed class RouterEvent {

    /** WAN link dropped — the router lost its internet connection. */
    data class WanDisconnected(
        val previousIp: String
    ) : RouterEvent()

    /** WAN link restored — the router reconnected with a (potentially new) IP. */
    data class WanReconnected(
        val newIp: String
    ) : RouterEvent()

    /** A previously unseen MAC address appeared on the LAN/WLAN. */
    data class NewDeviceJoined(
        val hostname: String,
        val mac: String,
        val ip: String
    ) : RouterEvent()

    /** A previously connected device is no longer visible. */
    data class DeviceLeft(
        val hostname: String
    ) : RouterEvent()

    /** A temperature sensor exceeded the configured threshold. */
    data class HighTemperature(
        val tempCelsius: Int,
        val component: String
    ) : RouterEvent()

    /** CPU usage exceeded the configured threshold. */
    data class HighCpuUsage(
        val percentage: Int
    ) : RouterEvent()

    /** RAM usage exceeded the configured threshold. */
    data class HighMemoryUsage(
        val percentage: Int
    ) : RouterEvent()
}
