package com.github.axet.bookreader.widgets

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.Keep
import com.github.axet.androidlibrary.widgets.ThemeUtils
import com.github.axet.bookreader.R

/**
 * View для отображения кнопки RTL на панели инструментов.
 */
@Keep
class ToolbarRTLView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ToolbarButtonView(context, attrs, defStyleAttr, defStyleRes) {

    override fun create() {
        super.create()
        image.isClickable = false
        image.isFocusable = false
        image.setImageResource(R.drawable.ic_gesture_black_24dp)
        image.setColorFilter(ThemeUtils.getColor(context, android.R.color.white))
        image.background = null
        text.text = "RTL"
    }
}
