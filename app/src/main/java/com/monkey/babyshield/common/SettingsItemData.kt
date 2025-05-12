package com.monkey.babyshield.common

interface SettingsItemData {
}


data class SettingSwitchItemData(
    val title: String = "",
    val subTitle: String = "",
    val checked: Boolean = false,
    val enabled: Boolean = true,
    val onToggle: (Boolean) -> Unit
) : SettingsItemData

data class SettingValueItemData(
    val title: String,
    val value: String,
    val onClick: () -> Unit
) : SettingsItemData

data class SettingValueColorItemData(
    val title: String,
    val value: Int,
    val onClick: () -> Unit
) : SettingsItemData

data class SettingsActionItem(
    val title: String,
    val onClick: () -> Unit
): SettingsItemData