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
    /** Fecha y hora ISO local (AAAA-MM-DDTHH:MM) para rotular el eje del gráfico; null si no se conoce. */
    val capturedAt: String? = null,

    val ecDsPerM: Double,
    val humidityPct: Double,
    val temperatureC: Double,
)

/**
 * Severidad de una alerta, en el orden del Backend (WATCH, WARNING, CRITICAL). En pantalla se
 * nombra como el nivel de salinidad equivalente: En vigilancia, Alto y Muy alto.
 */
enum class AlertSeverity { WATCH, HIGH, CRITICAL }

/** Acciones correctivas que acepta el Backend; registrar una resuelve la alerta. */
enum class CorrectiveActionType {
    LEACHING,
    IRRIGATION_ADJUSTMENT,
    DRAINAGE_IMPROVEMENT,
    SOIL_AMENDMENT,
    CROP_ROTATION,
}

data class Alert(
    val id: String,
    val plotId: String,
    val plotName: String,
    val severity: AlertSeverity,
    val message: String,
    val createdAt: String,
    val acknowledged: Boolean,
    /** Datos con los que la pantalla arma el mensaje en el idioma de la app. */
    val observedDsPerM: Double? = null,
    val thresholdDsPerM: Double? = null,
)
