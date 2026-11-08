package com.osoterra.mobile.data.repository

import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.domain.model.Subscription
import com.osoterra.mobile.domain.model.SubscriptionOverview
import com.osoterra.mobile.domain.repository.SubscriptionRepository
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/** Repositorio del modo `-Posoterra.fakeApi=true`: plan Basic de partida y las parcelas del mock, con latencia simulada. */
class SubscriptionRepositoryMock(
    private val subscriptions: InMemorySubscriptions,
) : SubscriptionRepository {

    override suspend fun getOverview(): DataResult<SubscriptionOverview> {
        delay(300.milliseconds)
        return subscriptions.getOverview()
    }

    override suspend fun subscribe(planId: String): DataResult<Subscription> {
        delay(400.milliseconds)
        return subscriptions.subscribe(planId)
    }

    override suspend fun cancel(subscriptionId: String): DataResult<Subscription> {
        delay(400.milliseconds)
        return subscriptions.cancel(subscriptionId)
    }
}
