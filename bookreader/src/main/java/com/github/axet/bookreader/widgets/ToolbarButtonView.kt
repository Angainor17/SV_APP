package com.github.axet.bookreader.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.Keep
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageButton
import com.github.axet.androidlibrary.widgets.ThemeUtils
import com.github.axet.bookreader.R

/**
 * Базовая view для кнопки на панели инструментов.
 */
@Keep
open class ToolbarButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    lateinit var image: AppCompatImageButton
    lateinit var text: TextView

    init {
        create()
    }

    @SuppressLint("RestrictedApi")
    open fun create() {
        image = AppCompatImageButton(context)
        image.setColorFilter(ThemeUtils.getThemeColor(context, com.github.axet.androidlibrary.R.attr.colorAccent))
        addView(image, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))

        text = TextView(ContextThemeWrapper(context, R.style.toolbar_bottom_icon_text)) // отсутствуют отступы
        val lp = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        lp.bottomMargin = ThemeUtils.dp2px(context, 7f).toInt()
        addView(text, lp)
    }
}
