package com.osoterra.mobile.data.remote

import com.osoterra.mobile.data.remote.dto.NotificationPreferenceDto
import com.osoterra.mobile.domain.model.AlertSeverity
import com.osoterra.mobile.domain.model.NotificationPreferences
import com.osoterra.mobile.domain.model.SalinityLevel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class RemoteSupportTest {

    @Test
    fun `grades salinity with the same excess ratio as the Backend alerts`() {
        assertEquals(SalinityLevel.NO_DATA, salinityLevel(null, 1.1))
        assertEquals(SalinityLevel.NO_DATA, salinityLevel(1.0, null))
        assertEquals(SalinityLevel.NORMAL, salinityLevel(1.1, 1.1))
        assertEquals(SalinityLevel.WATCH, salinityLevel(1.62, 1.5))
        assertEquals(SalinityLevel.HIGH, salinityLevel(1.9, 1.5))
        assertEquals(SalinityLevel.CRITICAL, salinityLevel(1.74, 1.1))
    }

    @Test
    fun `maps Backend severities to the style guide scale`() {
        // WATCH → En vigilancia, WARNING → Alto, CRITICAL → Muy alto.
        assertEquals(AlertSeverity.WATCH, alertSeverity("WATCH"))
        assertEquals(AlertSeverity.HIGH, alertSeverity("WARNING"))
        assertEquals(AlertSeverity.CRITICAL, alertSeverity("CRITICAL"))
    }

    @Test
    fun `formats Java local date-times for display`() {
        val es = Locale.forLanguageTag("es-419")
        val en = Locale.forLanguageTag("en-US")
        assertEquals("30 set, 08:05", displayDateTime("2026-09-30T08:05:00", es))
        assertEquals("Sep 30, 08:05", displayDateTime("2026-09-30T08:05:00", en))
        assertEquals("sin formato", displayDateTime("sin formato", es))
        assertEquals(null, displayDateTime(null, es))
        assertEquals("30 set", displayShortDate("2026-09-30T08:05:00", es))
        assertEquals("Sep 30", displayShortDate("2026-09-30T08:05:00", en))
        assertEquals(null, displayShortDate("Hoy, 08:15", es))
        assertEquals("08:05", displayTime("2026-09-30T08:05:00"))
        assertEquals("2026-09-01", isoDateOrToday("2026-09-01"))
    }

    @Test
    fun `splits the full name into first and last names`() {
        assertEquals("Elena" to "Ramos Díaz", splitFullName("  Elena   Ramos Díaz "))
        assertEquals("Elena" to "Elena", splitFullName("Elena"))
    }

    @Test
    fun `maps notification preferences both ways with the Backend contract`() {
        val fromApi = NotificationPreferenceDto(id = 1, minimumSeverity = "WARNING", channel = "BOTH").toPreferences()
        assertEquals(NotificationPreferences(AlertSeverity.HIGH, pushEnabled = true, emailEnabled = true), fromApi)

        val emailOnly = NotificationPreferences(AlertSeverity.CRITICAL, pushEnabled = false, emailEnabled = true).toSetDto()
        assertEquals("CRITICAL", emailOnly.minimumSeverity)
        assertEquals("EMAIL", emailOnly.channel)
        assertEquals("PUSH", NotificationPreferences(AlertSeverity.WATCH, true, false).toSetDto().channel)
        assertEquals("WATCH", NotificationPreferences(AlertSeverity.WATCH, true, false).toSetDto().minimumSeverity)
    }
}
