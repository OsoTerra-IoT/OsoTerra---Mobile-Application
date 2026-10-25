package com.osoterra.mobile.ui

import com.osoterra.mobile.domain.model.Crop
import com.osoterra.mobile.domain.model.Plot
import com.osoterra.mobile.domain.model.SalinityLevel
import com.osoterra.mobile.ui.common.dsPerM
import com.osoterra.mobile.ui.common.fmt
import com.osoterra.mobile.ui.dashboard.DashboardUiState
import com.osoterra.mobile.ui.plot.niceStep
import org.junit.Assert.assertEquals
import org.junit.Test

class HeuristicFixesTest {

    private fun plot(id: String, level: SalinityLevel) = Plot(
        id = id,
        name = "Parcela $id",
        farmName = "Finca",
        areaHectares = 1.0,
        crop = Crop("c", "Uva", 1.5, "Sensible"),
        salinityLevel = level,
        lastEcDsPerM = 1.0,
        lastReadingAt = null,
        deviceOnline = true,
    )

    @Test
    fun `the home headline counts only High and Critical plots`() {
        val state = DashboardUiState(
            isLoading = false,
            plots = listOf(
                plot("1", SalinityLevel.NORMAL),
                plot("2", SalinityLevel.WATCH),
                plot("3", SalinityLevel.HIGH),
                plot("4", SalinityLevel.CRITICAL),
                plot("5", SalinityLevel.NO_DATA),
            ),
        )
        assertEquals(listOf("3", "4"), state.needingAttention.map { it.id })
        assertEquals(listOf("2"), state.onWatch.map { it.id })
    }

    @Test
    fun `chart Y axis uses round steps from zero`() {
        assertEquals(0.5, niceStep(1.9), 1e-9)
        assertEquals(1.0, niceStep(3.6), 1e-9)
        assertEquals(2.5, niceStep(9.0), 1e-9)
        assertEquals(0.5, niceStep(0.0), 1e-9)
    }

    @Test
    fun `numbers use a decimal point and a spaced unit in both languages`() {
        assertEquals("1.90", 1.9.fmt(2))
        assertEquals("1.5 dS/m", 1.5.dsPerM(1))
    }
}
