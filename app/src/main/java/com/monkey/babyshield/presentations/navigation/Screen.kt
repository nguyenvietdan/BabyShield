package com.monkey.babyshield.presentations.navigation

sealed class Screen(val route: String, val title: String? = null) {
    data object HomeScreen: Screen("home_screen", "Home")
    data object SettingsScreen : Screen("settings_screen", "Settings")
}