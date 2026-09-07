package com.osoterra.mobile.domain.model

enum class UserRole { PRODUCER, ADVISOR }

data class User(
    val id: String,
    val fullName: String,
    val email: String,
    val role: UserRole,
)

data class RegisterData(
    val fullName: String,
    val email: String,
    val password: String,
    val role: UserRole,

    val licenseNumber: String? = null,
)

data class NewPlot(
    val farmId: String,
    val name: String,
    val areaHectares: Double,
    val latitude: Double?,
    val longitude: Double?,
    val cropId: String?,
)

data class Crop(
    val id: String,
    val name: String,

    val thresholdDsPerM: Double,

    val toleranceClass: String,
)

data class Farm(
    val id: String,
    val name: String,
    val department: String,
    val province: String,
    val district: String,
)

data class Plot(
    val id: String,
    val name: String,
    val farmName: String,
    val areaHectares: Double,
    val crop: Crop?,
    val salinityLevel: SalinityLevel,

    val lastEcDsPerM: Double?,

    val lastReadingAt: String?,
    val deviceOnline: Boolean,
)

data class Reading(
    val id: String,
    val timestamp: String,

    val ecDsPerM: Double,
    val humidityPct: Double,
    val temperatureC: Double,
)

enum class AlertSeverity { LOW, MEDIUM, HIGH, CRITICAL }

data class Alert(
    val id: String,
    val plotId: String,
    val plotName: String,
    val severity: AlertSeverity,
    val message: String,
    val createdAt: String,
    val acknowledged: Boolean,
)
