package com.osoterra.mobile.di

import android.content.Context
import com.osoterra.mobile.core.network.ApiClient
import com.osoterra.mobile.data.repository.AlertRepositoryMock
import com.osoterra.mobile.data.repository.AuthRepositoryMock
import com.osoterra.mobile.data.repository.FarmRepositoryMock
import com.osoterra.mobile.data.repository.PlotRepositoryMock
import com.osoterra.mobile.data.repository.SessionStore
import com.osoterra.mobile.domain.model.User
import com.osoterra.mobile.domain.repository.AlertRepository
import com.osoterra.mobile.domain.repository.AuthRepository
import com.osoterra.mobile.domain.repository.FarmRepository
import com.osoterra.mobile.domain.repository.PlotRepository

class AppContainer(context: Context) {

    @Volatile
    var sessionToken: String? = null

    private val sessionStore = SessionStore(context.applicationContext)

    val apiClient: ApiClient by lazy { ApiClient(tokenProvider = { sessionToken }) }

    private val farmRepositoryImpl = FarmRepositoryMock()

    val authRepository: AuthRepository =
        AuthRepositoryMock(sessionStore) { token -> sessionToken = token }
    val farmRepository: FarmRepository = farmRepositoryImpl
    val plotRepository: PlotRepository = PlotRepositoryMock(farmRepositoryImpl)
    val alertRepository: AlertRepository = AlertRepositoryMock()

    suspend fun bootstrapSession(): User? {
        sessionToken = sessionStore.readToken()
        return sessionStore.readUser()
    }
}
