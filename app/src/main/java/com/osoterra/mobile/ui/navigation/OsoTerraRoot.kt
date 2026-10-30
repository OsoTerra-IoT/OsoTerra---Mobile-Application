package com.osoterra.mobile.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.ui.common.DemoChipModel
import com.osoterra.mobile.ui.common.LocalDemoChip
import com.osoterra.mobile.ui.theme.Emerald100
import com.osoterra.mobile.ui.theme.Emerald800
import com.osoterra.mobile.ui.theme.OliveLine
import com.osoterra.mobile.ui.theme.SageInk
import com.osoterra.mobile.ui.theme.SalinityCritical
import com.osoterra.mobile.ui.theme.TextStrong
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.osoterra.mobile.di.rememberAppContainer
import com.osoterra.mobile.ui.advisor.AdvisorCompareScreen
import com.osoterra.mobile.ui.advisor.AdvisorDashboardScreen
import com.osoterra.mobile.ui.alerts.AlertsScreen
import com.osoterra.mobile.ui.auth.forgot.ForgotPasswordScreen
import com.osoterra.mobile.ui.auth.login.LoginScreen
import com.osoterra.mobile.ui.auth.register.RegisterScreen
import com.osoterra.mobile.ui.crops.CropCatalogScreen
import com.osoterra.mobile.ui.dashboard.DashboardScreen
import com.osoterra.mobile.ui.demo.DemoScenarioSheet
import com.osoterra.mobile.ui.devices.DevicesScreen
import com.osoterra.mobile.ui.farms.FarmsScreen
import com.osoterra.mobile.ui.plot.CreatePlotScreen
import com.osoterra.mobile.ui.plot.PlotDetailScreen
import com.osoterra.mobile.ui.profile.ProfileScreen
import com.osoterra.mobile.ui.settings.NotificationSettingsScreen
import com.osoterra.mobile.ui.startup.StartupScreen
import com.osoterra.mobile.ui.subscription.SubscriptionScreen
import kotlinx.coroutines.launch

private data class BottomDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
)

private val bottomDestinations = listOf(
    BottomDestination(Routes.DASHBOARD, R.string.nav_home, Icons.Outlined.Home),
    BottomDestination(Routes.ALERTS, R.string.nav_alerts, Icons.Outlined.Notifications),
    BottomDestination(Routes.PROFILE, R.string.nav_profile, Icons.Outlined.Person),
)

private val signedOutRoutes = setOf(Routes.STARTUP, Routes.LOGIN, Routes.REGISTER, Routes.FORGOT)

@Composable
fun OsoTerraRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomDestinations.map { it.route }

    // Si el Backend rechaza el token (sesión vencida), se vuelve al login y se avisa.
    val container = rememberAppContainer()
    val sessionExpired by container.sessionExpired.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val expiredMessage = stringResource(R.string.session_expired)
    LaunchedEffect(sessionExpired) {
        if (!sessionExpired) return@LaunchedEffect
        container.acknowledgeExpiredSession()
        if (currentRoute != Routes.LOGIN) {
            navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
        }
        snackbarHostState.showSnackbar(expiredMessage)
    }

    fun goToDashboardClearing(from: String) {
        navController.navigate(Routes.DASHBOARD) {
            popUpTo(from) { inclusive = true }
        }
    }

    // El login dibuja su propio panel detrás de la barra de estado.
    val fullBleed = currentRoute == Routes.LOGIN

    // Modo de prueba: chip «Datos de prueba» arriba en las pantallas con sesión.
    val demoMode by container.demoMode.collectAsState()
    val demoScenario by container.demoScenario.collectAsState()
    val showDemoBar = demoMode && currentRoute != null && currentRoute !in signedOutRoutes
    var demoSheetOpen by remember { mutableStateOf(false) }
    var demoBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    /**
     * Entra al inicio con el escenario recién cargado. Antes se descartan las pestañas
     * guardadas (y sus ViewModel), para que ninguna muestre datos del escenario anterior.
     */
    fun openDemoHome() {
        bottomDestinations.forEach { navController.clearBackStack(it.route) }
        navController.navigate(Routes.DASHBOARD) { popUpTo(0) { inclusive = true } }
    }

    if (demoSheetOpen && showDemoBar) {
        DemoScenarioSheet(
            selected = demoScenario,
            busy = demoBusy,
            onSelect = { scenario ->
                demoBusy = true
                scope.launch {
                    container.startDemoScenario(scenario)
                    demoBusy = false
                    demoSheetOpen = false
                    openDemoHome()
                }
            },
            onExit = {
                scope.launch {
                    container.disableDemoMode()
                    demoSheetOpen = false
                    bottomDestinations.forEach { navController.clearBackStack(it.route) }
                    navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                }
            },
            onDismiss = { demoSheetOpen = false },
        )
    }

    // Alertas sin atender para la insignia de la pestaña; se vuelve a contar al cambiar de pantalla.
    var pendingAlerts by remember { mutableIntStateOf(0) }
    LaunchedEffect(currentRoute, demoScenario, showBottomBar) {
        if (!showBottomBar) return@LaunchedEffect
        pendingAlerts = (container.alertRepository.getAlerts() as? DataResult.Success)?.data?.count { !it.acknowledged } ?: 0
    }
    val demoChip = if (showDemoBar) DemoChipModel(demoScenario?.number) { demoSheetOpen = true } else null

    CompositionLocalProvider(LocalDemoChip provides demoChip) {
    Scaffold(
        // Arriba no se reserva la barra de estado: la banda verde y las cabeceras la cubren.
        contentWindowInsets = when {
            fullBleed -> WindowInsets(0)
            currentRoute == Routes.STARTUP || currentRoute == null -> ScaffoldDefaults.contentWindowInsets
            else -> ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) androidx.compose.foundation.layout.Column {
                HorizontalDivider(color = OliveLine)
                NavigationBar(containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
                    bottomDestinations.forEach { dest ->
                        val label = stringResource(dest.labelRes)
                        val badge = if (dest.route == Routes.ALERTS) pendingAlerts else 0
                        val badgeDescription = if (badge > 0) {
                            pluralStringResource(R.plurals.alerts_summary, badge, badge)
                        } else {
                            null
                        }
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
                            icon = {
                                BadgedBox(badge = {
                                    if (badge > 0) Badge(containerColor = SalinityCritical) { Text("$badge") }
                                }) { Icon(dest.icon, contentDescription = null) }
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Emerald800,
                                selectedTextColor = TextStrong,
                                indicatorColor = Emerald100,
                                unselectedIconColor = SageInk,
                                unselectedTextColor = SageInk,
                            ),
                            modifier = if (badgeDescription != null) {
                                Modifier.semantics { contentDescription = "$label. $badgeDescription" }
                            } else {
                                Modifier
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.STARTUP,
            // Consumir los insets evita que los Scaffold de cada pantalla los apliquen otra vez.
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
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
                    onDemoStarted = ::openDemoHome,
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
                    onOpenAlerts = {
                        navController.navigate(Routes.ALERTS) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(Routes.ALERTS) {
                AlertsScreen(onPendingCount = { pendingAlerts = it })
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
}
