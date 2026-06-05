package com.github.axet.bookreader.widgets

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.widget.FrameLayout
import com.github.axet.bookreader.app.Plugin

/**
 * Layout с фоновым изображением (обоями).
 */
class WallpaperLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val bg = Plugin.View()

    override fun dispatchDraw(canvas: Canvas) {
        bg.drawWallpaper(canvas)
        super.dispatchDraw(canvas)
    }
}
