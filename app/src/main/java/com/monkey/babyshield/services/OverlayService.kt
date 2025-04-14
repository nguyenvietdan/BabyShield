package com.monkey.babyshield.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import com.monkey.babyshield.R
import com.monkey.babyshield.di.BabyShieldManagerEntryPoint
import com.monkey.babyshield.framework.NotificationHelper
import com.monkey.domain.repository.BabyShieldDataSource
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors

@AndroidEntryPoint
class OverlayService : Service() {

    private val TAG = "OverlayService"

    private lateinit var sharedPrefs: BabyShieldDataSource

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var unlockButton: ImageButton
    private var isLocked = true

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate: ")
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            BabyShieldManagerEntryPoint::class.java
        )
        sharedPrefs = entryPoint.getBabyShieldDataSource()
        isLocked = sharedPrefs.isLocked.value
        NotificationHelper.createNotificationChannel(applicationContext)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        setupOverlayView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand: ")
        startForeground(
            NotificationHelper.NOTIFICATION_ID,
            NotificationHelper.createNotification(applicationContext)
        )

        addOverlayToWindow()
        addLayoutParams(!isLocked)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        if (::overlayView.isInitialized && overlayView.parent != null) {
            windowManager.removeView(overlayView)
            windowManager.removeView(unlockButton)
        }
        super.onDestroy()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupOverlayView() {
        overlayView = View(this).apply {
            setBackgroundColor(0x00000000) // Semi-transparent black
            // This is the key part - consume touch events
            setOnTouchListener { _, _ ->
                isLocked
            }
        }

        unlockButton = ImageButton(this).apply {
            setImageResource(R.drawable.ic_lock)
            background = null
            alpha = 0.7f

            setOnClickListener {
                isLocked = !isLocked
                Log.i(TAG, "setupOverlayView: isLocked $isLocked")
                if (isLocked) {
                    setImageResource(R.drawable.ic_lock)
                    overlayView.setBackgroundColor(0x00000000) // Semi-transparent black
                } else {
                    setImageResource(R.drawable.ic_unlock)
                    overlayView.setBackgroundColor(0x00000000) // Fully transparent
                }

                addLayoutParams(!isLocked)
            }
        }
    }

    private fun addOverlayToWindow() {
        // Setup layout params for overlay
        val overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        // Setup layout params for unlock button
        val buttonParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 24
            y = 120
        }

        // Add views to window
        windowManager.addView(overlayView, overlayParams)
        windowManager.addView(unlockButton, buttonParams)
    }


    private fun addLayoutParams(shouldAddedTouchable: Boolean) {
        val params = overlayView.layoutParams as WindowManager.LayoutParams
        params.flags =
            if (shouldAddedTouchable) params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            else params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        windowManager.updateViewLayout(overlayView, params)
    }
}