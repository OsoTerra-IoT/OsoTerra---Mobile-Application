package com.osoterra.mobile.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.osoterra.mobile.R
import com.osoterra.mobile.ui.advisor.AdvisorCompareScreen
import com.osoterra.mobile.ui.advisor.AdvisorDashboardScreen
import com.osoterra.mobile.ui.alerts.AlertsScreen
import com.osoterra.mobile.ui.auth.forgot.ForgotPasswordScreen
import com.osoterra.mobile.ui.auth.login.LoginScreen
import com.osoterra.mobile.ui.auth.register.RegisterScreen
import com.osoterra.mobile.ui.crops.CropCatalogScreen
import com.osoterra.mobile.ui.dashboard.DashboardScreen
import com.osoterra.mobile.ui.devices.DevicesScreen
import com.osoterra.mobile.ui.farms.FarmsScreen
import com.osoterra.mobile.ui.plot.CreatePlotScreen
import com.osoterra.mobile.ui.plot.PlotDetailScreen
import com.osoterra.mobile.ui.profile.ProfileScreen
import com.osoterra.mobile.ui.settings.NotificationSettingsScreen
import com.osoterra.mobile.ui.startup.StartupScreen
import com.osoterra.mobile.ui.subscription.SubscriptionScreen

private data class BottomDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
)

private val bottomDestinations = listOf(
    BottomDestination(Routes.DASHBOARD, R.string.nav_home, Icons.Filled.Yard),
    BottomDestination(Routes.ALERTS, R.string.nav_alerts, Icons.Filled.Notifications),
    BottomDestination(Routes.PROFILE, R.string.nav_profile, Icons.Filled.Person),
)

@Composable
fun OsoTerraRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomDestinations.map { it.route }

    fun goToDashboardClearing(from: String) {
        navController.navigate(Routes.DASHBOARD) {
            popUpTo(from) { inclusive = true }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomDestinations.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = null) },
                            label = { Text(stringResource(dest.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.STARTUP,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.STARTUP) {
                StartupScreen(
                    onSignedIn = { goToDashboardClearing(Routes.STARTUP) },
                    onSignedOut = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.STARTUP) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = { goToDashboardClearing(Routes.LOGIN) },
                    onNavigateRegister = { navController.navigate(Routes.REGISTER) },
                    onNavigateForgot = { navController.navigate(Routes.FORGOT) },
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    onRegistered = { goToDashboardClearing(Routes.LOGIN) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.FORGOT) {
                ForgotPasswordScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onPlotClick = { plotId -> navController.navigate(Routes.plotDetail(plotId)) },
                    onCreatePlot = { navController.navigate(Routes.CREATE_PLOT) },
                )
            }
            composable(Routes.ALERTS) {
                AlertsScreen()
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onLogout = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onOpenFarms = { navController.navigate(Routes.FARMS) },
                    onOpenCrops = { navController.navigate(Routes.CROP_CATALOG) },
                    onOpenDevices = { navController.navigate(Routes.DEVICES) },
                    onOpenNotifications = { navController.navigate(Routes.NOTIFICATION_SETTINGS) },
                    onOpenSubscription = { navController.navigate(Routes.SUBSCRIPTION) },
                    onOpenAdvisor = { navController.navigate(Routes.ADVISOR_DASHBOARD) },
                )
            }
            composable(Routes.FARMS) {
                FarmsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.DEVICES) {
                DevicesScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.NOTIFICATION_SETTINGS) {
                NotificationSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SUBSCRIPTION) {
                SubscriptionScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ADVISOR_DASHBOARD) {
                AdvisorDashboardScreen(
                    onBack = { navController.popBackStack() },
                    onCompare = { ids -> navController.navigate(Routes.advisorCompare(ids)) },
                )
            }
            composable(
                route = Routes.ADVISOR_COMPARE,
                arguments = listOf(navArgument(Routes.ARG_PLOT_IDS) { type = NavType.StringType }),
            ) { entry ->
                val ids = entry.arguments?.getString(Routes.ARG_PLOT_IDS)
                    ?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                AdvisorCompareScreen(
                    plotIds = ids,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.CREATE_PLOT) {
                CreatePlotScreen(
                    onCreated = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                    onNeedFarm = { navController.navigate(Routes.FARMS) },
                )
            }
            composable(Routes.CROP_CATALOG) {
                CropCatalogScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.PLOT_DETAIL,
                arguments = listOf(navArgument(Routes.ARG_PLOT_ID) { type = NavType.StringType }),
            ) { entry ->
                val plotId = entry.arguments?.getString(Routes.ARG_PLOT_ID).orEmpty()
                PlotDetailScreen(
                    plotId = plotId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
