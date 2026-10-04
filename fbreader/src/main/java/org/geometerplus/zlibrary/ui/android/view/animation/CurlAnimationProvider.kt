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
import android.graphics.Path
import org.geometerplus.zlibrary.core.util.BitmapUtil
import org.geometerplus.zlibrary.core.view.ZLViewEnums
import org.geometerplus.zlibrary.ui.android.util.ZLAndroidColorUtil
import org.geometerplus.zlibrary.ui.android.view.ViewUtil
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class CurlAnimationProvider(bitmapManager: BitmapManager) : AnimationProvider(bitmapManager) {
    private val myFgPath = Path()
    private val myEdgePath = Path()
    private val myQuadPath = Path()
    private val myPaint = Paint()
    private val myBackPaint = Paint()
    private val myEdgePaint = Paint()
    private var mySpeedFactor: Float = 1f
    private var myBuffer: Bitmap? = null

    @Volatile
    private var myUseCanvasHack: Boolean = false

    init {
        myBackPaint.isAntiAlias = true
        myBackPaint.alpha = 0x40

        myEdgePaint.isAntiAlias = true
        myEdgePaint.style = Paint.Style.FILL
        myEdgePaint.setShadowLayer(15f, 0f, 0f, 0xC0000000.toInt())
    }

    override fun drawInternal(canvas: Canvas) {
        if (myUseCanvasHack) {
            // This is a hack that disables hardware acceleration
            //   1) for GLES20Canvas we got an UnsupportedOperationException in clipPath
            //   2) View.setLayerType(LAYER_TYPE_SOFTWARE) does not work properly in some cases
            if (myBuffer == null || myBuffer!!.width != myWidth || myBuffer!!.height != myHeight) {
                myBuffer = BitmapUtil.createBitmap(myWidth, myHeight, getBitmapTo().config!!)
            }
            val softCanvas = Canvas(myBuffer!!)
            drawInternalNoHack(softCanvas)
            canvas.drawBitmap(myBuffer!!, 0f, 0f, myPaint)
        } else {
            try {
                drawInternalNoHack(canvas)
            } catch (e: UnsupportedOperationException) {
                myUseCanvasHack = true
                drawInternal(canvas)
            }
        }
    }

    private fun drawInternalNoHack(canvas: Canvas) {
        drawBitmapTo(canvas, 0, 0, myPaint)

        val cornerX = if (myStartX > myWidth / 2) myWidth else 0
        val cornerY = if (myStartY > myHeight / 2) myHeight else 0
        val oppositeX = abs(myWidth - cornerX)
        val oppositeY = abs(myHeight - cornerY)
        val x: Int
        val y: Int
        if (myDirection!!.IsHorizontal) {
            x = myEndX
            if (getMode().Auto) {
                y = myEndY
            } else {
                y = if (cornerY == 0) {
                    maxOf(1, minOf(myHeight / 2, myEndY))
                } else {
                    maxOf(myHeight / 2, minOf(myHeight - 1, myEndY))
                }
            }
        } else {
            y = myEndY
            if (getMode().Auto) {
                x = myEndX
            } else {
                x = if (cornerX == 0) {
                    maxOf(1, minOf(myWidth / 2, myEndX))
                } else {
                    maxOf(myWidth / 2, minOf(myWidth - 1, myEndX))
                }
            }
        }
        val dX = maxOf(1, abs(x - cornerX))
        val dY = maxOf(1, abs(y - cornerY))

        val x1 = if (cornerX == 0) {
            (dY * dY / dX + dX) / 2
        } else {
            cornerX - (dY * dY / dX + dX) / 2
        }
        val y1 = if (cornerY == 0) {
            (dX * dX / dY + dY) / 2
        } else {
            cornerY - (dX * dX / dY + dY) / 2
        }

        var sX: Float
        var sY: Float
        run {
            val d1 = (x - x1).toFloat()
            val d2 = (y - cornerY).toFloat()
            sX = (sqrt((d1 * d1 + d2 * d2).toDouble()) / 2).toFloat()
            if (cornerX == 0) {
                sX = -sX
            }
        }
        run {
            val d1 = (x - cornerX).toFloat()
            val d2 = (y - y1).toFloat()
            sY = (sqrt((d1 * d1 + d2 * d2).toDouble()) / 2).toFloat()
            if (cornerY == 0) {
                sY = -sY
            }
        }

        myFgPath.rewind()
        myFgPath.moveTo(x.toFloat(), y.toFloat())
        myFgPath.lineTo(((x + cornerX) / 2).toFloat(), ((y + y1) / 2).toFloat())
        myFgPath.quadTo(cornerX.toFloat(), y1.toFloat(), cornerX.toFloat(), y1 - sY)
        if (abs(y1 - sY - cornerY) < myHeight.toFloat()) {
            myFgPath.lineTo(cornerX.toFloat(), oppositeY.toFloat())
        }
        myFgPath.lineTo(oppositeX.toFloat(), oppositeY.toFloat())
        if (abs(x1 - sX - cornerX) < myWidth.toFloat()) {
            myFgPath.lineTo(oppositeX.toFloat(), cornerY.toFloat())
        }
        myFgPath.lineTo(x1 - sX, cornerY.toFloat())
        myFgPath.quadTo(
            x1.toFloat(),
            cornerY.toFloat(),
            ((x + x1) / 2).toFloat(),
            ((y + cornerY) / 2).toFloat(),
        )

        myQuadPath.moveTo(x1 - sX, cornerY.toFloat())
        myQuadPath.quadTo(
            x1.toFloat(),
            cornerY.toFloat(),
            ((x + x1) / 2).toFloat(),
            ((y + cornerY) / 2).toFloat(),
        )
        canvas.drawPath(myQuadPath, myEdgePaint)
        myQuadPath.rewind()
        myQuadPath.moveTo(((x + cornerX) / 2).toFloat(), ((y + y1) / 2).toFloat())
        myQuadPath.quadTo(cornerX.toFloat(), y1.toFloat(), cornerX.toFloat(), y1 - sY)
        canvas.drawPath(myQuadPath, myEdgePaint)
        myQuadPath.rewind()

        canvas.save()
        canvas.clipPath(myFgPath)
        drawBitmapFrom(canvas, 0, 0, myPaint)
        canvas.restore()

        myEdgePaint.setColor(ZLAndroidColorUtil.rgb(ZLAndroidColorUtil.getAverageColor(getBitmapFrom())))

        myEdgePath.rewind()
        myEdgePath.moveTo(x.toFloat(), y.toFloat())
        myEdgePath.lineTo(((x + cornerX) / 2).toFloat(), ((y + y1) / 2).toFloat())
        myEdgePath.quadTo(
            ((x + 3 * cornerX) / 4).toFloat(),
            ((y + 3 * y1) / 4).toFloat(),
            ((x + 7 * cornerX) / 8).toFloat(),
            (y + 7 * y1 - 2 * sY) / 8,
        )
        myEdgePath.lineTo(
            (x + 7 * x1 - 2 * sX) / 8,
            ((y + 7 * cornerY) / 8).toFloat(),
        )
        myEdgePath.quadTo(
            ((x + 3 * x1) / 4).toFloat(),
            ((y + 3 * cornerY) / 4).toFloat(),
            ((x + x1) / 2).toFloat(),
            ((y + cornerY) / 2).toFloat(),
        )

        canvas.drawPath(myEdgePath, myEdgePaint)
    }

    override fun getPageToScrollTo(x: Int, y: Int): ZLViewEnums.PageIndex {
        val direction = myDirection ?: return ZLViewEnums.PageIndex.current

        return when (direction) {
            ZLViewEnums.Direction.leftToRight ->
                if (myStartX < myWidth / 2) ZLViewEnums.PageIndex.next else ZLViewEnums.PageIndex.previous
            ZLViewEnums.Direction.rightToLeft ->
                if (myStartX < myWidth / 2) ZLViewEnums.PageIndex.previous else ZLViewEnums.PageIndex.next
            ZLViewEnums.Direction.up ->
                if (myStartY < myHeight / 2) ZLViewEnums.PageIndex.previous else ZLViewEnums.PageIndex.next
            ZLViewEnums.Direction.down ->
                if (myStartY < myHeight / 2) ZLViewEnums.PageIndex.next else ZLViewEnums.PageIndex.previous
        }
    }

    override fun startAnimatedScrollingInternal(speed: Int) {
        mySpeedFactor = 2.0.pow(0.25 * speed).toFloat()
        mySpeed *= 1.5f
        doStep()
    }

    override fun setupAnimatedScrollingStart(x: Int?, y: Int?) {
        var newX = x
        var newY = y
        if (newX == null || newY == null) {
            if (myDirection!!.IsHorizontal) {
                newX = if (mySpeed < 0f) myWidth - 3 else 3
                newY = 1
            } else {
                newX = 1
                newY = if (mySpeed < 0f) myHeight - 3 else 3
            }
        } else {
            val cornerX = if (newX > myWidth / 2) myWidth else 0
            val cornerY = if (newY > myHeight / 2) myHeight else 0
            var deltaX = minOf(abs(newX - cornerX), myWidth / 5)
            var deltaY = minOf(abs(newY - cornerY), myHeight / 5)
            if (myDirection!!.IsHorizontal) {
                deltaY = minOf(deltaY, deltaX / 3)
            } else {
                deltaX = minOf(deltaX, deltaY / 3)
            }
            newX = abs(cornerX - deltaX)
            newY = abs(cornerY - deltaY)
        }
        myEndX = newX!!
        myStartX = newX
        myEndY = newY!!
        myStartY = newY
    }

    override fun doStep() {
        if (!getMode().Auto) {
            return
        }

        val speed = abs(mySpeed).toInt()
        mySpeed *= mySpeedFactor

        val cornerX = if (myStartX > myWidth / 2) myWidth else 0
        val cornerY = if (myStartY > myHeight / 2) myHeight else 0

        val boundX: Int
        val boundY: Int
        if (getMode() == Mode.AnimatedScrollingForward) {
            boundX = if (cornerX == 0) 2 * myWidth else -myWidth
            boundY = if (cornerY == 0) 2 * myHeight else -myHeight
        } else {
            boundX = cornerX
            boundY = cornerY
        }

        val deltaX = abs(myEndX - cornerX)
        val deltaY = abs(myEndY - cornerY)
        val speedX: Int
        val speedY: Int
        if (deltaX == 0 || deltaY == 0) {
            speedX = speed
            speedY = speed
        } else if (deltaX < deltaY) {
            speedX = speed
            speedY = speed * deltaY / deltaX
        } else {
            speedX = speed * deltaX / deltaY
            speedY = speed
        }

        val xSpeedIsPositive: Boolean
        val ySpeedIsPositive: Boolean
        if (getMode() == Mode.AnimatedScrollingForward) {
            xSpeedIsPositive = cornerX == 0
            ySpeedIsPositive = cornerY == 0
        } else {
            xSpeedIsPositive = cornerX != 0
            ySpeedIsPositive = cornerY != 0
        }

        if (xSpeedIsPositive) {
            myEndX += speedX
            if (myEndX >= boundX) {
                terminate()
            }
        } else {
            myEndX -= speedX
            if (myEndX <= boundX) {
                terminate()
            }
        }

        if (ySpeedIsPositive) {
            myEndY += speedY
            if (myEndY >= boundY) {
                terminate()
            }
        } else {
            myEndY -= speedY
            if (myEndY <= boundY) {
                terminate()
            }
        }
    }

    override fun drawFooterBitmapInternal(canvas: Canvas, footerBitmap: Bitmap, voffset: Int) {
        canvas.drawBitmap(footerBitmap, 0f, voffset.toFloat(), myPaint)
    }

    override fun setFilter() {
        ViewUtil.setColorLevel(myPaint, myColorLevel)
        ViewUtil.setColorLevel(myBackPaint, myColorLevel)
        ViewUtil.setColorLevel(myEdgePaint, myColorLevel)
    }
}
