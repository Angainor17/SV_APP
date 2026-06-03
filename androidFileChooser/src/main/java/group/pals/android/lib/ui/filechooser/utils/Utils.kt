package group.pals.android.lib.ui.filechooser.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Утилиты.
 */
object Utils {

    /**
     * Проверяет, есть ли у приложения **все** запрошенные разрешения.
     *
     * @param context [Context]
     * @param permissions список имён разрешений.
     * @return `true`, если у приложения есть все запрошенные разрешения.
     */
    fun hasPermissions(context: Context, vararg permissions: String): Boolean {
        for (p in permissions) {
            if (context.checkCallingOrSelfPermission(p) == PackageManager.PERMISSION_DENIED) {
                return false
            }
        }
        return true
    }

    /**
     * Проверяет, есть ли у приложения разрешения на работу с хранилищем.
     * Этот метод обрабатывает различия между версиями Android:
     * - Android 13+ (API 33+): Использует READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_AUDIO
     * - Android 11+ (API 30+): Использует WRITE_EXTERNAL_STORAGE (ограничено) или MANAGE_EXTERNAL_STORAGE
     * - Ниже Android 11: Использует READ/WRITE_EXTERNAL_STORAGE
     *
     * @param context [Context]
     * @return `true`, если у приложения есть разрешения на хранилище.
     */
    fun hasStoragePermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            hasPermissions(
                context,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            // Ниже Android 13
            hasPermissions(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
}
