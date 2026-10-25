package com.osoterra.mobile.domain.model

/**
 * Niveles de salinidad de la guía de estilo (sección 5.1): Normal, En vigilancia, Alto,
 * Muy alto y Sin lectura. Se gradúan con la misma razón ECe / umbral del cultivo que usa
 * el Backend para sus alertas (WATCH, WARNING y CRITICAL).
 */
enum class SalinityLevel {
    NORMAL,
    WATCH,
    HIGH,
    CRITICAL,
    NO_DATA,
}
