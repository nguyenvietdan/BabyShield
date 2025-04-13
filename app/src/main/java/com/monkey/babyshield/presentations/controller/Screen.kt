package com.monkey.babyshield.presentations.controller

sealed class Screen(val route: String) {
    data object HomeScreen: Screen("home_screen")
}