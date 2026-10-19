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

package org.geometerplus.zlibrary.ui.android.view

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import org.geometerplus.zlibrary.core.view.ZLViewEnums
import org.geometerplus.zlibrary.ui.android.view.animation.BitmapManager

class BitmapManagerImpl(private val myWidget: ZLAndroidWidget) : BitmapManager {

    private val myBitmaps = arrayOfNulls<Bitmap>(SIZE)
    private val myIndexes = arrayOfNulls<ZLViewEnums.PageIndex>(SIZE)
    private var myWidth = 0
    private var myHeight = 0

    fun setSize(w: Int, h: Int) {
        if (myWidth != w || myHeight != h) {
            myWidth = w
            myHeight = h
            for (i in 0 until SIZE) {
                myBitmaps[i] = null
                myIndexes[i] = null
            }
            System.gc()
            System.gc()
            System.gc()
        }
    }

    override fun getBitmap(index: ZLViewEnums.PageIndex): Bitmap {
        for (i in 0 until SIZE) {
            if (index == myIndexes[i]) {
                return myBitmaps[i]!!
            }
        }
        val iIndex = getInternalIndex(index)
        myIndexes[iIndex] = index
        if (myBitmaps[iIndex] == null) {
            try {
                myBitmaps[iIndex] = Bitmap.createBitmap(myWidth, myHeight, Bitmap.Config.RGB_565)
            } catch (e: OutOfMemoryError) {
                System.gc()
                System.gc()
                myBitmaps[iIndex] = Bitmap.createBitmap(myWidth, myHeight, Bitmap.Config.RGB_565)
            }
        }
        myWidget.drawOnBitmap(myBitmaps[iIndex]!!, index)
        return myBitmaps[iIndex]!!
    }

    override fun drawBitmap(canvas: Canvas, x: Int, y: Int, index: ZLViewEnums.PageIndex, paint: Paint) {
        canvas.drawBitmap(getBitmap(index), x.toFloat(), y.toFloat(), paint)
    }

    private fun getInternalIndex(index: ZLViewEnums.PageIndex): Int {
        for (i in 0 until SIZE) {
            if (myIndexes[i] == null) {
                return i
            }
        }
        for (i in 0 until SIZE) {
            if (myIndexes[i] != ZLViewEnums.PageIndex.current) {
                return i
            }
        }
        throw RuntimeException("That's impossible")
    }

    fun reset() {
        for (i in 0 until SIZE) {
            myIndexes[i] = null
        }
    }

    fun shift(forward: Boolean) {
        for (i in 0 until SIZE) {
            if (myIndexes[i] == null) {
                continue
            }
            myIndexes[i] = if (forward) myIndexes[i]!!.getPrevious() else myIndexes[i]!!.getNext()
        }
    }

    companion object {
        private const val SIZE = 2
    }
}
