package com.osoterra.mobile.domain.repository

import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.domain.model.Calibration
import com.osoterra.mobile.domain.model.Device

interface DeviceRepository {
    suspend fun getDevices(): DataResult<List<Device>>
    suspend fun registerDevice(activationCode: String, plotId: String): DataResult<Device>
    suspend fun getCalibrations(deviceId: String): DataResult<List<Calibration>>

    /**
     * Lectura cruda del sensor más cercana a la fecha del muestreo (YYYY-MM-DD); null si el
     * dispositivo aún no tiene lecturas. Es el valor sugerido al calibrar, y la persona puede cambiarlo.
     */
    suspend fun getSensorReading(deviceId: String, samplingDate: String): DataResult<Double?>

    suspend fun registerCalibration(
        deviceId: String,
        referenceValueDsPerM: Double,
        sensorReadingDsPerM: Double,
        laboratoryName: String,
        date: String,
    ): DataResult<Calibration>
}
