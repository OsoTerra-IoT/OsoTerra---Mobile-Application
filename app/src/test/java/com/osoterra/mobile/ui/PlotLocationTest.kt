package com.osoterra.mobile.ui

import com.osoterra.mobile.ui.plot.formatCoordinate
import org.junit.Assert.assertEquals
import org.junit.Test

class PlotLocationTest {

    @Test
    fun `coordinates keep six decimals with a dot whatever the phone language`() {
        assertEquals("-5.194490", formatCoordinate(-5.19449))
        assertEquals("-80.632823", formatCoordinate(-80.6328231))
    }
}
