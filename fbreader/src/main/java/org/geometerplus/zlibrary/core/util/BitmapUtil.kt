package org.geometerplus.zlibrary.core.util

import android.graphics.Bitmap
import  androidx.core.graphics.createBitmap as androidxCreateBitmap

object BitmapUtil {

    @JvmOverloads
    @JvmStatic
    fun createBitmap(
        width: Int,
        height: Int,
        config: Bitmap.Config = Bitmap.Config.RGB_565
    ): Bitmap {
        return try {
            androidxCreateBitmap(
                width = width,
                height = height,
                config = config
            )
        } catch (_: OutOfMemoryError) {
            System.gc()
            System.gc()
            androidxCreateBitmap(
                width = width,
                height = height,
                config = config
            )
        }
    }
}
