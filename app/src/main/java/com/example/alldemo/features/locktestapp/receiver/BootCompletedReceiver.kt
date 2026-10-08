package com.example.alldemo.features.locktestapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.alldemo.features.locktestapp.data.repository.LockedAppsRepository
import com.example.alldemo.features.locktestapp.service.AppLockService
import com.example.alldemo.features.locktestapp.util.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val repository = LockedAppsRepository(context.applicationContext)
            CoroutineScope(Dispatchers.IO).launch {
                val isServiceEnabled = repository.isServiceEnabledFlow.first()
                val hasPermissions = PermissionUtils.hasUsageStatsPermission(context) &&
                        PermissionUtils.hasOverlayPermission(context)

                if (isServiceEnabled && hasPermissions) {
                    AppLockService.startService(context.applicationContext)
                }
            }
        }
    }
}
