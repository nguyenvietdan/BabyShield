package com.monkey.babyshield.utils

import androidx.navigation.NavHostController
import com.monkey.babyshield.R
import com.monkey.babyshield.presentations.navigation.Screen

fun NavHostController.navigateWithPopupTo(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

fun getStringByRoute(route: String) = when (route) {
    Screen.HomeScreen.route -> R.string.home_screen_title
    Screen.SettingsScreen.route -> R.string.settings_screen_title
    else -> R.string.app_name
}