package com.osoterra.mobile.data.repository

import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.domain.model.Reading
import com.osoterra.mobile.domain.model.SalinityLevel
import com.osoterra.mobile.domain.model.SupervisedPlot
import com.osoterra.mobile.domain.repository.AdvisorRepository
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class AdvisorRepositoryMock : AdvisorRepository {

    private val supervised = listOf(
        SupervisedPlot("sp-001", "El Molino", "Juan Pérez", "Arroz", SalinityLevel.CRITICAL, 4.8),
        SupervisedPlot("sp-002", "La Huaca", "Juan Pérez", "Maíz amarillo duro", SalinityLevel.WARNING, 1.9),
        SupervisedPlot("sp-003", "Los Ceibos", "María Ríos", "Espárrago", SalinityLevel.NORMAL, 3.1),
        SupervisedPlot("sp-004", "Santa Elena", "María Ríos", "Arándano", SalinityLevel.CRITICAL, 2.4),
        SupervisedPlot("sp-005", "El Arenal", "Carlos Díaz", "Palta", SalinityLevel.WATCH, 1.6),
        SupervisedPlot("sp-006", "La Pradera", "Carlos Díaz", "Uva de mesa", SalinityLevel.NORMAL, 1.2),
        SupervisedPlot("sp-007", "San Rafael", "Carlos Díaz", "Arroz", SalinityLevel.WARNING, 3.4),
    )

    override suspend fun getSupervisedPlots(): DataResult<List<SupervisedPlot>> {
        delay(500.milliseconds)
        return DataResult.Success(supervised)
    }

    override suspend fun getSeries(plotId: String): DataResult<List<Reading>> {
        delay(300.milliseconds)
        val base = supervised.firstOrNull { it.id == plotId }?.lastEcDsPerM ?: 2.5
        val series = (0 until 20).map { i ->
            Reading(
                id = "$plotId-r-$i",
                timestamp = "Día ${(i / 4) + 1}, ${(i % 4) * 6}:00",
                ecDsPerM = (base + Random.nextDouble(-0.5, 0.5)).coerceAtLeast(0.1),
                humidityPct = Random.nextDouble(18.0, 34.0),
                temperatureC = Random.nextDouble(19.0, 28.0),
            )
        }
        return DataResult.Success(series)
    }
}
