package com.osoterra.mobile.di

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.osoterra.mobile.OsoTerraApp

@Composable
fun rememberAppContainer(): AppContainer {
    val context = LocalContext.current
    return (context.applicationContext as OsoTerraApp).container
}
