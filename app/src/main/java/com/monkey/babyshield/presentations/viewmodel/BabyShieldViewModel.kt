package com.monkey.babyshield.presentations.viewmodel

import android.content.Context
import android.provider.Settings
import android.util.Log
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import androidx.lifecycle.ViewModel
import com.monkey.babyshield.services.OverlayService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class BabyShieldViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = "BabyShieldViewModel"
    private val _overlayActive = MutableStateFlow(false)
    val overlayActive = _overlayActive.asStateFlow()
    private val _hasOverlayPermission = MutableStateFlow(Settings.canDrawOverlays(context))
    val hasOverlayPermission = _hasOverlayPermission.asStateFlow()

    private fun startOverlayService() {
        val intent = Intent(context, OverlayService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    private fun stopOverlayService() {
        val intent = Intent(context, OverlayService::class.java)
        context.stopService(intent)
    }

    fun activeOverlay() {
        _overlayActive.value = !_overlayActive.value.also { isActive ->
            if (isActive) {
                stopOverlayService()
            } else {
                startOverlayService()
            }
        }
        Log.i(TAG, "activeOverlay: ${_overlayActive.value} ")
    }

    fun checkOverlayPermission() = _hasOverlayPermission.value.also { isPermission ->
        Log.i(TAG, "checkOverlayPermission: checking $isPermission")
    }
}