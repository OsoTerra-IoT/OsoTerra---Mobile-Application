package com.osoterra.mobile.data.remote

import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.data.remote.api.OsoTerraApi
import com.osoterra.mobile.data.remote.dto.ActivationCodeDto
import com.osoterra.mobile.data.remote.dto.CropDto
import com.osoterra.mobile.data.remote.dto.CropIdDto
import com.osoterra.mobile.data.remote.dto.DeviceDto
import com.osoterra.mobile.data.remote.dto.FarmDto
import com.osoterra.mobile.data.remote.dto.PasswordResetRequestDto
import com.osoterra.mobile.data.remote.dto.PlotDto
import com.osoterra.mobile.data.remote.dto.PlotIdDto
import com.osoterra.mobile.data.remote.dto.RecordCorrectiveActionDto
import com.osoterra.mobile.data.remote.dto.RegisterCalibrationDto
import com.osoterra.mobile.data.remote.dto.RegisterFarmDto
import com.osoterra.mobile.data.remote.dto.RegisterPlotDto
import com.osoterra.mobile.data.remote.dto.SignInDto
import com.osoterra.mobile.data.remote.dto.SignUpDto
import com.osoterra.mobile.data.remote.dto.SoilReadingDto
import com.osoterra.mobile.data.repository.SessionStore
import com.osoterra.mobile.domain.model.Alert
import com.osoterra.mobile.domain.model.CorrectiveActionType
import com.osoterra.mobile.domain.model.NotificationPreferences
import com.osoterra.mobile.domain.model.Calibration
import com.osoterra.mobile.domain.model.Crop
import com.osoterra.mobile.domain.model.Device
import com.osoterra.mobile.domain.model.DeviceStatus
import com.osoterra.mobile.domain.model.Farm
import com.osoterra.mobile.domain.model.NewPlot
import com.osoterra.mobile.domain.model.Plot
import com.osoterra.mobile.domain.model.Reading
import com.osoterra.mobile.domain.model.RegisterData
import com.osoterra.mobile.domain.model.Subscription
import com.osoterra.mobile.domain.model.SupervisedPlot
import com.osoterra.mobile.domain.model.User
import com.osoterra.mobile.domain.model.UserRole
import com.osoterra.mobile.domain.repository.AdvisorRepository
import com.osoterra.mobile.domain.repository.AlertRepository
import com.osoterra.mobile.domain.repository.AuthRepository
import com.osoterra.mobile.domain.repository.DeviceRepository
import com.osoterra.mobile.domain.repository.FarmRepository
import com.osoterra.mobile.domain.repository.NotificationPreferencesRepository
import com.osoterra.mobile.domain.repository.PlotRepository
import com.osoterra.mobile.domain.repository.SubscriptionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.Locale

class AuthRepositoryRemote(
    private val api: OsoTerraApi,
    private val sessionStore: SessionStore,
    private val onSession: (String?) -> Unit,
) : AuthRepository {

    override suspend fun login(email: String, password: String): DataResult<User> {
        if (email.isBlank() || password.isBlank()) return DataResult.Error("Ingresa tu correo y contraseña")
        val result = apiCall { api.signIn(SignInDto(email.trim().lowercase(), password)) }
        return when (result) {
            is DataResult.Success -> {
                val user = result.data.user.toUser()
                sessionStore.save(result.data.token, user)
                onSession(result.data.token)
                DataResult.Success(user)
            }
            is DataResult.Error -> DataResult.Error(
                if (result.message.startsWith("Tu sesión")) "Correo o contraseña incorrectos" else result.message,
                result.cause,
            )
        }
    }

    override suspend fun register(data: RegisterData): DataResult<User> {
        if (data.role == UserRole.ADVISOR && data.licenseNumber.isNullOrBlank()) {
            return DataResult.Error("El asesor debe indicar su número de colegiatura")
        }
        val (firstName, lastName) = splitFullName(data.fullName)
        val signUp = apiCall {
            api.signUp(
                SignUpDto(
                    email = data.email.trim().lowercase(),
                    password = data.password,
                    firstName = firstName,
                    lastName = lastName,
                    role = if (data.role == UserRole.ADVISOR) "ADVISOR" else "FARMER",
                    professionalLicenseNumber = data.licenseNumber?.trim()?.takeIf { data.role == UserRole.ADVISOR },
                ),
            )
        }
        if (signUp is DataResult.Error) return signUp
        return login(data.email, data.password)
    }

    override suspend fun requestPasswordReset(email: String): DataResult<Unit> {
        if (email.isBlank()) return DataResult.Error("Ingresa tu correo")
        return apiCall { api.requestPasswordReset(PasswordResetRequestDto(email.trim().lowercase())) }
    }

    override suspend fun logout() {
        sessionStore.clear()
        onSession(null)
    }

    override suspend fun currentUser(): User? = sessionStore.readUser()
}

