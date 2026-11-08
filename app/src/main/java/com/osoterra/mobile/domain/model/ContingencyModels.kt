package com.osoterra.mobile.domain.model

/** Quién hizo una acción correctiva: el dispositivo por sí solo o una persona. */
enum class ActionSource { DEVICE, PERSON }

/**
 * Acción correctiva con su origen. Las del dispositivo son lavados con la minibomba y llevan el
 * número de lavado, el máximo y la duración, para armar el texto en el idioma de la app.
 */
data class CorrectiveActionRecord(
    val id: String,
    val type: CorrectiveActionType,
    /** Fecha y hora ISO local; los repositorios de prueba la formatean al leerla. */
    val performedAt: String,
    val source: ActionSource = ActionSource.PERSON,
    val flushNumber: Int? = null,
    val flushMax: Int? = null,
    val durationSeconds: Int? = null,
    val notes: String? = null,
) {
    val isAutomatic: Boolean get() = source == ActionSource.DEVICE
}

/** Estado de la contingencia: lavando o esperando (ACTIVE), o sin lavados y a la espera de una persona. */
enum class ContingencyState { ACTIVE, EXHAUSTED }

/**
 * Contingencia de salinidad crítica del dispositivo: tras 2 lecturas críticas seguidas enciende
 * la bomba (hasta [maxFlushes] lavados). Si se agotan, una persona debe actuar.
 */
data class SalinityContingency(
    val state: ContingencyState,
    val flushesUsed: Int,
    val maxFlushes: Int = DEFAULT_MAX_FLUSHES,
    val actions: List<CorrectiveActionRecord> = emptyList(),
) {
    val flushesLeft: Int get() = (maxFlushes - flushesUsed).coerceAtLeast(0)
    val needsPerson: Boolean get() = state == ContingencyState.EXHAUSTED

    /** Primero las acciones del dispositivo y luego las de las personas, cada grupo en su orden. */
    val automaticActions: List<CorrectiveActionRecord> get() = actions.filter { it.isAutomatic }
    val personActions: List<CorrectiveActionRecord> get() = actions.filterNot { it.isAutomatic }

    companion object {
        const val DEFAULT_MAX_FLUSHES = 3
    }
}
