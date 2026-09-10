package com.osoterra.mobile.domain.repository

import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.domain.model.Alert
import com.osoterra.mobile.domain.model.Crop
import com.osoterra.mobile.domain.model.Farm
import com.osoterra.mobile.domain.model.NewPlot
import com.osoterra.mobile.domain.model.Plot
import com.osoterra.mobile.domain.model.Reading
import com.osoterra.mobile.domain.model.RegisterData
import com.osoterra.mobile.domain.model.User

interface AuthRepository {
    suspend fun login(email: String, password: String): DataResult<User>
    suspend fun register(data: RegisterData): DataResult<User>
    suspend fun requestPasswordReset(email: String): DataResult<Unit>
    suspend fun logout()

    suspend fun currentUser(): User?
}

interface PlotRepository {
    suspend fun getPlots(): DataResult<List<Plot>>
    suspend fun getPlot(plotId: String): DataResult<Plot>
    suspend fun getReadings(plotId: String): DataResult<List<Reading>>
    suspend fun createPlot(plot: NewPlot): DataResult<Plot>
    suspend fun assignCrop(plotId: String, cropId: String): DataResult<Plot>
    suspend fun deactivatePlot(plotId: String): DataResult<Unit>

    suspend fun getCrops(): DataResult<List<Crop>>
}

interface FarmRepository {
    suspend fun getFarms(): DataResult<List<Farm>>
    suspend fun createFarm(
        name: String,
        department: String,
        province: String,
        district: String,
    ): DataResult<Farm>
}

interface AlertRepository {
    suspend fun getAlerts(): DataResult<List<Alert>>
    suspend fun acknowledge(alertId: String): DataResult<Unit>
}
