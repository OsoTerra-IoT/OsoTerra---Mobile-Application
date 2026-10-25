package com.osoterra.mobile.data.remote

import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.data.remote.dto.ApiErrorDto
import com.osoterra.mobile.data.remote.dto.CropDto
import com.osoterra.mobile.data.remote.dto.NotificationPreferenceDto
import com.osoterra.mobile.data.remote.dto.SetNotificationPreferenceDto
import com.osoterra.mobile.data.remote.dto.SoilReadingDto
import com.osoterra.mobile.data.remote.dto.UserAccountDto
import com.osoterra.mobile.domain.model.AlertSeverity
import com.osoterra.mobile.domain.model.Crop
import com.osoterra.mobile.domain.model.NotificationPreferences
import com.osoterra.mobile.domain.model.Reading
import com.osoterra.mobile.domain.model.SalinityLevel
import com.osoterra.mobile.domain.model.User
import com.osoterra.mobile.domain.model.UserRole
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val errorJson = Json { ignoreUnknownKeys = true }

/**
 * Ejecuta una llamada al API y traduce los fallos a mensajes para la persona usuaria:
 * el mensaje del Backend cuando responde con error, o uno de conexión si no responde.
 */
suspend fun <T> apiCall(block: suspend () -> T): DataResult<T> = try {
    DataResult.Success(block())
} catch (error: HttpException) {
    DataResult.Error(httpMessage(error), error)
} catch (error: IOException) {
    DataResult.Error("No se pudo conectar con el servidor. Revisa tu conexión.", error)
} catch (error: IllegalStateException) {
    DataResult.Error(error.message ?: "No se pudo completar la operación.", error)
} catch (error: NoSuchElementException) {
    DataResult.Error("No se encontró el recurso solicitado.", error)
}

private fun httpMessage(error: HttpException): String {
    val body = runCatching { error.response()?.errorBody()?.string() }.getOrNull()
    val apiError = body?.let { runCatching { errorJson.decodeFromString<ApiErrorDto>(it) }.getOrNull() }
    return when (error.code()) {
        401 -> "Tu sesión no es válida. Vuelve a iniciar sesión."
        403 -> "No tienes permiso para esta acción."
        404 -> "No se encontró el recurso solicitado."
        409 -> apiError?.message?.takeIf { it.isNotBlank() } ?: "La operación no es posible en el estado actual."
        else -> apiError?.fieldErrors?.values?.firstOrNull()
            ?: apiError?.message?.takeIf { it.isNotBlank() }
            ?: "El servidor respondió con un error (${error.code()})."
    }
}

/** Misma regla que el Backend para graduar alertas: lectura compensada / umbral del cultivo. */
fun salinityLevel(ecDsPerM: Double?, thresholdDsPerM: Double?): SalinityLevel {
    if (ecDsPerM == null || thresholdDsPerM == null || thresholdDsPerM <= 0.0) return SalinityLevel.NO_DATA
    val ratio = ecDsPerM / thresholdDsPerM
    return when {
        ratio >= 1.5 -> SalinityLevel.CRITICAL
        ratio >= 1.2 -> SalinityLevel.HIGH
        ratio > 1.0 -> SalinityLevel.WATCH
        else -> SalinityLevel.NORMAL
    }
}

/** WATCH → En vigilancia, WARNING → Alto, CRITICAL → Muy alto (guía de estilo, sección 5.1). */
fun alertSeverity(apiSeverity: String): AlertSeverity = when (apiSeverity) {
    "CRITICAL" -> AlertSeverity.CRITICAL
    "WARNING" -> AlertSeverity.HIGH
    else -> AlertSeverity.WATCH
}

/** Preferencias del Backend (severidad mínima y canal) en la escala de la app. */
fun NotificationPreferenceDto.toPreferences(): NotificationPreferences = NotificationPreferences(
    minSeverity = alertSeverity(minimumSeverity),
    pushEnabled = channel == "PUSH" || channel == "BOTH",
    emailEnabled = channel == "EMAIL" || channel == "BOTH",
)

