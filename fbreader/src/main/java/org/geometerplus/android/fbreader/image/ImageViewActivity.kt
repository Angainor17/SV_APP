/*
 * Copyright (C) 2010-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.android.fbreader.image

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import org.geometerplus.android.util.OrientationUtil
import org.geometerplus.zlibrary.core.image.ZLFileImage
import org.geometerplus.zlibrary.core.image.ZLImageManager
import org.geometerplus.zlibrary.core.util.ZLColor
import org.geometerplus.zlibrary.ui.android.image.ZLAndroidImageData
import org.geometerplus.zlibrary.ui.android.library.UncaughtExceptionHandler
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidLibrary
import org.geometerplus.zlibrary.ui.android.util.ZLAndroidColorUtil
import kotlin.math.sqrt

class ImageViewActivity : Activity() {

    private var myBitmap: Bitmap? = null
    private var myBgColor: ZLColor? = null

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        val library = ZLAndroidLibrary.Instance() as ZLAndroidLibrary
        val showStatusBar = library.ShowStatusBarOption.value
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            if (showStatusBar) 0 else WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        Thread.setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler(this))

        setContentView(ImageView())

        val intent = intent

        myBgColor = ZLColor(
            intent.getIntExtra(BACKGROUND_COLOR_KEY, ZLColor(127, 127, 127).intValue())
        )

        val url = intent.getStringExtra(URL_KEY)
        val prefix = ZLFileImage.SCHEME + "://"
        if (url != null && url.startsWith(prefix)) {
            val image = ZLFileImage.byUrlPath(url.substring(prefix.length))
            if (image == null) {
                // TODO: error message (?)
                finish()
            }
            try {
                val imageData = ZLImageManager.Instance().getImageData(image)
                myBitmap = (imageData as ZLAndroidImageData).fullSizeBitmap
            } catch (e: Exception) {
                // TODO: error message (?)
                e.printStackTrace()
                finish()
            }
        } else {
            // TODO: error message (?)
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        OrientationUtil.setOrientation(this, intent)
    }

    override fun onNewIntent(intent: Intent) {
        OrientationUtil.setOrientation(this, intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        myBitmap?.recycle()
        myBitmap = null
    }

    private inner class ImageView : View(this@ImageViewActivity) {
        private val myPaint = Paint()

        @Volatile
        private var myDx = 0
        @Volatile
        private var myDy = 0
        @Volatile
        private var myZoomFactor = 1.0f
        private var myMotionControl = false
        private var mySavedX = 0
        private var mySavedY = 0
        private var myStartPinchDistance2 = -1f
        private var myStartZoomFactor = 0f

        override fun onDraw(canvas: Canvas) {
            myPaint.color = ZLAndroidColorUtil.rgb(myBgColor!!)
            val w = width
            val h = height
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), myPaint)
            if (myBitmap == null || myBitmap!!.isRecycled) {
                return
            }

            val bw = (myBitmap!!.width * myZoomFactor).toInt()
            val bh = (myBitmap!!.height * myZoomFactor).toInt()

            val src = Rect(0, 0, (w / myZoomFactor).toInt(), (h / myZoomFactor).toInt())
            val dst = Rect(0, 0, w, h)
            if (bw <= w) {
                src.left = 0
                src.right = myBitmap!!.width
                dst.left = (w - bw) / 2
                dst.right = dst.left + bw
            } else {
                val bWidth = myBitmap!!.width
                val pWidth = (w / myZoomFactor).toInt()
                src.left = Math.min(bWidth - pWidth, Math.max((bWidth - pWidth) / 2 - myDx, 0))
                src.right += src.left
            }
            if (bh <= h) {
                src.top = 0
                src.bottom = myBitmap!!.height
                dst.top = (h - bh) / 2
                dst.bottom = dst.top + bh
            } else {
                val bHeight = myBitmap!!.height
                val pHeight = (h / myZoomFactor).toInt()
                src.top = Math.min(bHeight - pHeight, Math.max((bHeight - pHeight) / 2 - myDy, 0))
                src.bottom += src.top
            }
            canvas.drawBitmap(myBitmap!!, src, dst, myPaint)
        }

        private fun shift(dx: Int, dy: Int) {
            if (myBitmap == null || myBitmap!!.isRecycled) {
                return
            }

            val w = (width / myZoomFactor).toInt()
            val h = (height / myZoomFactor).toInt()
            val bw = myBitmap!!.width
            val bh = myBitmap!!.height

            val newDx: Int
            val newDy: Int

            if (w < bw) {
                val delta = (bw - w) / 2
                newDx = Math.max(-delta, Math.min(delta, myDx + dx))
            } else {
                newDx = myDx
            }
            if (h < bh) {
                val delta = (bh - h) / 2
                newDy = Math.max(-delta, Math.min(delta, myDy + dy))
            } else {
                newDy = myDy
            }

            if (newDx != myDx || newDy != myDy) {
                myDx = newDx
                myDy = newDy
                postInvalidate()
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            return when (event.pointerCount) {
                1 -> onSingleTouchEvent(event)
                2 -> onDoubleTouchEvent(event)
                else -> false
            }
        }

        private fun onSingleTouchEvent(event: MotionEvent): Boolean {
            val x = event.x.toInt()
            val y = event.y.toInt()

            when (event.action) {
                MotionEvent.ACTION_UP -> myMotionControl = false
                MotionEvent.ACTION_DOWN -> {
                    myMotionControl = true
                    mySavedX = x
                    mySavedY = y
                }
                MotionEvent.ACTION_MOVE -> {
                    if (myMotionControl) {
                        shift(
                            ((x - mySavedX) / myZoomFactor).toInt(),
                            ((y - mySavedY) / myZoomFactor).toInt()
                        )
                    }
                    myMotionControl = true
                    mySavedX = x
                    mySavedY = y
                }
            }
            return true
        }

        private fun onDoubleTouchEvent(event: MotionEvent): Boolean {
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_POINTER_UP -> myStartPinchDistance2 = -1f
                MotionEvent.ACTION_POINTER_DOWN -> {
                    val diffX = event.getX(0) - event.getX(1)
                    val diffY = event.getY(0) - event.getY(1)
                    myStartPinchDistance2 = Math.max(diffX * diffX + diffY * diffY, 10f)
                    myStartZoomFactor = myZoomFactor
                }
                MotionEvent.ACTION_MOVE -> {
                    val diffX = event.getX(0) - event.getX(1)
                    val diffY = event.getY(0) - event.getY(1)
                    val distance2 = Math.max(diffX * diffX + diffY * diffY, 10f)
                    if (myStartPinchDistance2 < 0) {
                        myStartPinchDistance2 = distance2
                        myStartZoomFactor = myZoomFactor
                    } else {
                        myZoomFactor = myStartZoomFactor * sqrt(distance2 / myStartPinchDistance2)
                        postInvalidate()
                    }
                }
            }
            return true
        }
    }

    companion object {
        const val URL_KEY = "fbreader.imageview.url"
        const val BACKGROUND_COLOR_KEY = "fbreader.imageview.background"
    }
}
