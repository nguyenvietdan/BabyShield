package com.monkey.babyshield.presentations.controller

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.monkey.babyshield.presentations.screens.HomeScreen

@Composable
fun BabyShieldGraph() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Screen.HomeScreen.route) {
        composable(Screen.HomeScreen.route) {
            HomeScreen()
        }
    }
}