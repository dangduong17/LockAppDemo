package com.example.alldemo.features.lockscreentestapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receiver for screen ON/OFF and user present broadcasts.
 */
class ScreenReceiver(
    private val onScreenEvent: (action: String, timestamp: Long) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        val now = System.currentTimeMillis()
        onScreenEvent(action, now)
    }
}
