package org.geometerplus.zlibrary.core.util

import android.graphics.Bitmap

object BitmapUtil {
    @JvmOverloads
    @JvmStatic
    fun createBitmap(width: Int, height: Int, config: Bitmap.Config = Bitmap.Config.RGB_565): Bitmap {
        return try {
            Bitmap.createBitmap(width, height, config)
        } catch (e: OutOfMemoryError) {
            System.gc()
            System.gc()
            Bitmap.createBitmap(width, height, config)
        }
    }
}
