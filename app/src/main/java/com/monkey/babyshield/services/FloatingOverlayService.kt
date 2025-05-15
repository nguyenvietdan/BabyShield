package com.monkey.babyshield.services

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.PointF
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.core.animation.doOnEnd
import com.monkey.babyshield.R
import com.monkey.babyshield.di.BabyShieldManagerEntryPoint
import com.monkey.babyshield.framework.NotificationHelper
import com.monkey.babyshield.utils.dpToPx
import com.monkey.domain.repository.BabyShieldDataSource
import com.monkey.domain.repository.BabyShieldDataSource.Companion.KEY_POSITION
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
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

    private lateinit var unlockButton: ImageView
    private lateinit var unlockParams: LayoutParams
    private var isLocked = true

    private var edgeMargin = 24
    private var initialPosition = Point(0, 0)
    private var initialTouch = PointF(0f, 0f)
    private var currentScreenWidth = 0
    private var currentScreenHeight = 0
    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO + Job())
    private var currentColor: Int = 0

    private val orientationChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_CONFIGURATION_CHANGED) {
                val newConfiguration = context?.resources?.configuration?.orientation
                when (newConfiguration) {
                    Configuration.ORIENTATION_LANDSCAPE, Configuration.ORIENTATION_PORTRAIT -> {
                        updateScreenSize(true)
                        snapToEdge()
                    }

                    Configuration.ORIENTATION_SQUARE, Configuration.ORIENTATION_UNDEFINED -> {}
                }
            }
        }
    }

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
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        currentColor = sharedPrefs.iconColor.value
        NotificationHelper.createNotificationChannel(applicationContext)
        setupOverlayView()
        isRunning = true
        registerReceiver(orientationChangeReceiver, IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED))
        observeDataChange()
    }

    private fun observeDataChange() {
        ioScope.launch {
            observeEdgeMarginChanges(sharedPrefs.edgeMargin)
        }

        ioScope.launch {
            observeLockIconSize(sharedPrefs.iconSize)
        }

        ioScope.launch {
            CoroutineScope(Dispatchers.Main).launch {
                sharedPrefs.alpha
                    .collectLatest {
                        unlockButton.alpha = it.toFloat() / 100
                    }
            }
        }

        ioScope.launch {
            observeColorChange(sharedPrefs.iconColor)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand: ")
        startForeground(
            NotificationHelper.NOTIFICATION_ID,
            NotificationHelper.createNotification(applicationContext)
        )
        addLayoutParams(!isLocked)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        if (::overlayView.isInitialized && overlayView.isAttachedToWindow) {
            windowManager.removeView(overlayView)
            windowManager.removeView(unlockButton)
        }
        unregisterReceiver(orientationChangeReceiver)
        ioScope.cancel()
        isRunning = false
        super.onDestroy()
    }

    private fun updateScreenSize(updateCoordinatesIfNeeded: Boolean = false) {
        val metrics = resources.displayMetrics
        if (updateCoordinatesIfNeeded) {
            val percentX = unlockParams.x / currentScreenWidth.toFloat()
            val percentY = unlockParams.y / currentScreenHeight.toFloat()
            unlockParams.x = (metrics.widthPixels * percentX).toInt()
            unlockParams.y = (metrics.heightPixels * percentY).toInt()
        }
        currentScreenWidth = metrics.widthPixels
        currentScreenHeight = metrics.heightPixels
    }

    @SuppressLint("ClickableViewAccessibility", "ResourceAsColor")
    private fun setupOverlayView() {
        updateScreenSize()

        overlayView = View(this).apply {
            setBackgroundColor(0x00000000) // Fully transparent
            // Semi-transparent black
            // This is the key part - consume touch events
            setOnTouchListener { _, _ ->
                isLocked
            }
        }

        unlockButton =
            ImageView(applicationContext)// floatingView.findViewById<ImageView>(R.id.blockButton)!!
                .apply {
                    if (isLocked) {
                        setImageResource(R.drawable.ic_lock)
                    } else {
                        setImageResource(R.drawable.ic_unlock)
                    }
                    background = null
                    alpha = sharedPrefs.alpha.value.toFloat() / 100 // 0.7
                    setOnTouchListener { view, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                initialPosition = Point(unlockParams.x, unlockParams.y)
                                initialTouch = PointF(event.rawX, event.rawY)
                                return@setOnTouchListener true
                            }

                            MotionEvent.ACTION_MOVE -> {
                                unlockParams.x =
                                    initialPosition.x + (event.rawX - initialTouch.x).toInt()
                                unlockParams.y =
                                    initialPosition.y + (event.rawY - initialTouch.y).toInt()
                                updateUnlockViewLayout()
                                return@setOnTouchListener true
                            }

                            MotionEvent.ACTION_UP -> {
                                val deltaX = abs(event.rawX - initialTouch.x)
                                val deltaY = abs(event.rawY - initialTouch.y)

                                if (deltaX < 10 && deltaY < 10) {
                                    toggleTouchBlocking()
                                } else {
                                    snapToEdge()
                                }
                                return@setOnTouchListener true
                            }
                        }
                        return@setOnTouchListener false
                    }
                }
        unlockParams = LayoutParams(
            DEFAULT_ICON_SIZE,
            DEFAULT_ICON_SIZE,
            LayoutParams.TYPE_APPLICATION_OVERLAY,
            LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = sharedPrefs.position.value.x
            y = sharedPrefs.position.value.y
        }
        addOverlayToWindow()
    }

    /**
     * Running on UI thread
     */
    @SuppressLint("Recycle")
    private fun snapToEdge() {
        val viewWidth = unlockButton.width
        val distanceToLeftEdge = unlockParams.x
        val distanceToRightEdge = currentScreenWidth - (unlockParams.x + viewWidth)

        val targetX = if (distanceToLeftEdge <= distanceToRightEdge) {
            edgeMargin
        } else {
            currentScreenWidth - viewWidth - edgeMargin
        }

        val animator = ValueAnimator.ofInt(unlockParams.x, targetX).apply {
            duration = ANIMATION_DURATION_MS
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                unlockParams.x = animation.animatedValue as Int
                updateUnlockViewLayout()

            }
            doOnEnd {
                CoroutineScope(Dispatchers.IO).launch {
                    sharedPrefs.save(KEY_POSITION, Point(unlockParams.x, unlockParams.y))
                }
            }
        }

        animator.start()
    }

    private suspend fun observeEdgeMarginChanges(edgeMarginFlow: Flow<Int>) {
        edgeMarginFlow.collectLatest { margin ->
            edgeMargin = margin
            if (unlockButton.layoutParams is LayoutParams) {
                withContext(Dispatchers.Main) {
                    snapToEdge()
                }
            }
        }
    }

    /**
     * Scale unlockButton Running on UI thread
     */
    @SuppressLint("Recycle")
    private fun scaleIconSizeWithAnimation(iconSize: Int) {
        val targetScale = iconSize.toFloat() + 1
        val startScale = unlockButton.scaleX
        val animator = ValueAnimator.ofFloat(startScale, targetScale).apply {
            duration = ANIMATION_DURATION_MS
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                unlockButton.scaleX = animation.animatedValue as Float
                unlockButton.scaleY = animation.animatedValue as Float
            }
        }

        animator.start()
    }

    /**
     * Running on UI thread
     */
    @SuppressLint("Recycle")
    private fun updateSizeWithAnimation(iconSize: Int) {
        if (!::unlockParams.isInitialized) return
        val targetSize = dpToPx(applicationContext, iconSize * DEFAULT_ICON_SIZE)
        val start = unlockParams.width
        if (start == targetSize) return
        val animator = ValueAnimator.ofInt(start, targetSize).apply {
            duration = ANIMATION_DURATION_MS
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                unlockParams.width = animation.animatedValue as Int
                unlockParams.height = animation.animatedValue as Int
                updateUnlockViewLayout()
            }
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

    private suspend fun observeColorChange(color: Flow<Int>) {
        color.collectLatest { newColor ->
            withContext(Dispatchers.Main) {
                updateColorAnimation(newColor)
            }
        }
    }

    private fun updateColorAnimation(color: Int) {
        if (!::unlockButton.isInitialized) return
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), currentColor, color).apply {
            duration = ANIMATION_DURATION_MS
            addUpdateListener { animation ->
                unlockButton.setColorFilter(animation.animatedValue as Int)
            }
            doOnEnd {
                currentColor = color
            }
        }
        colorAnimation.start()
    }

    @SuppressLint("ResourceAsColor")
    private fun toggleTouchBlocking() {
        Log.i(TAG, "setupOverlayView: isLocked $isLocked")
        isLocked = !isLocked
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

    private fun addOverlayToWindow() {
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
        windowManager.addView(unlockButton, unlockParams)
    }

    private fun addLayoutParams(shouldAddedTouchable: Boolean) {
        val params = overlayView.layoutParams as LayoutParams
        params.flags =
            if (shouldAddedTouchable) params.flags or LayoutParams.FLAG_NOT_TOUCHABLE
            else params.flags and LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        windowManager.updateViewLayout(overlayView, params)
    }

    private fun updateUnlockViewLayout() {
        if (unlockButton.isAttachedToWindow && ::unlockParams.isInitialized) {
            windowManager.updateViewLayout(unlockButton, unlockParams)
        }
    }

    companion object {
        private const val ANIMATION_DURATION_MS = 300L
        private const val DEFAULT_ICON_SIZE = 48

        var isRunning: Boolean = false
        var isTouchBlocked: Boolean = false
    }
}