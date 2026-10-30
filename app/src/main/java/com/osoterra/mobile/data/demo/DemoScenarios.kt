package com.osoterra.mobile.data.demo

import com.osoterra.mobile.data.remote.salinityLevel
import com.osoterra.mobile.domain.model.Alert
import com.osoterra.mobile.domain.model.AlertSeverity
import com.osoterra.mobile.domain.model.Crop
import com.osoterra.mobile.domain.model.Device
import com.osoterra.mobile.domain.model.DeviceStatus
import com.osoterra.mobile.domain.model.Farm
import com.osoterra.mobile.domain.model.NotificationPreferences
import com.osoterra.mobile.domain.model.Plot
import com.osoterra.mobile.domain.model.Reading
import com.osoterra.mobile.domain.model.SupervisedPlot
import com.osoterra.mobile.domain.model.User
import com.osoterra.mobile.domain.model.UserRole
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.round
import kotlin.math.sin

/** Escenarios numerados del modo de prueba para las entrevistas de validación de usabilidad. */
enum class DemoScenario(val number: Int, val role: UserRole) {
    NO_PLOTS(1, UserRole.PRODUCER),
    ALL_NORMAL(2, UserRole.PRODUCER),
    CRITICAL_ALERT(3, UserRole.PRODUCER),
    ADVISOR_REQUEST(4, UserRole.PRODUCER),
    ADVISOR_CLIENTS(5, UserRole.ADVISOR),
    ADVISOR_ALERTS(6, UserRole.ADVISOR),
    ;

    companion object {
        fun fromNumber(number: Int): DemoScenario? = entries.firstOrNull { it.number == number }
    }
}

/** Solicitud de asesoría tal como se guarda en memoria (fecha ISO, se formatea al leerla). */
data class DemoAdvisoryRequest(
    val id: String,
    val advisorName: String,
    val advisorEmail: String,
    val requestedAt: String,
)

/**
 * Todo lo que ve la app en un escenario. Las fechas (`Plot.lastReadingAt`, `Alert.createdAt`,
 * `Device.lastSeen`, `Reading.capturedAt`) se guardan en ISO local (AAAA-MM-DDTHH:MM:SS) y los
 * repositorios de prueba las formatean al leerlas, en el idioma activo de la app.
 */
data class DemoSnapshot(
    val scenario: DemoScenario,
    val user: User,
    val farms: List<Farm>,
    val plots: List<Plot>,
    val readings: Map<String, List<Reading>>,
    val alerts: List<Alert>,
    val devices: List<Device>,
    /** Parcelas de los clientes del asesor (vacío para el productor). */
    val supervised: List<SupervisedPlot>,
    val requests: List<DemoAdvisoryRequest>,
    /** Códigos de dispositivo sin vincular que acepta «Registrar dispositivo». */
    val availableSensorCodes: List<String>,
    val preferences: NotificationPreferences = NotificationPreferences(),
)

/**
 * Proveedor de escenarios: arma datos realistas de la costa norte del Perú, sin red.
 * Los niveles se calculan con la misma regla ECe / umbral que el Backend (`salinityLevel`),
 * así que un escenario nunca contradice las píldoras de la app.
 */
object DemoScenarios {

    const val SENSOR_CODE = "OS-014"

    val grape = Crop("c-uva", "Uva de mesa", 1.5, "Moderadamente sensible")
    private val avocado = Crop("c-palta", "Palta", 1.1, "Sensible")
    private val blueberry = Crop("c-arandano", "Arándano", 1.5, "Sensible")
    private val mango = Crop("c-mango", "Mango", 1.5, "Sensible")
    private val asparagus = Crop("c-esparrago", "Espárrago", 4.1, "Tolerante")
    private val rice = Crop("c-arroz", "Arroz", 3.0, "Moderadamente sensible")

    val crops = listOf(grape, avocado, blueberry, mango, asparagus, rice)

    private const val DAYS = 30

    /** La última lectura llegó hace 40 minutos; las alertas, un poco después. */
    private const val LAST_READING_MINUTES = 40

    private val producer = User(
        id = "demo-u-productor",
        fullName = "Martín Yovera Sosa",
        email = "martin.yovera@ejemplo.com",
        role = UserRole.PRODUCER,
    )

    private val advisor = User(
        id = "demo-u-asesor",
        fullName = "Lucía Farfán Castillo",
        email = "lucia.farfan@ejemplo.com",
        role = UserRole.ADVISOR,
    )

