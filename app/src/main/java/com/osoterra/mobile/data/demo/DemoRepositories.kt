package com.osoterra.mobile.data.demo

import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.data.remote.displayDateTime
import com.osoterra.mobile.data.remote.salinityLevel
import com.osoterra.mobile.data.repository.SessionStore
import com.osoterra.mobile.domain.model.AdvisoryRequest
import com.osoterra.mobile.domain.model.Alert
import com.osoterra.mobile.domain.model.Calibration
import com.osoterra.mobile.domain.model.CorrectiveActionType
import com.osoterra.mobile.domain.model.Crop
import com.osoterra.mobile.domain.model.Device
import com.osoterra.mobile.domain.model.DeviceStatus
import com.osoterra.mobile.domain.model.Farm
import com.osoterra.mobile.domain.model.NewPlot
import com.osoterra.mobile.domain.model.NotificationPreferences
import com.osoterra.mobile.domain.model.Plot
import com.osoterra.mobile.domain.model.Reading
import com.osoterra.mobile.domain.model.RegisterData
import com.osoterra.mobile.domain.model.Subscription
import com.osoterra.mobile.domain.model.SupervisedPlot
import com.osoterra.mobile.domain.model.User
import com.osoterra.mobile.domain.model.UserRole
import com.osoterra.mobile.domain.repository.AdvisorRepository
import com.osoterra.mobile.domain.repository.AdvisoryLinkRepository
import com.osoterra.mobile.domain.repository.AlertRepository
import com.osoterra.mobile.domain.repository.AuthRepository
import com.osoterra.mobile.domain.repository.DeviceRepository
import com.osoterra.mobile.domain.repository.FarmRepository
import com.osoterra.mobile.domain.repository.NotificationPreferencesRepository
import com.osoterra.mobile.domain.repository.PlotRepository
import com.osoterra.mobile.domain.repository.SubscriptionRepository
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

/*
 * Repositorios del modo de prueba: cumplen los mismos contratos de `domain.repository` que los
 * remotos, pero leen y escriben el escenario en memoria (DemoStore). Sin red ni Backend.
 * Una espera corta deja ver los estados de carga como en el producto real.
 */

private suspend fun pause() = delay(250.milliseconds)

// Las fechas se formatean al leerlas, con el idioma elegido en la app (no el del teléfono).
private fun DemoStore.date(iso: String?) = displayDateTime(iso, locale())
private fun Plot.forDisplay(store: DemoStore) = copy(lastReadingAt = store.date(lastReadingAt))
private fun Alert.forDisplay(store: DemoStore) = copy(createdAt = store.date(createdAt) ?: createdAt)
private fun Device.forDisplay(store: DemoStore) = copy(lastSeen = store.date(lastSeen))
private fun Reading.forDisplay(store: DemoStore) = copy(timestamp = store.date(capturedAt) ?: timestamp)

const val DEMO_TOKEN_PREFIX = "demo-token-"

class DemoAuthRepository(
    private val store: DemoStore,
    private val sessionStore: SessionStore,
    private val onSession: (String?) -> Unit,
    private val selectedScenario: () -> DemoScenario,
) : AuthRepository {

    /** Entra con el escenario elegido: cualquier correo y contraseña sirven. */
    override suspend fun login(email: String, password: String): DataResult<User> {
        pause()
        if (email.isBlank() || password.isBlank()) return DataResult.Error("Ingresa tu correo y contraseña")
        val snapshot = store.require(selectedScenario())
        startSession(snapshot.user)
        return DataResult.Success(snapshot.user)
    }

    /** Una cuenta nueva empieza sin parcelas (escenario 1) con el nombre y el rol elegidos. */
    override suspend fun register(data: RegisterData): DataResult<User> {
        pause()
        if (data.role == UserRole.ADVISOR && data.licenseNumber.isNullOrBlank()) {
            return DataResult.Error("El asesor debe indicar su número de colegiatura")
        }
        store.load(DemoScenario.NO_PLOTS)
        val user = User("demo-u-${UUID.randomUUID()}", data.fullName.trim(), data.email.trim(), data.role)
        store.update { it.copy(user = user) }
        startSession(user)
        return DataResult.Success(user)
    }

    override suspend fun requestPasswordReset(email: String): DataResult<Unit> {
        pause()
        return if (email.isBlank()) DataResult.Error("Ingresa tu correo") else DataResult.Success(Unit)
    }

    override suspend fun logout() {
        sessionStore.clear()
        onSession(null)
    }

    override suspend fun currentUser(): User? = sessionStore.readUser()

    suspend fun startSession(user: User) {
        val token = DEMO_TOKEN_PREFIX + UUID.randomUUID()
        sessionStore.save(token, user)
        onSession(token)
    }
}