/**
 * Lee lo que la persona usuaria puede ver: sus fincas si es productor, o las de los
 * productores con vínculo de asesoría aceptado si es asesor.
 */
class RemoteMonitoringReader(
    private val api: OsoTerraApi,
    private val sessionStore: SessionStore,
) {
    suspend fun farms(): List<FarmDto> {
        val user = sessionStore.readUser()
        return if (user?.role == UserRole.ADVISOR) {
            coroutineScope {
                api.linkedAccounts().map { farmer -> async { api.farmsOf(farmer.id) } }.awaitAll().flatten()
            }
        } else {
            api.myFarms()
        }
    }

    suspend fun plots(): List<Pair<FarmDto, PlotDto>> = coroutineScope {
        farms().map { farm -> async { api.plotsOf(farm.id).filter { it.active }.map { farm to it } } }
            .awaitAll()
            .flatten()
    }

    private var cropCache: List<CropDto>? = null
    suspend fun crops(): List<CropDto> = cropCache ?: api.crops().also { cropCache = it }

    suspend fun toPlot(farm: FarmDto, plot: PlotDto): Plot = coroutineScope {
        val readings = async { api.readingsOf(plot.id) }
        val devices = async { api.devicesOf(plot.id) }
        val crop = plot.cropId?.let { id -> crops().firstOrNull { it.id == id } }
        val latest = readings.await().maxByOrNull { it.capturedAt }
        Plot(
            id = plot.id.toString(),
            name = plot.name,
            farmName = farm.name,
            areaHectares = plot.areaHectares,
            crop = crop?.toCrop(),
            salinityLevel = salinityLevel(latest?.compensatedConductivityDsM, crop?.salinityThresholdDsM),
            lastEcDsPerM = latest?.compensatedConductivityDsM,
            lastReadingAt = displayDateTime(latest?.capturedAt),
            deviceOnline = devices.await().any { it.status == "ACTIVE" },
        )
    }

    suspend fun readings(plotId: Long): List<SoilReadingDto> =
        api.readingsOf(plotId).sortedBy { it.capturedAt }
}

class FarmRepositoryRemote(
    private val api: OsoTerraApi,
    private val reader: RemoteMonitoringReader,
) : FarmRepository {

    override suspend fun getFarms(): DataResult<List<Farm>> = apiCall { reader.farms().map { it.toFarm() } }

    override suspend fun createFarm(
        name: String,
        department: String,
        province: String,
        district: String,
    ): DataResult<Farm> {
        if (listOf(name, department, province, district).any { it.isBlank() }) {
            return DataResult.Error("Completa el nombre, departamento, provincia y distrito")
        }
        return apiCall {
            api.registerFarm(RegisterFarmDto(name.trim(), department.trim(), province.trim(), district.trim())).toFarm()
        }
    }

    private fun FarmDto.toFarm() = Farm(id.toString(), name, department, province, district)
}

