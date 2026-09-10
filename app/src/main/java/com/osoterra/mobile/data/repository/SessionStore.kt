package com.osoterra.mobile.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.osoterra.mobile.domain.model.User
import com.osoterra.mobile.domain.model.UserRole
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "osoterra_session")

class SessionStore(private val context: Context) {

    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_ROLE = stringPreferencesKey("user_role")
    }

    suspend fun save(token: String, user: User) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TOKEN] = token
            prefs[Keys.USER_ID] = user.id
            prefs[Keys.USER_NAME] = user.fullName
            prefs[Keys.USER_EMAIL] = user.email
            prefs[Keys.USER_ROLE] = user.role.name
        }
    }

    suspend fun readToken(): String? = context.dataStore.data.first()[Keys.TOKEN]

    suspend fun readUser(): User? {
        val prefs = context.dataStore.data.first()
        val id = prefs[Keys.USER_ID] ?: return null
        val role = runCatching { UserRole.valueOf(prefs[Keys.USER_ROLE] ?: "") }
            .getOrDefault(UserRole.PRODUCER)
        return User(
            id = id,
            fullName = prefs[Keys.USER_NAME].orEmpty(),
            email = prefs[Keys.USER_EMAIL].orEmpty(),
            role = role,
        )
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
