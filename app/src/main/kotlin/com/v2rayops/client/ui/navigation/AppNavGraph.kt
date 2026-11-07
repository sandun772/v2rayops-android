package com.v2rayops.client.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.v2rayops.client.auth.ui.LoginScreen
import com.v2rayops.client.settings.ui.SettingsScreen
import com.v2rayops.client.ui.dashboard.DashboardScreen
import com.v2rayops.client.ui.logs.DevLogsScreen
import com.v2rayops.client.ui.optimizer.NetworkOptimizerScreen
import com.v2rayops.client.ui.servers.ServerSelectionScreen
import com.v2rayops.client.ui.splash.SplashScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    deepLinkCode: String? = null,
    onDeepLinkHandled: () -> Unit = {},
    startDestination: String = Screen.Splash.route
) {
    LaunchedEffect(deepLinkCode) {
        if (!deepLinkCode.isNullOrBlank()) {
            navController.navigate(Screen.Login.route)
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                deepLinkCode = deepLinkCode,
                onLoginSuccess = {
                    onDeepLinkHandled()
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToServers = {
                    navController.navigate(Screen.ServerSelection.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToLogs = {
                    navController.navigate(Screen.Logs.route)
                },
                onNavigateToOptimizer = {
                    navController.navigate(Screen.Optimizer.route)
                }
            )
        }

        composable(Screen.ServerSelection.route) {
            ServerSelectionScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onNavigateToLogs = {
                    navController.navigate(Screen.Logs.route)
                },
                onNavigateToOptimizer = {
                    navController.navigate(Screen.Optimizer.route)
                },
                onLogoutSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Logs.route) {
            DevLogsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Optimizer.route) {
            NetworkOptimizerScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
