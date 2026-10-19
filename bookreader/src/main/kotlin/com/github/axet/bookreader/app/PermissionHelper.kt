package com.github.axet.bookreader.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
