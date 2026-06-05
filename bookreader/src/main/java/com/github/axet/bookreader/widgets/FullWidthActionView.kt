package com.github.axet.bookreader.widgets

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * View для полноширинного элемента действия в меню.
 */
class FullWidthActionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    constructor(context: Context, id: Int) : this(context) {
        val c = LayoutInflater.from(context)
        c.inflate(id, this)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val f = parent as ViewGroup // FrameLayout
        val m = f.parent as ViewGroup // NavigationMenuItemView
        val t = m.findViewById<View>(com.google.android.material.R.id.design_menu_item_text)
        t?.visibility = GONE
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
    }
}
