package com.osoterra.mobile.data.demo

import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.domain.model.AlertSeverity
import com.osoterra.mobile.domain.model.NotificationPreferences
import com.osoterra.mobile.domain.model.SalinityLevel
import com.osoterra.mobile.domain.model.UserRole
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DemoScenariosTest {

    private val now: Calendar = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 29, 10, 30, 0) }

    private fun build(scenario: DemoScenario) = DemoScenarios.build(scenario, now)

    private fun SalinityLevel.matching(): AlertSeverity? = when (this) {
        SalinityLevel.WATCH -> AlertSeverity.WATCH
        SalinityLevel.HIGH -> AlertSeverity.HIGH
        SalinityLevel.CRITICAL -> AlertSeverity.CRITICAL
        else -> null
    }

    @Test
    fun `there are six numbered scenarios with their role`() {
        assertEquals((1..6).toList(), DemoScenario.entries.map { it.number })
        assertEquals(
            listOf(UserRole.PRODUCER, UserRole.PRODUCER, UserRole.PRODUCER, UserRole.PRODUCER, UserRole.ADVISOR, UserRole.ADVISOR),
            DemoScenario.entries.map { build(it).user.role },
        )
        assertEquals(DemoScenario.ADVISOR_ALERTS, DemoScenario.fromNumber(6))
        assertEquals(null, DemoScenario.fromNumber(7))
    }

    @Test
    fun `scenario 1 is a new account with no plots and sensor OS-014 to link`() {
        val s = build(DemoScenario.NO_PLOTS)
        assertTrue(s.plots.isEmpty())
        assertTrue(s.alerts.isEmpty())
        assertTrue(s.devices.isEmpty())
        assertEquals(listOf("OS-014"), s.availableSensorCodes)
    }

    @Test
    fun `scenario 2 has two grape plots at Normal`() {
        val s = build(DemoScenario.ALL_NORMAL)
        assertEquals(2, s.plots.size)
        assertTrue(s.plots.all { it.crop?.name == "Uva de mesa" && it.salinityLevel == SalinityLevel.NORMAL })
        assertTrue(s.alerts.isEmpty())
    }

    @Test
    fun `scenario 3 has Normal, Watch and Critical plots and an unattended Critical alert`() {
        val s = build(DemoScenario.CRITICAL_ALERT)
        assertEquals(
            listOf(SalinityLevel.NORMAL, SalinityLevel.WATCH, SalinityLevel.CRITICAL),
            s.plots.map { it.salinityLevel },
        )
        val critical = s.plots.single { it.salinityLevel == SalinityLevel.CRITICAL }
        assertEquals(2.3, critical.lastEcDsPerM!!, 1e-9)
        assertEquals(1.5, critical.crop!!.thresholdDsPerM, 1e-9)
        val alert = s.alerts.single()
        assertEquals(AlertSeverity.CRITICAL, alert.severity)
        assertEquals(critical.id, alert.plotId)
        assertFalse(alert.acknowledged)
        assertTrue(s.requests.isEmpty())
    }

    @Test
    fun `scenario 4 is scenario 3 plus a pending advisory request`() {
        val three = build(DemoScenario.CRITICAL_ALERT)
        val four = build(DemoScenario.ADVISOR_REQUEST)
        assertEquals(three.plots, four.plots)
        assertEquals(three.alerts, four.alerts)
        assertEquals(1, four.requests.size)
        assertTrue(four.requests.single().advisorName.startsWith("Ing."))
    }

    @Test
    fun `scenario 5 gives the advisor two clients with a 30 day trend where one plot worsens`() {
        val s = build(DemoScenario.ADVISOR_CLIENTS)
        assertEquals(2, s.supervised.map { it.producerName }.distinct().size)
        assertEquals(2, s.supervised.size)
        assertTrue(s.alerts.isEmpty())
        val changes = s.supervised.map { plot ->
            val series = s.readings.getValue(plot.id)
            assertEquals(30, series.size)
            series.last().ecDsPerM - series.first().ecDsPerM
        }
        assertEquals(1, changes.count { it > 0.3 })
        assertEquals(1, changes.count { kotlin.math.abs(it) < 0.15 })
    }

    @Test
    fun `scenario 6 adds unattended High and Critical alerts to scenario 5`() {
        val five = build(DemoScenario.ADVISOR_CLIENTS)
        val six = build(DemoScenario.ADVISOR_ALERTS)
        assertTrue(six.plots.map { it.id }.containsAll(five.plots.map { it.id }))
        assertEquals(setOf(AlertSeverity.HIGH, AlertSeverity.CRITICAL), six.alerts.map { it.severity }.toSet())
        assertTrue(six.alerts.none { it.acknowledged })
    }

    @Test
    fun `every alert matches the level of its plot and the last reading is the current value`() {
        DemoScenario.entries.map(::build).forEach { s ->
            s.alerts.forEach { alert ->
                val plot = s.plots.single { it.id == alert.plotId }
                assertEquals(plot.salinityLevel.matching(), alert.severity)
                assertEquals(plot.lastEcDsPerM!!, alert.observedDsPerM!!, 1e-9)
            }
            s.plots.forEach { plot ->
                assertEquals(plot.lastEcDsPerM!!, s.readings.getValue(plot.id).last().ecDsPerM, 1e-9)
            }
        }
    }

    @Test
    fun `loading a scenario again restarts it`() = runTest {
        val store = DemoStore(now = { now })
        store.load(DemoScenario.CRITICAL_ALERT)
        val alerts = DemoAlertRepository(store)
        val alertId = store.snapshot!!.alerts.single().id
        alerts.acknowledge(alertId)
        assertTrue((alerts.getAlerts() as DataResult.Success).data.single().acknowledged)

        store.load(DemoScenario.CRITICAL_ALERT)
        assertFalse((alerts.getAlerts() as DataResult.Success).data.single().acknowledged)
    }

    @Test
    fun `the farmer can choose to be notified only at Critical`() = runTest {
        val store = DemoStore(now = { now }).apply { load(DemoScenario.CRITICAL_ALERT) }
        val prefs = DemoNotificationPreferencesRepository(store)
        prefs.savePreferences(NotificationPreferences(minSeverity = AlertSeverity.CRITICAL))
        assertEquals(AlertSeverity.CRITICAL, (prefs.getPreferences() as DataResult.Success).data.minSeverity)
    }

    @Test
    fun `sensor OS-014 links to a new plot and an unknown code is rejected`() = runTest {
        val store = DemoStore(now = { now }).apply { load(DemoScenario.NO_PLOTS) }
        val plots = DemoPlotRepository(store)
        val devices = DemoDeviceRepository(store)
        val farmId = store.snapshot!!.farms.first().id
        val plot = (plots.createPlot(com.osoterra.mobile.domain.model.NewPlot(farmId, "Sector Norte", 2.0, null, null, "c-uva")) as DataResult.Success).data

        assertTrue(devices.registerDevice("OS-999", plot.id) is DataResult.Error)
        assertTrue(devices.registerDevice("os-014", plot.id) is DataResult.Success)
        val linked = (plots.getPlot(plot.id) as DataResult.Success).data
        assertTrue(linked.deviceOnline)
        assertEquals(SalinityLevel.NORMAL, linked.salinityLevel)
        assertTrue(devices.registerDevice("OS-014", plot.id) is DataResult.Error)
    }

    @Test
    fun `accepting the advisory request removes it from the pending list`() = runTest {
        val store = DemoStore(now = { now }).apply { load(DemoScenario.ADVISOR_REQUEST) }
        val links = DemoAdvisoryLinkRepository(store)
        val request = (links.getPendingRequests() as DataResult.Success).data.single()
        links.accept(request.id)
        assertTrue((links.getPendingRequests() as DataResult.Success).data.isEmpty())
    }
}
