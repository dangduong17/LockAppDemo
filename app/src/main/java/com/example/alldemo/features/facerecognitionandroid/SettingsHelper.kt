package com.example.alldemo.features.facerecognitionandroid

import android.content.Context
import androidx.camera.core.CameraSelector

object SettingsHelper {
    private const val PREF_NAME = "FaceSDKPrefs"
    const val DEFAULT_CAMERA_LENS = "front"
    const val DEFAULT_LIVENESS_THRESHOLD = "0.7"
    const val DEFAULT_IDENTIFY_THRESHOLD = "0.8"
    const val DEFAULT_LIVENESS_LEVEL = "0"

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    @JvmStatic
    fun getLivenessThreshold(context: Context): Float {
        return (getPrefs(context).getString("liveness_threshold", DEFAULT_LIVENESS_THRESHOLD)
            ?: DEFAULT_LIVENESS_THRESHOLD).toFloatOrNull() ?: 0.7f
    }

    @JvmStatic
    fun setLivenessThreshold(context: Context, value: Float) {
        getPrefs(context).edit().putString("liveness_threshold", value.toString()).apply()
    }

    @JvmStatic
    fun getIdentifyThreshold(context: Context): Float {
        return (getPrefs(context).getString("identify_threshold", DEFAULT_IDENTIFY_THRESHOLD)
            ?: DEFAULT_IDENTIFY_THRESHOLD).toFloatOrNull() ?: 0.8f
    }

    @JvmStatic
    fun setIdentifyThreshold(context: Context, value: Float) {
        getPrefs(context).edit().putString("identify_threshold", value.toString()).apply()
    }

    @JvmStatic
    fun getCameraLens(context: Context): Int {
        val cameraLens = getPrefs(context).getString("camera_lens", DEFAULT_CAMERA_LENS)
        return if (cameraLens == "back") {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
    }

    @JvmStatic
    fun setCameraLens(context: Context, lens: String) {
        getPrefs(context).edit().putString("camera_lens", lens).apply()
    }

    @JvmStatic
    fun getLivenessLevel(context: Context): Int {
        val livenessLevel = getPrefs(context).getString("liveness_level", DEFAULT_LIVENESS_LEVEL)
        return if (livenessLevel == "0") 0 else 1
    }

    @JvmStatic
    fun setLivenessLevel(context: Context, level: Int) {
        getPrefs(context).edit().putString("liveness_level", level.toString()).apply()
    }
}
