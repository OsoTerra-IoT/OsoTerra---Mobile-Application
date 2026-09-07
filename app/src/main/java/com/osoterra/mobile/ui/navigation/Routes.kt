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

    const val PLOT_DETAIL = "plot/{plotId}"
    fun plotDetail(plotId: String) = "plot/$plotId"
    const val ARG_PLOT_ID = "plotId"
}
