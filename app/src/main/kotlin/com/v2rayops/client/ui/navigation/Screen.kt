package com.v2rayops.client.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Dashboard : Screen("dashboard")
    data object ServerSelection : Screen("servers")
    data object Settings : Screen("settings")
}
