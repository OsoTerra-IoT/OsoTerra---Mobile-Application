package com.osoterra.mobile.ui.common

import java.util.Locale

/**
 * Números con punto decimal en los dos idiomas, como en la guía de estilo («3.5 dS/m»,
 * «4.2 ha») y en la Web App. La unidad va siempre separada por un espacio.
 */
fun Double.fmt(decimals: Int = 2): String = String.format(Locale.US, "%.${decimals}f", this)

/** «1.90 dS/m». */
fun Double.dsPerM(decimals: Int = 2): String = "${fmt(decimals)} dS/m"
