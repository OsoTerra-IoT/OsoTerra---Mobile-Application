package com.osoterra.mobile.data.repository

import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.domain.model.Alert
import com.osoterra.mobile.domain.model.Crop
import com.osoterra.mobile.domain.model.Farm
import com.osoterra.mobile.domain.model.NewPlot
import com.osoterra.mobile.domain.model.Plot
import com.osoterra.mobile.domain.model.Reading
import com.osoterra.mobile.domain.model.RegisterData
import com.osoterra.mobile.domain.model.SalinityLevel
import com.osoterra.mobile.domain.model.User
import com.osoterra.mobile.domain.model.UserRole
import com.osoterra.mobile.domain.repository.AlertRepository
import com.osoterra.mobile.domain.repository.AuthRepository
import com.osoterra.mobile.domain.repository.FarmRepository
import com.osoterra.mobile.domain.repository.PlotRepository
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import java.util.UUID

class AuthRepositoryMock(
    private val sessionStore: SessionStore,
    private val onSession: (String?) -> Unit,
) : AuthRepository {

    override suspend fun login(email: String, password: String): DataResult<User> {
        delay(600.milliseconds)
        if (email.isBlank() || password.isBlank()) {
            return DataResult.Error("Ingresa tu correo y contraseña")
        }
        val user = MockData.demoUser.copy(email = email)
        persist(user)
        return DataResult.Success(user)
    }

    override suspend fun register(data: RegisterData): DataResult<User> {
        delay(700.milliseconds)
        if (data.role == UserRole.ADVISOR && data.licenseNumber.isNullOrBlank()) {
            return DataResult.Error("El asesor debe indicar su número de colegiatura")
        }
        val user = User(
            id = "u-${UUID.randomUUID()}",
            fullName = data.fullName,
            email = data.email,
            role = data.role,
        )
        persist(user)
        return DataResult.Success(user)
    }

    override suspend fun requestPasswordReset(email: String): DataResult<Unit> {
        delay(500.milliseconds)
        if (email.isBlank()) return DataResult.Error("Ingresa tu correo")

        return DataResult.Success(Unit)
    }

    override suspend fun logout() {
        sessionStore.clear()
        onSession(null)
    }

    override suspend fun currentUser(): User? = sessionStore.readUser()

    private suspend fun persist(user: User) {
        val token = "mock-token-${UUID.randomUUID()}"
        sessionStore.save(token, user)
        onSession(token)
    }
}

class FarmRepositoryMock : FarmRepository {
    private val farms = MockData.farms.toMutableList()

    fun farmName(id: String): String? = farms.firstOrNull { it.id == id }?.name

    override suspend fun getFarms(): DataResult<List<Farm>> {
        delay(300.milliseconds)
        return DataResult.Success(farms.toList())
    }

    override suspend fun createFarm(
        name: String,
        department: String,
        province: String,
        district: String,
    ): DataResult<Farm> {
        delay(400.milliseconds)
        if (name.isBlank()) return DataResult.Error("El nombre de la finca es obligatorio")
        val farm = Farm(
            id = "f-${UUID.randomUUID()}",
            name = name.trim(),
            department = department.trim(),
            province = province.trim(),
            district = district.trim(),
        )
        farms += farm
        return DataResult.Success(farm)
    }
}

class PlotRepositoryMock(
    private val farmRepository: FarmRepositoryMock,
) : PlotRepository {

    private val plots = MockData.plots.toMutableList()

    override suspend fun getPlots(): DataResult<List<Plot>> {
        delay(500.milliseconds)
        return DataResult.Success(plots.toList())
    }

    override suspend fun getPlot(plotId: String): DataResult<Plot> {
        delay(300.milliseconds)
        val plot = plots.firstOrNull { it.id == plotId }
        return if (plot != null) DataResult.Success(plot)
        else DataResult.Error("No se encontró la parcela")
    }

    override suspend fun getReadings(plotId: String): DataResult<List<Reading>> {
        delay(400.milliseconds)
        return DataResult.Success(MockData.readingsFor(plotId))
    }

    override suspend fun createPlot(plot: NewPlot): DataResult<Plot> {
        delay(500.milliseconds)
        if (plot.name.isBlank()) return DataResult.Error("El nombre de la parcela es obligatorio")
        if (plot.areaHectares <= 0.0) return DataResult.Error("La superficie debe ser mayor a cero")
        val crop = plot.cropId?.let { id -> MockData.crops.firstOrNull { it.id == id } }
        val created = Plot(
            id = "p-${UUID.randomUUID()}",
            name = plot.name.trim(),
            farmName = farmRepository.farmName(plot.farmId) ?: "Finca",
            areaHectares = plot.areaHectares,
            crop = crop,
            salinityLevel = SalinityLevel.NO_DATA,
            lastEcDsPerM = null,
            lastReadingAt = null,
            deviceOnline = false,
        )
        plots += created
        return DataResult.Success(created)
    }

    override suspend fun assignCrop(plotId: String, cropId: String): DataResult<Plot> {
        delay(300.milliseconds)
        val index = plots.indexOfFirst { it.id == plotId }
        if (index < 0) return DataResult.Error("No se encontró la parcela")
        val crop = MockData.crops.firstOrNull { it.id == cropId }
            ?: return DataResult.Error("Cultivo no válido")
        val updated = plots[index].copy(crop = crop)
        plots[index] = updated
        return DataResult.Success(updated)
    }

    override suspend fun deactivatePlot(plotId: String): DataResult<Unit> {
        delay(300.milliseconds)
        val removed = plots.removeAll { it.id == plotId }
        return if (removed) DataResult.Success(Unit)
        else DataResult.Error("No se encontró la parcela")
    }

    override suspend fun getCrops(): DataResult<List<Crop>> {
        delay(200.milliseconds)
        return DataResult.Success(MockData.crops)
    }
}

class AlertRepositoryMock : AlertRepository {
    private val acknowledged = mutableSetOf<String>()

    override suspend fun getAlerts(): DataResult<List<Alert>> {
        delay(400.milliseconds)
        val list = MockData.alerts.map { it.copy(acknowledged = it.acknowledged || it.id in acknowledged) }
        return DataResult.Success(list)
    }

    override suspend fun acknowledge(alertId: String): DataResult<Unit> {
        delay(200.milliseconds)
        acknowledged += alertId
        return DataResult.Success(Unit)
    }
}
