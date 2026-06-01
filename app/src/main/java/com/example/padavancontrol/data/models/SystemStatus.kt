package com.example.padavancontrol.data.models

data class SystemStatus(
    val cpuUsage: Int,
    val ramTotal: Long,
    val ramUsed: Long,
    val uptime: Long,
    val cpuTemp: Float? = null,
    val wifiTemp: Float? = null,
    // New parameters appended with default values to preserve positional backward compatibility
    val cpuTotal: Long = 0L,
    val cpuBusy: Long = 0L,
    val ramFree: Long = 0L,
    val ramCached: Long = 0L,
    val ramBuffers: Long = 0L,
    val swapTotal: Long = 0L,
    val swapUsed: Long = 0L,
    val loadAvg: String = "0.00 0.00 0.00"
)
