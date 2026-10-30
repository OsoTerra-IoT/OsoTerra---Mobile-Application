package com.osoterra.mobile.di

import android.content.Context
import com.osoterra.mobile.BuildConfig
import com.osoterra.mobile.core.network.ApiClient
import com.osoterra.mobile.core.util.LocaleStore
import com.osoterra.mobile.data.demo.DEMO_TOKEN_PREFIX
import com.osoterra.mobile.data.demo.DemoAdvisorRepository
import com.osoterra.mobile.data.demo.DemoAdvisoryLinkRepository
import com.osoterra.mobile.data.demo.DemoAlertRepository
import com.osoterra.mobile.data.demo.DemoAuthRepository
import com.osoterra.mobile.data.demo.DemoDeviceRepository
import com.osoterra.mobile.data.demo.DemoFarmRepository
import com.osoterra.mobile.data.demo.DemoModeStore
import com.osoterra.mobile.data.demo.DemoNotificationPreferencesRepository
import com.osoterra.mobile.data.demo.DemoPlotRepository
import com.osoterra.mobile.data.demo.DemoScenario
import com.osoterra.mobile.data.demo.DemoStore
import com.osoterra.mobile.data.demo.DemoSubscriptionRepository
import com.osoterra.mobile.data.remote.AdvisorRepositoryRemote
import com.osoterra.mobile.data.remote.AdvisoryLinkRepositoryRemote
import com.osoterra.mobile.data.remote.AlertRepositoryRemote
import com.osoterra.mobile.data.remote.AuthRepositoryRemote
import com.osoterra.mobile.data.remote.DeviceRepositoryRemote
import com.osoterra.mobile.data.remote.FarmRepositoryRemote
import com.osoterra.mobile.data.remote.NotificationPreferencesRemote
import com.osoterra.mobile.data.remote.PlotRepositoryRemote
import com.osoterra.mobile.data.remote.RemoteMonitoringReader
import com.osoterra.mobile.data.remote.SubscriptionRepositoryRemote
import com.osoterra.mobile.data.repository.AdvisorRepositoryMock
import com.osoterra.mobile.data.repository.AdvisoryLinkRepositoryMock
import com.osoterra.mobile.data.repository.AlertRepositoryMock
import com.osoterra.mobile.data.repository.AuthRepositoryMock
import com.osoterra.mobile.data.repository.DeviceRepositoryMock
import com.osoterra.mobile.data.repository.FarmRepositoryMock
import com.osoterra.mobile.data.repository.NotificationPreferencesStore
import com.osoterra.mobile.data.repository.PlotRepositoryMock
import com.osoterra.mobile.data.repository.SessionStore
import com.osoterra.mobile.data.repository.SubscriptionRepositoryMock
import com.osoterra.mobile.domain.model.User
import com.osoterra.mobile.domain.repository.AdvisorRepository
import com.osoterra.mobile.domain.repository.AdvisoryLinkRepository
import com.osoterra.mobile.domain.repository.AlertRepository
import com.osoterra.mobile.domain.repository.AuthRepository
import com.osoterra.mobile.domain.repository.DeviceRepository
import com.osoterra.mobile.domain.repository.FarmRepository
import com.osoterra.mobile.domain.repository.NotificationPreferencesRepository
import com.osoterra.mobile.domain.repository.PlotRepository
import com.osoterra.mobile.domain.repository.SubscriptionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Service locator de la app. Con `BuildConfig.USE_FAKE_API` usa los repositorios mock;
 * si no, los remotos, que consumen el RESTful API del Backend en `BuildConfig.API_BASE_URL`.
 * Con el modo de prueba activo (enlace «Modo de prueba» del login o `BuildConfig.DEMO_MODE`)
 * usa los repositorios en memoria de `data.demo`, sin red.
 * Las pantallas solo conocen los contratos de `domain.repository`, así que no cambian.
 */
class AppContainer(context: Context) {

    @Volatile
    var sessionToken: String? = null

    private val appContext = context.applicationContext

    private val sessionStore = SessionStore(appContext)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val expired = MutableStateFlow(false)

    /** true cuando el Backend rechazó el token guardado; la navegación vuelve al login. */
    val sessionExpired: StateFlow<Boolean> = expired.asStateFlow()

    val apiClient: ApiClient by lazy {
        ApiClient(tokenProvider = { sessionToken }, onUnauthorized = ::expireSession)
    }

    private fun expireSession() {
        sessionToken = null
        expired.value = true
        scope.launch { sessionStore.clear() }
    }

    fun acknowledgeExpiredSession() {
        expired.value = false
    }

    val usesFakeApi: Boolean = BuildConfig.USE_FAKE_API

    private val onSession: (String?) -> Unit = { token -> sessionToken = token }

    /** Los repositorios de un modo de funcionamiento (producto real o modo de prueba). */
    private class RepositorySet(
        val auth: AuthRepository,
        val farm: FarmRepository,
        val plot: PlotRepository,
        val alert: AlertRepository,
        val device: DeviceRepository,
        val subscription: SubscriptionRepository,
        val advisor: AdvisorRepository,
        val notificationPreferences: NotificationPreferencesRepository,
        val advisoryLinks: AdvisoryLinkRepository,
    )

