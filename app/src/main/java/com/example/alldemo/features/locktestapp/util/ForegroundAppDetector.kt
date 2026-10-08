package com.example.alldemo.features.locktestapp.util

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

object ForegroundAppDetector {

    private const val TAG = "ForegroundAppDetector"
    private var cachedHomePackages: Set<String>? = null

    fun getHomePackages(context: Context): Set<String> {
        cachedHomePackages?.let { return it }

        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfos = try {
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error querying home activities", e)
            emptyList()
        }

        val packages = resolveInfos.mapNotNull { it.activityInfo?.packageName }.toSet()
        if (packages.isNotEmpty()) {
            cachedHomePackages = packages
        }
        return packages
    }

    fun getForegroundAppPackage(context: Context): String? {
        if (!PermissionUtils.hasUsageStatsPermission(context)) {
            Log.w(TAG, "Usage stats permission not granted")
            return null
        }

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        if (usm == null) {
            Log.e(TAG, "UsageStatsManager service unavailable")
            return null
        }

        val endTime = System.currentTimeMillis()
        val startTime = endTime - 60_000

        try {
            val usageEvents = usm.queryEvents(startTime, endTime)
            var lastResumedPackage: String? = null
            var maxEventTime = 0L

            val event = UsageEvents.Event()
            while (usageEvents != null && usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                @Suppress("DEPRECATION")
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    if (event.timeStamp >= maxEventTime) {
                        maxEventTime = event.timeStamp
                        lastResumedPackage = event.packageName
                    }
                }
            }

            if (!lastResumedPackage.isNullOrEmpty()) {
                Log.d(TAG, "queryEvents identified foreground package: $lastResumedPackage (eventTime=$maxEventTime)")
                return lastResumedPackage
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during queryEvents execution", e)
        }

        try {
            val statsStartTime = endTime - 10 * 60 * 1000L
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, statsStartTime, endTime)
            if (!stats.isNullOrEmpty()) {
                val mostRecent = stats
                    .filter { !it.packageName.isNullOrEmpty() && it.lastTimeUsed > 0 }
                    .maxByOrNull { it.lastTimeUsed }
                if (mostRecent != null) {
                    Log.d(TAG, "queryUsageStats fallback identified foreground package: ${mostRecent.packageName} (lastTimeUsed=${mostRecent.lastTimeUsed})")
                    return mostRecent.packageName
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during queryUsageStats fallback execution", e)
        }

        Log.w(TAG, "No foreground package could be determined from UsageStats")
        return null
    }

    fun isIgnoredPackage(context: Context, packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return true
        if (packageName == context.packageName) return true
        if (packageName == "com.android.systemui") return true
        if (getHomePackages(context).contains(packageName)) return true
        return false
    }
}
