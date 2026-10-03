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

package org.geometerplus.zlibrary.ui.android.util

import android.graphics.Bitmap
import android.graphics.Color
import org.geometerplus.zlibrary.core.util.ZLColor

object ZLAndroidColorUtil {
    @JvmStatic
    fun rgba(color: ZLColor?, alpha: Int): Int =
        if (color != null) {
            Color.argb(alpha, color.Red.toInt(), color.Green.toInt(), color.Blue.toInt())
        } else {
            Color.argb(alpha, 0, 0, 0)
        }

    @JvmStatic
    fun rgb(color: ZLColor?): Int =
        if (color != null) {
            Color.rgb(color.Red.toInt(), color.Green.toInt(), color.Blue.toInt())
        } else {
            0
        }

    @JvmStatic
    fun getAverageColor(bitmap: Bitmap): ZLColor {
        val w = minOf(bitmap.width, 7)
        val h = minOf(bitmap.height, 7)
        var r = 0L
        var g = 0L
        var b = 0L
        for (i in 0 until w) {
            for (j in 0 until h) {
                val color = bitmap.getPixel(i, j)
                r += (color and 0xFF0000).toLong()
                g += (color and 0xFF00).toLong()
                b += (color and 0xFF).toLong()
            }
        }
        val n = w * h
        r /= n
        g /= n
        b /= n
        r = r shr 16
        g = g shr 8
        return ZLColor((r and 0xFFL).toInt(), (g and 0xFFL).toInt(), (b and 0xFFL).toInt())
    }
}
