package org.geometerplus.zlibrary.ui.android.view.animation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import org.geometerplus.zlibrary.core.view.ZLViewEnums

interface BitmapManager {
    fun getBitmap(index: ZLViewEnums.PageIndex): Bitmap?
    fun drawBitmap(canvas: Canvas, x: Int, y: Int, index: ZLViewEnums.PageIndex, paint: Paint)
}
