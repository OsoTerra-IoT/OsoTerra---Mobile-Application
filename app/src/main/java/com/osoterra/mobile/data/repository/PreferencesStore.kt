package com.osoterra.mobile.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.osoterra.mobile.domain.model.AlertSeverity
import com.osoterra.mobile.domain.model.NotificationPreferences
import com.osoterra.mobile.domain.repository.NotificationPreferencesRepository
import kotlinx.coroutines.flow.first

private val Context.prefsDataStore by preferencesDataStore(name = "osoterra_prefs")

class NotificationPreferencesStore(
    private val context: Context,
) : NotificationPreferencesRepository {

    private object Keys {
        val MIN_SEVERITY = stringPreferencesKey("min_severity")
        val PUSH = booleanPreferencesKey("push_enabled")
        val EMAIL = booleanPreferencesKey("email_enabled")
    }

    override suspend fun getPreferences(): NotificationPreferences {
        val prefs = context.prefsDataStore.data.first()
        val severity = runCatching { AlertSeverity.valueOf(prefs[Keys.MIN_SEVERITY] ?: "") }
            .getOrDefault(AlertSeverity.MEDIUM)
        return NotificationPreferences(
            minSeverity = severity,
            pushEnabled = prefs[Keys.PUSH] ?: true,
            emailEnabled = prefs[Keys.EMAIL] ?: false,
        )
    }

    override suspend fun savePreferences(preferences: NotificationPreferences) {
        context.prefsDataStore.edit { prefs ->
            prefs[Keys.MIN_SEVERITY] = preferences.minSeverity.name
            prefs[Keys.PUSH] = preferences.pushEnabled
            prefs[Keys.EMAIL] = preferences.emailEnabled
        }
    }
}
