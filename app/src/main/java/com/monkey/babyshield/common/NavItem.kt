package com.monkey.babyshield.common

import androidx.compose.ui.graphics.vector.ImageVector
import com.monkey.babyshield.presentations.navigation.Screen

data class NavItem(
    val screen: Screen,
    val icon: ImageVector,
    val contentDescription: String
)