class DemoFarmRepository(private val store: DemoStore) : FarmRepository {
    override suspend fun getFarms(): DataResult<List<Farm>> {
        pause()
        return DataResult.Success(store.snapshot?.farms.orEmpty())
    }

    override suspend fun createFarm(name: String, department: String, province: String, district: String): DataResult<Farm> {
        pause()
        if (name.isBlank()) return DataResult.Error("El nombre de la finca es obligatorio")
        val farm = Farm("demo-f-${UUID.randomUUID()}", name.trim(), department.trim(), province.trim(), district.trim())
        store.update { it.copy(farms = it.farms + farm) }
        return DataResult.Success(farm)
    }
}

class DemoPlotRepository(private val store: DemoStore) : PlotRepository {

    override suspend fun getPlots(): DataResult<List<Plot>> {
        pause()
        return DataResult.Success(store.snapshot?.plots.orEmpty().map { it.forDisplay(store) })
    }

    override suspend fun getPlot(plotId: String): DataResult<Plot> {
        pause()
        val plot = store.snapshot?.plots?.firstOrNull { it.id == plotId }
        return if (plot != null) DataResult.Success(plot.forDisplay(store)) else DataResult.Error("No se encontró la parcela")
    }

    override suspend fun getReadings(plotId: String): DataResult<List<Reading>> {
        pause()
        return DataResult.Success(store.snapshot?.readings?.get(plotId).orEmpty().map { it.forDisplay(store) })
    }

    override suspend fun createPlot(plot: NewPlot): DataResult<Plot> {
        pause()
        if (plot.name.isBlank()) return DataResult.Error("El nombre de la parcela es obligatorio")
        if (plot.areaHectares <= 0.0) return DataResult.Error("La superficie debe ser mayor a cero")
        val snapshot = store.snapshot ?: return DataResult.Error("Elige un escenario de prueba")
        val created = Plot(
            id = "demo-p-${UUID.randomUUID()}",
            name = plot.name.trim(),
            farmName = snapshot.farms.firstOrNull { it.id == plot.farmId }?.name ?: "Fundo",
            areaHectares = plot.areaHectares,
            crop = plot.cropId?.let { id -> DemoScenarios.crops.firstOrNull { it.id == id } },
            salinityLevel = salinityLevel(null, null),
            lastEcDsPerM = null,
            lastReadingAt = null,
            deviceOnline = false,
        )
        store.update { it.copy(plots = it.plots + created) }
        return DataResult.Success(created)
    }

    override suspend fun assignCrop(plotId: String, cropId: String): DataResult<Plot> {
        pause()
        val crop = DemoScenarios.crops.firstOrNull { it.id == cropId } ?: return DataResult.Error("Cultivo no válido")
        var updated: Plot? = null
        store.update { snapshot ->
            snapshot.copy(
                plots = snapshot.plots.map { plot ->
                    if (plot.id != plotId) plot
                    else plot.copy(crop = crop, salinityLevel = salinityLevel(plot.lastEcDsPerM, crop.thresholdDsPerM))
                        .also { updated = it }
                },
            )
        }
        return updated?.let { DataResult.Success(it.forDisplay(store)) } ?: DataResult.Error("No se encontró la parcela")
    }

