package com.osoterra.mobile.data.remote

import com.osoterra.mobile.domain.model.AlertSeverity
import com.osoterra.mobile.domain.model.SalinityLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteSupportTest {

    @Test
    fun `grades salinity with the same excess ratio as the Backend alerts`() {
        assertEquals(SalinityLevel.NO_DATA, salinityLevel(null, 1.1))
        assertEquals(SalinityLevel.NO_DATA, salinityLevel(1.0, null))
        assertEquals(SalinityLevel.NORMAL, salinityLevel(1.1, 1.1))
        assertEquals(SalinityLevel.WATCH, salinityLevel(1.62, 1.5))
        assertEquals(SalinityLevel.WARNING, salinityLevel(1.9, 1.5))
        assertEquals(SalinityLevel.CRITICAL, salinityLevel(1.74, 1.1))
    }

    @Test
    fun `maps Backend severities to the app scale`() {
        assertEquals(AlertSeverity.MEDIUM, alertSeverity("WATCH"))
        assertEquals(AlertSeverity.HIGH, alertSeverity("WARNING"))
        assertEquals(AlertSeverity.CRITICAL, alertSeverity("CRITICAL"))
    }

    @Test
    fun `formats Java local date-times for display`() {
        assertEquals("30 set, 08:05", displayDateTime("2026-09-30T08:05:00"))
        assertEquals("sin formato", displayDateTime("sin formato"))
        assertEquals(null, displayDateTime(null))
        assertEquals("2026-09-01", isoDateOrToday("2026-09-01"))
    }

    @Test
    fun `splits the full name into first and last names`() {
        assertEquals("Elena" to "Ramos Díaz", splitFullName("  Elena   Ramos Díaz "))
        assertEquals("Elena" to "Elena", splitFullName("Elena"))
    }
}