/** El Backend exige un canal; sin push queda el correo. */
fun NotificationPreferences.toSetDto(): SetNotificationPreferenceDto = SetNotificationPreferenceDto(
    minimumSeverity = when (minSeverity) {
        AlertSeverity.CRITICAL -> "CRITICAL"
        AlertSeverity.HIGH -> "WARNING"
        AlertSeverity.WATCH -> "WATCH"
    },
    channel = when {
        pushEnabled && emailEnabled -> "BOTH"
        pushEnabled -> "PUSH"
        else -> "EMAIL"
    },
)

fun UserAccountDto.toUser(): User = User(
    id = id.toString(),
    fullName = "$firstName $lastName".trim(),
    email = email,
    role = if (role == "ADVISOR") UserRole.ADVISOR else UserRole.PRODUCER,
)

fun CropDto.toCrop(): Crop = Crop(
    id = id.toString(),
    name = commonName,
    thresholdDsPerM = salinityThresholdDsM,
    toleranceClass = saltToleranceClass,
)

fun SoilReadingDto.toReading(): Reading = Reading(
    id = id.toString(),
    timestamp = displayDateTime(capturedAt) ?: capturedAt,
    capturedAt = capturedAt,
    ecDsPerM = compensatedConductivityDsM,
    humidityPct = moisturePercentage,
    temperatureC = temperatureCelsius,
)

private val MONTHS_ES = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "set", "oct", "nov", "dic")
private val MONTHS_EN = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** Abreviaturas de mes en el idioma de la app (Locale por defecto, que fija LocaleStore). */
fun shortMonths(locale: Locale = Locale.getDefault()): List<String> =
    if (locale.language == "en") MONTHS_EN else MONTHS_ES
private val LOCAL_DATE_TIME = Regex("^(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}).*")
private val ISO_DATE = Regex("\\d{4}-\\d{2}-\\d{2}")

/**
 * "2026-09-30T08:00:00" → "30 set, 08:00" ("Sep 30, 08:00" en inglés). Sin java.time, que exige
 * API 26 y la app admite desde API 24; deja el texto como está si no tiene ese formato.
 */
fun displayDateTime(isoLocalDateTime: String?, locale: Locale = Locale.getDefault()): String? =
    isoLocalDateTime?.let { text ->
        val match = LOCAL_DATE_TIME.matchEntire(text) ?: return@let text
        val (_, month, day, hour, minute) = match.destructured
        "${shortDate(month.toInt(), day.toInt(), locale)}, $hour:$minute"
    }

/** "2026-09-30T08:00:00" → "30 set" ("Sep 30"), para el eje de tiempo de los gráficos. */
fun displayShortDate(isoLocalDateTime: String?, locale: Locale = Locale.getDefault()): String? =
    isoLocalDateTime?.let { LOCAL_DATE_TIME.matchEntire(it) }?.destructured?.let { (_, month, day) ->
        shortDate(month.toInt(), day.toInt(), locale)
    }

/** "2026-09-30T08:05:00" → "08:05", para el eje de tiempo cuando todas las lecturas son del mismo día. */
fun displayTime(isoLocalDateTime: String?): String? =
    isoLocalDateTime?.let { LOCAL_DATE_TIME.matchEntire(it) }?.destructured?.let { (_, _, _, hour, minute) -> "$hour:$minute" }

private fun shortDate(month: Int, day: Int, locale: Locale): String {
    val name = shortMonths(locale)[month - 1]
    return if (locale.language == "en") "$name $day" else "$day $name"
}

/** La fecha si ya es YYYY-MM-DD; si no, la de hoy. */
fun isoDateOrToday(date: String): String =
    date.takeIf { ISO_DATE.matches(it) } ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

/** Separa el nombre completo del registro en nombres y apellidos, como los pide el API. */
fun splitFullName(fullName: String): Pair<String, String> {
    val parts = fullName.trim().split(Regex("\\s+"))
    return if (parts.size == 1) parts[0] to parts[0]
    else parts.first() to parts.drop(1).joinToString(" ")
}
