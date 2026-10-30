package com.osoterra.mobile.ui.profile

import com.osoterra.mobile.ui.common.StatusBarScrim
import androidx.compose.foundation.layout.WindowInsets
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

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
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.osoterra.mobile.R
import com.osoterra.mobile.core.util.LocaleStore
import com.osoterra.mobile.di.rememberAppContainer
import com.osoterra.mobile.domain.model.User
import com.osoterra.mobile.domain.model.UserRole
import androidx.compose.foundation.BorderStroke

import com.osoterra.mobile.ui.common.BandHeader
import com.osoterra.mobile.ui.common.SectionLabel
import com.osoterra.mobile.ui.common.SerenaCard
import com.osoterra.mobile.ui.common.SerenaGutter
import com.osoterra.mobile.ui.common.SoftIcon
import com.osoterra.mobile.ui.common.StaggeredEntrance
import com.osoterra.mobile.ui.theme.BandMuted
import com.osoterra.mobile.ui.theme.OliveLine
import com.osoterra.mobile.ui.theme.SageInk
import com.osoterra.mobile.ui.theme.White
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

    Scaffold(contentWindowInsets = WindowInsets(0), containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BandHeader(
                eyebrow = stringResource(R.string.profile_title),
                title = user?.fullName?.takeIf { it.isNotBlank() } ?: "",
                extra = { user?.let { IdentityRow(it) } },
            )
            Column(
                Modifier.padding(horizontal = SerenaGutter),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StaggeredEntrance(0) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(stringResource(R.string.profile_section_field))
                        EntryGroup(field)
                    }
                }
                StaggeredEntrance(1) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(stringResource(R.string.profile_section_account))
                        EntryGroup(account)
                    }
                }
                StaggeredEntrance(2) { LanguageCard() }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            container.authRepository.logout()
                            container.sessionToken = null
                            onLogout()
                        }
                    },
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, OliveLine),
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = SageInk)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.profile_logout), color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
            StatusBarScrim()
        }
    }
}

/** Iniciales, rol y correo sobre la banda. */
@Composable
private fun IdentityRow(user: User) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 14.dp)) {
        Box(
            Modifier
                .size(44.dp)
                .background(Emerald100, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                user.fullName.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = Emerald800,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                stringResource(
                    if (user.role == UserRole.ADVISOR) R.string.profile_role_advisor else R.string.profile_role_producer,
                ),
                style = MaterialTheme.typography.titleSmall,
                color = White,
            )
            Text(user.email, style = MaterialTheme.typography.bodySmall, color = BandMuted)
        }
    }
}

/**
 * Selector de idioma ES / EN, como el de la Web App. La app abre en español; al cambiar,
 * se guarda la elección y la pantalla se vuelve a crear con los textos del otro idioma.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageCard() {
    val context = LocalContext.current
    val current = remember { LocaleStore.language(context) }
    val options = listOf(
        LocaleStore.SPANISH to stringResource(R.string.language_es),
        LocaleStore.ENGLISH to stringResource(R.string.language_en),
    )
    SerenaCard(padding = 20.dp) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SoftIcon(Icons.Outlined.Language, size = 36.dp)
                Spacer(Modifier.width(14.dp))
                Text(
                    stringResource(R.string.profile_language),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
            }
            // Sin párrafo de ayuda: el selector ES / EN se explica solo.
            Spacer(Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, (code, label) ->
                    SegmentedButton(
                        selected = current == code,
                        onClick = {
                            if (current != code) {
                                LocaleStore.setLanguage(context, code)
                                context.findActivity()?.recreate()
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text(label) }
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun EntryGroup(entries: List<ProfileEntry>) {
    SerenaCard(padding = 0.dp) {
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            entries.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider(color = OliveLine, modifier = Modifier.padding(start = 72.dp, end = 20.dp))
                Surface(onClick = entry.onClick, color = White) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp)
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SoftIcon(entry.icon, size = 36.dp)
                        Spacer(Modifier.width(14.dp))
                        Text(stringResource(entry.labelRes), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = SageInk,
                        )
                    }
                }
            }
        }
    }
}
