package com.github.axet.bookreader.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Helper class for handling storage permissions across different Android versions.
 *
 * Android 13+ (API 33+) introduced granular media permissions:
 * - READ_MEDIA_IMAGES
 * - READ_MEDIA_VIDEO
 * - READ_MEDIA_AUDIO
 *
 * Android 14+ (API 34+) added:
 * - READ_MEDIA_VISUAL_USER_SELECTED (partial photo access)
 *
 * Android 11+ (API 30+) requires MANAGE_EXTERNAL_STORAGE for all files access.
 */
object PermissionHelper {

    /**
     * Storage permissions for read-only access.
     * Returns appropriate permissions based on Android version.
     */
    val STORAGE_PERMISSIONS_RO: Array<String>
        get() = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    /**
     * Storage permissions for read-write access.
     * On Android 10+, writing to media directories uses MediaStore API
     * which doesn't require WRITE_EXTERNAL_STORAGE for app-specific directories.
     */
    val STORAGE_PERMISSIONS_RW: Array<String>
        get() = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
            else -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

    /**
     * Check if all required storage permissions are granted.
     *
     * @param context Context for permission check
     * @param isWrite If true, checks read-write permissions; otherwise read-only
     * @return true if all required permissions are granted
     */
    fun hasStoragePermissions(context: Context, isWrite: Boolean = false): Boolean {
        val permissions = if (isWrite) STORAGE_PERMISSIONS_RW else STORAGE_PERMISSIONS_RO
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if app has MANAGE_EXTERNAL_STORAGE permission (Android 11+).
     * This permission is required for full file system access.
     *
     * @param context Context for permission check
     * @return true if app has all files access or is on pre-Android 11
     */
    fun hasAllFilesAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            hasStoragePermissions(context, true)
        }
    }

    /**
     * Request MANAGE_EXTERNAL_STORAGE permission by opening system settings.
     * Only works on Android 11+ (API 30+).
     *
     * @param context Context to start settings activity
     */
    fun requestAllFilesAccess(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback to generic all files access settings
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                context.startActivity(intent)
            }
        }
    }

    /**
     * Create a permission launcher using ActivityResultContracts.
     * This is the modern approach to request permissions.
     *
     * @param activity The activity to register the launcher with
     * @param onResult Callback with (allGranted, permissionResults)
     * @return ActivityResultLauncher for requesting permissions
     */
    fun createPermissionLauncher(
        activity: ComponentActivity,
        onResult: (Boolean, Map<String, Boolean>) -> Unit
    ): ActivityResultLauncher<Array<String>> {
        return activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            onResult(allGranted, permissions)
        }
    }

    /**
     * Check if should show permission rationale for a specific permission.
     *
     * @param activity The activity for checking rationale
     * @param permission The permission to check
     * @return true if rationale should be shown
     */
    fun shouldShowRationale(activity: ComponentActivity, permission: String): Boolean {
        return activity.shouldShowRequestPermissionRationale(permission)
    }

    /**
     * Get list of denied permissions from permission result.
     *
     * @param permissions Map of permission results
     * @return List of denied permission names
     */
    fun getDeniedPermissions(permissions: Map<String, Boolean>): List<String> {
        return permissions.filter { !it.value }.keys.toList()
    }

    /**
     * Check if any permission was permanently denied (user checked "Don't ask again").
     *
     * @param activity The activity for checking rationale
     * @param deniedPermissions List of denied permissions
     * @return true if any permission was permanently denied
     */
    fun hasPermanentlyDeniedPermissions(
        activity: ComponentActivity,
        deniedPermissions: List<String>
    ): Boolean {
        return deniedPermissions.any { !shouldShowRationale(activity, it) }
    }

    /**
     * Open app settings for user to grant permissions manually.
     *
     * @param context Context to start settings activity
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
