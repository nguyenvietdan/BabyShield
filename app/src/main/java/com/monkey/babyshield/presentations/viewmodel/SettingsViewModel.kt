package com.monkey.babyshield.presentations.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monkey.domain.repository.BabyShieldDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val sharedPrefs: BabyShieldDataSource
) : ViewModel() {

    private val _editingType = MutableStateFlow<WheelPickerType?>(null)
    val editingType:StateFlow<WheelPickerType?> = _editingType.asStateFlow()

    fun openSheet(type: WheelPickerType) {
        _editingType.value = type
    }

    fun closeSheet() {
        _editingType.value = null
    }

    fun updateLocked(isLocked: Boolean) {
        updateSharedPreference(BabyShieldDataSource.KEY_IS_LOCKED, isLocked)
    }

    fun updateEdgeMargin(margin: Int) {
        updateSharedPreference(BabyShieldDataSource.KEY_EDGE_MARGIN, margin)
    }

    fun updateAlpha(alpha: Int) {
        updateSharedPreference(BabyShieldDataSource.KEY_ALPHA, alpha)
    }

    fun updateIconSize(iconSize: Int) {
        updateSharedPreference(BabyShieldDataSource.KEY_ICON_SIZE, iconSize)
    }

    fun updateIconColor(color: Int) {
        updateSharedPreference(BabyShieldDataSource.KEY_ICON_COLOR, color)
    }

    private fun updateSharedPreference(key: String, value: Any) {
        viewModelScope.launch {
            sharedPrefs.save(key, value)
        }
    }
}

enum class WheelPickerType { EDGE_MARGIN, ALPHA, ICON_SIZE, ICON_COLOR }