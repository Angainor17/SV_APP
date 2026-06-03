package group.pals.android.lib.ui.filechooser.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * Utilities.
 */
public class Utils {

    /**
     * Checks if the app has <b>all</b> {@code permissions} granted.
     *
     * @param context     {@link Context}
     * @param permissions list of permission names.
     * @return {@code true} if the app has all {@code permissions} asked.
     */
    public static boolean hasPermissions(Context context, String... permissions) {
        for (String p : permissions)
            if (context.checkCallingOrSelfPermission(p) == PackageManager.PERMISSION_DENIED)
                return false;
        return true;
    }

    /**
     * Checks if the app has storage permissions granted.
     * This method handles the differences between Android versions:
     * - Android 13+ (API 33+): Uses READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_AUDIO
     * - Android 11+ (API 30+): Uses WRITE_EXTERNAL_STORAGE (limited) or MANAGE_EXTERNAL_STORAGE
     * - Below Android 11: Uses READ/WRITE_EXTERNAL_STORAGE
     *
     * @param context {@link Context}
     * @return {@code true} if the app has storage permissions.
     */
    public static boolean hasStoragePermissions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            return hasPermissions(context,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO);
        } else {
            // Below Android 13
            return hasPermissions(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }
}
