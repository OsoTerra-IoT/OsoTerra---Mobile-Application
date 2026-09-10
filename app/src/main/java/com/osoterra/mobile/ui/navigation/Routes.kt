package com.osoterra.mobile.ui.navigation

object Routes {
    const val STARTUP = "startup"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT = "forgot"

    const val DASHBOARD = "dashboard"
    const val ALERTS = "alerts"
    const val PROFILE = "profile"

    const val FARMS = "farms"
    const val CREATE_PLOT = "create_plot"
    const val CROP_CATALOG = "crop_catalog"
    const val DEVICES = "devices"
    const val NOTIFICATION_SETTINGS = "notification_settings"
    const val SUBSCRIPTION = "subscription"

    const val ADVISOR_DASHBOARD = "advisor"
    const val ADVISOR_COMPARE = "advisor/compare/{plotIds}"
    fun advisorCompare(plotIds: List<String>) = "advisor/compare/${plotIds.joinToString(",")}"
    const val ARG_PLOT_IDS = "plotIds"

    const val PLOT_DETAIL = "plot/{plotId}"
    fun plotDetail(plotId: String) = "plot/$plotId"
    const val ARG_PLOT_ID = "plotId"
}
