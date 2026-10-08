package com.example.alldemo.features.locktestapp.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.alldemo.features.locktestapp.data.model.AppInfo

object InstalledAppsLoader {

    @SuppressLint("QueryPermissionsNeeded")
    fun getInstalledApps(context: Context, lockedPackages: Set<String>): List<AppInfo> {
        val pm = context.packageManager
        val selfPackage = context.packageName

        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = try {
            pm.queryIntentActivities(launcherIntent, 0)
        } catch (_: Exception) {
            emptyList()
        }
        val launcherPackages = resolveInfos.map { it.activityInfo.packageName }.toSet()

        val installedApplications = try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (_: Exception) {
            emptyList()
        }

        val appList = mutableListOf<AppInfo>()

        for (appInfo in installedApplications) {
            if (appInfo.packageName == selfPackage) continue

            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isLauncherApp = launcherPackages.contains(appInfo.packageName)

            if (!isLauncherApp && isSystem) {
                continue
            }

            val appName = try {
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                appInfo.packageName
            }

            val icon = try {
                pm.getApplicationIcon(appInfo)
            } catch (_: Exception) {
                pm.defaultActivityIcon
            }

            val isLocked = lockedPackages.contains(appInfo.packageName)

            appList.add(
                AppInfo(
                    packageName = appInfo.packageName,
                    appName = appName,
                    icon = icon,
                    isSystemApp = isSystem,
                    isLocked = isLocked
                )
            )
        }

        return appList.sortedBy { it.appName.lowercase() }
    }
}
