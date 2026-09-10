package com.osoterra.mobile.data.repository

import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.domain.model.Subscription
import com.osoterra.mobile.domain.repository.SubscriptionRepository
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class SubscriptionRepositoryMock : SubscriptionRepository {
    override suspend fun getSubscription(): DataResult<Subscription> {
        delay(300.milliseconds)
        return DataResult.Success(
            Subscription(
                planName = "Plan Gratuito",
                isFree = true,
                renewsAt = null,
                plotsUsed = 4,
                plotsAllowed = 5,
            )
        )
    }
}
