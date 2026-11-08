package com.osoterra.mobile.data.repository

import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.domain.model.BillingCycle
import com.osoterra.mobile.domain.model.Subscription
import com.osoterra.mobile.domain.model.SubscriptionOverview
import com.osoterra.mobile.domain.model.SubscriptionPlan
import com.osoterra.mobile.domain.model.SubscriptionStatus
import com.osoterra.mobile.domain.repository.SubscriptionRepository

/** Catálogo de planes que siembra el Backend (`SubscriptionPlanSeeder`); precios provisionales en soles. */
object SubscriptionPlans {
    val free = SubscriptionPlan("1", "Free", 0.0, "PEN", BillingCycle.NONE, 2, true)
    val basic = SubscriptionPlan("2", "Basic", 29.90, "PEN", BillingCycle.MONTHLY, 10, false)
    val pro = SubscriptionPlan("3", "Pro", 299.90, "PEN", BillingCycle.ANNUAL, 50, false)
    val catalog = listOf(free, basic, pro)

    /** El plan más pequeño que cabe [plots] parcelas; el más grande si ninguno alcanza. */
    fun smallestFitting(plots: Int): SubscriptionPlan = catalog.firstOrNull { it.maxPlots >= plots } ?: catalog.last()
}

/**
 * Suscripciones en memoria con las mismas reglas del Backend, para los modos mock y de prueba:
 * un plan gratuito se activa al instante y reemplaza al anterior; uno de pago queda pendiente de
 * pago (no hay cobro) y el plan vigente se conserva; el límite de parcelas es el mayor de los
 * planes activos y, sin plan activo, no hay límite.
 */
class InMemorySubscriptions(
    private val plans: List<SubscriptionPlan> = SubscriptionPlans.catalog,
    initialPlanId: String?,
    private val plotsUsed: () -> Int,
) : SubscriptionRepository {

    private data class Stored(val id: Long, val plan: SubscriptionPlan, var status: SubscriptionStatus)

    private val stored = mutableListOf<Stored>()
    private var nextId = 1L

    init {
        plans.firstOrNull { it.id == initialPlanId }?.let { stored += Stored(nextId++, it, SubscriptionStatus.ACTIVE) }
    }

    private fun Stored.toSubscription() = Subscription(
        id = id.toString(),
        planId = plan.id,
        planName = plan.name,
        isFree = plan.isFree,
        status = status,
        renewsAt = null,
        plotsUsed = plotsUsed(),
        plotsAllowed = plan.maxPlots,
    )

    /** Límite de parcelas activas que aplica hoy; null si no hay un plan activo. */
    @Synchronized
    fun plotLimit(): Int? = stored.filter { it.status == SubscriptionStatus.ACTIVE }.maxOfOrNull { it.plan.maxPlots }

    override suspend fun getOverview(): DataResult<SubscriptionOverview> = synchronized(this) {
        DataResult.Success(
        SubscriptionOverview(
            current = stored.filter { it.status == SubscriptionStatus.ACTIVE }.maxByOrNull { it.plan.maxPlots }
                ?.toSubscription(),
            pending = stored.lastOrNull { it.status == SubscriptionStatus.PENDING_PAYMENT }?.toSubscription(),
            plans = plans.sortedBy { it.priceAmount },
        ),
        )
    }

    override suspend fun subscribe(planId: String): DataResult<Subscription> = synchronized(this) {
        val plan = plans.firstOrNull { it.id == planId } ?: return@synchronized DataResult.Error("No se encontró el plan", httpCode = 404)
        val created = Stored(nextId++, plan, if (plan.isFree) SubscriptionStatus.ACTIVE else SubscriptionStatus.PENDING_PAYMENT)
        if (plan.isFree) {
            stored.filter { it.status == SubscriptionStatus.ACTIVE }.forEach { it.status = SubscriptionStatus.CANCELLED }
        }
        stored += created
        DataResult.Success(created.toSubscription())
    }

    override suspend fun cancel(subscriptionId: String): DataResult<Subscription> = synchronized(this) {
        val target = stored.firstOrNull { it.id.toString() == subscriptionId }
            ?: return@synchronized DataResult.Error("No se encontró la suscripción", httpCode = 404)
        if (target.status == SubscriptionStatus.CANCELLED) {
            return@synchronized DataResult.Error("La suscripción ya está cancelada", httpCode = 409)
        }
        target.status = SubscriptionStatus.CANCELLED
        DataResult.Success(target.toSubscription())
    }
}
