package com.example.alldemo.features.lockscreentestapp.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.alldemo.features.lockscreentestapp.ui.LockscreenOverlayContent
import com.example.alldemo.features.lockscreentestapp.util.LatencyLogRepository
import com.example.alldemo.features.lockscreentestapp.util.PermissionManager
import com.example.alldemo.ui.theme.AllDemoTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages adding and removing the Lockscreen Overlay View to [WindowManager].
 */
class OverlayManager(private val context: Context) {

    private val windowManager: WindowManager? =
        context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager

    private var overlayView: View? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    private val _isShowing = MutableStateFlow(false)
    val isShowing: StateFlow<Boolean> = _isShowing.asStateFlow()

    fun showOverlay(
        screenEvent: String,
        eventTimestamp: Long = System.currentTimeMillis(),
        onUnlockedCallback: (() -> Unit)? = null
    ) {
        if (!PermissionManager.hasOverlayPermission(context)) {
            return
        }

        if (windowManager == null) {
            return
        }

        // If overlay is already attached, remove previous view first
        if (overlayView != null) {
            removeOverlay()
        }

        val myLifecycleOwner = OverlayLifecycleOwner()
        myLifecycleOwner.onCreate()
        myLifecycleOwner.onStart()
        lifecycleOwner = myLifecycleOwner

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(myLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(myLifecycleOwner)
            setViewTreeViewModelStoreOwner(myLifecycleOwner)

            setContent {
                AllDemoTheme {
                    LockscreenOverlayContent(
                        screenEvent = screenEvent,
                        eventTimestamp = eventTimestamp,
                        onUnlock = {
                            removeOverlay()
                            onUnlockedCallback?.invoke()
                        }
                    )
                }
            }
        }

        @Suppress("DEPRECATION")
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        @Suppress("DEPRECATION")
        val flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        var hasLoggedLatency = false
        val layoutListener = object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (!hasLoggedLatency) {
                    hasLoggedLatency = true
                    val renderEndTime = System.currentTimeMillis()
                    val latencyMs = (renderEndTime - eventTimestamp).coerceAtLeast(0)

                    LatencyLogRepository.logEvent(
                        timestamp = renderEndTime,
                        eventType = screenEvent,
                        latencyMs = latencyMs
                    )

                    if (composeView.viewTreeObserver.isAlive) {
                        composeView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                }
            }
        }
        composeView.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)

        try {
            windowManager.addView(composeView, params)
            overlayView = composeView
            _isShowing.value = true
        } catch (e: Exception) {
            e.printStackTrace()
            myLifecycleOwner.onStop()
            myLifecycleOwner.onDestroy()
            lifecycleOwner = null
            overlayView = null
            _isShowing.value = false
        }
    }

    fun removeOverlay() {
        val view = overlayView
        if (view != null) {
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
        }

        lifecycleOwner?.let { owner ->
            owner.onStop()
            owner.onDestroy()
            lifecycleOwner = null
        }

        _isShowing.value = false
    }
}
