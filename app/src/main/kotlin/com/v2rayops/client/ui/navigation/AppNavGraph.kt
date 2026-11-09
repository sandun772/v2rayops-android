package com.v2rayops.client.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.v2rayops.client.auth.ui.LoginScreen
import com.v2rayops.client.domain.repository.AuthDeepLinkPayload
import com.v2rayops.client.settings.ui.SettingsScreen
import com.v2rayops.client.ui.dashboard.DashboardScreen
import com.v2rayops.client.ui.logs.DevLogsScreen
import com.v2rayops.client.ui.optimizer.NetworkOptimizerScreen
import com.v2rayops.client.ui.routefinder.RouteFinderScreen
import com.v2rayops.client.ui.servers.ServerSelectionScreen
import com.v2rayops.client.ui.splash.SplashScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    deepLinkPayload: AuthDeepLinkPayload? = null,
    onDeepLinkHandled: () -> Unit = {},
    startDestination: String = Screen.Splash.route
) {
    LaunchedEffect(deepLinkPayload) {
        if (deepLinkPayload != null && deepLinkPayload.customerCode.isNotBlank()) {
            navController.navigate(Screen.Login.route)
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(
            route = Screen.Splash.route,
            exitTransition = {
                fadeOut(animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing))
            }
        ) {
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

        composable(
            route = Screen.Login.route,
            enterTransition = {
                fadeIn(animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing))
            }
        ) {
            LoginScreen(
                deepLinkPayload = deepLinkPayload,
                onLoginSuccess = {
                    onDeepLinkHandled()
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Dashboard.route,
            enterTransition = {
                fadeIn(animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing))
            }
        ) {
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
                },
                onNavigateToRouteFinder = {
                    navController.navigate(Screen.RouteFinder.route)
                }
            )
        }

        composable(Screen.ServerSelection.route) {
            ServerSelectionScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onNavigateToFinetune = {
                    navController.navigate(Screen.Optimizer.route)
                },
                onNavigateToRouting = {
                    navController.navigate(Screen.RouteFinder.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onNavigateToLogs = {
                    navController.navigate(Screen.Logs.route)
                },
                onNavigateToOptimizer = {
                    navController.navigate(Screen.Optimizer.route)
                },
                onNavigateToRouteFinder = {
                    navController.navigate(Screen.RouteFinder.route)
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
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onNavigateToServers = {
                    navController.navigate(Screen.ServerSelection.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToRouteFinder = {
                    navController.navigate(Screen.RouteFinder.route)
                }
            )
        }

        composable(Screen.RouteFinder.route) {
            RouteFinderScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onNavigateToFinetune = {
                    navController.navigate(Screen.Optimizer.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
    }
}
