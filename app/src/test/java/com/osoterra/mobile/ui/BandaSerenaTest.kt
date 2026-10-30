package com.osoterra.mobile.ui

import com.osoterra.mobile.domain.model.Crop
import com.osoterra.mobile.domain.model.Plot
import com.osoterra.mobile.domain.model.Reading
import com.osoterra.mobile.domain.model.SalinityLevel
import com.osoterra.mobile.ui.dashboard.DashboardUiState
import com.osoterra.mobile.ui.dashboard.DashboardViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BandaSerenaTest {

    private fun plot(id: String, name: String, level: SalinityLevel) = Plot(
        id = id,
        name = name,
        farmName = "Finca",
        areaHectares = 1.0,
        crop = Crop("c", "Uva", 1.5, "Sensible"),
        salinityLevel = level,
        lastEcDsPerM = 1.0,
        lastReadingAt = null,
        deviceOnline = true,
    )

    private fun reading(at: String?) = Reading("r", at ?: "", at, 1.0, 20.0, 20.0)

    @Test
    fun `the big card shows the most urgent plot and the rest go to tiles`() {
        val state = DashboardUiState(
            isLoading = false,
            plots = listOf(
                plot("1", "Norte", SalinityLevel.NORMAL),
                plot("2", "Sur", SalinityLevel.WATCH),
                plot("3", "Algarrobo", SalinityLevel.CRITICAL),
            ),
        )
        assertEquals("3", state.focusPlot?.id)
        assertEquals(listOf("2", "1"), state.otherPlots.map { it.id })
    }

    @Test
    fun `the advisor sees the short plot name when the client list has it`() {
        val algarrobo = plot("3", "Sector El Algarrobo · Martín Yovera", SalinityLevel.CRITICAL)
        val state = DashboardUiState(plots = listOf(algarrobo), plotTitles = mapOf("3" to "Sector El Algarrobo"))
        assertEquals("Sector El Algarrobo", state.titleOf(algarrobo))
        assertEquals("Norte", state.titleOf(plot("1", "Norte", SalinityLevel.NORMAL)))
    }

    @Test
    fun `the trend caption counts days only when the dates are known`() {
        val month = listOf(reading("2026-10-01T06:00:00"), reading("2026-10-30T02:00:00"))
        assertEquals(30, DashboardViewModel.daySpan(month))
        assertNull(DashboardViewModel.daySpan(listOf(reading(null), reading("2026-10-30T02:00:00"))))
        assertNull(DashboardViewModel.daySpan(listOf(reading("2026-10-30T01:00:00"), reading("2026-10-30T02:00:00"))))
    }
}
