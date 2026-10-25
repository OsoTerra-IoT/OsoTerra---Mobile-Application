package com.osoterra.mobile.domain.model

data class NotificationPreferences(
    val minSeverity: AlertSeverity = AlertSeverity.WATCH,
    val pushEnabled: Boolean = true,
    val emailEnabled: Boolean = false,
)

data class Subscription(
    val planName: String,
    val isFree: Boolean,
    val renewsAt: String?,
    val plotsUsed: Int,
    val plotsAllowed: Int,
)
