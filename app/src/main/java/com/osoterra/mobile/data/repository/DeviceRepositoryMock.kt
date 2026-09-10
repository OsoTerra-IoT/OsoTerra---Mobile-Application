package com.osoterra.mobile.data.repository

import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.domain.model.Calibration
import com.osoterra.mobile.domain.model.Device
import com.osoterra.mobile.domain.model.DeviceStatus
import com.osoterra.mobile.domain.repository.DeviceRepository
import com.osoterra.mobile.domain.repository.PlotRepository
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class DeviceRepositoryMock(
    private val plotRepository: PlotRepository,
) : DeviceRepository {

    private val devices = mutableListOf(
        Device("d-001", "OSO-4521-AB", "p-001", "Parcela El Molino", DeviceStatus.ONLINE, 82, "Hoy, 08:15"),
        Device("d-002", "OSO-7788-CD", "p-002", "Parcela La Huaca", DeviceStatus.LOW_BATTERY, 14, "Hoy, 07:40"),
        Device("d-003", "OSO-1290-EF", "p-003", "Parcela Los Ceibos", DeviceStatus.OFFLINE, 0, "Ayer, 18:20"),
    )

    private val calibrations = mutableMapOf(
        "d-001" to mutableListOf(
            Calibration("cal-001", "2026-08-20", 3.6, 1.05),
        ),
    )

    override suspend fun getDevices(): DataResult<List<Device>> {
        delay(400.milliseconds)
        return DataResult.Success(devices.toList())
    }

    override suspend fun registerDevice(activationCode: String, plotId: String): DataResult<Device> {
        delay(500.milliseconds)
        val code = activationCode.trim()
        if (code.isBlank()) return DataResult.Error("Ingresa el código de activación")
        if (devices.any { it.code.equals(code, ignoreCase = true) }) {
            return DataResult.Error("El dispositivo ya está registrado")
        }
        val plotName = (plotRepository.getPlot(plotId) as? DataResult.Success)?.data?.name ?: "Parcela"
        val device = Device(
            id = "d-${UUID.randomUUID()}",
            code = code,
            plotId = plotId,
            plotName = plotName,
            status = DeviceStatus.ONLINE,
            batteryPct = 100,
            lastSeen = null,
        )
        devices += device
        return DataResult.Success(device)
    }

    override suspend fun getCalibrations(deviceId: String): DataResult<List<Calibration>> {
        delay(200.milliseconds)
        return DataResult.Success(calibrations[deviceId]?.toList() ?: emptyList())
    }

    override suspend fun registerCalibration(
        deviceId: String,
        referenceValueDsPerM: Double,
        date: String,
    ): DataResult<Calibration> {
        delay(400.milliseconds)
        if (referenceValueDsPerM <= 0.0) {
            return DataResult.Error("El valor de referencia debe ser mayor a cero")
        }
        // Factor de corrección simulado: referencia de laboratorio sobre una lectura base del sensor.
        val sensorBase = 3.5
        val factor = referenceValueDsPerM / sensorBase
        val calibration = Calibration(
            id = "cal-${UUID.randomUUID()}",
            date = date,
            referenceValueDsPerM = referenceValueDsPerM,
            factor = (factor * 100).toInt() / 100.0,
        )
        calibrations.getOrPut(deviceId) { mutableListOf() }.add(0, calibration)
        return DataResult.Success(calibration)
    }
}
