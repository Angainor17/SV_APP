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

package org.geometerplus.zlibrary.core.image

private val PalmImage8bitColormap: Array<IntArray> = arrayOf(
    intArrayOf(255, 255, 255), intArrayOf(255, 204, 255), intArrayOf(255, 153, 255), intArrayOf(255, 102, 255),
    intArrayOf(255, 51, 255), intArrayOf(255, 0, 255), intArrayOf(255, 255, 204), intArrayOf(255, 204, 204),
    intArrayOf(255, 153, 204), intArrayOf(255, 102, 204), intArrayOf(255, 51, 204), intArrayOf(255, 0, 204),
    intArrayOf(255, 255, 153), intArrayOf(255, 204, 153), intArrayOf(255, 153, 153), intArrayOf(255, 102, 153),
    intArrayOf(255, 51, 153), intArrayOf(255, 0, 153), intArrayOf(204, 255, 255), intArrayOf(204, 204, 255),
    intArrayOf(204, 153, 255), intArrayOf(204, 102, 255), intArrayOf(204, 51, 255), intArrayOf(204, 0, 255),
    intArrayOf(204, 255, 204), intArrayOf(204, 204, 204), intArrayOf(204, 153, 204), intArrayOf(204, 102, 204),
    intArrayOf(204, 51, 204), intArrayOf(204, 0, 204), intArrayOf(204, 255, 153), intArrayOf(204, 204, 153),
    intArrayOf(204, 153, 153), intArrayOf(204, 102, 153), intArrayOf(204, 51, 153), intArrayOf(204, 0, 153),
    intArrayOf(153, 255, 255), intArrayOf(153, 204, 255), intArrayOf(153, 153, 255), intArrayOf(153, 102, 255),
    intArrayOf(153, 51, 255), intArrayOf(153, 0, 255), intArrayOf(153, 255, 204), intArrayOf(153, 204, 204),
    intArrayOf(153, 153, 204), intArrayOf(153, 102, 204), intArrayOf(153, 51, 204), intArrayOf(153, 0, 204),
    intArrayOf(153, 255, 153), intArrayOf(153, 204, 153), intArrayOf(153, 153, 153), intArrayOf(153, 102, 153),
    intArrayOf(153, 51, 153), intArrayOf(153, 0, 153), intArrayOf(102, 255, 255), intArrayOf(102, 204, 255),
    intArrayOf(102, 153, 255), intArrayOf(102, 102, 255), intArrayOf(102, 51, 255), intArrayOf(102, 0, 255),
    intArrayOf(102, 255, 204), intArrayOf(102, 204, 204), intArrayOf(102, 153, 204), intArrayOf(102, 102, 204),
    intArrayOf(102, 51, 204), intArrayOf(102, 0, 204), intArrayOf(102, 255, 153), intArrayOf(102, 204, 153),
    intArrayOf(102, 153, 153), intArrayOf(102, 102, 153), intArrayOf(102, 51, 153), intArrayOf(102, 0, 153),
    intArrayOf(51, 255, 255), intArrayOf(51, 204, 255), intArrayOf(51, 153, 255), intArrayOf(51, 102, 255),
    intArrayOf(51, 51, 255), intArrayOf(51, 0, 255), intArrayOf(51, 255, 204), intArrayOf(51, 204, 204),
    intArrayOf(51, 153, 204), intArrayOf(51, 102, 204), intArrayOf(51, 51, 204), intArrayOf(51, 0, 204),
    intArrayOf(51, 255, 153), intArrayOf(51, 204, 153), intArrayOf(51, 153, 153), intArrayOf(51, 102, 153),
    intArrayOf(51, 51, 153), intArrayOf(51, 0, 153), intArrayOf(0, 255, 255), intArrayOf(0, 204, 255),
    intArrayOf(0, 153, 255), intArrayOf(0, 102, 255), intArrayOf(0, 51, 255), intArrayOf(0, 0, 255),
    intArrayOf(0, 255, 204), intArrayOf(0, 204, 204), intArrayOf(0, 153, 204), intArrayOf(0, 102, 204),
    intArrayOf(0, 51, 204), intArrayOf(0, 0, 204), intArrayOf(0, 255, 153), intArrayOf(0, 204, 153),
    intArrayOf(0, 153, 153), intArrayOf(0, 102, 153), intArrayOf(0, 51, 153), intArrayOf(0, 0, 153),
    intArrayOf(255, 255, 102), intArrayOf(255, 204, 102), intArrayOf(255, 153, 102), intArrayOf(255, 102, 102),
    intArrayOf(255, 51, 102), intArrayOf(255, 0, 102), intArrayOf(255, 255, 51), intArrayOf(255, 204, 51),
    intArrayOf(255, 153, 51), intArrayOf(255, 102, 51), intArrayOf(255, 51, 51), intArrayOf(255, 0, 51),
    intArrayOf(255, 255, 0), intArrayOf(255, 204, 0), intArrayOf(255, 153, 0), intArrayOf(255, 102, 0),
    intArrayOf(255, 51, 0), intArrayOf(255, 0, 0), intArrayOf(204, 255, 102), intArrayOf(204, 204, 102),
    intArrayOf(204, 153, 102), intArrayOf(204, 102, 102), intArrayOf(204, 51, 102), intArrayOf(204, 0, 102),
    intArrayOf(204, 255, 51), intArrayOf(204, 204, 51), intArrayOf(204, 153, 51), intArrayOf(204, 102, 51),
    intArrayOf(204, 51, 51), intArrayOf(204, 0, 51), intArrayOf(204, 255, 0), intArrayOf(204, 204, 0),
    intArrayOf(204, 153, 0), intArrayOf(204, 102, 0), intArrayOf(204, 51, 0), intArrayOf(204, 0, 0),
    intArrayOf(153, 255, 102), intArrayOf(153, 204, 102), intArrayOf(153, 153, 102), intArrayOf(153, 102, 102),
    intArrayOf(153, 51, 102), intArrayOf(153, 0, 102), intArrayOf(153, 255, 51), intArrayOf(153, 204, 51),
    intArrayOf(153, 153, 51), intArrayOf(153, 102, 51), intArrayOf(153, 51, 51), intArrayOf(153, 0, 51),
    intArrayOf(153, 255, 0), intArrayOf(153, 204, 0), intArrayOf(153, 153, 0), intArrayOf(153, 102, 0),
    intArrayOf(153, 51, 0), intArrayOf(153, 0, 0), intArrayOf(102, 255, 102), intArrayOf(102, 204, 102),
    intArrayOf(102, 153, 102), intArrayOf(102, 102, 102), intArrayOf(102, 51, 102), intArrayOf(102, 0, 102),
    intArrayOf(102, 255, 51), intArrayOf(102, 204, 51), intArrayOf(102, 153, 51), intArrayOf(102, 102, 51),
    intArrayOf(102, 51, 51), intArrayOf(102, 0, 51), intArrayOf(102, 255, 0), intArrayOf(102, 204, 0),
    intArrayOf(102, 153, 0), intArrayOf(102, 102, 0), intArrayOf(102, 51, 0), intArrayOf(102, 0, 0),
    intArrayOf(51, 255, 102), intArrayOf(51, 204, 102), intArrayOf(51, 153, 102), intArrayOf(51, 102, 102),
    intArrayOf(51, 51, 102), intArrayOf(51, 0, 102), intArrayOf(51, 255, 51), intArrayOf(51, 204, 51),
    intArrayOf(51, 153, 51), intArrayOf(51, 102, 51), intArrayOf(51, 51, 51), intArrayOf(51, 0, 51),
    intArrayOf(51, 255, 0), intArrayOf(51, 204, 0), intArrayOf(51, 153, 0), intArrayOf(51, 102, 0),
    intArrayOf(51, 51, 0), intArrayOf(51, 0, 0), intArrayOf(0, 255, 102), intArrayOf(0, 204, 102),
    intArrayOf(0, 153, 102), intArrayOf(0, 102, 102), intArrayOf(0, 51, 102), intArrayOf(0, 0, 102),
    intArrayOf(0, 255, 51), intArrayOf(0, 204, 51), intArrayOf(0, 153, 51), intArrayOf(0, 102, 51),
    intArrayOf(0, 51, 51), intArrayOf(0, 0, 51), intArrayOf(0, 255, 0), intArrayOf(0, 204, 0),
    intArrayOf(0, 153, 0), intArrayOf(0, 102, 0), intArrayOf(0, 51, 0), intArrayOf(17, 17, 17),
    intArrayOf(34, 34, 34), intArrayOf(68, 68, 68), intArrayOf(85, 85, 85), intArrayOf(119, 119, 119),
    intArrayOf(136, 136, 136), intArrayOf(170, 170, 170), intArrayOf(187, 187, 187), intArrayOf(221, 221, 221),
    intArrayOf(238, 238, 238), intArrayOf(192, 192, 192), intArrayOf(128, 0, 0), intArrayOf(128, 0, 128),
    intArrayOf(0, 128, 0), intArrayOf(0, 128, 128), intArrayOf(0, 0, 0), intArrayOf(0, 0, 0),
    intArrayOf(0, 0, 0), intArrayOf(0, 0, 0), intArrayOf(0, 0, 0), intArrayOf(0, 0, 0),
    intArrayOf(0, 0, 0), intArrayOf(0, 0, 0), intArrayOf(0, 0, 0), intArrayOf(0, 0, 0),
    intArrayOf(0, 0, 0), intArrayOf(0, 0, 0), intArrayOf(0, 0, 0), intArrayOf(0, 0, 0),
    intArrayOf(0, 0, 0), intArrayOf(0, 0, 0), intArrayOf(0, 0, 0), intArrayOf(0, 0, 0),
    intArrayOf(0, 0, 0), intArrayOf(0, 0, 0), intArrayOf(0, 0, 0), intArrayOf(0, 0, 0),
    intArrayOf(0, 0, 0), intArrayOf(0, 0, 0), intArrayOf(0, 0, 0), intArrayOf(0, 0, 0),
)

private fun uShort(byteData: ByteArray, offset: Int): Int =
    ((byteData[offset].toInt() and 0xFF) shl 8) + (byteData[offset + 1].toInt() and 0xFF)

abstract class ZLImageManager protected constructor() {
    init {
        ourInstance = this
    }

    abstract fun getImageData(image: ZLImage): ZLImageData

    protected class PalmImageHeader(byteData: ByteArray) {
        @JvmField
        val Width: Int

        @JvmField
        val Height: Int

        @JvmField
        val BytesPerRow: Int

        @JvmField
        val Flags: Int

        @JvmField
        val BitsPerPixel: Byte

        @JvmField
        val CompressionType: Byte

        init {
            Width = uShort(byteData, 0)
            Height = uShort(byteData, 2)
            BytesPerRow = uShort(byteData, 4)
            Flags = uShort(byteData, 6)
            BitsPerPixel = byteData[8]
            CompressionType = if ((Flags and 0x8000) != 0) byteData[13] else 0xFF.toByte()
        }
    }

    companion object {
        private var ourInstance: ZLImageManager? = null

        @JvmStatic
        fun Instance(): ZLImageManager = ourInstance!!
    }
}
