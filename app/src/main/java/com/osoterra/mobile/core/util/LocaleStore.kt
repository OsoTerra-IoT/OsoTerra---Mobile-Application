package com.osoterra.mobile.core.util

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit
import java.util.Locale

/**
 * Idioma de la app. Como la Web App, abre en español (es_419) aunque el teléfono esté en
 * inglés, y la persona puede cambiar a inglés (en_US) desde el Perfil. Se guarda en
 * SharedPreferences porque hay que leerlo de forma síncrona antes de crear la Activity.
 * Solo usa APIs disponibles desde la API 17, así funciona igual en Android 7 (API 24).
 */
object LocaleStore {
    const val SPANISH = "es"
    const val ENGLISH = "en"

    private const val FILE = "osoterra_locale"
    private const val KEY = "language"

    fun language(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, SPANISH) ?: SPANISH

    fun setLanguage(context: Context, language: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit { putString(KEY, language) }
    }

    fun localeFor(language: String): Locale =
        if (language == ENGLISH) Locale.forLanguageTag("en-US") else Locale.forLanguageTag("es-419")

    /** Contexto con el idioma elegido; también fija el Locale por defecto para formatear fechas. */
    fun wrap(base: Context): Context {
        val locale = localeFor(language(base))
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
