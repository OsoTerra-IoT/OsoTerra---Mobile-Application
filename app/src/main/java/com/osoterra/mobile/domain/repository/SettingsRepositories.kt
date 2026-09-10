package com.osoterra.mobile.domain.repository

import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.domain.model.NotificationPreferences
import com.osoterra.mobile.domain.model.Subscription

interface NotificationPreferencesRepository {
    suspend fun getPreferences(): NotificationPreferences
    suspend fun savePreferences(preferences: NotificationPreferences)
}

interface SubscriptionRepository {
    suspend fun getSubscription(): DataResult<Subscription>
}