    private val palmeras = Farm("demo-f-palmeras", "Fundo Las Palmeras", "Piura", "Piura", "Tambogrande")
    private val sanJose = Farm("demo-f-sanjose", "Fundo San José", "Lambayeque", "Lambayeque", "Olmos")

    /** Arma el escenario con fechas relativas a [now] (para que siempre parezca reciente). */
    fun build(scenario: DemoScenario, now: Calendar = Calendar.getInstance()): DemoSnapshot {
        val clock = Clock(now)
        return when (scenario) {
            DemoScenario.NO_PLOTS -> producerSnapshot(scenario, clock, plots = emptyList())
            DemoScenario.ALL_NORMAL -> producerSnapshot(
                scenario,
                clock,
                plots = listOf(
                    PlotSpec("demo-p-norte", "Sector Norte", palmeras, 2.5, grape, start = 1.05, end = 1.10, seed = 1),
                    PlotSpec("demo-p-sur", "Sector Sur", palmeras, 1.8, grape, start = 1.25, end = 1.30, seed = 2),
                ),
            )
            DemoScenario.CRITICAL_ALERT -> criticalProducer(scenario, clock)
            DemoScenario.ADVISOR_REQUEST -> criticalProducer(scenario, clock).copy(
                requests = listOf(
                    DemoAdvisoryRequest(
                        id = "demo-link-1",
                        advisorName = "Ing. ${advisor.fullName}",
                        advisorEmail = advisor.email,
                        requestedAt = clock.minutesAgo(95),
                    ),
                ),
            )
            DemoScenario.ADVISOR_CLIENTS -> advisorSnapshot(scenario, clock, withAlerts = false)
            DemoScenario.ADVISOR_ALERTS -> advisorSnapshot(scenario, clock, withAlerts = true)
        }
    }

    // --- Productor ---------------------------------------------------------------------------

    private fun criticalProducer(scenario: DemoScenario, clock: Clock): DemoSnapshot {
        val plots = listOf(
            PlotSpec("demo-p-norte", "Sector Norte", palmeras, 2.5, grape, start = 1.05, end = 1.10, seed = 1),
            PlotSpec("demo-p-sur", "Sector Sur", palmeras, 1.8, grape, start = 1.35, end = 1.66, seed = 2),
            PlotSpec("demo-p-algarrobo", "Sector El Algarrobo", palmeras, 3.2, grape, start = 1.55, end = 2.30, seed = 3),
        )
        return producerSnapshot(scenario, clock, plots).let { snapshot ->
            snapshot.copy(alerts = listOf(alertFor(plots[2], clock, AlertSeverity.CRITICAL, minutesAgo = 35)))
        }
    }

    private fun producerSnapshot(scenario: DemoScenario, clock: Clock, plots: List<PlotSpec>): DemoSnapshot =
        DemoSnapshot(
            scenario = scenario,
            user = producer,
            farms = listOf(palmeras),
            plots = plots.map { it.toPlot(clock) },
            readings = plots.associate { it.id to it.readings(clock) },
            alerts = emptyList(),
            devices = plots.mapIndexed { index, spec -> spec.device(index, clock) },
            supervised = emptyList(),
            requests = emptyList(),
            availableSensorCodes = listOf(SENSOR_CODE),
        )

    // --- Asesor ------------------------------------------------------------------------------

    private const val MARTIN = "Martín Yovera Sosa"
    private const val ROSA = "Rosa Chunga Ipanaqué"

    private fun advisorSnapshot(scenario: DemoScenario, clock: Clock, withAlerts: Boolean): DemoSnapshot {
        val base = listOf(
            // Estable en Normal.
            PlotSpec("demo-a-norte", "Sector Norte", palmeras, 2.5, grape, 1.05, 1.10, seed = 1, producer = MARTIN),
            // Empeora en los 30 días: de Normal a En vigilancia.
            PlotSpec("demo-a-huerta", "Lote Huerta Vieja", sanJose, 2.0, grape, 1.10, 1.68, seed = 4, producer = ROSA),
        )
        val extra = if (withAlerts) {
            listOf(
                PlotSpec("demo-a-algarrobo", "Sector El Algarrobo", palmeras, 3.2, grape, 1.55, 2.30, seed = 3, producer = MARTIN),
                PlotSpec("demo-a-capilla", "Lote La Capilla", sanJose, 1.6, grape, 1.40, 1.95, seed = 5, producer = ROSA),
            )
        } else {
            emptyList()
        }
        val all = base + extra
        val alerts = if (withAlerts) {
            listOf(
                alertFor(extra[0], clock, AlertSeverity.CRITICAL, minutesAgo = 35),
                alertFor(extra[1], clock, AlertSeverity.HIGH, minutesAgo = 38),
            )
        } else {
            emptyList()
        }
        val plots = all.map { it.toPlot(clock) }
        return DemoSnapshot(
            scenario = scenario,
            user = advisor,
            farms = listOf(palmeras, sanJose),
            plots = plots,
            readings = all.associate { it.id to it.readings(clock) },
            alerts = alerts,
            devices = all.mapIndexed { index, spec -> spec.device(index, clock) },
            supervised = all.zip(plots).map { (spec, plot) ->
                SupervisedPlot(
                    id = plot.id,
                    plotName = spec.name,
                    producerName = spec.producer.orEmpty(),
                    cropName = plot.crop?.name,
                    salinityLevel = plot.salinityLevel,
                    lastEcDsPerM = plot.lastEcDsPerM,
                )
            },
            requests = emptyList(),
            availableSensorCodes = listOf(SENSOR_CODE),
        )
    }

