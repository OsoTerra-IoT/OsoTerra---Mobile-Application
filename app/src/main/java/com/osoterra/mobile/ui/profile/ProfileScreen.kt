package com.osoterra.mobile.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.CardMembership
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.osoterra.mobile.R
import com.osoterra.mobile.di.rememberAppContainer
import com.osoterra.mobile.domain.model.User
import com.osoterra.mobile.domain.model.UserRole
import com.osoterra.mobile.ui.common.ScreenHeader
import com.osoterra.mobile.ui.theme.Emerald100
import com.osoterra.mobile.ui.theme.Emerald800
import com.osoterra.mobile.ui.theme.Line
import kotlinx.coroutines.launch

private data class ProfileEntry(val labelRes: Int, val icon: ImageVector, val onClick: () -> Unit)

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
    // La persona que inició sesión, no un usuario de demostración.
    val user by produceState<User?>(initialValue = null) { value = container.authRepository.currentUser() }
    val isAdvisor = user?.role == UserRole.ADVISOR

    val field = listOf(
        ProfileEntry(R.string.profile_farms, Icons.Outlined.Agriculture, onOpenFarms),
        ProfileEntry(R.string.profile_devices, Icons.Outlined.Sensors, onOpenDevices),
        ProfileEntry(R.string.profile_crops, Icons.Outlined.Grass, onOpenCrops),
    )
    val account = listOfNotNull(
        ProfileEntry(R.string.profile_notifications, Icons.Outlined.Notifications, onOpenNotifications),
        ProfileEntry(R.string.profile_subscription, Icons.Outlined.CardMembership, onOpenSubscription),
        if (isAdvisor) ProfileEntry(R.string.profile_advisor, Icons.Outlined.Groups, onOpenAdvisor) else null,
    )

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScreenHeader(
                eyebrow = stringResource(R.string.profile_title),
                title = user?.fullName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.profile_title),
            )
            user?.let { IdentityRow(it) }
            EntryGroup(field)
            EntryGroup(account)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    scope.launch {
                        container.authRepository.logout()
                        container.sessionToken = null
                        onLogout()
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.profile_logout))
            }
        }
    }
}

@Composable
private fun IdentityRow(user: User) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(48.dp)
                .background(Emerald100, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                user.fullName.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = Emerald800,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                stringResource(
                    if (user.role == UserRole.ADVISOR) R.string.profile_role_advisor else R.string.profile_role_producer,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(user.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EntryGroup(entries: List<ProfileEntry>) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            entries.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider(color = Line, modifier = Modifier.padding(start = 56.dp))
                Surface(onClick = entry.onClick, color = MaterialTheme.colorScheme.surface) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(entry.icon, contentDescription = null, tint = Emerald800)
                        Spacer(Modifier.width(16.dp))
                        Text(stringResource(entry.labelRes), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
