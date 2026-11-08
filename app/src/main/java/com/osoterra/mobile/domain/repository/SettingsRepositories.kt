package com.osoterra.mobile.domain.repository

import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.domain.model.NotificationPreferences
import com.osoterra.mobile.domain.model.Subscription
import com.osoterra.mobile.domain.model.SubscriptionOverview

interface NotificationPreferencesRepository {
    suspend fun getPreferences(): DataResult<NotificationPreferences>
    suspend fun savePreferences(preferences: NotificationPreferences): DataResult<Unit>
}

interface SubscriptionRepository {
    suspend fun getOverview(): DataResult<SubscriptionOverview>

    /**
     * Contrata o cambia a [planId] (`POST /subscriptions`). Un plan gratuito queda activo y reemplaza
     * al anterior; uno de pago queda pendiente de pago (el cobro no está en la app) y el plan
     * vigente se conserva hasta que se confirme el pago.
     */
    suspend fun subscribe(planId: String): DataResult<Subscription>

    /** Cancela la suscripción (`POST /subscriptions/{id}/cancellation`). */
    suspend fun cancel(subscriptionId: String): DataResult<Subscription>
}
