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
import android.graphics.Color
import android.graphics.Paint
import org.geometerplus.zlibrary.ui.android.view.ViewUtil

class ShiftAnimationProvider(bitmapManager: BitmapManager) : SimpleAnimationProvider(bitmapManager) {
    private val myPaint = Paint()

    init {
        myPaint.setColor(Color.rgb(127, 127, 127))
    }

    override fun drawInternal(canvas: Canvas) {
        if (myDirection!!.IsHorizontal) {
            val dX = myEndX - myStartX
            drawBitmapTo(canvas, if (dX > 0) dX - myWidth else dX + myWidth, 0, myPaint)
            drawBitmapFrom(canvas, dX, 0, myPaint)
            if (dX > 0 && dX < myWidth) {
                canvas.drawLine(dX.toFloat(), 0f, dX.toFloat(), (myHeight + 1).toFloat(), myPaint)
            } else if (dX < 0 && dX > -myWidth) {
                canvas.drawLine(
                    (dX + myWidth).toFloat(),
                    0f,
                    (dX + myWidth).toFloat(),
                    (myHeight + 1).toFloat(),
                    myPaint,
                )
            }
        } else {
            val dY = myEndY - myStartY
            drawBitmapTo(canvas, 0, if (dY > 0) dY - myHeight else dY + myHeight, myPaint)
            drawBitmapFrom(canvas, 0, dY, myPaint)
            if (dY > 0 && dY < myHeight) {
                canvas.drawLine(0f, dY.toFloat(), (myWidth + 1).toFloat(), dY.toFloat(), myPaint)
            } else if (dY < 0 && dY > -myHeight) {
                canvas.drawLine(
                    0f,
                    (dY + myHeight).toFloat(),
                    (myWidth + 1).toFloat(),
                    (dY + myHeight).toFloat(),
                    myPaint,
                )
            }
        }
    }

    override fun drawFooterBitmapInternal(canvas: Canvas, footerBitmap: Bitmap, voffset: Int) {
        canvas.drawBitmap(footerBitmap, 0f, voffset.toFloat(), myPaint)
        if (myDirection!!.IsHorizontal) {
            val dX = myEndX - myStartX
            if (dX > 0 && dX < myWidth) {
                canvas.drawLine(
                    dX.toFloat(),
                    voffset.toFloat(),
                    dX.toFloat(),
                    (voffset + footerBitmap.height).toFloat(),
                    myPaint,
                )
            } else if (dX < 0 && dX > -myWidth) {
                canvas.drawLine(
                    (dX + myWidth).toFloat(),
                    voffset.toFloat(),
                    (dX + myWidth).toFloat(),
                    (voffset + footerBitmap.height).toFloat(),
                    myPaint,
                )
            }
        }
    }

    override fun setFilter() {
        ViewUtil.setColorLevel(myPaint, myColorLevel)
    }
}
