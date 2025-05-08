package com.monkey.babyshield.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.ImageView
import com.monkey.babyshield.R
import com.monkey.babyshield.di.BabyShieldManagerEntryPoint
import com.monkey.babyshield.framework.NotificationHelper
import com.monkey.domain.repository.BabyShieldDataSource
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

class FloatingOverlayService : Service() {

    private val TAG = "FloatingOverlayService"

    private lateinit var sharedPrefs: BabyShieldDataSource

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View

    private lateinit var floatingView: View
    private lateinit var unlockButton: ImageView
    private var isLocked = true

    private var edgeMargin = 24
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var currentScreenWidth = 0
    private var currentScreenHeight = 0

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate: ")
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            BabyShieldManagerEntryPoint::class.java
        )
        sharedPrefs = entryPoint.getBabyShieldDataSource()
        isLocked = sharedPrefs.isLocked.value
        edgeMargin = sharedPrefs.edgeMargin.value

        Log.i(TAG, "onCreate: isLocked $isLocked")

        NotificationHelper.createNotificationChannel(applicationContext)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        setupOverlayView()
        isRunning = true
        CoroutineScope(Dispatchers.IO).launch {
            sharedPrefs.edgeMargin.collectLatest {
                edgeMargin = it
                if (floatingView.layoutParams is LayoutParams) {
                    snapToEdge(floatingView.layoutParams as WindowManager.LayoutParams)
                }
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            sharedPrefs.alpha.collectLatest {
                unlockButton.alpha = it.toFloat() / 100 // 0.7
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand: ")
        startForeground(
            NotificationHelper.NOTIFICATION_ID,
            NotificationHelper.createNotification(applicationContext)
        )

        //addOverlayToWindow()
        addLayoutParams(!isLocked)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        if (::overlayView.isInitialized && overlayView.parent != null) {
            windowManager.removeView(overlayView)
            windowManager.removeView(floatingView)
        }
        isRunning = false
        super.onDestroy()
    }


    @SuppressLint("ClickableViewAccessibility", "ResourceAsColor")
    private fun setupOverlayView() {
        val metrics = resources.displayMetrics
        currentScreenWidth = metrics.widthPixels
        currentScreenHeight = metrics.heightPixels

        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = View(this).apply {
            setBackgroundColor(0x00000000) // Fully transparent
             // Semi-transparent black
            // This is the key part - consume touch events
            setOnTouchListener { _, _ ->
                isLocked
            }
        }

        floatingView = inflater.inflate(R.layout.floating_button, null)

        unlockButton = floatingView.findViewById<ImageView>(R.id.blockButton)!!
            .apply {
                if (isLocked) {
                    setImageResource(R.drawable.ic_lock)
                } else {
                    setImageResource(R.drawable.ic_unlock)
                }
                background = null
                alpha = sharedPrefs.alpha.value.toFloat() / 100 // 0.7
            }
        val floatingParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            LayoutParams.TYPE_APPLICATION_OVERLAY,
            LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = edgeMargin
            y = 120
        }
        setTouchListener(floatingParams)
        addOverlayToWindow(floatingParams)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setTouchListener(params: LayoutParams) {
        Log.e(TAG, "setTouchListener: ")
        floatingView.setOnTouchListener { view, event ->

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    return@setOnTouchListener true
                }

                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingView, params)
                    return@setOnTouchListener true
                }

                MotionEvent.ACTION_UP -> {
                    val deltaX = abs(event.rawX - initialTouchX)
                    val deltaY = abs(event.rawY - initialTouchY)

                    if (deltaX < 10 && deltaY < 10) {
                        toggleTouchBlocking()
                    } else {
                        snapToEdge(params)
                    }
                    return@setOnTouchListener true
                }
            }
            return@setOnTouchListener false
        }
    }

    private fun snapToEdge(params: LayoutParams) {
        val viewWidth = unlockButton.width
        val viewHeight = unlockButton.height

        val distanceToLeftEdge = params.x
        val distanceToRightEdge = currentScreenWidth - (params.x + viewWidth)
        val distanceToTopEdge = params.y
        val distanceToBottomEdge = currentScreenHeight - (params.y + viewHeight)

        if (distanceToLeftEdge <= distanceToRightEdge) {
            params.x = edgeMargin
        } else {
            params.x = currentScreenWidth - viewWidth - edgeMargin
        }

        /*if (distanceToTopEdge <= distanceToBottomEdge) {
            params.y = edgeMargin
        } else {
            params.y = currentScreenHeight - viewHeight - edgeMargin
        }*/

        Handler(Looper.getMainLooper()).post {
            windowManager.updateViewLayout(floatingView, params)
        }
    }

    @SuppressLint("ResourceAsColor")
    private fun toggleTouchBlocking() {
        isLocked = !isLocked
        Log.i(TAG, "setupOverlayView: isLocked $isLocked")

        updateUnlockButton()
        addLayoutParams(!isLocked)
    }

    @SuppressLint("ResourceAsColor")
    private fun updateUnlockButton() {
        if (isLocked) {
            unlockButton.setImageResource(R.drawable.ic_lock)
            //overlayView.setBackgroundColor(0x00000000) // Semi-transparent black
        } else {
            unlockButton.setImageResource(R.drawable.ic_unlock)
            //overlayView.setBackgroundColor(0x00000000) // Fully transparent
        }
    }

    private fun addOverlayToWindow(buttonParams: WindowManager.LayoutParams) {
        // Setup layout params for overlay
        val overlayParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
            LayoutParams.TYPE_APPLICATION_OVERLAY,
            LayoutParams.FLAG_NOT_FOCUSABLE or
                    LayoutParams.FLAG_NOT_TOUCHABLE or
                    LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        // Add views to window
        windowManager.addView(overlayView, overlayParams)
        windowManager.addView(floatingView, buttonParams)
    }


    private fun addLayoutParams(shouldAddedTouchable: Boolean) {
        val params = overlayView.layoutParams as WindowManager.LayoutParams
        params.flags =
            if (shouldAddedTouchable) params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            else params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        windowManager.updateViewLayout(overlayView, params)
    }

    companion object {

        var isRunning: Boolean = false
        var isTouchBlocked: Boolean = false
    }
}