package com.monkey.babyshield.presentations.navigation

import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.monkey.babyshield.presentations.screens.HomeScreen
import com.monkey.babyshield.presentations.screens.SettingsScreen

@Composable
fun BabyShieldGraph(
    navHostController: NavHostController,
    drawerState: DrawerState,
    onScreenChanged: (String) -> Unit
) {
    NavHost(navController = navHostController, startDestination = Screen.HomeScreen.route) {
        composable(Screen.HomeScreen.route) {
            onScreenChanged(Screen.HomeScreen.route)
            HomeScreen(drawerState = drawerState)
        }
        composable(Screen.SettingsScreen.route) {
            onScreenChanged(Screen.SettingsScreen.route)
            SettingsScreen(navController = navHostController, drawerState = drawerState)
        }
    }
}