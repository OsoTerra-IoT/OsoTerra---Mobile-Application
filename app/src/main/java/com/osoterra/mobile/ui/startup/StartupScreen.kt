package com.osoterra.mobile.ui.startup

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.osoterra.mobile.R
import com.osoterra.mobile.di.rememberAppContainer

@Composable
fun StartupScreen(
    onSignedIn: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val container = rememberAppContainer()

    LaunchedEffect(Unit) {
        val user = container.bootstrapSession()
        if (user != null) onSignedIn() else onSignedOut()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.osoterra_logo),
            contentDescription = "OsoTerra",
            modifier = Modifier.size(200.dp),
        )
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
