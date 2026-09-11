package com.osoterra.mobile.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osoterra.mobile.R
import com.osoterra.mobile.data.repository.MockData
import com.osoterra.mobile.di.rememberAppContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onOpenFarms: () -> Unit,
    onOpenCrops: () -> Unit,
    onOpenDevices: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSubscription: () -> Unit,
    onOpenAdvisor: () -> Unit,
) {
    val container = rememberAppContainer()
    val scope = rememberCoroutineScope()

    val user = MockData.demoUser

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(Modifier.height(16.dp))
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(96.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = user.fullName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.profile_role_producer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(28.dp))

            ListItem(
                headlineContent = { Text(stringResource(R.string.profile_farms)) },
                leadingContent = { Icon(Icons.Filled.Agriculture, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenFarms),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.profile_crops)) },
                leadingContent = { Icon(Icons.Filled.Grass, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenCrops),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.profile_devices)) },
                leadingContent = { Icon(Icons.Filled.Sensors, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenDevices),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.profile_subscription)) },
                leadingContent = { Icon(Icons.Filled.CardMembership, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenSubscription),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.profile_notifications)) },
                leadingContent = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenNotifications),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.profile_advisor)) },
                leadingContent = { Icon(Icons.Filled.Groups, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenAdvisor),
            )

            Spacer(Modifier.height(32.dp))

            OutlinedButton(
                onClick = {
                    scope.launch {
                        container.authRepository.logout()
                        container.sessionToken = null
                        onLogout()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.profile_logout))
            }
        }
    }
}
