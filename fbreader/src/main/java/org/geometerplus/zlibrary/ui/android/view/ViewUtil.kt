package org.geometerplus.zlibrary.ui.android.view

import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter

object ViewUtil {
    @JvmStatic
    fun setColorLevel(paint: Paint, level: Int?) {
        if (level != null) {
            paint.colorFilter = PorterDuffColorFilter(
                Color.rgb(level, level, level),
                PorterDuff.Mode.MULTIPLY
            )
        } else {
            paint.colorFilter = null
        }
    }
}
