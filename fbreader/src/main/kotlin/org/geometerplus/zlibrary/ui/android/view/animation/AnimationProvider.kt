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
import org.geometerplus.zlibrary.core.library.ZLibrary
import org.geometerplus.zlibrary.core.view.ZLViewEnums
import java.util.LinkedList
import kotlin.math.abs
import kotlin.math.sqrt

abstract class AnimationProvider protected constructor(
    private val myBitmapManager: BitmapManager,
) {
    private val myDrawInfos = LinkedList<DrawInfo>()
    protected var myStartX: Int = 0
    protected var myStartY: Int = 0
    protected var myEndX: Int = 0
    protected var myEndY: Int = 0
    protected var myDirection: ZLViewEnums.Direction? = null
    protected var mySpeed: Float = 0f
    protected var myWidth: Int = 0
    protected var myHeight: Int = 0
    protected var myColorLevel: Int? = null
    private var myMode: Mode = Mode.NoScrolling

    fun getMode(): Mode = myMode

    fun terminate() {
        myMode = Mode.NoScrolling
        mySpeed = 0f
        myDrawInfos.clear()
    }

    fun startManualScrolling(x: Int, y: Int) {
        if (!myMode.Auto) {
            myMode = Mode.PreManualScrolling
            myEndX = x
            myStartX = x
            myEndY = y
            myStartY = y
        }
    }

    private fun detectManualMode(): Mode {
        val dX = abs(myStartX - myEndX)
        val dY = abs(myStartY - myEndY)
        val direction = myDirection!!
        if (direction.IsHorizontal) {
            if (dY > ZLibrary.Instance().getDisplayDPI() / 2 && dY > dX) {
                return Mode.NoScrolling
            } else if (dX > ZLibrary.Instance().getDisplayDPI() / 10) {
                return Mode.ManualScrolling
            }
        } else {
            if (dX > ZLibrary.Instance().getDisplayDPI() / 2 && dX > dY) {
                return Mode.NoScrolling
            } else if (dY > ZLibrary.Instance().getDisplayDPI() / 10) {
                return Mode.ManualScrolling
            }
        }
        return Mode.PreManualScrolling
    }

    fun scrollTo(x: Int, y: Int) {
        when (myMode) {
            Mode.ManualScrolling -> {
                myEndX = x
                myEndY = y
            }
            Mode.PreManualScrolling -> {
                myEndX = x
                myEndY = y
                myMode = detectManualMode()
            }
            else -> {
            }
        }
    }

    fun startAnimatedScrolling(x: Int, y: Int, speed: Int) {
        if (myMode != Mode.ManualScrolling) {
            return
        }

        if (getPageToScrollTo(x, y) == ZLViewEnums.PageIndex.current) {
            return
        }

        val direction = myDirection!!
        val dpi = ZLibrary.Instance().getDisplayDPI()
        val diff = if (direction.IsHorizontal) x - myStartX else y - myStartY
        val minDiff = if (direction.IsHorizontal) {
            if (myWidth > myHeight) myWidth / 4 else myWidth / 3
        } else {
            if (myHeight > myWidth) myHeight / 4 else myHeight / 3
        }
        var forward = abs(diff) > minOf(minDiff, dpi / 2)

        myMode = if (forward) Mode.AnimatedScrollingForward else Mode.AnimatedScrollingBackward

        var velocity = 15f
        if (myDrawInfos.size > 1) {
            var duration = 0
            for (info in myDrawInfos) {
                duration += info.duration
            }
            duration /= myDrawInfos.size
            val time = System.currentTimeMillis()
            myDrawInfos.add(DrawInfo(x, y, time, time + duration))
            velocity = 0f
            for (i in 1 until myDrawInfos.size) {
                val info0 = myDrawInfos[i - 1]
                val info1 = myDrawInfos[i]
                val dX = (info0.x - info1.x).toFloat()
                val dY = (info0.y - info1.y).toFloat()
                velocity += (sqrt((dX * dX + dY * dY).toDouble()) / maxOf(1L, info1.start - info0.start)).toFloat()
            }
            velocity /= myDrawInfos.size - 1
            velocity *= duration
            velocity = minOf(100f, maxOf(15f, velocity))
        }
        myDrawInfos.clear()

        if (getPageToScrollTo() == ZLViewEnums.PageIndex.previous) {
            forward = !forward
        }

        mySpeed = when (direction) {
            ZLViewEnums.Direction.up,
            ZLViewEnums.Direction.rightToLeft,
            -> if (forward) -velocity else velocity

            ZLViewEnums.Direction.leftToRight,
            ZLViewEnums.Direction.down,
            -> if (forward) velocity else -velocity
        }

        startAnimatedScrollingInternal(speed)
    }

    fun startAnimatedScrolling(pageIndex: ZLViewEnums.PageIndex, x: Int?, y: Int?, speed: Int) {
        if (myMode.Auto) {
            return
        }

        terminate()
        myMode = Mode.AnimatedScrollingForward

        mySpeed = when (myDirection!!) {
            ZLViewEnums.Direction.up,
            ZLViewEnums.Direction.rightToLeft,
            -> if (pageIndex == ZLViewEnums.PageIndex.next) -15f else 15f

            ZLViewEnums.Direction.leftToRight,
            ZLViewEnums.Direction.down,
            -> if (pageIndex == ZLViewEnums.PageIndex.next) 15f else -15f
        }
        setupAnimatedScrollingStart(x, y)
        startAnimatedScrollingInternal(speed)
    }

    protected abstract fun startAnimatedScrollingInternal(speed: Int)

    protected abstract fun setupAnimatedScrollingStart(x: Int?, y: Int?)

    fun inProgress(): Boolean {
        return when (myMode) {
            Mode.NoScrolling,
            Mode.PreManualScrolling,
            -> false

            else -> true
        }
    }

    protected fun getScrollingShift(): Int =
        if (myDirection!!.IsHorizontal) myEndX - myStartX else myEndY - myStartY

    fun setup(direction: ZLViewEnums.Direction, width: Int, height: Int, colorLevel: Int?) {
        myDirection = direction
        myWidth = width
        myHeight = height
        myColorLevel = colorLevel
    }

    abstract fun doStep()

    fun getScrolledPercent(): Int {
        val full = if (myDirection!!.IsHorizontal) myWidth else myHeight
        val shift = abs(getScrollingShift())
        return 100 * shift / full
    }

    fun draw(canvas: Canvas) {
        val start = System.currentTimeMillis()
        setFilter()
        drawInternal(canvas)
        myDrawInfos.add(DrawInfo(myEndX, myEndY, start, System.currentTimeMillis()))
        if (myDrawInfos.size > 3) {
            myDrawInfos.removeAt(0)
        }
    }

    fun drawFooterBitmap(canvas: Canvas, footerBitmap: Bitmap, voffset: Int) {
        setFilter()
        drawFooterBitmapInternal(canvas, footerBitmap, voffset)
    }

    protected abstract fun setFilter()

    protected abstract fun drawInternal(canvas: Canvas)

    protected abstract fun drawFooterBitmapInternal(canvas: Canvas, footerBitmap: Bitmap, voffset: Int)

    abstract fun getPageToScrollTo(x: Int, y: Int): ZLViewEnums.PageIndex

    fun getPageToScrollTo(): ZLViewEnums.PageIndex = getPageToScrollTo(myEndX, myEndY)

    protected fun getBitmapFrom(): Bitmap =
        myBitmapManager.getBitmap(ZLViewEnums.PageIndex.current)!!

    protected fun getBitmapTo(): Bitmap =
        myBitmapManager.getBitmap(getPageToScrollTo())!!

    protected fun drawBitmapFrom(canvas: Canvas, x: Int, y: Int, paint: Paint) {
        myBitmapManager.drawBitmap(canvas, x, y, ZLViewEnums.PageIndex.current, paint)
    }

    protected fun drawBitmapTo(canvas: Canvas, x: Int, y: Int, paint: Paint) {
        myBitmapManager.drawBitmap(canvas, x, y, getPageToScrollTo(), paint)
    }

    enum class Mode(@JvmField val Auto: Boolean) {
        NoScrolling(false),
        PreManualScrolling(false),
        ManualScrolling(false),
        AnimatedScrollingForward(true),
        AnimatedScrollingBackward(true),
    }

    private class DrawInfo(x: Int, y: Int, start: Long, finish: Long) {
        val x: Int = x
        val y: Int = y
        val start: Long = start
        val duration: Int = (finish - start).toInt()
    }
}
