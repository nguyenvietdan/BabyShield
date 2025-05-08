package com.monkey.babyshield.presentations.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.monkey.babyshield.R
import com.monkey.babyshield.common.NavItem
import com.monkey.babyshield.presentations.navigation.Screen
import com.monkey.babyshield.utils.getStringByRoute
import kotlinx.coroutines.launch

@Composable
fun AppDrawer(
    drawerState: DrawerState,
    currentScreen: String,
    onScreenSelected: (Screen) -> Unit,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val navigationItems = listOf(
        NavItem(
            screen = Screen.HomeScreen,
            icon = Icons.Default.Home,
            contentDescription = "Home"
        ),
        NavItem(
            screen = Screen.SettingsScreen,
            icon = Icons.Default.Settings,
            contentDescription = "Settings"
        )
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier.height(dimensionResource(R.dimen.nav_header_height)),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    //Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider()// todo checking height
                    Spacer(modifier = Modifier.height(12.dp))
                    navigationItems.forEach { item ->
                        val selected = item.screen.route == currentScreen
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.contentDescription
                                )
                            },
                            label = { Text(text = stringResource(getStringByRoute(item.screen.route))) },
                            selected = selected,
                            onClick = {
                                onScreenSelected(item.screen)
                                coroutineScope.launch {
                                    drawerState.close()
                                }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        },
        content = content
    )
}