    override suspend fun deactivatePlot(plotId: String): DataResult<Unit> {
        pause()
        val exists = store.snapshot?.plots?.any { it.id == plotId } == true
        if (!exists) return DataResult.Error("No se encontró la parcela")
        store.update { snapshot ->
            snapshot.copy(
                plots = snapshot.plots.filterNot { it.id == plotId },
                alerts = snapshot.alerts.filterNot { it.plotId == plotId },
                devices = snapshot.devices.map { if (it.plotId == plotId) it.copy(plotId = null, plotName = null) else it },
            )
        }
        return DataResult.Success(Unit)
    }

    override suspend fun getCrops(): DataResult<List<Crop>> {
        pause()
        return DataResult.Success(DemoScenarios.crops)
    }
}

class DemoAlertRepository(private val store: DemoStore) : AlertRepository {

    override suspend fun getAlerts(): DataResult<List<Alert>> {
        pause()
        return DataResult.Success(store.snapshot?.alerts.orEmpty().map { it.forDisplay(store) })
    }

    override suspend fun acknowledge(alertId: String): DataResult<Unit> {
        pause()
        store.update { s -> s.copy(alerts = s.alerts.map { if (it.id == alertId) it.copy(acknowledged = true) else it }) }
        return DataResult.Success(Unit)
    }

    /** Registrar una acción correctiva resuelve la alerta, como en el Backend. */
    override suspend fun recordCorrectiveAction(alertId: String, type: CorrectiveActionType, notes: String): DataResult<Unit> {
        pause()
        store.update { s -> s.copy(alerts = s.alerts.filterNot { it.id == alertId }) }
        return DataResult.Success(Unit)
    }
}

class DemoDeviceRepository(private val store: DemoStore) : DeviceRepository {

    private val calibrations = mutableMapOf<String, MutableList<Calibration>>()

    override suspend fun getDevices(): DataResult<List<Device>> {
        pause()
        return DataResult.Success(store.snapshot?.devices.orEmpty().map { it.forDisplay(store) })
    }

    /**
     * Vincula un código disponible (OS-014) a la parcela. La parcela recibe al instante su
     * primera lectura, para que se vea cómo cambia la pantalla al conectar el dispositivo.
     */
    override suspend fun registerDevice(activationCode: String, plotId: String): DataResult<Device> {
        pause()
        val code = activationCode.trim().uppercase()
        val snapshot = store.snapshot ?: return DataResult.Error("Elige un escenario de prueba")
        if (code.isBlank()) return DataResult.Error("Ingresa el código de activación")
        if (snapshot.devices.any { it.code.equals(code, ignoreCase = true) }) {
            return DataResult.Error("El dispositivo ya está registrado")
        }
        if (snapshot.availableSensorCodes.none { it.equals(code, ignoreCase = true) }) {
            return DataResult.Error("No encontramos ese código. Revisa la etiqueta del dispositivo.")
        }
        val plot = snapshot.plots.firstOrNull { it.id == plotId } ?: return DataResult.Error("No se encontró la parcela")
        val now = store.nowIso()
        val device = Device("demo-d-${UUID.randomUUID()}", code, plot.id, plot.name, DeviceStatus.ONLINE, null, now)
        val firstEc = FIRST_READING_DS_PER_M
        val reading = Reading("${plot.id}-r-first", now, now, firstEc, 27.0, 24.0)
        store.update { s ->
            s.copy(
                devices = s.devices + device,
                availableSensorCodes = s.availableSensorCodes.filterNot { it.equals(code, ignoreCase = true) },
                plots = s.plots.map {
                    if (it.id != plot.id) it
                    else it.copy(
                        deviceOnline = true,
                        lastEcDsPerM = it.lastEcDsPerM ?: firstEc,
                        lastReadingAt = it.lastReadingAt ?: now,
                        salinityLevel = salinityLevel(it.lastEcDsPerM ?: firstEc, it.crop?.thresholdDsPerM),
                    )
                },
                readings = if (s.readings[plot.id].isNullOrEmpty()) s.readings + (plot.id to listOf(reading)) else s.readings,
            )
        }
        return DataResult.Success(device.forDisplay(store))
    }

