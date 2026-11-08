package com.osoterra.mobile.domain.model

data class NotificationPreferences(
    val minSeverity: AlertSeverity = AlertSeverity.WATCH,
    val pushEnabled: Boolean = true,
    val emailEnabled: Boolean = false,
)

enum class BillingCycle { NONE, MONTHLY, ANNUAL }

enum class SubscriptionStatus { ACTIVE, PENDING_PAYMENT, SUSPENDED, CANCELLED }

/** Plan del catálogo (`GET /subscription-plans`). */
data class SubscriptionPlan(
    val id: String,
    val name: String,
    val priceAmount: Double,
    val priceCurrency: String,
    val billingCycle: BillingCycle,
    val maxPlots: Int,
    val isFree: Boolean,
)

/** Suscripción de la persona; [plotsUsed] son las parcelas activas que cuentan contra [plotsAllowed]. */
data class Subscription(
    val id: String,
    val planId: String,
    val planName: String,
    val isFree: Boolean,
    val status: SubscriptionStatus,
    val renewsAt: String?,
    val plotsUsed: Int,
    val plotsAllowed: Int,
)

/**
 * Lo que muestra la pantalla: el plan vigente (si hay), la solicitud pendiente de pago (si hay)
 * y los planes que se pueden elegir.
 */
data class SubscriptionOverview(
    val current: Subscription?,
    val pending: Subscription?,
    val plans: List<SubscriptionPlan>,
)
