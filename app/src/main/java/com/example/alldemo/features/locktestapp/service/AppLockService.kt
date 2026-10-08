package com.example.alldemo.features.locktestapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.example.alldemo.MainActivity
import com.example.alldemo.R
import com.example.alldemo.features.locktestapp.data.repository.LockedAppsRepository
import com.example.alldemo.features.locktestapp.util.ForegroundAppDetector
import com.example.alldemo.features.locktestapp.util.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppLockService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var repository: LockedAppsRepository
    private lateinit var overlayManager: OverlayLockManager

    private var lockedPackages: Set<String> = emptySet()
    private var currentPin: String = LockedAppsRepository.DEFAULT_PIN
    private var currentlyUnlockedPackage: String? = null

    companion object {
        private const val TAG = "AppLockService"

        const val CHANNEL_ID = "app_lock_service_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_SERVICE = "com.example.alldemo.features.locktestapp.action.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.alldemo.features.locktestapp.action.STOP_SERVICE"

        var isServiceRunning = false
            private set

        fun startService(context: Context) {
            val intent = Intent(context, AppLockService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.d(TAG, "startService requested successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start AppLockService", e)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AppLockService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            try {
                context.stopService(intent)
                Log.d(TAG, "stopService requested successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop AppLockService", e)
            }
        }
    }

    override fun onCreate() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        super.onCreate()

        repository = LockedAppsRepository(applicationContext)
        overlayManager = OverlayLockManager(applicationContext)
        isServiceRunning = true

        createNotificationChannel()
        startForegroundWithNotification()

        observeRepository()
        startMonitoringLoop()
        Log.d(TAG, "AppLockService onCreate completed")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand with action: $action")
        if (action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun observeRepository() {
        serviceScope.launch {
            repository.lockedPackagesFlow.collectLatest { set ->
                lockedPackages = set
                Log.d(TAG, "Updated locked packages set: $lockedPackages")
            }
        }
        serviceScope.launch {
            repository.pinFlow.collectLatest { pin ->
                currentPin = pin
            }
        }
    }

    private fun startMonitoringLoop() {
        serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    checkForegroundApp()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in monitoring loop iteration", e)
                }
                delay(500)
            }
        }
    }

    private fun checkForegroundApp() {
        val hasUsage = PermissionUtils.hasUsageStatsPermission(applicationContext)
        val hasOverlay = PermissionUtils.hasOverlayPermission(applicationContext)

        if (!hasUsage || !hasOverlay) {
            Log.w(TAG, "Permissions not granted (UsageAccess=$hasUsage, Overlay=$hasOverlay). Hiding overlay.")
            if (overlayManager.isOverlayShowing()) {
                serviceScope.launch(Dispatchers.Main) {
                    overlayManager.hideOverlay()
                }
            }
            return
        }

        val topPackage = ForegroundAppDetector.getForegroundAppPackage(applicationContext)

        if (!topPackage.isNullOrEmpty() &&
            topPackage != currentlyUnlockedPackage &&
            topPackage != applicationContext.packageName &&
            topPackage != "com.android.systemui"
        ) {
            Log.d(TAG, "Foreground package changed to $topPackage. Resetting unlocked package (was $currentlyUnlockedPackage).")
            currentlyUnlockedPackage = null
        }

        if (topPackage == null || ForegroundAppDetector.isIgnoredPackage(applicationContext, topPackage)) {
            if (overlayManager.isOverlayShowing()) {
                serviceScope.launch(Dispatchers.Main) {
                    overlayManager.hideOverlay()
                }
            }
            return
        }

        val isLocked = lockedPackages.contains(topPackage)
        val isUnlocked = (topPackage == currentlyUnlockedPackage)

        if (isLocked && !isUnlocked) {
            Log.d(TAG, "Locked app in foreground: $topPackage. Showing overlay...")
            serviceScope.launch(Dispatchers.Main) {
                val appLabel = getAppName(topPackage)
                val appIcon = getAppIcon(topPackage)

                overlayManager.showOverlay(
                    packageName = topPackage,
                    appName = appLabel,
                    icon = appIcon,
                    correctPin = currentPin,
                    onUnlockSuccess = {
                        Log.d(TAG, "PIN correct! Unlocking package: $topPackage")
                        currentlyUnlockedPackage = topPackage
                    },
                    onGoHome = {
                        Log.d(TAG, "User exited overlay to Home")
                        currentlyUnlockedPackage = null
                        goToHomeScreen()
                    },
                    lifecycleOwner = this@AppLockService,
                    savedStateRegistryOwner = this@AppLockService
                )
            }
        } else {
            if (overlayManager.isOverlayShowing() && overlayManager.showingPackageName != topPackage) {
                serviceScope.launch(Dispatchers.Main) {
                    overlayManager.hideOverlay()
                }
            }
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            packageName
        }
    }

    private fun getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (_: Exception) {
            null
        }
    }

    private fun goToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Lock Protection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Running in the background to protect your locked applications"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundWithNotification() {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("App Lock Protection Active")
            .setContentText("Monitoring protected applications")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
                )
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "AppLockService onDestroy called")
        serviceScope.launch(Dispatchers.Main) {
            overlayManager.hideOverlay()
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        serviceJob.cancel()
        isServiceRunning = false
        super.onDestroy()
    }
}
