package com.monkey.babyshield.presentations.screens

import android.graphics.Point
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.monkey.babyshield.common.LockIconSize
import com.monkey.babyshield.common.SettingSwitchItemData
import com.monkey.babyshield.common.SettingValueColorItemData
import com.monkey.babyshield.common.SettingValueItemData
import com.monkey.babyshield.common.SettingsItemData
import com.monkey.babyshield.presentations.components.DefaultWheelMinutesTimePicker
import com.monkey.babyshield.presentations.theme.InactiveGreen
import com.monkey.babyshield.presentations.viewmodel.SettingsViewModel
import com.monkey.babyshield.presentations.viewmodel.WheelPickerType
import com.monkey.babyshield.utils.getLocalizedString
import kotlinx.coroutines.launch
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    drawerState: DrawerState,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            drawerState.apply {
                                if (isOpen) close() else open()
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu"
                        )
                    }
                }
            )
        }
    ) { padding ->
        SettingsScreenContentExtend(padding, settingsViewModel)
    }
}

@Composable
fun SettingsScreenContentExtend(
    paddingValues: PaddingValues,
    settingsViewModel: SettingsViewModel
) {
    val isLocked by settingsViewModel.sharedPrefs.isLocked.collectAsState()
    val edgeMargin by settingsViewModel.sharedPrefs.edgeMargin.collectAsState()
    val alphaValue by settingsViewModel.sharedPrefs.alpha.collectAsState()
    val iconSize by settingsViewModel.sharedPrefs.iconSize.collectAsState()
    val iconColor by settingsViewModel.sharedPrefs.iconColor.collectAsState()

    val context = LocalContext.current

    val metrics = context.resources.displayMetrics
    val screenWidth = metrics.widthPixels
    val screenHeight = metrics.heightPixels
    val minSize = min(screenWidth, screenHeight)


    val editingType by settingsViewModel.editingType.collectAsState()
    val showSheet = editingType != null

    if (showSheet) {
        val selectedValue = when (editingType) {
            WheelPickerType.EDGE_MARGIN -> edgeMargin
            WheelPickerType.ALPHA -> alphaValue
            WheelPickerType.ICON_SIZE -> iconSize
            else -> 0
        }
        val maxValue = when (editingType) {
            WheelPickerType.EDGE_MARGIN -> minSize / 4
            WheelPickerType.ALPHA -> 100
            WheelPickerType.ICON_SIZE -> 100
            else -> 0
        }

        val minValue = when (editingType) {
            WheelPickerType.EDGE_MARGIN -> 24
            WheelPickerType.ALPHA -> 20
            WheelPickerType.ICON_SIZE -> 24
            else -> 0
        }

        when (editingType) {
            WheelPickerType.EDGE_MARGIN, WheelPickerType.ALPHA -> {
                WheelPickerDialog(
                    selected = selectedValue,
                    minValue = minValue,
                    maxValue = maxValue,
                    onDismiss = { settingsViewModel.closeSheet() },
                    onConfirm = { value ->
                        when (editingType) {
                            WheelPickerType.EDGE_MARGIN -> settingsViewModel.updateEdgeMargin(value)
                            WheelPickerType.ALPHA -> settingsViewModel.updateAlpha(value)
                            else -> {}
                        }
                        settingsViewModel.closeSheet()
                    }
                )
            }

            WheelPickerType.ICON_SIZE -> {
                SizeSettingsDialog(
                    selected = iconSize,
                    screenWidth = minSize,
                    onDismiss = { settingsViewModel.closeSheet() },
                    onConfirm = {
                        settingsViewModel.updateIconSize(it)
                        settingsViewModel.closeSheet()
                    }
                )
            }
            WheelPickerType.ICON_COLOR -> {
                ColorSettings(
                    selected = iconColor,
                    onDismiss = { settingsViewModel.closeSheet() },
                    onConfirm = {
                        settingsViewModel.updateIconColor(it)
                        settingsViewModel.closeSheet()
                    }
                )
            }

            else -> {
                Log.i("dan.nv", "SettingsScreenContentExtend: there is no option")
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .padding(paddingValues)
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            SettingGroup(
                title = "Baby shield Settings",
                items = listOf(
                    SettingSwitchItemData(
                        title = "Locked",
                        subTitle = "Auto locked when enabled",
                        checked = isLocked,
                        onToggle = { settingsViewModel.updateLocked(it) }
                    ),
                    SettingValueItemData("Edge margin", "$edgeMargin") {
                        settingsViewModel.openSheet(WheelPickerType.EDGE_MARGIN)
                    }
                )
            )
        }

        item {
            SettingGroup(
                title = "Unlock button Settings",
                items = listOf(
                    SettingValueItemData("Alpha", "$alphaValue") {
                        settingsViewModel.openSheet(WheelPickerType.ALPHA)
                    },
                    SettingValueItemData("IconSize", LockIconSize.entries[iconSize].getLocalizedString()) {
                        settingsViewModel.openSheet(WheelPickerType.ICON_SIZE)
                    },
                    SettingValueColorItemData("IconColor", iconColor) {
                        settingsViewModel.openSheet(WheelPickerType.ICON_COLOR)
                    }
                )
            )
        }

        // Add more SettingGroups here for AppSettings, UiSettings...
    }
}


@Composable
fun SettingGroup(title: String, items: List<SettingsItemData>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp))
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        items.forEach { item ->
            when (item) {
                is SettingSwitchItemData -> SwitchRow(
                    title = item.title,
                    subTitle = item.subTitle,
                    checked = item.checked,
                    enabled = item.enabled,
                    onToggle = item.onToggle
                )

                is SettingValueItemData -> ValueRow(item.title, item.value, item.onClick)
                /*is SettingsActionItem -> ActionRow(item.title, item.onClick)*/
                is SettingValueColorItemData -> ColorRow(item.title, item.value, item.onClick)
            }
        }
    }
}