    /** Producto real: el RESTful API del Backend (o los mock de `USE_FAKE_API`). */
    private val product: RepositorySet by lazy {
        if (usesFakeApi) {
            val farms = FarmRepositoryMock()
            val plots = PlotRepositoryMock(farms)
            RepositorySet(
                auth = AuthRepositoryMock(sessionStore, onSession),
                farm = farms,
                plot = plots,
                alert = AlertRepositoryMock(),
                device = DeviceRepositoryMock(plots),
                subscription = SubscriptionRepositoryMock(),
                advisor = AdvisorRepositoryMock(),
                notificationPreferences = NotificationPreferencesStore(appContext),
                advisoryLinks = AdvisoryLinkRepositoryMock(),
            )
        } else {
            val api = apiClient.api
            val reader = RemoteMonitoringReader(api, sessionStore)
            RepositorySet(
                auth = AuthRepositoryRemote(api, sessionStore, onSession),
                farm = FarmRepositoryRemote(api, reader),
                plot = PlotRepositoryRemote(api, reader),
                alert = AlertRepositoryRemote(api, reader),
                device = DeviceRepositoryRemote(api, reader),
                subscription = SubscriptionRepositoryRemote(api),
                advisor = AdvisorRepositoryRemote(api, reader),
                notificationPreferences = NotificationPreferencesRemote(api),
                advisoryLinks = AdvisoryLinkRepositoryRemote(api, sessionStore),
            )
        }
    }

    // --- Modo de prueba (entrevistas de usabilidad): escenarios en memoria, sin red ----------

    private val demoModeStore = DemoModeStore(appContext, defaultEnabled = BuildConfig.DEMO_MODE)
    private val demoStore = DemoStore(locale = { LocaleStore.localeFor(LocaleStore.language(appContext)) })

    private val demoAuth = DemoAuthRepository(demoStore, sessionStore, onSession) {
        demoModeStore.scenario ?: DemoScenario.CRITICAL_ALERT
    }

    private val demo: RepositorySet by lazy {
        RepositorySet(
            auth = demoAuth,
            farm = DemoFarmRepository(demoStore),
            plot = DemoPlotRepository(demoStore),
            alert = DemoAlertRepository(demoStore),
            device = DemoDeviceRepository(demoStore),
            subscription = DemoSubscriptionRepository(demoStore),
            advisor = DemoAdvisorRepository(demoStore),
            notificationPreferences = DemoNotificationPreferencesRepository(demoStore),
            advisoryLinks = DemoAdvisoryLinkRepository(demoStore),
        )
    }

    private val demoEnabled = MutableStateFlow(demoModeStore.enabled)

    /** true mientras el modo de prueba está activo (se recuerda hasta desactivarlo). */
    val demoMode: StateFlow<Boolean> = demoEnabled.asStateFlow()

    private val demoSelected = MutableStateFlow(demoModeStore.scenario)

    /** Escenario de prueba cargado; null si aún no se eligió ninguno. */
    val demoScenario: StateFlow<DemoScenario?> = demoSelected.asStateFlow()

    private val active: RepositorySet
        get() = if (demoEnabled.value) demo else product

    // Getters: cada ViewModel recibe los repositorios del modo activo al crearse.
    val authRepository: AuthRepository get() = active.auth
    val farmRepository: FarmRepository get() = active.farm
    val plotRepository: PlotRepository get() = active.plot
    val alertRepository: AlertRepository get() = active.alert
    val deviceRepository: DeviceRepository get() = active.device
    val subscriptionRepository: SubscriptionRepository get() = active.subscription
    val advisorRepository: AdvisorRepository get() = active.advisor
    val notificationPreferencesRepository: NotificationPreferencesRepository get() = active.notificationPreferences
    val advisoryLinkRepository: AdvisoryLinkRepository get() = active.advisoryLinks

    init {
        if (demoEnabled.value) demoModeStore.scenario?.let { demoStore.load(it) }
    }

    fun enableDemoMode() {
        demoModeStore.enabled = true
        demoEnabled.value = true
    }

    /** Vuelve al producto real: olvida el escenario y cierra la sesión de prueba. */
    suspend fun disableDemoMode() {
        if (sessionStore.readToken()?.startsWith(DEMO_TOKEN_PREFIX) == true) {
            sessionStore.clear()
            sessionToken = null
        }
        demoModeStore.enabled = false
        demoModeStore.scenario = null
        demoStore.clear()
        demoSelected.value = null
        demoEnabled.value = false
    }

    /** Carga (o reinicia) un escenario y entra con su usuario y rol. */
    suspend fun startDemoScenario(scenario: DemoScenario) {
        enableDemoMode()
        demoModeStore.scenario = scenario
        demoSelected.value = scenario
        val snapshot = demoStore.load(scenario)
        demoAuth.startSession(snapshot.user)
    }

    suspend fun bootstrapSession(): User? {
        val token = sessionStore.readToken()
        val isDemoSession = token?.startsWith(DEMO_TOKEN_PREFIX) == true
        // Una sesión de prueba no sirve con el producto real, ni al revés.
        if (token != null && isDemoSession != demoEnabled.value) {
            sessionStore.clear()
            sessionToken = null
            return null
        }
        if (isDemoSession && demoStore.snapshot == null) {
            demoStore.load(demoModeStore.scenario ?: DemoScenario.CRITICAL_ALERT)
        }
        sessionToken = token
        return sessionStore.readUser()
    }
}
