package com.example.alldemo.features.lockscreentestapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.alldemo.MainActivity
import com.example.alldemo.R
import com.example.alldemo.features.lockscreentestapp.overlay.OverlayManager
import com.example.alldemo.features.lockscreentestapp.receiver.ScreenReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OverlayService : Service() {

    private var overlayManager: OverlayManager? = null
    private var screenReceiver: ScreenReceiver? = null
    private var isReceiverRegistered = false
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        overlayManager = OverlayManager(this)

        serviceScope.launch {
            overlayManager?.isShowing?.collect { showing ->
                _isOverlayShowing.value = showing
            }
        }

        registerScreenReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            _isServiceRunning.value = true
        } catch (e: Exception) {
            e.printStackTrace()
            _isServiceRunning.value = false
            stopSelf()
            return START_NOT_STICKY
        }

        if (action == ACTION_SHOW_TEST_OVERLAY) {
            val now = System.currentTimeMillis()
            _lastScreenEvent.value = "MANUAL_TEST"
            _lastEventTimestamp.value = now
            overlayManager?.showOverlay(
                screenEvent = "MANUAL_TEST",
                eventTimestamp = now
            )
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterScreenReceiver()
        overlayManager?.removeOverlay()
        overlayManager = null
        serviceJob.cancel()
        _isServiceRunning.value = false
        _isOverlayShowing.value = false
    }

    private fun registerScreenReceiver() {
        if (!isReceiverRegistered) {
            screenReceiver = ScreenReceiver { action, timestamp ->
                val normalizedEvent = when (action) {
                    Intent.ACTION_SCREEN_OFF -> "SCREEN_OFF"
                    Intent.ACTION_SCREEN_ON -> "SCREEN_ON"
                    Intent.ACTION_USER_PRESENT -> "USER_PRESENT"
                    else -> action
                }

                _lastScreenEvent.value = normalizedEvent
                _lastEventTimestamp.value = timestamp

                when (action) {
                    Intent.ACTION_SCREEN_OFF, Intent.ACTION_SCREEN_ON -> {
                        overlayManager?.showOverlay(
                            screenEvent = normalizedEvent,
                            eventTimestamp = timestamp
                        )
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        // ACTION_USER_PRESENT status updated in state
                    }
                }
            }

            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            try {
                ContextCompat.registerReceiver(
                    this,
                    screenReceiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
                isReceiverRegistered = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun unregisterScreenReceiver() {
        if (isReceiverRegistered && screenReceiver != null) {
            try {
                unregisterReceiver(screenReceiver)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            screenReceiver = null
            isReceiverRegistered = false
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Overlay Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for Lockscreen Overlay Foreground Service"
            }
            val notificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Lockscreen Overlay Active")
            .setContentText("Listening for SCREEN_OFF, SCREEN_ON & USER_PRESENT")
            .setSmallIcon(R.drawable.placeholder)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(0, "Stop Service", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "overlay_service_channel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_SERVICE = "com.example.alldemo.ACTION_STOP_SERVICE"
        const val ACTION_SHOW_TEST_OVERLAY = "com.example.alldemo.ACTION_SHOW_TEST_OVERLAY"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _lastScreenEvent = MutableStateFlow("NONE")
        val lastScreenEvent: StateFlow<String> = _lastScreenEvent.asStateFlow()

        private val _lastEventTimestamp = MutableStateFlow(System.currentTimeMillis())
        val lastEventTimestamp: StateFlow<Long> = _lastEventTimestamp.asStateFlow()

        private val _isOverlayShowing = MutableStateFlow(false)
        val isOverlayShowing: StateFlow<Boolean> = _isOverlayShowing.asStateFlow()

        fun startOverlayService(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopOverlayService(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            context.stopService(intent)
            _isServiceRunning.value = false
            _isOverlayShowing.value = false
        }

        fun triggerTestOverlay(context: Context) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_SHOW_TEST_OVERLAY
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
