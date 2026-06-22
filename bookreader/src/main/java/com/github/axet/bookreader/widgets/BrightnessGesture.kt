package com.github.axet.bookreader.widgets

import android.view.MotionEvent
import com.github.axet.androidlibrary.widgets.ThemeUtils

open class BrightnessGesture(private val fb: FBReaderView) {
    private var myStartY: Int = 0
    private var myIsBrightnessAdjustmentInProgress: Boolean = false
    private var myStartBrightness: Int = 0
    var areaWidth: Int = ThemeUtils.dp2px(fb.context, 36f).toInt()
    private var myColorLevel: Int? = null

    fun onTouchEvent(e: MotionEvent): Boolean {
        val x = e.x.toInt()
        val y = e.y.toInt()
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (fb.app!!.MiscOptions.AllowScreenBrightnessAdjustment.value && x < areaWidth) {
                    myIsBrightnessAdjustmentInProgress = true
                    myStartY = y
                    myStartBrightness = fb.widget!!.screenBrightness
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (myIsBrightnessAdjustmentInProgress) {
                    if (x >= areaWidth * 2) {
                        myIsBrightnessAdjustmentInProgress = false
                        return false
                    } else {
                        val delta = ((myStartBrightness + 30) * (myStartY - y) / fb.height).toFloat()
                        fb.widget!!.setScreenBrightness((myStartBrightness + delta).toInt())
                        return true
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (myIsBrightnessAdjustmentInProgress) {
                    myIsBrightnessAdjustmentInProgress = false
                    return true
                }
            }
        }
        return false
    }

    fun setScreenBrightness(percent: Int): Int? {
        var percent = percent
        if (percent < 1)
            percent = 1
        else if (percent > 100)
            percent = 100

        val level: Float
        val oldColorLevel = myColorLevel
        if (percent >= 25) {
            level = .01f + (percent - 25) * .99f / 75
            myColorLevel = null
        } else {
            level = .01f
            myColorLevel = 0x60 + ((0xFF - 0x60) * kotlin.math.max(percent, 0) / 25)
        }

        val attrs = fb.w!!.attributes
        attrs.screenBrightness = level
        fb.w!!.attributes = attrs

        return myColorLevel
    }

    fun getScreenBrightness(): Int {
        if (myColorLevel != null)
            return (myColorLevel!! - 0x60) * 25 / (0xFF - 0x60)

        var level = fb.w!!.attributes.screenBrightness
        level = if (level >= 0) level else .5f

        return 25 + ((level - .01f) * 75 / .99f).toInt()
    }
}