    override suspend fun getCalibrations(deviceId: String): DataResult<List<Calibration>> {
        pause()
        return DataResult.Success(calibrations[deviceId]?.toList().orEmpty())
    }

    override suspend fun registerCalibration(deviceId: String, referenceValueDsPerM: Double, date: String): DataResult<Calibration> {
        pause()
        if (referenceValueDsPerM <= 0.0) return DataResult.Error("El valor de referencia debe ser mayor a cero")
        val sensorEc = store.snapshot?.let { s ->
            val plotId = s.devices.firstOrNull { it.id == deviceId }?.plotId
            s.plots.firstOrNull { it.id == plotId }?.lastEcDsPerM
        } ?: FIRST_READING_DS_PER_M
        val calibration = Calibration(
            id = "demo-cal-${UUID.randomUUID()}",
            date = date,
            referenceValueDsPerM = referenceValueDsPerM,
            factor = (referenceValueDsPerM / sensorEc * 100).toInt() / 100.0,
        )
        calibrations.getOrPut(deviceId) { mutableListOf() }.add(0, calibration)
        return DataResult.Success(calibration)
    }

    private companion object {
        /** Primera lectura de un dispositivo recién vinculado: Normal para la uva (umbral 1,5). */
        const val FIRST_READING_DS_PER_M = 0.9
    }
}

class DemoAdvisorRepository(private val store: DemoStore) : AdvisorRepository {

    override suspend fun getSupervisedPlots(): DataResult<List<SupervisedPlot>> {
        pause()
        return DataResult.Success(store.snapshot?.supervised.orEmpty())
    }

    override suspend fun getSeries(plotId: String): DataResult<List<Reading>> {
        pause()
        return DataResult.Success(store.snapshot?.readings?.get(plotId).orEmpty().map { it.forDisplay(store) })
    }
}

class DemoAdvisoryLinkRepository(private val store: DemoStore) : AdvisoryLinkRepository {

    override suspend fun getPendingRequests(): DataResult<List<AdvisoryRequest>> {
        pause()
        return DataResult.Success(
            store.snapshot?.requests.orEmpty().map {
                AdvisoryRequest(it.id, it.advisorName, it.advisorEmail, store.date(it.requestedAt))
            },
        )
    }

    override suspend fun accept(requestId: String): DataResult<Unit> = respond(requestId)

    override suspend fun decline(requestId: String): DataResult<Unit> = respond(requestId)

    private suspend fun respond(requestId: String): DataResult<Unit> {
        pause()
        store.update { s -> s.copy(requests = s.requests.filterNot { it.id == requestId }) }
        return DataResult.Success(Unit)
    }
}

/** Preferencias del escenario: se guardan en memoria y vuelven al valor inicial al reiniciarlo. */
class DemoNotificationPreferencesRepository(private val store: DemoStore) : NotificationPreferencesRepository {

    override suspend fun getPreferences(): DataResult<NotificationPreferences> {
        pause()
        return DataResult.Success(store.snapshot?.preferences ?: NotificationPreferences())
    }

    override suspend fun savePreferences(preferences: NotificationPreferences): DataResult<Unit> {
        store.update { it.copy(preferences = preferences) }
        return DataResult.Success(Unit)
    }
}

class DemoSubscriptionRepository(private val store: DemoStore) : SubscriptionRepository {
    override suspend fun getSubscription(): DataResult<Subscription> {
        pause()
        return DataResult.Success(
            Subscription(
                planName = "Plan Gratuito",
                isFree = true,
                renewsAt = null,
                plotsUsed = store.snapshot?.plots?.size ?: 0,
                plotsAllowed = 5,
            ),
        )
    }
}
