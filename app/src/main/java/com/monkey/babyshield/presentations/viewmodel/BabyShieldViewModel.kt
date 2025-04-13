package com.monkey.babyshield.presentations.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat.startActivityForResult
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

    private val _shouldRequestOverlayPermission = MutableStateFlow(false)
    val shouldRequestOverlayPermission = _shouldRequestOverlayPermission.asStateFlow()

    init {

    }

    fun activeOverlay() {
        _overlayActive.value = !_overlayActive.value
        if (_overlayActive.value) {
            startOverlayService()
        } else {
            stopOverlayService()
        }
        Log.i(TAG, "activeOverlay: ${_overlayActive.value}")
    }

    fun requestOverlayPermission() {
        _shouldRequestOverlayPermission.value = true
    }

    fun checkOverlayPermission() {
        _hasOverlayPermission.value = Settings.canDrawOverlays(context)
    }

    fun onOverlayPermissionResult(isGranted: Boolean) {
        _hasOverlayPermission.value = isGranted
        _shouldRequestOverlayPermission.value = false
    }

    fun startOverlayService() {
        val intent = Intent(context, OverlayService::class.java)
        context.startForegroundService(intent)
    }

    private fun stopOverlayService() {
        val intent = Intent(context, OverlayService::class.java)
        context.stopService(intent)
    }


    companion object {
        private const val OVERLAY_PERMISSION_REQ_CODE = 1234
    }

}