package com.osoterra.mobile.domain.model

/** Solicitud de un asesor para ver las parcelas del productor; espera su aceptación. */
data class AdvisoryRequest(
    val id: String,
    val advisorName: String,
    val advisorEmail: String,
    /** Fecha y hora ya formateadas para la pantalla; null si no se conoce. */
    val requestedAt: String?,
)
