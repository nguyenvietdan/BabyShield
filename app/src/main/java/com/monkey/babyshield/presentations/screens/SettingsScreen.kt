package com.monkey.babyshield.presentations.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.monkey.babyshield.R
import com.monkey.babyshield.common.LockIconSize
import com.monkey.babyshield.common.SettingSwitchItemData
import com.monkey.babyshield.common.SettingValueColorItemData
import com.monkey.babyshield.common.SettingValueItemData
import com.monkey.babyshield.common.SettingsActionItem
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
                title = { Text(text = stringResource(R.string.settings)) },
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

    if (editingType != null) {
        val selectedValue = when (editingType) {
            WheelPickerType.EDGE_MARGIN -> edgeMargin
            WheelPickerType.ALPHA -> alphaValue
            WheelPickerType.ICON_SIZE -> iconSize
            WheelPickerType.ICON_COLOR -> iconColor
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

        DialogSetting(
            selected = selectedValue,
            minValue = minValue,
            maxValue = maxValue,
            screenWidth = minSize,
            type = editingType,
            onDismiss = { settingsViewModel.closeSheet() },
            onConfirm = { value ->
                when (editingType) {
                    WheelPickerType.EDGE_MARGIN -> settingsViewModel.updateEdgeMargin(value)
                    WheelPickerType.ALPHA -> settingsViewModel.updateAlpha(value)
                    WheelPickerType.ICON_SIZE -> settingsViewModel.updateIconSize(value)
                    WheelPickerType.ICON_COLOR -> settingsViewModel.updateIconColor(value)
                    else -> {}
                }
                settingsViewModel.closeSheet()
            }
        )
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
                title = stringResource(R.string.baby_shield_settings),
                items = listOf(
                    SettingSwitchItemData(
                        title = stringResource(R.string.locked),
                        subTitle = stringResource(R.string.locked_desc),
                        checked = isLocked,
                        onToggle = { settingsViewModel.updateLocked(it) }
                    ),
                    SettingValueItemData(stringResource(R.string.edge_margin), "$edgeMargin") {
                        settingsViewModel.openSheet(WheelPickerType.EDGE_MARGIN)
                    }
                )
            )
        }

        item {
            SettingGroup(
                title = stringResource(R.string.unlock_button_settings),
                items = listOf(
                    SettingValueItemData(stringResource(R.string.transparency), "$alphaValue") {
                        settingsViewModel.openSheet(WheelPickerType.ALPHA)
                    },
                    SettingValueItemData(
                        stringResource(R.string.icon_size),
                        LockIconSize.entries[iconSize].getLocalizedString()
                    ) {
                        settingsViewModel.openSheet(WheelPickerType.ICON_SIZE)
                    },
                    SettingValueColorItemData(stringResource(R.string.icon_color), iconColor) {
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
                is SettingsActionItem -> ActionRow(item.title, item.onClick)
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
fun DialogSetting(
    selected: Int,
    minValue: Int,
    maxValue: Int,
    screenWidth: Int,
    type: WheelPickerType?,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var currentValue by remember { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(currentValue) }) {
                Text(text = stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        title = { Text(text = getTitle(type)) },
        text = {
            when (type) {
                WheelPickerType.ICON_COLOR -> ColorContent(
                    currentValue,
                    onClick = { value -> currentValue = value })

                WheelPickerType.EDGE_MARGIN, WheelPickerType.ALPHA -> DefaultWheelMinutesTimePicker(
                    startTime = currentValue,
                    minTime = minValue,
                    maxTime = maxValue,
                    onSelectedMinutes = {
                        currentValue = it
                    }
                )

                WheelPickerType.ICON_SIZE -> SizeSettingsText(
                    selected = currentValue,
                    screenWidth = screenWidth,
                    onClick = { currentValue = it })

                else -> {}
            }
        }
    )
}

@Composable
private fun getTitle(type: WheelPickerType?) = when(type) {
    WheelPickerType.EDGE_MARGIN -> stringResource(R.string.select_edge_margin)
    WheelPickerType.ALPHA -> stringResource(R.string.select_alpha)
    WheelPickerType.ICON_SIZE -> stringResource(R.string.select_icon_size)
    WheelPickerType.ICON_COLOR -> stringResource(R.string.select_icon_color)
    else -> ""
}

@Composable
fun SizeSettingsText(
    selected: Int,
    screenWidth: Int,
    onClick: (Int) -> Unit
) {
    if (screenWidth > 1080) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SizeSettingsContent(selected = selected, onClick = onClick)
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SizeSettingsContent(selected = selected, onClick = onClick)
        }
    }
}

@Composable
fun SizeSettingsContent(
    selected: Int,
    onClick: (Int) -> Unit
) {
    LockIconSize.entries.toTypedArray().forEachIndexed { index, size ->
        TextButton(
            onClick = { onClick(index) },
            shape = ButtonDefaults.textShape,
            colors = if (selected == index) {
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

@Composable
fun ColorContent(
    selected: Int,
    onClick: (Int) -> Unit
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
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        colors.forEachIndexed { index, color ->
            if (index != 0) Spacer(modifier = Modifier.width(2.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color = color, shape = RoundedCornerShape(10.dp))
                    .clickable {
                        onClick(color.toArgb())
                    }
                    .padding(horizontal = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                if (color.toArgb() == selected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SizeSettingsDialogPreview() {
}