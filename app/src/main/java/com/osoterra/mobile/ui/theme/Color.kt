package com.osoterra.mobile.ui.theme

import androidx.compose.ui.graphics.Color

// Tokens de la guía de estilo (sección 5.1 del informe), los mismos de la Landing Page y la Web App.
val Emerald50 = Color(0xFFECFDF5)
val Emerald100 = Color(0xFFD1FAE5)
/** Íconos y elementos grandes. */
val Emerald600 = Color(0xFF059669)
/** Acción: botones con texto blanco (contraste 5,48:1). */
val Emerald700 = Color(0xFF047857)
val Emerald800 = Color(0xFF065F46)
val Emerald900 = Color(0xFF064E3B)
val SurfaceGreen = Color(0xFFF0FDF4)
/** Borde de las superficies verdes destacadas (guía de estilo: #BBF7D0). */
val SurfaceGreenBorder = Color(0xFFBBF7D0)

/** Tinta «suelo» para texto principal. */
val Ink = Color(0xFF16211C)
/** Fondo «sal»: ahora el mismo marfil del tema. */
val Salt = Color(0xFFFAF9F6)
val Line = Color(0xFFE4E6D9)
val Track = Color(0xFFEFEEE7)
val Neutral600 = Color(0xFF4B5563)
val White = Color(0xFFFFFFFF)

// Niveles de salinidad: texto oscuro sobre fondo suave, siempre con ícono y etiqueta.
val SalinityNormal = Color(0xFF1B6234)
val SalinityNormalContainer = Color(0xFFE1F0E4)
val SalinityWatch = Color(0xFF70510B)
val SalinityWatchContainer = Color(0xFFFFF1C6)
val SalinityHigh = Color(0xFF85440D)
val SalinityHighContainer = Color(0xFFFFF0DD)
val SalinityCritical = Color(0xFFA12426)
val SalinityCriticalContainer = Color(0xFFFFEAEB)
val SalinityNoData = Color(0xFF3E5150)
val SalinityNoDataContainer = Color(0xFFE6EBEA)

// Tema «Tierra Serena» (variante 6 del rediseño): fondo marfil, banda bosque y salvia suave.
/** Fondo marfil de todas las pantallas. */
val Ivory = Color(0xFFFAF9F6)
/** Banda superior y cabeceras serenas (blanco encima: 7,6:1). */
val Forest = Emerald800
/** Salvia: solo trazos y decoración, nunca texto. */
val Sage = Color(0xFF7D8471)
/** Salvia oscura para texto secundario (6,9:1 sobre marfil, 7,3:1 sobre blanco). */
val SageInk = Color(0xFF535A47)
/** Divisores y bordes de mosaicos. */
val OliveLine = Color(0xFFE4E6D9)
/** Texto sobre la banda que no es titular (13:1 sobre la banda). */
val BandMuted = Emerald100
/** Íconos de acento sobre la banda. */
val BandAccent = Color(0xFFA7F3D0)
/** Texto principal (neutral 900 de la guía). */
val TextStrong = Color(0xFF111827)

// Segmentos del medidor sobre la banda: tonos claros del nivel (el texto dice la cantidad).
val MeterCritical = Color(0xFFFCA5A5)
val MeterHigh = Color(0xFFFDBA74)
val MeterWatch = Color(0xFFFDE68A)
val MeterNormal = Color(0xFF86EFAC)
val MeterNoData = Color(0xFFCBD5D1)
