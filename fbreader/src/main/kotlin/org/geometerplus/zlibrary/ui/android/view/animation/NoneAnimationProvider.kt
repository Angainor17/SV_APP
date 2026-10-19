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
import org.geometerplus.zlibrary.core.view.ZLViewEnums
import org.geometerplus.zlibrary.ui.android.view.ViewUtil

class NoneAnimationProvider(bitmapManager: BitmapManager) : AnimationProvider(bitmapManager) {
    private val myPaint = Paint()

    override fun drawInternal(canvas: Canvas) {
        drawBitmapFrom(canvas, 0, 0, myPaint)
    }

    override fun doStep() {
        if (getMode().Auto) {
            terminate()
        }
    }

    override fun setupAnimatedScrollingStart(x: Int?, y: Int?) {
        if (myDirection!!.IsHorizontal) {
            myStartX = if (mySpeed < 0f) myWidth else 0
            myEndX = myWidth - myStartX
            myEndY = 0
            myStartY = 0
        } else {
            myEndX = 0
            myStartX = 0
            myStartY = if (mySpeed < 0f) myHeight else 0
            myEndY = myHeight - myStartY
        }
    }

    override fun startAnimatedScrollingInternal(speed: Int) {
    }

    override fun getPageToScrollTo(x: Int, y: Int): ZLViewEnums.PageIndex {
        val direction = myDirection ?: return ZLViewEnums.PageIndex.current

        return when (direction) {
            ZLViewEnums.Direction.rightToLeft ->
                if (myStartX < x) ZLViewEnums.PageIndex.previous else ZLViewEnums.PageIndex.next
            ZLViewEnums.Direction.leftToRight ->
                if (myStartX < x) ZLViewEnums.PageIndex.next else ZLViewEnums.PageIndex.previous
            ZLViewEnums.Direction.up ->
                if (myStartY < y) ZLViewEnums.PageIndex.previous else ZLViewEnums.PageIndex.next
            ZLViewEnums.Direction.down ->
                if (myStartY < y) ZLViewEnums.PageIndex.next else ZLViewEnums.PageIndex.previous
        }
    }

    override fun drawFooterBitmapInternal(canvas: Canvas, footerBitmap: Bitmap, voffset: Int) {
        canvas.drawBitmap(footerBitmap, 0f, voffset.toFloat(), myPaint)
    }

    override fun setFilter() {
        ViewUtil.setColorLevel(myPaint, myColorLevel)
    }
}
