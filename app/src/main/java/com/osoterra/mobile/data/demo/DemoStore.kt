package com.osoterra.mobile.data.demo

import android.content.Context
import java.util.Calendar
import java.util.Locale

/**
 * Estado en memoria del modo de prueba. Cargar un escenario reemplaza todo; lo que la persona
 * haga después (marcar alertas, vincular un dispositivo, guardar preferencias) solo vive aquí
 * hasta que se vuelva a cargar un escenario o se cierre la app.
 */
class DemoStore(
    private val now: () -> Calendar = { Calendar.getInstance() },
    /** Idioma de la app para formatear fechas. */
    val locale: () -> Locale = { Locale.getDefault() },
) {

    @Volatile
    private var current: DemoSnapshot? = null

    val snapshot: DemoSnapshot?
        get() = current

    @Synchronized
    fun load(scenario: DemoScenario): DemoSnapshot = DemoScenarios.build(scenario, now()).also { current = it }

    /** El escenario activo; si aún no hay uno, carga [fallback]. */
    @Synchronized
    fun require(fallback: DemoScenario): DemoSnapshot = current ?: load(fallback)

    @Synchronized
    fun update(transform: (DemoSnapshot) -> DemoSnapshot): DemoSnapshot? {
        val updated = current?.let(transform)
        current = updated
        return updated
    }

    fun clear() {
        current = null
    }

    fun nowIso(): String = DemoScenarios.Clock(now()).nowIso()
}

/**
 * Recuerda si el modo de prueba está activo y qué escenario se eligió, hasta que se desactive.
 * SharedPreferences (lectura síncrona) porque el contenedor decide sus repositorios al arrancar.
 */
class DemoModeStore(context: Context, private val defaultEnabled: Boolean) {

    private val prefs = context.applicationContext.getSharedPreferences("osoterra_demo", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, defaultEnabled)
        set(value) {
            prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        }

    var scenario: DemoScenario?
        get() = DemoScenario.fromNumber(prefs.getInt(KEY_SCENARIO, 0))
        set(value) {
            prefs.edit().putInt(KEY_SCENARIO, value?.number ?: 0).apply()
        }

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_SCENARIO = "scenario"
    }
}
