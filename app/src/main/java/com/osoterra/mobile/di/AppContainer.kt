package com.osoterra.mobile.di

import android.content.Context
import com.osoterra.mobile.BuildConfig
import com.osoterra.mobile.core.network.ApiClient
import com.osoterra.mobile.data.remote.AdvisorRepositoryRemote
import com.osoterra.mobile.data.remote.AlertRepositoryRemote
import com.osoterra.mobile.data.remote.AuthRepositoryRemote
import com.osoterra.mobile.data.remote.DeviceRepositoryRemote
import com.osoterra.mobile.data.remote.FarmRepositoryRemote
import com.osoterra.mobile.data.remote.NotificationPreferencesRemote
import com.osoterra.mobile.data.remote.PlotRepositoryRemote
import com.osoterra.mobile.data.remote.RemoteMonitoringReader
import com.osoterra.mobile.data.remote.SubscriptionRepositoryRemote
import com.osoterra.mobile.data.repository.AdvisorRepositoryMock
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
 * Las pantallas solo conocen los contratos de `domain.repository`, así que no cambian.
 */
class AppContainer(context: Context) {

    @Volatile
    var sessionToken: String? = null

    private val sessionStore = SessionStore(context.applicationContext)

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

    val authRepository: AuthRepository
    val farmRepository: FarmRepository
    val plotRepository: PlotRepository
    val alertRepository: AlertRepository
    val deviceRepository: DeviceRepository
    val subscriptionRepository: SubscriptionRepository
    val advisorRepository: AdvisorRepository
    val notificationPreferencesRepository: NotificationPreferencesRepository

    init {
        if (usesFakeApi) {
            val farms = FarmRepositoryMock()
            val plots = PlotRepositoryMock(farms)
            authRepository = AuthRepositoryMock(sessionStore, onSession)
            farmRepository = farms
            plotRepository = plots
            alertRepository = AlertRepositoryMock()
            deviceRepository = DeviceRepositoryMock(plots)
            subscriptionRepository = SubscriptionRepositoryMock()
            advisorRepository = AdvisorRepositoryMock()
            notificationPreferencesRepository = NotificationPreferencesStore(context.applicationContext)
        } else {
            val api = apiClient.api
            val reader = RemoteMonitoringReader(api, sessionStore)
            authRepository = AuthRepositoryRemote(api, sessionStore, onSession)
            farmRepository = FarmRepositoryRemote(api, reader)
            plotRepository = PlotRepositoryRemote(api, reader)
            alertRepository = AlertRepositoryRemote(api, reader)
            deviceRepository = DeviceRepositoryRemote(api, reader)
            subscriptionRepository = SubscriptionRepositoryRemote(api)
            advisorRepository = AdvisorRepositoryRemote(api, reader)
            notificationPreferencesRepository = NotificationPreferencesRemote(api)
        }
    }

    suspend fun bootstrapSession(): User? {
        sessionToken = sessionStore.readToken()
        return sessionStore.readUser()
    }
}
