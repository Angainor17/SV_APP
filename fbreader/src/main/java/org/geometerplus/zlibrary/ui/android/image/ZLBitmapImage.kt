package org.geometerplus.zlibrary.ui.android.image

import android.graphics.Bitmap
import org.geometerplus.zlibrary.core.image.ZLImage

class ZLBitmapImage(private val myBitmap: Bitmap) : ZLImage {
    fun getBitmap(): Bitmap = myBitmap

    override fun getURI(): String = "bitmap image"
}
