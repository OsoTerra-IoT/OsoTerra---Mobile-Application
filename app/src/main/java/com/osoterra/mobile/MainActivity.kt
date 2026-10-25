package com.osoterra.mobile

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.osoterra.mobile.core.util.LocaleStore
import com.osoterra.mobile.ui.navigation.OsoTerraRoot
import com.osoterra.mobile.ui.theme.OsoTerraTheme

class MainActivity : ComponentActivity() {
    /** Español por defecto; inglés si la persona lo eligió en el Perfil. */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleStore.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OsoTerraTheme {
                OsoTerraRoot()
            }
        }
    }
}