class PlotRepositoryRemote(
    private val api: OsoTerraApi,
    private val reader: RemoteMonitoringReader,
) : PlotRepository {

    override suspend fun getPlots(): DataResult<List<Plot>> = apiCall {
        coroutineScope { reader.plots().map { (farm, plot) -> async { reader.toPlot(farm, plot) } }.awaitAll() }
    }

    override suspend fun getPlot(plotId: String): DataResult<Plot> = apiCall {
        val plot = api.plot(plotId.toLong())
        val farm = reader.farms().first { it.id == plot.farmId }
        reader.toPlot(farm, plot)
    }

    override suspend fun getReadings(plotId: String): DataResult<List<Reading>> =
        apiCall { reader.readings(plotId.toLong()).map { it.toReading() } }

    override suspend fun createPlot(plot: NewPlot): DataResult<Plot> {
        if (plot.name.isBlank()) return DataResult.Error("El nombre de la parcela es obligatorio")
        if (plot.areaHectares <= 0.0) return DataResult.Error("La superficie debe ser mayor a cero")
        val latitude = plot.latitude
        val longitude = plot.longitude
        if (latitude == null || longitude == null) {
            return DataResult.Error("Indica la latitud y longitud de la parcela")
        }
        return apiCall {
            var created = api.registerPlot(
                RegisterPlotDto(plot.farmId.toLong(), plot.name.trim(), plot.areaHectares, latitude, longitude),
            )
            plot.cropId?.let { created = api.assignCrop(created.id, CropIdDto(it.toLong())) }
            val farm = reader.farms().first { it.id == created.farmId }
            reader.toPlot(farm, created)
        }
    }

    override suspend fun assignCrop(plotId: String, cropId: String): DataResult<Plot> = apiCall {
        val plot = api.assignCrop(plotId.toLong(), CropIdDto(cropId.toLong()))
        val farm = reader.farms().first { it.id == plot.farmId }
        reader.toPlot(farm, plot)
    }

    override suspend fun deactivatePlot(plotId: String): DataResult<Unit> =
        apiCall { api.deactivatePlot(plotId.toLong()); Unit }

    override suspend fun getCrops(): DataResult<List<Crop>> = apiCall { reader.crops().map { it.toCrop() } }
}

class AlertRepositoryRemote(
    private val api: OsoTerraApi,
    private val reader: RemoteMonitoringReader,
) : AlertRepository {

    override suspend fun getAlerts(): DataResult<List<Alert>> = apiCall {
        coroutineScope {
            reader.plots().map { (_, plot) ->
                async {
                    api.alertsOf(plot.id).filter { it.status != "RESOLVED" }.map { alert ->
                        Alert(
                            id = alert.id.toString(),
                            plotId = plot.id.toString(),
                            plotName = plot.name,
                            severity = alertSeverity(alert.severity),
                            // La pantalla arma el mensaje en el idioma de la app con estos datos.
                            message = "",
                            createdAt = displayDateTime(alert.generatedAt).orEmpty(),
                            acknowledged = alert.status == "ACKNOWLEDGED",
                            observedDsPerM = alert.observedConductivityDsM,
                            thresholdDsPerM = alert.appliedThresholdDsM,
                        )
                    }
                }
            }.awaitAll().flatten().sortedByDescending { it.severity.ordinal }
        }
    }

    override suspend fun acknowledge(alertId: String): DataResult<Unit> =
        apiCall { api.acknowledgeAlert(alertId.toLong()); Unit }

    override suspend fun recordCorrectiveAction(
        alertId: String,
        type: CorrectiveActionType,
        notes: String,
    ): DataResult<Unit> = apiCall {
        api.recordCorrectiveAction(
            alertId.toLong(),
            RecordCorrectiveActionDto(
                actionType = type.name,
                executedAt = isoDateOrToday(""),
                notes = notes.trim().ifBlank { null },
            ),
        )
        Unit
    }
}

/** Preferencias de notificación guardadas en el Backend (Salinity Alerting). */
class NotificationPreferencesRemote(private val api: OsoTerraApi) : NotificationPreferencesRepository {

    override suspend fun getPreferences(): DataResult<NotificationPreferences> = apiCall {
        api.myNotificationPreference().toPreferences()
    }

    override suspend fun savePreferences(preferences: NotificationPreferences): DataResult<Unit> = apiCall {
        api.setNotificationPreference(preferences.toSetDto())
        Unit
    }
}

