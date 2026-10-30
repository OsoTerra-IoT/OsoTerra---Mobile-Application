package com.osoterra.mobile.data.remote.api

import com.osoterra.mobile.data.remote.dto.ActivationCodeDto
import com.osoterra.mobile.data.remote.dto.AdvisoryLinkDto
import com.osoterra.mobile.data.remote.dto.NotificationPreferenceDto
import com.osoterra.mobile.data.remote.dto.RecordCorrectiveActionDto
import com.osoterra.mobile.data.remote.dto.SetNotificationPreferenceDto
import com.osoterra.mobile.data.remote.dto.AuthenticatedUserDto
import com.osoterra.mobile.data.remote.dto.CalibrationRecordDto
import com.osoterra.mobile.data.remote.dto.CropDto
import com.osoterra.mobile.data.remote.dto.CropIdDto
import com.osoterra.mobile.data.remote.dto.DeviceDto
import com.osoterra.mobile.data.remote.dto.FarmDto
import com.osoterra.mobile.data.remote.dto.PasswordResetRequestDto
import com.osoterra.mobile.data.remote.dto.PlotDto
import com.osoterra.mobile.data.remote.dto.PlotIdDto
import com.osoterra.mobile.data.remote.dto.RegisterCalibrationDto
import com.osoterra.mobile.data.remote.dto.RegisterFarmDto
import com.osoterra.mobile.data.remote.dto.RegisterPlotDto
import com.osoterra.mobile.data.remote.dto.SalinityAlertDto
import com.osoterra.mobile.data.remote.dto.SignInDto
import com.osoterra.mobile.data.remote.dto.SignUpDto
import com.osoterra.mobile.data.remote.dto.SoilReadingDto
import com.osoterra.mobile.data.remote.dto.SubscriptionDto
import com.osoterra.mobile.data.remote.dto.SubscriptionPlanDto
import com.osoterra.mobile.data.remote.dto.UserAccountDto
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Endpoints del RESTful API de OsoSense que usa la app, agrupados por bounded context. */
interface OsoTerraApi {

    // Identity and Access Management
    @POST("auth/signup")
    suspend fun signUp(@Body body: SignUpDto): UserAccountDto

    @POST("auth/signin")
    suspend fun signIn(@Body body: SignInDto): AuthenticatedUserDto

    @POST("auth/password-reset-requests")
    suspend fun requestPasswordReset(@Body body: PasswordResetRequestDto)

    @GET("users/me")
    suspend fun currentUser(): UserAccountDto

    @GET("users/me/linked-accounts")
    suspend fun linkedAccounts(): List<UserAccountDto>

    @GET("advisory-links/mine")
    suspend fun myAdvisoryLinks(): List<AdvisoryLinkDto>

    @POST("advisory-links/{id}/acceptance")
    suspend fun acceptAdvisoryLink(@Path("id") id: Long): AdvisoryLinkDto

    @POST("advisory-links/{id}/revocation")
    suspend fun revokeAdvisoryLink(@Path("id") id: Long): AdvisoryLinkDto

    // Subscription and Billing
    @GET("subscription-plans")
    suspend fun subscriptionPlans(): List<SubscriptionPlanDto>

    @GET("subscriptions/mine")
    suspend fun mySubscriptions(): List<SubscriptionDto>

    // Farm Management
    @GET("crops")
    suspend fun crops(): List<CropDto>

    @GET("farms/mine")
    suspend fun myFarms(): List<FarmDto>

    @GET("farms")
    suspend fun farmsOf(@Query("ownerId") ownerId: Long): List<FarmDto>

    @POST("farms")
    suspend fun registerFarm(@Body body: RegisterFarmDto): FarmDto

    @GET("plots")
    suspend fun plotsOf(@Query("farmId") farmId: Long): List<PlotDto>

    @GET("plots/{id}")
    suspend fun plot(@Path("id") id: Long): PlotDto

    @POST("plots")
    suspend fun registerPlot(@Body body: RegisterPlotDto): PlotDto

    @POST("plots/{id}/crop")
    suspend fun assignCrop(@Path("id") plotId: Long, @Body body: CropIdDto): PlotDto

    @POST("plots/{id}/deactivation")
    suspend fun deactivatePlot(@Path("id") plotId: Long): PlotDto

    @GET("devices")
    suspend fun devicesOf(@Query("plotId") plotId: Long): List<DeviceDto>

    @POST("devices")
    suspend fun registerDevice(@Body body: ActivationCodeDto): DeviceDto

    @POST("devices/{id}/attachment")
    suspend fun attachDevice(@Path("id") deviceId: Long, @Body body: PlotIdDto): DeviceDto

    // Soil Monitoring
    @GET("soil-readings")
    suspend fun readingsOf(@Query("plotId") plotId: Long): List<SoilReadingDto>

    @GET("calibration-records")
    suspend fun calibrationsOf(@Query("deviceId") deviceId: Long): List<CalibrationRecordDto>

    @POST("calibration-records")
    suspend fun registerCalibration(@Body body: RegisterCalibrationDto): CalibrationRecordDto

    // Salinity Alerting
    @GET("salinity-alerts")
    suspend fun alertsOf(@Query("plotId") plotId: Long): List<SalinityAlertDto>

    @POST("salinity-alerts/{id}/acknowledgement")
    suspend fun acknowledgeAlert(@Path("id") alertId: Long): SalinityAlertDto

    @POST("salinity-alerts/{id}/corrective-actions")
    suspend fun recordCorrectiveAction(
        @Path("id") alertId: Long,
        @Body body: RecordCorrectiveActionDto,
    ): JsonObject

    @GET("notification-preferences/mine")
    suspend fun myNotificationPreference(): NotificationPreferenceDto

    @PUT("notification-preferences/mine")
    suspend fun setNotificationPreference(@Body body: SetNotificationPreferenceDto): NotificationPreferenceDto
}
