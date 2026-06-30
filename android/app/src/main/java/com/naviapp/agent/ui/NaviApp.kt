package com.naviapp.agent.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.naviapp.agent.data.SettingsStore
import com.naviapp.agent.ui.home.HomeScreen
import com.naviapp.agent.ui.insights.AgentInsightsScreen
import com.naviapp.agent.ui.results.RouteResultsScreen
import com.naviapp.agent.ui.settings.SettingsScreen

@Composable
fun NaviApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val settingsStore = SettingsStore(context)

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                settingsStore = settingsStore,
                onRouteRecommended = { requestId ->
                    navController.navigate("results/$requestId")
                },
                onOpenSettings = {
                    navController.navigate("settings")
                }
            )
        }
        composable(
            "results/{requestId}",
            arguments = listOf(navArgument("requestId") { type = NavType.StringType })
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
            RouteResultsScreen(
                requestId = requestId,
                onBack = { navController.popBackStack() },
                onShowInsights = { navController.navigate("insights/$requestId") }
            )
        }
        composable(
            "insights/{requestId}",
            arguments = listOf(navArgument("requestId") { type = NavType.StringType })
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
            AgentInsightsScreen(
                requestId = requestId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                settingsStore = settingsStore,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