class DeviceRepositoryRemote(
    private val api: OsoTerraApi,
    private val reader: RemoteMonitoringReader,
) : DeviceRepository {

    override suspend fun getDevices(): DataResult<List<Device>> = apiCall {
        coroutineScope {
            reader.plots().map { (_, plot) -> async { api.devicesOf(plot.id).map { it.toDevice(plot.name) } } }
                .awaitAll()
                .flatten()
        }
    }

    override suspend fun registerDevice(activationCode: String, plotId: String): DataResult<Device> {
        val code = activationCode.trim()
        if (code.isBlank()) return DataResult.Error("Ingresa el código de activación")
        return apiCall {
            val device = api.registerDevice(ActivationCodeDto(code))
            val attached = api.attachDevice(device.id, PlotIdDto(plotId.toLong()))
            attached.toDevice(api.plot(plotId.toLong()).name)
        }
    }

    override suspend fun getCalibrations(deviceId: String): DataResult<List<Calibration>> = apiCall {
        api.calibrationsOf(deviceId.toLong())
            .sortedByDescending { it.samplingDate }
            .map { Calibration(it.id.toString(), it.samplingDate, it.labConductivityDsM, it.resultingFactor) }
    }

    /**
     * El Backend compara la muestra de laboratorio con lo que medía el sensor al tomarla;
     * se usa la última lectura cruda de la parcela del dispositivo.
     */
    override suspend fun registerCalibration(
        deviceId: String,
        referenceValueDsPerM: Double,
        date: String,
    ): DataResult<Calibration> {
        if (referenceValueDsPerM <= 0.0) return DataResult.Error("El valor de referencia debe ser mayor a cero")
        return apiCall {
            val plotId = reader.plots().map { it.second.id }
                .firstOrNull { id -> api.devicesOf(id).any { it.id == deviceId.toLong() } }
                ?: error("Dispositivo sin parcela")
            val sensorValue = reader.readings(plotId).lastOrNull()?.rawConductivityDsM
                ?: error("Aún no hay lecturas del sensor para comparar")
            val record = api.registerCalibration(
                RegisterCalibrationDto(
                    deviceId = deviceId.toLong(),
                    labConductivityDsM = referenceValueDsPerM,
                    samplingDate = isoDateOrToday(date),
                    laboratoryName = "Laboratorio de referencia",
                    deviceReadingAtSampling = sensorValue,
                ),
            )
            Calibration(record.id.toString(), record.samplingDate, record.labConductivityDsM, record.resultingFactor)
        }
    }

    private fun DeviceDto.toDevice(plotName: String?) = Device(
        id = id.toString(),
        code = activationCode,
        plotId = plotId?.toString(),
        plotName = plotName,
        status = when {
            status != "ACTIVE" -> DeviceStatus.OFFLINE
            (batteryLevel ?: 100) < 20 -> DeviceStatus.LOW_BATTERY
            else -> DeviceStatus.ONLINE
        },
        batteryPct = batteryLevel,
        lastSeen = displayDateTime(lastSeenAt),
    )
}

class SubscriptionRepositoryRemote(private val api: OsoTerraApi) : SubscriptionRepository {

    override suspend fun getSubscription(): DataResult<Subscription> {
        val result = apiCall {
            val subscription = api.mySubscriptions().firstOrNull { it.status == "ACTIVE" }
                ?: api.mySubscriptions().firstOrNull()
            subscription to api.subscriptionPlans()
        }
        return when (result) {
            is DataResult.Error -> result
            is DataResult.Success -> {
                val (subscription, plans) = result.data
                if (subscription == null) return DataResult.Error("Aún no tienes una suscripción activa")
                val plan = plans.firstOrNull { it.id == subscription.subscriptionPlanId }
                DataResult.Success(
                    Subscription(
                        planName = plan?.name ?: "Plan",
                        isFree = plan?.free ?: false,
                        renewsAt = subscription.periodEndDate,
                        plotsUsed = subscription.quotaConsumed,
                        plotsAllowed = subscription.quotaTotal,
                    ),
                )
            }
        }
    }
}

class AdvisorRepositoryRemote(
    private val api: OsoTerraApi,
    private val reader: RemoteMonitoringReader,
) : AdvisorRepository {

    override suspend fun getSupervisedPlots(): DataResult<List<SupervisedPlot>> = apiCall {
        val producers = api.linkedAccounts().associateBy { it.id }
        coroutineScope {
            reader.plots().map { (farm, plot) ->
                async {
                    val summary = reader.toPlot(farm, plot)
                    SupervisedPlot(
                        id = summary.id,
                        plotName = summary.name,
                        producerName = producers[farm.ownerId]?.toUser()?.fullName.orEmpty(),
                        cropName = summary.crop?.name,
                        salinityLevel = summary.salinityLevel,
                        lastEcDsPerM = summary.lastEcDsPerM,
                    )
                }
            }.awaitAll()
        }
    }

    override suspend fun getSeries(plotId: String): DataResult<List<Reading>> =
        apiCall { reader.readings(plotId.toLong()).map { it.toReading() } }
}