    // --- Piezas comunes ----------------------------------------------------------------------

    private fun alertFor(
        spec: PlotSpec,
        clock: Clock,
        severity: AlertSeverity,
        minutesAgo: Int,
    ) = Alert(
        id = "alert-${spec.id}",
        plotId = spec.id,
        plotName = spec.displayName,
        severity = severity,
        message = "",
        createdAt = clock.minutesAgo(minutesAgo),
        acknowledged = false,
        observedDsPerM = spec.end,
        thresholdDsPerM = spec.crop.thresholdDsPerM,
    )

    private data class PlotSpec(
        val id: String,
        val name: String,
        val farm: Farm,
        val hectares: Double,
        val crop: Crop,
        val start: Double,
        val end: Double,
        val seed: Int,
        /** Cliente del asesor; null en los escenarios de productor. */
        val producer: String? = null,
    ) {
        /** Para el asesor, el nombre lleva al cliente: así se sabe de quién es la alerta. */
        val displayName: String
            get() = producer?.let { "$name · ${it.split(' ').take(2).joinToString(" ")}" } ?: name

        fun toPlot(clock: Clock) = Plot(
            id = id,
            name = displayName,
            farmName = farm.name,
            areaHectares = hectares,
            crop = crop,
            salinityLevel = salinityLevel(end, crop.thresholdDsPerM),
            lastEcDsPerM = end,
            lastReadingAt = clock.minutesAgo(LAST_READING_MINUTES),
            deviceOnline = true,
        )

        /** Una lectura diaria (06:00) durante 30 días; la de hoy es la última y da el valor actual. */
        fun readings(clock: Clock): List<Reading> = (0 until DAYS).map { i ->
            val daysAgo = DAYS - 1 - i
            val progress = i / (DAYS - 1).toDouble()
            val wobble = if (i == DAYS - 1) 0.0 else 0.03 * sin(i * 1.7 + seed)
            val ec = round2(start + (end - start) * progress + wobble)
            val capturedAt = if (daysAgo == 0) clock.minutesAgo(LAST_READING_MINUTES) else clock.iso(daysAgo, hour = 6, minute = 0)
            Reading(
                id = "$id-r-$i",
                timestamp = capturedAt,
                capturedAt = capturedAt,
                ecDsPerM = ec,
                humidityPct = round1(26.0 + 3.0 * sin(i * 0.9 + seed)),
                temperatureC = round1(24.5 + 1.8 * sin(i * 0.6 + seed * 2)),
            )
        }

        fun device(index: Int, clock: Clock) = Device(
            id = "demo-d-$id",
            code = "OS-%03d".format(Locale.US, index + 1),
            plotId = id,
            plotName = displayName,
            status = DeviceStatus.ONLINE,
            batteryPct = null,
            lastSeen = clock.minutesAgo(LAST_READING_MINUTES),
        )
    }

    /** Fechas relativas a «ahora» sin java.time (la app admite API 24). */
    class Clock(private val now: Calendar) {
        private val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

        fun iso(daysAgo: Int, hour: Int, minute: Int): String {
            val calendar = (now.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
            }
            return format.format(calendar.time)
        }

        fun minutesAgo(minutes: Int): String {
            val calendar = (now.clone() as Calendar).apply {
                add(Calendar.MINUTE, -minutes)
                set(Calendar.SECOND, 0)
            }
            return format.format(calendar.time)
        }

        fun nowIso(): String = format.format(now.time)
    }

    private fun round2(value: Double) = round(value * 100) / 100
    private fun round1(value: Double) = round(value * 10) / 10
}
