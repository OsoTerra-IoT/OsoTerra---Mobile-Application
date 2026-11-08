package com.osoterra.mobile.ui

import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.data.remote.buildSubscriptionOverview
import com.osoterra.mobile.data.remote.dto.SubscriptionDto
import com.osoterra.mobile.data.remote.dto.SubscriptionPlanDto
import com.osoterra.mobile.data.repository.InMemorySubscriptions
import com.osoterra.mobile.data.repository.SubscriptionPlans
import com.osoterra.mobile.domain.model.Crop
import com.osoterra.mobile.domain.model.Farm
import com.osoterra.mobile.domain.model.NewPlot
import com.osoterra.mobile.domain.model.Plot
import com.osoterra.mobile.domain.model.Reading
import com.osoterra.mobile.domain.model.Subscription
import com.osoterra.mobile.domain.model.SubscriptionOverview
import com.osoterra.mobile.domain.model.SubscriptionStatus
import com.osoterra.mobile.domain.repository.FarmRepository
import com.osoterra.mobile.domain.repository.PlotRepository
import com.osoterra.mobile.domain.repository.SubscriptionRepository
import com.osoterra.mobile.ui.plot.CreatePlotViewModel
import com.osoterra.mobile.ui.subscription.SubscriptionAction
import com.osoterra.mobile.ui.subscription.SubscriptionNotice
import com.osoterra.mobile.ui.subscription.SubscriptionViewModel
import com.osoterra.mobile.ui.subscription.formatMoney
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionTest {

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private fun planDto(id: Long, name: String, price: Double, cycle: String, max: Int, free: Boolean, active: Boolean = true) =
        SubscriptionPlanDto(id, name, price, "PEN", cycle, max, free, active)

    private fun subDto(id: Long, planId: Long, status: String, total: Int, consumed: Int = 3) =
        SubscriptionDto(id, 7, planId, status, total, consumed, "2026-11-01", null)

    private val planDtos = listOf(
        planDto(3, "Pro", 299.90, "ANNUAL", 50, false),
        planDto(1, "Free", 0.0, "NONE", 2, true),
        planDto(2, "Basic", 29.90, "MONTHLY", 10, false),
        planDto(4, "Vieja", 5.0, "MONTHLY", 5, false, active = false),
    )

    @Test
    fun `overview shows the active plan with real usage, the pending request and active plans by price`() {
        val overview = buildSubscriptionOverview(
            listOf(subDto(1, 1, "ACTIVE", 2), subDto(2, 2, "ACTIVE", 10), subDto(3, 3, "PENDING_PAYMENT", 50), subDto(4, 2, "CANCELLED", 10)),
            planDtos,
        )
        // El Backend aplica el mayor límite entre las activas.
        assertEquals("Basic", overview.current?.planName)
        assertEquals(3, overview.current?.plotsUsed)
        assertEquals(10, overview.current?.plotsAllowed)
        assertEquals("Pro", overview.pending?.planName)
        assertEquals(SubscriptionStatus.PENDING_PAYMENT, overview.pending?.status)
        assertEquals(listOf("Free", "Basic", "Pro"), overview.plans.map { it.name })
    }

    @Test
    fun `overview without subscriptions has no current plan but still lists the plans`() {
        val overview = buildSubscriptionOverview(emptyList(), planDtos)
        assertNull(overview.current)
        assertNull(overview.pending)
        assertEquals(3, overview.plans.size)
    }

    @Test
    fun `a free plan replaces the active one and a paid plan stays pending`() = runBlocking {
        val repo = InMemorySubscriptions(initialPlanId = SubscriptionPlans.basic.id, plotsUsed = { 4 })
        assertEquals(10, repo.plotLimit())

        val paid = (repo.subscribe(SubscriptionPlans.pro.id) as DataResult.Success).data
        assertEquals(SubscriptionStatus.PENDING_PAYMENT, paid.status)
        assertEquals(10, repo.plotLimit())

        val free = (repo.subscribe(SubscriptionPlans.free.id) as DataResult.Success).data
        assertEquals(SubscriptionStatus.ACTIVE, free.status)
        assertEquals(2, repo.plotLimit())
        val overview = (repo.getOverview() as DataResult.Success).data
        assertEquals("Free", overview.current?.planName)
        assertEquals("Pro", overview.pending?.planName)
    }

    @Test
    fun `cancelling removes the plan and cancelling twice is a conflict`() = runBlocking {
        val repo = InMemorySubscriptions(initialPlanId = SubscriptionPlans.basic.id, plotsUsed = { 0 })
        val current = (repo.getOverview() as DataResult.Success).data.current!!
        assertTrue(repo.cancel(current.id) is DataResult.Success)
        assertNull(repo.plotLimit())
        val again = repo.cancel(current.id) as DataResult.Error
        assertEquals(409, again.httpCode)
    }

    @Test
    fun `price is shown in soles with two decimals`() {
        assertEquals("S/ 29.90", formatMoney(29.9, "PEN"))
        assertEquals("S/ 299.90", formatMoney(299.9, "PEN"))
        assertEquals("EUR 10.00", formatMoney(10.0, "EUR"))
    }

    private class FakeSubscriptions(
        var overviewFails: Boolean = false,
        var actionFails: Boolean = false,
    ) : SubscriptionRepository {
        private val inner = InMemorySubscriptions(initialPlanId = SubscriptionPlans.basic.id, plotsUsed = { 4 })
        val subscribed = mutableListOf<String>()
        val cancelled = mutableListOf<String>()
        override suspend fun getOverview(): DataResult<SubscriptionOverview> =
            if (overviewFails) DataResult.Error("Sin conexión") else inner.getOverview()
        override suspend fun subscribe(planId: String): DataResult<Subscription> {
            subscribed += planId
            return if (actionFails) DataResult.Error("Falló") else inner.subscribe(planId)
        }
        override suspend fun cancel(subscriptionId: String): DataResult<Subscription> {
            cancelled += subscriptionId
            return if (actionFails) DataResult.Error("Falló") else inner.cancel(subscriptionId)
        }
    }

    @Test
    fun `a load failure is an error with retry, not an empty screen`() {
        val repo = FakeSubscriptions(overviewFails = true)
        val viewModel = SubscriptionViewModel(repo)
        assertEquals("Sin conexión", viewModel.uiState.value.loadError)
        assertNull(viewModel.uiState.value.overview)
        repo.overviewFails = false
        viewModel.load()
        assertNull(viewModel.uiState.value.loadError)
        assertNotNull(viewModel.uiState.value.overview)
    }

    @Test
    fun `nothing is sent until the person confirms`() {
        val repo = FakeSubscriptions()
        val viewModel = SubscriptionViewModel(repo)
        val pro = viewModel.uiState.value.overview!!.plans.first { it.name == "Pro" }
        viewModel.askChange(pro)
        assertTrue(viewModel.uiState.value.confirming is SubscriptionAction.Change)
        assertTrue(repo.subscribed.isEmpty())

        viewModel.dismissConfirmation()
        assertTrue(repo.subscribed.isEmpty())

        viewModel.askChange(pro)
        viewModel.confirm()
        assertEquals(listOf(pro.id), repo.subscribed)
        assertEquals(SubscriptionNotice.Kind.PENDING_PAYMENT, viewModel.uiState.value.notice?.kind)
        assertEquals("Pro", viewModel.uiState.value.overview?.pending?.planName)
    }

    @Test
    fun `switching to the free plan reports it as activated and refreshes the usage`() {
        val viewModel = SubscriptionViewModel(FakeSubscriptions())
        val free = viewModel.uiState.value.overview!!.plans.first { it.isFree }
        viewModel.askChange(free)
        assertTrue((viewModel.uiState.value.confirming as SubscriptionAction.Change).replacesCurrent)
        viewModel.confirm()
        assertEquals(SubscriptionNotice.Kind.ACTIVATED, viewModel.uiState.value.notice?.kind)
        assertEquals("Free", viewModel.uiState.value.overview?.current?.planName)
        assertEquals(2, viewModel.uiState.value.overview?.current?.plotsAllowed)
    }

    @Test
    fun `a failed action keeps the error, can be retried and clears on success`() {
        val repo = FakeSubscriptions(actionFails = true)
        val viewModel = SubscriptionViewModel(repo)
        viewModel.askCancel(viewModel.uiState.value.overview!!.current!!)
        viewModel.confirm()
        assertEquals("Falló", viewModel.uiState.value.actionError)
        assertNotNull(viewModel.uiState.value.failedAction)
        assertTrue(!viewModel.uiState.value.isWorking)

        repo.actionFails = false
        viewModel.retryAction()
        assertNull(viewModel.uiState.value.actionError)
        assertEquals(SubscriptionNotice.Kind.CANCELLED, viewModel.uiState.value.notice?.kind)
        assertNull(viewModel.uiState.value.overview?.current)
        assertEquals(2, repo.cancelled.size)
    }

    // --- Crear parcela por encima del límite del plan (409) ---

    private class LimitedPlots : PlotRepository {
        override suspend fun getPlots(): DataResult<List<Plot>> = DataResult.Success(emptyList())
        override suspend fun getPlot(plotId: String): DataResult<Plot> = error("no")
        override suspend fun getReadings(plotId: String): DataResult<List<Reading>> = error("no")
        override suspend fun createPlot(plot: NewPlot): DataResult<Plot> =
            DataResult.Error("The subscription plan allows 2 active plots and you already have 2", httpCode = 409)
        override suspend fun assignCrop(plotId: String, cropId: String): DataResult<Plot> = error("no")
        override suspend fun deactivatePlot(plotId: String): DataResult<Unit> = error("no")
        override suspend fun getCrops(): DataResult<List<Crop>> = DataResult.Success(emptyList())
    }

    private class OneFarm : FarmRepository {
        override suspend fun getFarms(): DataResult<List<Farm>> =
            DataResult.Success(listOf(Farm("1", "Finca", "Lambayeque", "Chiclayo", "Pomalca")))
        override suspend fun createFarm(name: String, department: String, province: String, district: String): DataResult<Farm> =
            error("no")
    }

    @Test
    fun `creating a plot over the plan limit shows the localized limit message, not the raw Backend text`() {
        val viewModel = CreatePlotViewModel(LimitedPlots(), OneFarm())
        viewModel.onNameChange("Lote 3")
        viewModel.onAreaChange("1.5")
        viewModel.onLatChange("-6.7")
        viewModel.onLngChange("-79.8")
        viewModel.createPlot()
        val state = viewModel.uiState.value
        assertNotNull(state.errorRes)
        assertNull(state.errorMessage)
        assertTrue(!state.isSaving)
    }
}
