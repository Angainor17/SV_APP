package org.geometerplus.zlibrary.core.util

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap

object BitmapUtil {
    @JvmOverloads
    @JvmStatic
    fun createBitmap(width: Int, height: Int, config: Bitmap.Config = Bitmap.Config.RGB_565): Bitmap {
        return try {
            createBitmap(width, height, config)
        } catch (e: OutOfMemoryError) {
            System.gc()
            System.gc()
            createBitmap(width, height, config)
        }
    }
}
