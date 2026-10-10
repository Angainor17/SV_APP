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
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.CornerPathEffect
import android.graphics.EmbossMaskFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.Typeface
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.fonts.FontEntry
import org.geometerplus.zlibrary.core.image.ZLImageData
import org.geometerplus.zlibrary.core.options.ZLBooleanOption
import org.geometerplus.zlibrary.core.util.SystemInfo
import org.geometerplus.zlibrary.core.util.ZLColor
import org.geometerplus.zlibrary.core.view.ZLPaintContext
import org.geometerplus.zlibrary.ui.android.image.ZLAndroidImageData
import org.geometerplus.zlibrary.ui.android.util.ZLAndroidColorUtil

class ZLAndroidPaintContext(
    systemInfo: SystemInfo,
    private val myCanvas: Canvas,
    private val myGeometry: Geometry,
    private val myScrollbarWidth: Int,
) : ZLPaintContext(systemInfo) {

    private val myTextPaint = Paint()
    private val myLinePaint = Paint()
    private val myFillPaint = Paint()
    private val myOutlinePaint = Paint()
    private var myBackgroundColor = ZLColor(0, 0, 0)

    init {
        myTextPaint.isLinearText = false
        myTextPaint.isAntiAlias = AntiAliasOption.getValue()
        // DEV_KERN_TEXT_FLAG is deprecated and kerning is enabled by default
        myTextPaint.isDither = DitheringOption.getValue()
        myTextPaint.isSubpixelText = SubpixelOption.getValue()

        myLinePaint.style = Paint.Style.STROKE

        myFillPaint.isAntiAlias = AntiAliasOption.getValue()

        myOutlinePaint.isAntiAlias = true
        myOutlinePaint.isDither = true
        myOutlinePaint.strokeWidth = 4f
        myOutlinePaint.style = Paint.Style.STROKE
        myOutlinePaint.pathEffect = CornerPathEffect(5f)
        myOutlinePaint.maskFilter = EmbossMaskFilter(floatArrayOf(1f, 1f, 1f), 0.4f, 6f, 3.5f)
    }

    override fun clear(wallpaperFile: ZLFile, mode: FillMode) {
        if (!wallpaperFile.equals(ourWallpaperFile) || mode != ourFillMode) {
            ourWallpaperFile = wallpaperFile
            ourFillMode = mode
            ourWallpaper = null
            try {
                val fileBitmap = BitmapFactory.decodeStream(wallpaperFile.getInputStream())
                when (mode) {
                    FillMode.tileMirror -> {
                        val w = fileBitmap.width
                        val h = fileBitmap.height
                        val wallpaper = Bitmap.createBitmap(2 * w, 2 * h, fileBitmap.config!!)
                        val wallpaperCanvas = Canvas(wallpaper)
                        val wallpaperPaint = Paint()

                        val m = Matrix()
                        wallpaperCanvas.drawBitmap(fileBitmap, m, wallpaperPaint)
                        m.preScale(-1f, 1f)
                        m.postTranslate((2 * w).toFloat(), 0f)
                        wallpaperCanvas.drawBitmap(fileBitmap, m, wallpaperPaint)
                        m.preScale(1f, -1f)
                        m.postTranslate(0f, (2 * h).toFloat())
                        wallpaperCanvas.drawBitmap(fileBitmap, m, wallpaperPaint)
                        m.preScale(-1f, 1f)
                        m.postTranslate((-2 * w).toFloat(), 0f)
                        wallpaperCanvas.drawBitmap(fileBitmap, m, wallpaperPaint)
                        ourWallpaper = wallpaper
                    }

                    else -> {
                        ourWallpaper = fileBitmap
                    }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
        val wallpaper = ourWallpaper
        if (wallpaper != null) {
            myBackgroundColor = ZLAndroidColorUtil.getAverageColor(wallpaper)
            val w = wallpaper.width
            val h = wallpaper.height
            val g = myGeometry
            when (mode) {
                FillMode.fullscreen -> {
                    val m = Matrix()
                    m.preScale(1f * g.ScreenSize.Width / w, 1f * g.ScreenSize.Height / h)
                    m.postTranslate((-g.LeftMargin).toFloat(), (-g.TopMargin).toFloat())
                    myCanvas.drawBitmap(wallpaper, m, myFillPaint)
                }

                FillMode.stretch -> {
                    val m = Matrix()
                    val sw = 1f * g.ScreenSize.Width / w
                    val sh = 1f * g.ScreenSize.Height / h
                    val scale: Float
                    var dx = g.LeftMargin.toFloat()
                    var dy = g.TopMargin.toFloat()
                    if (sw < sh) {
                        scale = sh
                        dx += (scale * w - g.ScreenSize.Width) / 2
                    } else {
                        scale = sw
                        dy += (scale * h - g.ScreenSize.Height) / 2
                    }
                    m.preScale(scale, scale)
                    m.postTranslate(-dx, -dy)
                    myCanvas.drawBitmap(wallpaper, m, myFillPaint)
                }

                FillMode.tileVertically -> {
                    val m = Matrix()
                    val dx = g.LeftMargin
                    val dy = g.TopMargin % h
                    m.preScale(1f * g.ScreenSize.Width / w, 1f)
                    m.postTranslate((-dx).toFloat(), (-dy).toFloat())
                    var ch = g.AreaSize.Height + dy
                    while (ch > 0) {
                        myCanvas.drawBitmap(wallpaper, m, myFillPaint)
                        m.postTranslate(0f, h.toFloat())
                        ch -= h
                    }
                }

                FillMode.tileHorizontally -> {
                    val m = Matrix()
                    val dx = g.LeftMargin % w
                    val dy = g.TopMargin
                    m.preScale(1f, 1f * g.ScreenSize.Height / h)
                    m.postTranslate((-dx).toFloat(), (-dy).toFloat())
                    var cw = g.AreaSize.Width + dx
                    while (cw > 0) {
                        myCanvas.drawBitmap(wallpaper, m, myFillPaint)
                        m.postTranslate(w.toFloat(), 0f)
                        cw -= w
                    }
                }

                FillMode.tile,
                FillMode.tileMirror,
                -> {
                    val dx = g.LeftMargin % w
                    val dy = g.TopMargin % h
                    val fullw = g.AreaSize.Width + dx
                    val fullh = g.AreaSize.Height + dy
                    var cw = 0
                    while (cw < fullw) {
                        var ch = 0
                        while (ch < fullh) {
                            myCanvas.drawBitmap(wallpaper, (cw - dx).toFloat(), (ch - dy).toFloat(), myFillPaint)
                            ch += h
                        }
                        cw += w
                    }
                }
            }
        } else {
            clear(ZLColor(128, 128, 128))
        }
    }

    override fun clear(color: ZLColor) {
        myBackgroundColor = color
        myFillPaint.color = ZLAndroidColorUtil.rgb(color)
        myCanvas.drawRect(
            0f,
            0f,
            myGeometry.AreaSize.Width.toFloat(),
            myGeometry.AreaSize.Height.toFloat(),
            myFillPaint,
        )
    }

    override fun getBackgroundColor(): ZLColor = myBackgroundColor

    override fun fillPolygon(xs: IntArray, ys: IntArray) {
        val path = Path()
        val last = xs.size - 1
        path.moveTo(xs[last].toFloat(), ys[last].toFloat())
        for (i in 0..last) {
            path.lineTo(xs[i].toFloat(), ys[i].toFloat())
        }
        myCanvas.drawPath(path, myFillPaint)
    }

    override fun drawPolygonalLine(xs: IntArray, ys: IntArray) {
        val path = Path()
        val last = xs.size - 1
        path.moveTo(xs[last].toFloat(), ys[last].toFloat())
        for (i in 0..last) {
            path.lineTo(xs[i].toFloat(), ys[i].toFloat())
        }
        myCanvas.drawPath(path, myLinePaint)
    }

    override fun drawOutline(xs: IntArray, ys: IntArray) {
        val last = xs.size - 1
        var xStart = (xs[0] + xs[last]) / 2
        var yStart = (ys[0] + ys[last]) / 2
        var xEnd = xStart
        var yEnd = yStart
        if (xs[0] != xs[last]) {
            if (xs[0] > xs[last]) {
                xStart -= 5
                xEnd += 5
            } else {
                xStart += 5
                xEnd -= 5
            }
        } else {
            if (ys[0] > ys[last]) {
                yStart -= 5
                yEnd += 5
            } else {
                yStart += 5
                yEnd -= 5
            }
        }

        val path = Path()
        path.moveTo(xStart.toFloat(), yStart.toFloat())
        for (i in 0..last) {
            path.lineTo(xs[i].toFloat(), ys[i].toFloat())
        }
        path.lineTo(xEnd.toFloat(), yEnd.toFloat())
        myCanvas.drawPath(path, myOutlinePaint)
    }

    override fun setFontInternal(
        entries: List<FontEntry>,
        size: Int,
        bold: Boolean,
        italic: Boolean,
        underline: Boolean,
        strikeThrough: Boolean,
    ) {
        var typeface: Typeface? = null
        for (e in entries) {
            typeface = AndroidFontUtil.typeface(getSystemInfo(), e, bold, italic)
            if (typeface != null) {
                break
            }
        }
        myTextPaint.typeface = typeface
        myTextPaint.textSize = size.toFloat()
        myTextPaint.isUnderlineText = underline
        myTextPaint.isStrikeThruText = strikeThrough
    }

    override fun setTextColor(color: ZLColor) {
        myTextPaint.color = ZLAndroidColorUtil.rgb(color)
    }

    override fun setLineColor(color: ZLColor) {
        myLinePaint.color = ZLAndroidColorUtil.rgb(color)
        myOutlinePaint.color = ZLAndroidColorUtil.rgb(color)
    }

    override fun setLineWidth(width: Int) {
        myLinePaint.strokeWidth = width.toFloat()
    }

    override fun setFillColor(color: ZLColor, alpha: Int) {
        myFillPaint.color = ZLAndroidColorUtil.rgba(color, alpha)
    }

    override fun getWidth(): Int = myGeometry.AreaSize.Width - myScrollbarWidth

    override fun getHeight(): Int = myGeometry.AreaSize.Height

    override fun getStringWidth(string: CharArray, offset: Int, length: Int): Int {
        var containsSoftHyphen = false
        for (i in offset until offset + length) {
            if (string[i] == '­') {
                containsSoftHyphen = true
                break
            }
        }
        return if (!containsSoftHyphen) {
            (myTextPaint.measureText(String(string, offset, length)) + 0.5f).toInt()
        } else {
            val corrected = CharArray(length)
            var len = 0
            for (o in offset until offset + length) {
                val chr = string[o]
                if (chr != '­') {
                    corrected[len++] = chr
                }
            }
            (myTextPaint.measureText(corrected, 0, len) + 0.5f).toInt()
        }
    }

    override fun getSpaceWidthInternal(): Int =
        (myTextPaint.measureText(" ", 0, 1) + 0.5f).toInt()

    override fun getCharHeightInternal(chr: Char): Int {
        val r = Rect()
        val txt = charArrayOf(chr)
        myTextPaint.getTextBounds(txt, 0, 1, r)
        return r.bottom - r.top
    }

    override fun getStringHeightInternal(): Int = (myTextPaint.textSize + 0.5f).toInt()

    override fun getDescentInternal(): Int = (myTextPaint.descent() + 0.5f).toInt()

    override fun drawString(x: Int, y: Int, string: CharArray, offset: Int, length: Int) {
        var containsSoftHyphen = false
        for (i in offset until offset + length) {
            if (string[i] == '­') {
                containsSoftHyphen = true
                break
            }
        }
        if (!containsSoftHyphen) {
            myCanvas.drawText(string, offset, length, x.toFloat(), y.toFloat(), myTextPaint)
        } else {
            val corrected = CharArray(length)
            var len = 0
            for (o in offset until offset + length) {
                val chr = string[o]
                if (chr != '­') {
                    corrected[len++] = chr
                }
            }
            myCanvas.drawText(corrected, 0, len, x.toFloat(), y.toFloat(), myTextPaint)
        }
    }

    override fun imageSize(imageData: ZLImageData, maxSize: Size, scaling: ScalingType): Size? {
        val bitmap = (imageData as ZLAndroidImageData).getBitmap(maxSize, scaling)
        return if (bitmap != null && !bitmap.isRecycled) {
            Size(bitmap.width, bitmap.height)
        } else {
            null
        }
    }

    override fun drawImage(
        x: Int,
        y: Int,
        imageData: ZLImageData,
        maxSize: Size,
        scaling: ScalingType,
        adjustingMode: ColorAdjustingMode,
    ) {
        val bitmap = (imageData as ZLAndroidImageData).getBitmap(maxSize, scaling)
        if (bitmap != null && !bitmap.isRecycled) {
            when (adjustingMode) {
                ColorAdjustingMode.LIGHTEN_TO_BACKGROUND ->
                    myFillPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.LIGHTEN)

                ColorAdjustingMode.DARKEN_TO_BACKGROUND ->
                    myFillPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DARKEN)

                ColorAdjustingMode.NONE -> {}
            }
            myCanvas.drawBitmap(bitmap, x.toFloat(), (y - bitmap.height).toFloat(), myFillPaint)
            myFillPaint.xfermode = null
        }
    }

    override fun drawLine(x0: Int, y0: Int, x1: Int, y1: Int) {
        val canvas = myCanvas
        val paint = myLinePaint
        paint.isAntiAlias = false
        canvas.drawLine(x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat(), paint)
        canvas.drawPoint(x0.toFloat(), y0.toFloat(), paint)
        canvas.drawPoint(x1.toFloat(), y1.toFloat(), paint)
        paint.isAntiAlias = true
    }

    override fun fillRectangle(x0: Int, y0: Int, x1: Int, y1: Int) {
        var xx0 = x0
        var yy0 = y0
        var xx1 = x1
        var yy1 = y1
        if (xx1 < xx0) {
            val swap = xx1
            xx1 = xx0
            xx0 = swap
        }
        if (yy1 < yy0) {
            val swap = yy1
            yy1 = yy0
            yy0 = swap
        }
        myCanvas.drawRect(xx0.toFloat(), yy0.toFloat(), (xx1 + 1).toFloat(), (yy1 + 1).toFloat(), myFillPaint)
    }

    override fun fillCircle(x: Int, y: Int, radius: Int) {
        myCanvas.drawCircle(x.toFloat(), y.toFloat(), radius.toFloat(), myFillPaint)
    }

    class Geometry(
        screenWidth: Int,
        screenHeight: Int,
        width: Int,
        height: Int,
        @JvmField val LeftMargin: Int,
        @JvmField val TopMargin: Int,
    ) {
        @JvmField
        val ScreenSize: Size = Size(screenWidth, screenHeight)

        @JvmField
        val AreaSize: Size = Size(width, height)
    }

    companion object {
        @JvmField
        val AntiAliasOption = ZLBooleanOption("Fonts", "AntiAlias", true)

        @JvmField
        val DeviceKerningOption = ZLBooleanOption("Fonts", "DeviceKerning", false)

        @JvmField
        val DitheringOption = ZLBooleanOption("Fonts", "Dithering", false)

        @JvmField
        val SubpixelOption = ZLBooleanOption("Fonts", "Subpixel", false)

        private var ourWallpaperFile: ZLFile? = null
        private var ourWallpaper: Bitmap? = null
        private var ourFillMode: ZLPaintContext.FillMode? = null
    }
}
