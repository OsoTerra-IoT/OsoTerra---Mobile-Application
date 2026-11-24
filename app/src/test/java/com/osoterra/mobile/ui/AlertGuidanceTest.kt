package com.osoterra.mobile.ui

import com.osoterra.mobile.R
import com.osoterra.mobile.domain.model.AlertSeverity
import com.osoterra.mobile.ui.alerts.recommendation
import org.junit.Assert.assertEquals
import org.junit.Test

class AlertGuidanceTest {

    @Test
    fun `each alert level has its own recommended action`() {
        assertEquals(R.string.alerts_recommendation_watch, AlertSeverity.WATCH.recommendation())
        assertEquals(R.string.alerts_recommendation_high, AlertSeverity.HIGH.recommendation())
        assertEquals(R.string.alerts_recommendation_critical, AlertSeverity.CRITICAL.recommendation())
        assertEquals(AlertSeverity.entries.size, AlertSeverity.entries.map { it.recommendation() }.toSet().size)
    }
}
