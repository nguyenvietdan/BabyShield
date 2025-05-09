package com.monkey.babyshield.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.monkey.babyshield.R
import com.monkey.babyshield.common.LockIconSize

@Composable
fun LockIconSize.getLocalizedString(): String {
    val context = LocalContext.current
    return when (this) {
        LockIconSize.SMALL -> context.getString(R.string.small_icon_size)
        LockIconSize.MEDIUM -> context.getString(R.string.medium_icon_size)
        LockIconSize.LARGE -> context.getString(R.string.large_icon_size)
    }
}