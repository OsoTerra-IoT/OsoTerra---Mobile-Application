package com.osoterra.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.osoterra.mobile.ui.navigation.OsoTerraRoot
import com.osoterra.mobile.ui.theme.OsoTerraTheme

class MainActivity : ComponentActivity() {
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
