package com.osoterra.mobile.domain.repository

import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.domain.model.Calibration
import com.osoterra.mobile.domain.model.Device

interface DeviceRepository {
    suspend fun getDevices(): DataResult<List<Device>>
    suspend fun registerDevice(activationCode: String, plotId: String): DataResult<Device>
    suspend fun getCalibrations(deviceId: String): DataResult<List<Calibration>>
    suspend fun registerCalibration(
        deviceId: String,
        referenceValueDsPerM: Double,
        date: String,
    ): DataResult<Calibration>
}
