package com.osoterra.mobile.data.remote.dto

import kotlinx.serialization.Serializable

/*
 * Recursos del RESTful API de OsoSense (`/api/v1`), campo por campo igual que los
 * records `interfaces/rest/resources` del Backend. Las fechas llegan como LocalDateTime
 * de Java (ISO sin zona horaria).
 */

// Identity and Access Management
@Serializable
data class UserAccountDto(
    val id: Long,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val professionalLicenseNumber: String? = null,
    val active: Boolean = true,
    val createdAt: String? = null,
)

@Serializable
data class SignUpDto(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val professionalLicenseNumber: String?,
)

@Serializable
data class SignInDto(val email: String, val password: String)

@Serializable
data class AuthenticatedUserDto(val token: String, val expiresAt: String, val user: UserAccountDto)

@Serializable
data class PasswordResetRequestDto(val email: String)

@Serializable
data class ApiErrorDto(val message: String = "", val fieldErrors: Map<String, String> = emptyMap())

// Subscription and Billing
@Serializable
data class SubscriptionPlanDto(
    val id: Long,
    val name: String,
    val priceAmount: Double,
    val priceCurrency: String,
    val billingCycle: String,
    val maxPlots: Int,
    val free: Boolean,
    val active: Boolean,
)

@Serializable
data class SubscriptionDto(
    val id: Long,
    val userAccountId: Long,
    val subscriptionPlanId: Long,
    val status: String,
    val quotaTotal: Int,
    val quotaConsumed: Int,
    val periodStartDate: String,
    val periodEndDate: String? = null,
)

// Farm Management
@Serializable
data class CropDto(
    val id: Long,
    val commonName: String,
    val scientificName: String? = null,
    val salinityThresholdDsM: Double,
    val saltToleranceClass: String,
    val sourceReference: String? = null,
)

@Serializable
data class FarmDto(
    val id: Long,
    val ownerId: Long,
    val name: String,
    val department: String,
    val province: String,
    val district: String,
)

@Serializable
data class RegisterFarmDto(val name: String, val department: String, val province: String, val district: String)

@Serializable
data class PlotDto(
    val id: Long,
    val farmId: Long,
    val cropId: Long? = null,
    val name: String,
    val areaHectares: Double,
    val latitude: Double,
    val longitude: Double,
    val active: Boolean = true,
)

@Serializable
data class RegisterPlotDto(
    val farmId: Long,
    val name: String,
    val areaHectares: Double,
    val latitude: Double,
    val longitude: Double,
)

@Serializable
data class CropIdDto(val cropId: Long)

@Serializable
data class DeviceDto(
    val id: Long,
    val plotId: Long? = null,
    val activationCode: String,
    val status: String,
    val batteryLevel: Int? = null,
    val firmwareVersion: String? = null,
    val lastSeenAt: String? = null,
)

@Serializable
data class ActivationCodeDto(val activationCode: String)

@Serializable
data class PlotIdDto(val plotId: Long)

// Soil Monitoring
@Serializable
data class SoilReadingDto(
    val id: Long,
    val deviceId: Long,
    val plotId: Long,
    val rawConductivityDsM: Double,
    val compensatedConductivityDsM: Double,
    val moisturePercentage: Double,
    val temperatureCelsius: Double,
    val capturedAt: String,
)

@Serializable
data class CalibrationRecordDto(
    val id: Long,
    val deviceId: Long,
    val labConductivityDsM: Double,
    val samplingDate: String,
    val laboratoryName: String,
    val deviceReadingAtSampling: Double,
    val resultingFactor: Double,
)

@Serializable
data class RegisterCalibrationDto(
    val deviceId: Long,
    val labConductivityDsM: Double,
    val samplingDate: String,
    val laboratoryName: String,
    val deviceReadingAtSampling: Double,
)

// Salinity Alerting
@Serializable
data class SalinityAlertDto(
    val id: Long,
    val plotId: Long,
    val soilReadingId: Long,
    val observedConductivityDsM: Double,
    val appliedThresholdDsM: Double,
    val excessRatio: Double,
    val severity: String,
    val status: String,
    val generatedAt: String,
)
