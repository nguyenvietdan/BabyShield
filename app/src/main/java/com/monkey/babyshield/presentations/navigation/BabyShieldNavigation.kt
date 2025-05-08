package com.monkey.babyshield.presentations.navigation

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.monkey.babyshield.presentations.components.AppDrawer
import com.monkey.babyshield.utils.navigateWithPopupTo

@Composable
fun BabyShieldNavigation() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var currentScreen by rememberSaveable { mutableStateOf(Screen.HomeScreen.route) }

    AppDrawer(
        drawerState = drawerState,
        currentScreen = currentScreen,
        onScreenSelected = { screen ->
            navController.navigateWithPopupTo(screen.route)
            currentScreen = screen.route
        }
    ) {
        BabyShieldGraph(navController, drawerState) { route -> currentScreen = route }
    }
}