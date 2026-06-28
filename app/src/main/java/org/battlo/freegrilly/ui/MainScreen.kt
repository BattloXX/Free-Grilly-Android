package org.battlo.freegrilly.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import org.battlo.freegrilly.ui.components.AdaptiveScaffold
import org.battlo.freegrilly.ui.dashboard.DashboardScreen
import org.battlo.freegrilly.ui.history.SessionHistoryScreen
import org.battlo.freegrilly.ui.library.MeatLibraryScreen
import org.battlo.freegrilly.ui.onboarding.OnboardingScreen
import org.battlo.freegrilly.ui.probedetail.ProbeDetailScreen
import org.battlo.freegrilly.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Dashboard : Screen("dashboard")
    object ProbeDetail : Screen("probe/{probeId}") {
        fun createRoute(probeId: Int) = "probe/$probeId"
    }
    object MeatLibrary : Screen("library?probeId={probeId}") {
        fun createRoute(probeId: Int? = null) =
            if (probeId != null) "library?probeId=$probeId" else "library"
    }
    object History : Screen("history")
    object Settings : Screen("settings")
}

val mainNavRoutes = listOf(
    Screen.Dashboard.route,
    Screen.MeatLibrary.route.substringBefore("?"),
    Screen.History.route,
    Screen.Settings.route,
)

@Composable
fun MainScreen(
    windowSizeClass: WindowSizeClass,
    startDestination: String,
) {
    val navController = rememberNavController()
    val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val isOnboarding = currentRoute?.startsWith("onboarding") == true

    val navigate: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(Screen.Dashboard.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navHost: @Composable () -> Unit = {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(onSetupComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onProbeClick = { navController.navigate(Screen.ProbeDetail.createRoute(it)) },
                    onNavigateToOnboarding = {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    },
                )
            }
            composable(
                Screen.ProbeDetail.route,
                arguments = listOf(navArgument("probeId") { type = NavType.IntType }),
            ) { back ->
                val probeId = back.arguments?.getInt("probeId") ?: return@composable
                ProbeDetailScreen(
                    probeId = probeId,
                    onBack = { navController.popBackStack() },
                    onOpenLibrary = { navController.navigate(Screen.MeatLibrary.createRoute(it)) },
                )
            }
            composable(
                Screen.MeatLibrary.route,
                arguments = listOf(navArgument("probeId") { type = NavType.IntType; defaultValue = -1 }),
            ) { back ->
                val probeId = back.arguments?.getInt("probeId")?.takeIf { it != -1 }
                MeatLibraryScreen(
                    targetProbeId = probeId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Screen.History.route) {
                SessionHistoryScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToOnboarding = { navController.navigate(Screen.Onboarding.route) },
                )
            }
        }
    }

    if (isOnboarding) {
        navHost()
    } else {
        AdaptiveScaffold(
            isCompact = isCompact,
            currentRoute = currentRoute ?: "",
            onNavigate = navigate,
            content = navHost,
        )
    }
}
