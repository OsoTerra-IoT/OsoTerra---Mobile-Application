package com.osoterra.mobile

import android.app.Application
import com.osoterra.mobile.di.AppContainer

class OsoTerraApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
