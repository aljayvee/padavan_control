package com.example.padavancontrol.data.models

data class TrafficSnapshot(
    val timestamp: Long,
    val rxBytes: Long,
    val txBytes: Long,
    val rxSpeed: Double,  // bytes/sec
    val txSpeed: Double   // bytes/sec
)
