package com.monkey.domain.repository

import android.graphics.Color
import kotlinx.coroutines.flow.StateFlow

interface BabyShieldDataSource {

    val isLocked: StateFlow<Boolean>
    val edgeMargin: StateFlow<Int>
    val alpha: StateFlow<Int>
    val iconSize: StateFlow<Int>
    val iconColor: StateFlow<Long>
    val positionY: StateFlow<Int>

    suspend fun save(key: String, value: Any)

    companion object {
        const val DATA_NAME = "com.monkey.babyshield.data.prefs"
        const val KEY_IS_LOCKED = "is_locked"
        const val KEY_EDGE_MARGIN = "edge_margin"
        const val KEY_ALPHA = "alpha"
        const val KEY_ICON_SIZE = "icon_size"
        const val KEY_ICON_COLOR = "icon_color"
        const val KEY_POSITION_Y = "position_y"
    }
}