/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.zlibrary.ui.android.view.animation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import org.geometerplus.zlibrary.ui.android.view.ViewUtil
import kotlin.math.abs

class SlideAnimationProvider(bitmapManager: BitmapManager) : SimpleAnimationProvider(bitmapManager) {
    private val myDarkPaint = Paint()
    private val myPaint = Paint()

    private fun setDarkFilter(visible: Int, full: Int) {
        var darkColorLevel = 145 + 100 * abs(visible) / full
        val colorLevel = myColorLevel
        if (colorLevel != null) {
            darkColorLevel = darkColorLevel * colorLevel / 0xFF
        }
        ViewUtil.setColorLevel(myDarkPaint, darkColorLevel)
    }

    override fun setFilter() {
        ViewUtil.setColorLevel(myPaint, myColorLevel)
    }

    private fun drawShadowHorizontal(canvas: Canvas, left: Int, right: Int, dY: Int) {
        val orientation = if (dY > 0) {
            GradientDrawable.Orientation.BOTTOM_TOP
        } else {
            GradientDrawable.Orientation.TOP_BOTTOM
        }
        val colors = intArrayOf(0x46000000, 0x00000000)
        val gradient = GradientDrawable(orientation, colors)
        gradient.gradientType = GradientDrawable.LINEAR_GRADIENT
        gradient.setDither(true)
        if (dY > 0) {
            gradient.setBounds(left, dY - 16, right, dY)
        } else {
            gradient.setBounds(left, myHeight + dY, right, myHeight + dY + 16)
        }
        gradient.draw(canvas)
    }

    private fun drawShadowVertical(canvas: Canvas, top: Int, bottom: Int, dX: Int) {
        val orientation = if (dX > 0) {
            GradientDrawable.Orientation.RIGHT_LEFT
        } else {
            GradientDrawable.Orientation.LEFT_RIGHT
        }
        val colors = intArrayOf(0x46000000, 0x00000000)
        val gradient = GradientDrawable(orientation, colors)
        gradient.gradientType = GradientDrawable.LINEAR_GRADIENT
        gradient.setDither(true)
        if (dX > 0) {
            gradient.setBounds(dX - 16, top, dX, bottom)
        } else {
            gradient.setBounds(myWidth + dX, top, myWidth + dX + 16, bottom)
        }
        gradient.draw(canvas)
    }

    override fun drawInternal(canvas: Canvas) {
        if (myDirection!!.IsHorizontal) {
            val dX = myEndX - myStartX
            setDarkFilter(dX, myWidth)
            drawBitmapTo(canvas, 0, 0, myDarkPaint)
            drawBitmapFrom(canvas, dX, 0, myPaint)
            drawShadowVertical(canvas, 0, myHeight, dX)
        } else {
            val dY = myEndY - myStartY
            setDarkFilter(dY, myHeight)
            drawBitmapTo(canvas, 0, 0, myDarkPaint)
            drawBitmapFrom(canvas, 0, dY, myPaint)
            drawShadowHorizontal(canvas, 0, myWidth, dY)
        }
    }

    private fun drawBitmapInternal(
        canvas: Canvas,
        bm: Bitmap,
        left: Int,
        right: Int,
        height: Int,
        voffset: Int,
        paint: Paint,
    ) {
        canvas.drawBitmap(
            bm,
            Rect(left, 0, right, height),
            Rect(left, voffset, right, voffset + height),
            paint,
        )
    }

    override fun drawFooterBitmapInternal(canvas: Canvas, footerBitmap: Bitmap, voffset: Int) {
        if (myDirection!!.IsHorizontal) {
            val dX = myEndX - myStartX
            setDarkFilter(dX, myWidth)
            val h = footerBitmap.height
            if (dX > 0) {
                drawBitmapInternal(canvas, footerBitmap, 0, dX, h, voffset, myDarkPaint)
                drawBitmapInternal(canvas, footerBitmap, dX, myWidth, h, voffset, myPaint)
            } else {
                drawBitmapInternal(canvas, footerBitmap, myWidth + dX, myWidth, h, voffset, myDarkPaint)
                drawBitmapInternal(canvas, footerBitmap, 0, myWidth + dX, h, voffset, myPaint)
            }
            drawShadowVertical(canvas, voffset, voffset + h, dX)
        } else {
            canvas.drawBitmap(footerBitmap, 0f, voffset.toFloat(), myPaint)
        }
    }
}
