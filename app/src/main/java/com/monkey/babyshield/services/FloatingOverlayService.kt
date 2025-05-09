package com.monkey.babyshield.services

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import com.monkey.babyshield.R
import com.monkey.babyshield.di.BabyShieldManagerEntryPoint
import com.monkey.babyshield.framework.NotificationHelper
import com.monkey.babyshield.utils.dpToPx
import com.monkey.domain.repository.BabyShieldDataSource
import com.monkey.domain.repository.BabyShieldDataSource.Companion.KEY_POSITION_Y
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            observeEdgeMarginChanges(sharedPrefs.edgeMargin)
        }

        CoroutineScope(Dispatchers.IO).launch {
            observeLockIconSize(sharedPrefs.iconSize)
        }

        CoroutineScope(Dispatchers.Main).launch {
            sharedPrefs.alpha
                .collectLatest {
                    unlockButton.alpha = it.toFloat() / 100
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
            y = sharedPrefs.positionY.value
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

    /**
     * Running on UI thread
     */
    @SuppressLint("Recycle")
    private fun snapToEdge(params: LayoutParams) {
        val viewWidth = unlockButton.width
        val viewHeight = unlockButton.height

        val distanceToLeftEdge = params.x
        val distanceToRightEdge = currentScreenWidth - (params.x + viewWidth)

        val targetX = if (distanceToLeftEdge <= distanceToRightEdge) {
            edgeMargin
        } else {
            currentScreenWidth - viewWidth - edgeMargin
        }

        val animator = ValueAnimator.ofInt(params.x, targetX)
        animator.duration = ANIMATION_DURATION_MS // 300ms
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener { animation ->
            params.x = animation.animatedValue as Int
            windowManager.updateViewLayout(this.floatingView, params)
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                CoroutineScope(Dispatchers.IO).launch {
                    sharedPrefs.save(KEY_POSITION_Y, params.y)
                    // todo save position to shared prefs
                }
            }
        })
        animator.start()
    }

    private suspend fun observeEdgeMarginChanges(edgeMarginFlow: Flow<Int>) {
        edgeMarginFlow.collectLatest { margin ->
            edgeMargin = margin
            if (floatingView.layoutParams is LayoutParams) {
                withContext(Dispatchers.Main) {
                    snapToEdge(floatingView.layoutParams as LayoutParams)
                }
            }
        }
    }

    /**
     * Running on UI thread
     */
    @SuppressLint("Recycle")
    private fun scaleIconSizeWithAnimation(iconSize: Int) {
        val targetScale = iconSize.toFloat() + 1
        val startScale = unlockButton.scaleX
        val animator = ValueAnimator.ofFloat(startScale, targetScale)
        animator.duration = ANIMATION_DURATION_MS
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener { animation ->
            unlockButton.scaleX = animation.animatedValue as Float
            unlockButton.scaleY = animation.animatedValue as Float
        }
        animator.start()
    }

    /**
     * Running on UI thread
     */
    @SuppressLint("Recycle")
    private fun updateSizeWithAnimation(iconSize: Int) {
        val params = unlockButton.layoutParams as? FrameLayout.LayoutParams ?: return
        val targetSize = dpToPx(applicationContext, iconSize * DEFAULT_ICON_SIZE)
        val start = params.width
        if (start == targetSize) return
        val animator = ValueAnimator.ofInt(start, targetSize)
        animator.duration = ANIMATION_DURATION_MS
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener { animation ->
            params.width = animation.animatedValue as Int
            params.height = animation.animatedValue as Int
            unlockButton.layoutParams = params
        }
        animator.start()
    }

    private suspend fun observeLockIconSize(lockIconSizeFlow: Flow<Int>) {
        lockIconSizeFlow.collectLatest { size ->
            withContext(Dispatchers.Main) {
                updateSizeWithAnimation(size + 1)
            }
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
        } else {
            unlockButton.setImageResource(R.drawable.ic_unlock)
        }
    }

    private fun addOverlayToWindow(buttonParams: LayoutParams) {
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
        val params = overlayView.layoutParams as LayoutParams
        params.flags =
            if (shouldAddedTouchable) params.flags or LayoutParams.FLAG_NOT_TOUCHABLE
            else params.flags and LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        windowManager.updateViewLayout(overlayView, params)
    }

    companion object {
        private const val ANIMATION_DURATION_MS = 300L
        private const val DEFAULT_ICON_SIZE = 48

        var isRunning: Boolean = false
        var isTouchBlocked: Boolean = false
    }
}