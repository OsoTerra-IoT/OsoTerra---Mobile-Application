package com.osoterra.mobile.domain.model

enum class DeviceStatus { ONLINE, OFFLINE, LOW_BATTERY }

data class Device(
    val id: String,
    val code: String,
    val plotId: String?,
    val plotName: String?,
    val status: DeviceStatus,
    val batteryPct: Int,
    val lastSeen: String?,
)

data class Calibration(
    val id: String,
    val date: String,
    val referenceValueDsPerM: Double,
    val factor: Double,
)
