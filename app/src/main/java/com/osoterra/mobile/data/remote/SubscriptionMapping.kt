package com.osoterra.mobile.data.remote

import com.osoterra.mobile.data.remote.dto.SubscriptionDto
import com.osoterra.mobile.data.remote.dto.SubscriptionPlanDto
import com.osoterra.mobile.domain.model.BillingCycle
import com.osoterra.mobile.domain.model.Subscription
import com.osoterra.mobile.domain.model.SubscriptionOverview
import com.osoterra.mobile.domain.model.SubscriptionPlan
import com.osoterra.mobile.domain.model.SubscriptionStatus

fun SubscriptionPlanDto.toPlan(): SubscriptionPlan = SubscriptionPlan(
    id = id.toString(),
    name = name,
    priceAmount = priceAmount,
    priceCurrency = priceCurrency,
    billingCycle = when (billingCycle) {
        "MONTHLY" -> BillingCycle.MONTHLY
        "ANNUAL" -> BillingCycle.ANNUAL
        else -> BillingCycle.NONE
    },
    maxPlots = maxPlots,
    isFree = free,
)

private fun statusOf(text: String): SubscriptionStatus =
    SubscriptionStatus.entries.firstOrNull { it.name == text } ?: SubscriptionStatus.SUSPENDED

fun SubscriptionDto.toSubscription(plans: List<SubscriptionPlanDto>): Subscription {
    val plan = plans.firstOrNull { it.id == subscriptionPlanId }
    return Subscription(
        id = id.toString(),
        planId = subscriptionPlanId.toString(),
        planName = plan?.name ?: "Plan",
        isFree = plan?.free ?: false,
        status = statusOf(status),
        renewsAt = periodEndDate,
        plotsUsed = quotaConsumed,
        plotsAllowed = quotaTotal,
    )
}

/**
 * Arma lo que muestra la pantalla. El plan vigente es la suscripción activa de mayor límite
 * (el Backend aplica el mayor límite entre las activas); la pendiente es la solicitud de pago
 * más reciente. Solo se ofrecen los planes activos, del más barato al más caro.
 */
fun buildSubscriptionOverview(
    subscriptions: List<SubscriptionDto>,
    plans: List<SubscriptionPlanDto>,
): SubscriptionOverview = SubscriptionOverview(
    current = subscriptions.filter { it.status == "ACTIVE" }
        .maxByOrNull { it.quotaTotal }
        ?.toSubscription(plans),
    pending = subscriptions.filter { it.status == "PENDING_PAYMENT" }
        .maxByOrNull { it.id }
        ?.toSubscription(plans),
    plans = plans.filter { it.active }.sortedBy { it.priceAmount }.map { it.toPlan() },
)
