package com.osoterra.mobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.osoterra.mobile.R

/** Plus Jakarta Sans, familia única de la guía de estilo (OFL 1.1), empaquetada en la app. */
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
)

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
)

/**
 * Escala pensada para la edad promedio del segmento de productores (54,5 años): el cuerpo
 * no baja de 16 sp y los datos clave se leen de un vistazo.
 */
val OsoTypography = Typography(
    displaySmall = style(40, 44, FontWeight.ExtraBold, -0.03),
    headlineLarge = style(32, 38, FontWeight.ExtraBold, -0.025),
    headlineMedium = style(26, 32, FontWeight.Bold, -0.02),
    headlineSmall = style(22, 28, FontWeight.Bold, -0.01),
    titleLarge = style(20, 26, FontWeight.Bold),
    titleMedium = style(18, 24, FontWeight.SemiBold),
    titleSmall = style(16, 22, FontWeight.SemiBold),
    bodyLarge = style(18, 26, FontWeight.Normal),
    bodyMedium = style(16, 24, FontWeight.Normal),
    bodySmall = style(14, 20, FontWeight.Normal),
    labelLarge = style(16, 20, FontWeight.SemiBold),
    labelMedium = style(14, 18, FontWeight.SemiBold),
    labelSmall = style(12, 16, FontWeight.SemiBold, 0.04),
)
