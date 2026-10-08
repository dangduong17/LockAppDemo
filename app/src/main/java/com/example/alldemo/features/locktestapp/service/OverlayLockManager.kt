package com.example.alldemo.features.locktestapp.service

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.alldemo.features.locktestapp.ui.overlay.OverlayPinScreen
import com.example.alldemo.ui.theme.AllDemoTheme

class OverlayLockManager(private val context: Context) {

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var currentOverlayView: ComposeView? = null
    var showingPackageName: String? = null
        private set

    companion object {
        private const val TAG = "OverlayLockManager"
    }

    fun isOverlayShowing(): Boolean = currentOverlayView != null

    fun showOverlay(
        packageName: String,
        appName: String,
        icon: Drawable?,
        correctPin: String,
        onUnlockSuccess: () -> Unit,
        onGoHome: () -> Unit,
        lifecycleOwner: LifecycleOwner,
        savedStateRegistryOwner: SavedStateRegistryOwner
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post {
                showOverlay(packageName, appName, icon, correctPin, onUnlockSuccess, onGoHome, lifecycleOwner, savedStateRegistryOwner)
            }
            return
        }

        if (showingPackageName == packageName && currentOverlayView != null) {
            Log.d(TAG, "Overlay already showing for package: $packageName")
            return
        }

        if (currentOverlayView != null) {
            Log.d(TAG, "Dismissing existing overlay for $showingPackageName before showing $packageName")
            hideOverlayInternal()
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val viewModelStoreOwner = object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = ViewModelStore()
        }

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
            setViewTreeViewModelStoreOwner(viewModelStoreOwner)

            setContent {
                AllDemoTheme {
                    OverlayPinScreen(
                        appName = appName,
                        appIcon = icon,
                        correctPin = correctPin,
                        onUnlockSuccess = {
                            hideOverlay()
                            onUnlockSuccess()
                        },
                        onGoHome = {
                            hideOverlay()
                            onGoHome()
                        }
                    )
                }
            }
        }

        try {
            windowManager.addView(composeView, layoutParams)
            currentOverlayView = composeView
            showingPackageName = packageName
            Log.d(TAG, "Successfully added overlay view for $packageName")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding overlay view for package $packageName", e)
            currentOverlayView = null
            showingPackageName = null
        }
    }

    fun hideOverlay() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { hideOverlayInternal() }
        } else {
            hideOverlayInternal()
        }
    }

    private fun hideOverlayInternal() {
        val view = currentOverlayView ?: return
        try {
            windowManager.removeView(view)
            Log.d(TAG, "Successfully removed overlay view for package $showingPackageName")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing overlay view for package $showingPackageName", e)
        } finally {
            currentOverlayView = null
            showingPackageName = null
        }
    }
}
