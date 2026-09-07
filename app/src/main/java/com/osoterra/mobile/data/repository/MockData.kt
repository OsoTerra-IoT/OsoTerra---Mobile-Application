package com.osoterra.mobile.data.repository

import com.osoterra.mobile.domain.model.Alert
import com.osoterra.mobile.domain.model.AlertSeverity
import com.osoterra.mobile.domain.model.Crop
import com.osoterra.mobile.domain.model.Farm
import com.osoterra.mobile.domain.model.Plot
import com.osoterra.mobile.domain.model.Reading
import com.osoterra.mobile.domain.model.SalinityLevel
import com.osoterra.mobile.domain.model.User
import com.osoterra.mobile.domain.model.UserRole
import kotlin.random.Random

object MockData {

    val demoUser = User(
        id = "u-001",
        fullName = "Juan Pérez Quiroga",
        email = "productor@osoterra.com",
        role = UserRole.PRODUCER,
    )

    private val arroz = Crop("c-arroz", "Arroz", 3.0, "Moderadamente sensible")
    private val maiz = Crop("c-maiz", "Maíz amarillo duro", 1.7, "Sensible")
    private val esparrago = Crop("c-esparrago", "Espárrago", 6.0, "Tolerante")

    val crops = listOf(
        maiz,
        Crop("c-uva", "Uva de mesa", 1.5, "Sensible"),
        Crop("c-palta", "Palta", 1.8, "Sensible"),
        arroz,
        esparrago,
        Crop("c-algodon", "Algodón", 7.7, "Tolerante"),
    )

    val farms = listOf(
        Farm("f-001", "Finca San Isidro", "Lambayeque", "Chiclayo", "Pomalca"),
        Farm("f-002", "Finca Santa Rosa", "La Libertad", "Trujillo", "Moche"),
    )

    val plots = listOf(
        Plot(
            id = "p-001",
            name = "Parcela El Molino",
            farmName = "Finca San Isidro",
            areaHectares = 2.5,
            crop = arroz,
            salinityLevel = SalinityLevel.CRITICAL,
            lastEcDsPerM = 4.8,
            lastReadingAt = "Hoy, 08:15",
            deviceOnline = true,
        ),
        Plot(
            id = "p-002",
            name = "Parcela La Huaca",
            farmName = "Finca San Isidro",
            areaHectares = 1.8,
            crop = maiz,
            salinityLevel = SalinityLevel.WARNING,
            lastEcDsPerM = 1.9,
            lastReadingAt = "Hoy, 07:40",
            deviceOnline = true,
        ),
        Plot(
            id = "p-003",
            name = "Parcela Los Ceibos",
            farmName = "Finca Santa Rosa",
            areaHectares = 3.2,
            crop = esparrago,
            salinityLevel = SalinityLevel.NORMAL,
            lastEcDsPerM = 3.1,
            lastReadingAt = "Ayer, 18:20",
            deviceOnline = true,
        ),
        Plot(
            id = "p-004",
            name = "Parcela El Arenal",
            farmName = "Finca Santa Rosa",
            areaHectares = 1.0,
            crop = null,
            salinityLevel = SalinityLevel.NO_DATA,
            lastEcDsPerM = null,
            lastReadingAt = null,
            deviceOnline = false,
        ),
    )

    val alerts = listOf(
        Alert(
            id = "a-001",
            plotId = "p-001",
            plotName = "Parcela El Molino",
            severity = AlertSeverity.CRITICAL,
            message = "La salinidad superó el umbral del arroz (3,0 dS/m). Considera un riego de lavado.",
            createdAt = "Hoy, 08:16",
            acknowledged = false,
        ),
        Alert(
            id = "a-002",
            plotId = "p-002",
            plotName = "Parcela La Huaca",
            severity = AlertSeverity.HIGH,
            message = "La salinidad se acerca al umbral del maíz (1,7 dS/m).",
            createdAt = "Hoy, 07:41",
            acknowledged = false,
        ),
        Alert(
            id = "a-003",
            plotId = "p-003",
            plotName = "Parcela Los Ceibos",
            severity = AlertSeverity.LOW,
            message = "Lectura estable dentro del rango tolerado por el espárrago.",
            createdAt = "Ayer, 18:21",
            acknowledged = true,
        ),
    )

    fun readingsFor(plotId: String): List<Reading> {
        val base = when (plotId) {
            "p-001" -> 3.8
            "p-002" -> 1.4
            "p-003" -> 2.6
            else -> 2.0
        }
        return (0 until 24).map { i ->
            Reading(
                id = "$plotId-r-$i",
                timestamp = "Día ${(i / 4) + 1}, ${(i % 4) * 6}:00",
                ecDsPerM = (base + Random.nextDouble(-0.4, 0.6)).coerceAtLeast(0.1),
                humidityPct = Random.nextDouble(18.0, 34.0),
                temperatureC = Random.nextDouble(19.0, 28.0),
            )
        }
    }
}
