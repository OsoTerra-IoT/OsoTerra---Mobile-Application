package com.osoterra.mobile.ui

import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.domain.model.Calibration
import com.osoterra.mobile.domain.model.Crop
import com.osoterra.mobile.domain.model.Device
import com.osoterra.mobile.domain.model.Farm
import com.osoterra.mobile.domain.model.NewPlot
import com.osoterra.mobile.domain.model.Plot
import com.osoterra.mobile.domain.model.Reading
import com.osoterra.mobile.domain.model.SalinityLevel
import com.osoterra.mobile.domain.model.SupervisedPlot
import com.osoterra.mobile.domain.repository.AdvisorRepository
import com.osoterra.mobile.domain.repository.DeviceRepository
import com.osoterra.mobile.domain.repository.FarmRepository
import com.osoterra.mobile.domain.repository.PlotRepository
import com.osoterra.mobile.ui.advisor.AdvisorDashboardViewModel
import com.osoterra.mobile.ui.crops.CropCatalogViewModel
import com.osoterra.mobile.ui.devices.DevicesViewModel
import com.osoterra.mobile.ui.farms.FarmsViewModel
import com.osoterra.mobile.ui.plot.PlotDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val DENIED = "No tienes permiso para esta acción."

/** Un error de red o un 403 no debe verse como «no hay nada»: cada lista guarda el fallo aparte. */
@OptIn(ExperimentalCoroutinesApi::class)
class ErrorStatesTest {

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private val plot = Plot("1", "Lote", "Finca", 1.0, null, SalinityLevel.NORMAL, 1.0, null, true)

    private class FakeDevices(var fail: Boolean) : DeviceRepository {
        override suspend fun getDevices(): DataResult<List<Device>> =
            if (fail) DataResult.Error(DENIED) else DataResult.Success(emptyList())
        override suspend fun registerDevice(activationCode: String, plotId: String): DataResult<Device> = error("no")
        override suspend fun getCalibrations(deviceId: String): DataResult<List<Calibration>> =
            DataResult.Success(emptyList())
        override suspend fun getSensorReading(deviceId: String, samplingDate: String): DataResult<Double?> =
            DataResult.Success(null)
        override suspend fun registerCalibration(
            deviceId: String,
            referenceValueDsPerM: Double,
            sensorReadingDsPerM: Double,
            laboratoryName: String,
            date: String,
        ): DataResult<Calibration> = error("no")
    }

    private class FakePlots(
        var plotsFail: Boolean = false,
        var readingsFail: Boolean = false,
        var cropsFail: Boolean = false,
        val plot: Plot,
    ) : PlotRepository {
        override suspend fun getPlots(): DataResult<List<Plot>> =
            if (plotsFail) DataResult.Error(DENIED) else DataResult.Success(listOf(plot))
        override suspend fun getPlot(plotId: String): DataResult<Plot> = DataResult.Success(plot)
        override suspend fun getReadings(plotId: String): DataResult<List<Reading>> =
            if (readingsFail) DataResult.Error("Sin conexión") else DataResult.Success(emptyList())
        override suspend fun createPlot(plot: NewPlot): DataResult<Plot> = error("no")
        override suspend fun assignCrop(plotId: String, cropId: String): DataResult<Plot> = error("no")
        override suspend fun deactivatePlot(plotId: String): DataResult<Unit> = error("no")
        override suspend fun getCrops(): DataResult<List<Crop>> =
            if (cropsFail) DataResult.Error("Sin conexión") else DataResult.Success(emptyList())
    }

    @Test
    fun `devices that fail to load set loadError and an empty list stays an empty list`() {
        val repo = FakeDevices(fail = true)
        val viewModel = DevicesViewModel(repo, FakePlots(plot = plot))
        assertEquals(DENIED, viewModel.uiState.value.loadError)
        assertTrue(viewModel.uiState.value.devices.isEmpty())

        repo.fail = false
        viewModel.load()
        assertNull(viewModel.uiState.value.loadError)
        assertTrue(!viewModel.uiState.value.isLoading)
    }

    @Test
    fun `farms that fail to load set loadError and retry clears it`() {
        var fail = true
        val repo = object : FarmRepository {
            override suspend fun getFarms(): DataResult<List<Farm>> =
                if (fail) DataResult.Error(DENIED) else DataResult.Success(emptyList())
            override suspend fun createFarm(name: String, department: String, province: String, district: String): DataResult<Farm> = error("no")
        }
        val viewModel = FarmsViewModel(repo)
        assertEquals(DENIED, viewModel.uiState.value.loadError)
        fail = false
        viewModel.load()
        assertNull(viewModel.uiState.value.loadError)
    }

    @Test
    fun `advisor clients that fail to load are an error not an empty list`() {
        var fail = true
        val repo = object : AdvisorRepository {
            override suspend fun getSupervisedPlots(): DataResult<List<SupervisedPlot>> =
                if (fail) DataResult.Error(DENIED) else DataResult.Success(emptyList())
            override suspend fun getSeries(plotId: String): DataResult<List<Reading>> = error("no")
        }
        val viewModel = AdvisorDashboardViewModel(repo)
        assertEquals(DENIED, viewModel.uiState.value.errorMessage)
        fail = false
        viewModel.load()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `crop catalog failure is reported`() {
        val plots = FakePlots(cropsFail = true, plot = plot)
        val viewModel = CropCatalogViewModel(plots)
        assertNotNull(viewModel.uiState.value.errorMessage)
        plots.cropsFail = false
        viewModel.load()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `plot detail keeps the plot but flags a readings failure`() {
        val plots = FakePlots(readingsFail = true, plot = plot)
        val viewModel = PlotDetailViewModel(plots, "1")
        assertEquals("Sin conexión", viewModel.uiState.value.readingsError)
        assertEquals("Lote", viewModel.uiState.value.plot?.name)
        plots.readingsFail = false
        viewModel.load()
        assertNull(viewModel.uiState.value.readingsError)
    }
}
