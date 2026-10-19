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
import org.geometerplus.zlibrary.core.view.ZLPaintContext.ColorAdjustingMode
import org.geometerplus.zlibrary.core.view.ZLPaintContext.FillMode
import org.geometerplus.zlibrary.core.view.ZLPaintContext.ScalingType
import org.geometerplus.zlibrary.core.view.ZLPaintContext.Size

class DummyPaintContext : ZLPaintContext(
    object : SystemInfo {
        override fun tempDirectory(): String? = ""
        override fun networkCacheDirectory(): String? = ""
    }
) {
    override fun clear(wallpaperFile: ZLFile, mode: FillMode) {
    }

    override fun clear(color: ZLColor) {
    }

    override fun getBackgroundColor(): ZLColor = ZLColor(0, 0, 0)

    override fun setFontInternal(
        entries: List<FontEntry>,
        size: Int,
        bold: Boolean,
        italic: Boolean,
        underline: Boolean,
        strikeThrought: Boolean,
    ) {
    }

    override fun setTextColor(color: ZLColor) {
    }

    override fun setLineColor(color: ZLColor) {
    }

    override fun setLineWidth(width: Int) {
    }

    override fun setFillColor(color: ZLColor, alpha: Int) {
    }

    override fun getWidth(): Int = 1

    override fun getHeight(): Int = 1

    override fun getCharHeightInternal(chr: Char): Int = 1

    override fun getStringWidth(string: CharArray, offset: Int, length: Int): Int = 1

    override fun getSpaceWidthInternal(): Int = 1

    override fun getStringHeightInternal(): Int = 1

    override fun getDescentInternal(): Int = 1

    override fun drawString(x: Int, y: Int, string: CharArray, offset: Int, length: Int) {
    }

    override fun imageSize(image: ZLImageData, maxSize: Size, scaling: ScalingType): Size? = null

    override fun drawImage(
        x: Int,
        y: Int,
        image: ZLImageData,
        maxSize: Size,
        scaling: ScalingType,
        adjustingMode: ColorAdjustingMode,
    ) {
    }

    override fun drawLine(x0: Int, y0: Int, x1: Int, y1: Int) {
    }

    override fun fillRectangle(x0: Int, y0: Int, x1: Int, y1: Int) {
    }

    override fun fillPolygon(xs: IntArray, ys: IntArray) {
    }

    override fun drawPolygonalLine(xs: IntArray, ys: IntArray) {
    }

    override fun drawOutline(xs: IntArray, ys: IntArray) {
    }

    override fun fillCircle(x: Int, y: Int, radius: Int) {
    }
}
