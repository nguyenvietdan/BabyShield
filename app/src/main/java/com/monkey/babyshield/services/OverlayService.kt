package com.monkey.babyshield.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import androidx.core.app.NotificationCompat
import com.monkey.babyshield.R


class OverlayService: Service() {

    private val TAG = "OverlayService"

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var unlockButton: ImageButton
    private var isLocked = true

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate: ")
        createNotificationChannel()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        setupOverlayView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand: ")
        startForeground(NOTIFICATION_ID, createNotification())
        addOverlayToWindow()
        val params = overlayView.layoutParams as WindowManager.LayoutParams
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        windowManager.updateViewLayout(overlayView, params)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        if (::overlayView.isInitialized && overlayView.parent != null) {
            windowManager.removeView(overlayView)
        }
        super.onDestroy()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupOverlayView() {
        // Create the overlay view
        overlayView = View(this).apply {
            setBackgroundColor(0x33000000) // Semi-transparent black

            // This is the key part - consume touch events
            setOnTouchListener { _, event ->
                Log.i(TAG, "setupOverlayView: $isLocked")
                isLocked
                /*when (event.action) {
                    MotionEvent.ACTION_DOWN,
                    MotionEvent.ACTION_MOVE,
                    MotionEvent.ACTION_UP -> isLocked // Return true to consume the event
                    else -> false
                }*/
            }

        }

        // Create the unlock button
        unlockButton = ImageButton(this).apply {
            setImageResource(R.drawable.ic_lock)
            background = null
            alpha = 0.7f

            setOnClickListener {
                isLocked = !isLocked
                if (isLocked) {
                    setImageResource(R.drawable.ic_lock)
                    overlayView.setBackgroundColor(0x33000000) // Semi-transparent black

                    val params = overlayView.layoutParams as WindowManager.LayoutParams
                    params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                    windowManager.updateViewLayout(overlayView, params)
                } else {
                    setImageResource(R.drawable.ic_unlock)
                    overlayView.setBackgroundColor(0x00000000) // Fully transparent

                    val params = overlayView.layoutParams as WindowManager.LayoutParams
                    params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    windowManager.updateViewLayout(overlayView, params)
                }
            }
        }
    }

    private fun addOverlayToWindow() {
        // Setup layout params for overlay
        val overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        // Setup layout params for unlock button
        val buttonParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Touch Blocker Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Touch Blocker Service Channel"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Chặn Chạm Màn Hình")
            .setContentText("Dịch vụ chặn chạm đang hoạt động")
            .setSmallIcon(R.drawable.ic_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "TouchBlockerChannel"
    }
}