@Composable
fun SwitchRow(
    title: String,
    subTitle: String = "",
    checked: Boolean,
    enabled: Boolean = true,
    onToggle: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(subTitle, style = MaterialTheme.typography.bodyMedium) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onToggle, enabled = enabled)
        },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun ValueRow(title: String, value: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(value, style = MaterialTheme.typography.bodyMedium) },
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        },
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 0.dp)
            .clickable { onClick() },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun ColorRow(title: String, value: Int, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
        supportingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color = Color(value), shape = RoundedCornerShape(10.dp))
                    .padding(horizontal = 2.dp)
            )
        },
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        },
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 0.dp)
            .clickable { onClick() },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun ActionRow(title: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        },
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 0.dp)
            .clickable { onClick() },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun WheelPickerDialog(
    selected: Int,
    minValue: Int,
    maxValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(currentIndex) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            DefaultWheelMinutesTimePicker(
                startTime = currentIndex,
                minTime = minValue,
                maxTime = maxValue,
                onSelectedMinutes = {
                    currentIndex = it
                }
            )
        }
    )
}

@Composable
fun SizeSettingsDialog(
    selected: Int,
    screenWidth: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(currentIndex) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text(text = "Select size") },
        text = {
            if (screenWidth > 1080) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LockIconSize.entries.toTypedArray().forEachIndexed { index, size ->
                        TextButton(
                            onClick = { currentIndex = index },
                            shape = ButtonDefaults.textShape,
                            colors = if (currentIndex == index) {
                                ButtonDefaults.textButtonColors()
                                    .copy(containerColor = InactiveGreen)
                            } else {
                                ButtonDefaults.textButtonColors()
                            },
                            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 32.dp)
                        ) {
                            Text(size.getLocalizedString().uppercase())
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    LockIconSize.entries.toTypedArray().forEachIndexed { index, size ->
                        TextButton(
                            onClick = { currentIndex = index },
                            shape = ButtonDefaults.textShape,
                            colors = if (currentIndex == index) {
                                ButtonDefaults.textButtonColors()
                                    .copy(containerColor = InactiveGreen)
                            } else {
                                ButtonDefaults.textButtonColors()
                            },
                            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 32.dp)
                        ) {
                            Text(size.getLocalizedString().uppercase())
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun ColorSettings(
    selected: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val colors = listOf(
        Color.Red,
        Color.Green,
        Color.Blue,
        Color.Yellow,
        Color.Cyan,
        Color.Magenta,
        Color.LightGray,
        Color.DarkGray,
        Color.White,
        Color.Gray
    )
    var selectedColor by remember { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedColor) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text(text = "Select the color")},
        text = {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                colors.forEachIndexed{ index, color ->
                    if (index != 0 ) Spacer(modifier = Modifier.width(2.dp))
                    Box(
                     modifier = Modifier
                         .size(40.dp)
                         .background(color = color, shape = RoundedCornerShape(10.dp))
                         /*.border(
                             width = 2.dp,
                             color = if (color.toArgb() == selectedColor) Color.Black else Color.Transparent,
                             shape = CircleShape
                         )*/
                         .clickable { selectedColor = color.toArgb() }
                         .padding(horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (color.toArgb() == selectedColor) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.Black
                            )
                        }
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SizeSettingsDialogPreview() {
    ColorSettings(
        Color.Red.toArgb(),
        {}
    ) { }
}