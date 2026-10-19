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

package org.geometerplus.zlibrary.core.view

import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.fonts.FontEntry
import org.geometerplus.zlibrary.core.image.ZLImageData
import org.geometerplus.zlibrary.core.util.SystemInfo
import org.geometerplus.zlibrary.core.util.ZLColor
import java.util.TreeMap

abstract class ZLPaintContext protected constructor(private val mySystemInfo: SystemInfo) {
    private var myResetFont = true
    private var myFontEntries: List<FontEntry>? = null
    private var myFontSize = 0
    private var myFontIsBold = false
    private var myFontIsItalic = false
    private var myFontIsUnderlined = false
    private var myFontIsStrikedThrough = false
    private var mySpaceWidth = -1
    private var myStringHeight = -1
    private val myCharHeights = TreeMap<Char, Int>()
    private var myDescent = -1

    protected fun getSystemInfo(): SystemInfo = mySystemInfo

    abstract fun clear(wallpaperFile: ZLFile, mode: FillMode)

    abstract fun clear(color: ZLColor)

    abstract fun getBackgroundColor(): ZLColor

    fun setFont(
        entries: List<FontEntry>?,
        size: Int,
        bold: Boolean,
        italic: Boolean,
        underline: Boolean,
        strikeThrough: Boolean,
    ) {
        if (entries != null && entries != myFontEntries) {
            myFontEntries = entries
            myResetFont = true
        }
        if (myFontSize != size) {
            myFontSize = size
            myResetFont = true
        }
        if (myFontIsBold != bold) {
            myFontIsBold = bold
            myResetFont = true
        }
        if (myFontIsItalic != italic) {
            myFontIsItalic = italic
            myResetFont = true
        }
        if (myFontIsUnderlined != underline) {
            myFontIsUnderlined = underline
            myResetFont = true
        }
        if (myFontIsStrikedThrough != strikeThrough) {
            myFontIsStrikedThrough = strikeThrough
            myResetFont = true
        }
        if (myResetFont) {
            myResetFont = false
            setFontInternal(myFontEntries ?: emptyList(), size, bold, italic, underline, strikeThrough)
            mySpaceWidth = -1
            myStringHeight = -1
            myDescent = -1
            myCharHeights.clear()
        }
    }

    protected abstract fun setFontInternal(
        entries: List<FontEntry>,
        size: Int,
        bold: Boolean,
        italic: Boolean,
        underline: Boolean,
        strikeThrough: Boolean,
    )

    abstract fun setTextColor(color: ZLColor)

    abstract fun setLineColor(color: ZLColor)

    abstract fun setLineWidth(width: Int)

    fun setFillColor(color: ZLColor) {
        setFillColor(color, 0xFF)
    }

    abstract fun setFillColor(color: ZLColor, alpha: Int)

    abstract fun getWidth(): Int

    abstract fun getHeight(): Int

    fun getStringWidth(string: String): Int =
        getStringWidth(string.toCharArray(), 0, string.length)

    abstract fun getStringWidth(string: CharArray, offset: Int, length: Int): Int

    fun getSpaceWidth(): Int {
        var spaceWidth = mySpaceWidth
        if (spaceWidth == -1) {
            spaceWidth = getSpaceWidthInternal()
            mySpaceWidth = spaceWidth
        }
        return spaceWidth
    }

    protected abstract fun getSpaceWidthInternal(): Int

    fun getStringHeight(): Int {
        var stringHeight = myStringHeight
        if (stringHeight == -1) {
            stringHeight = getStringHeightInternal()
            myStringHeight = stringHeight
        }
        return stringHeight
    }

    protected abstract fun getStringHeightInternal(): Int

    fun getCharHeight(chr: Char): Int {
        val cached = myCharHeights[chr]
        if (cached != null) {
            return cached
        }
        val height = getCharHeightInternal(chr)
        myCharHeights[chr] = height
        return height
    }

    protected abstract fun getCharHeightInternal(chr: Char): Int

    fun getDescent(): Int {
        var descent = myDescent
        if (descent == -1) {
            descent = getDescentInternal()
            myDescent = descent
        }
        return descent
    }

    protected abstract fun getDescentInternal(): Int

    fun drawString(x: Int, y: Int, string: String) {
        drawString(x, y, string.toCharArray(), 0, string.length)
    }

    abstract fun drawString(x: Int, y: Int, string: CharArray, offset: Int, length: Int)

    abstract fun imageSize(image: ZLImageData, maxSize: Size, scaling: ScalingType): Size?

    abstract fun drawImage(
        x: Int,
        y: Int,
        image: ZLImageData,
        maxSize: Size,
        scaling: ScalingType,
        adjustingMode: ColorAdjustingMode,
    )

    abstract fun drawLine(x0: Int, y0: Int, x1: Int, y1: Int)

    abstract fun fillRectangle(x0: Int, y0: Int, x1: Int, y1: Int)

    abstract fun drawPolygonalLine(xs: IntArray, ys: IntArray)

    abstract fun fillPolygon(xs: IntArray, ys: IntArray)

    abstract fun drawOutline(xs: IntArray, ys: IntArray)

    abstract fun fillCircle(x: Int, y: Int, radius: Int)

    @Suppress("EnumEntryName")
    enum class FillMode {
        tile,
        tileMirror,
        fullscreen,
        stretch,
        tileVertically,
        tileHorizontally,
    }

    enum class ScalingType {
        OriginalSize,
        IntegerCoefficient,
        FitMaximum,
    }

    enum class ColorAdjustingMode {
        NONE,
        DARKEN_TO_BACKGROUND,
        LIGHTEN_TO_BACKGROUND,
    }

    class Size(@JvmField val Width: Int, @JvmField val Height: Int) {
        override fun equals(other: Any?): Boolean {
            if (other === this) {
                return true
            }
            if (other !is Size) {
                return false
            }
            return Width == other.Width && Height == other.Height
        }

        override fun toString(): String = "ZLPaintContext.Size[${Width}x${Height}]"
    }
}
