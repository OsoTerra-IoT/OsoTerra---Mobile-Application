package com.osoterra.mobile.ui.subscription

import java.util.Locale

/** Símbolo para mostrar: «S/» para soles; cualquier otra moneda, su código. */
fun currencySymbol(code: String): String = when (code) {
    "PEN" -> "S/"
    "USD" -> "US$"
    else -> code
}

/** «S/ 29.90»; siempre con dos decimales y punto, sin depender del idioma del teléfono. */
fun formatMoney(amount: Double, currency: String): String =
    "${currencySymbol(currency)} ${String.format(Locale.US, "%.2f", amount)}"
