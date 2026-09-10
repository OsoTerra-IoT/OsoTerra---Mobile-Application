package com.osoterra.mobile.domain.model

data class SupervisedPlot(
    val id: String,
    val plotName: String,
    val producerName: String,
    val cropName: String?,
    val salinityLevel: SalinityLevel,
    val lastEcDsPerM: Double?,
)

/** Orden de criticidad para ordenar de más crítico a menos (US41). */
fun SalinityLevel.criticalityRank(): Int = when (this) {
    SalinityLevel.CRITICAL -> 4
    SalinityLevel.WARNING -> 3
    SalinityLevel.WATCH -> 2
    SalinityLevel.NORMAL -> 1
    SalinityLevel.NO_DATA -> 0
}
