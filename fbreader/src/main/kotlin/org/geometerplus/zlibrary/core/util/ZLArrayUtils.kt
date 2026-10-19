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

package org.geometerplus.zlibrary.core.util

object ZLArrayUtils {
    @JvmStatic
    fun createCopy(array: BooleanArray, dataSize: Int, newLength: Int): BooleanArray {
        val newArray = BooleanArray(newLength)
        if (dataSize > 0) {
            System.arraycopy(array, 0, newArray, 0, dataSize)
        }
        return newArray
    }

    @JvmStatic
    fun createCopy(array: ByteArray, dataSize: Int, newLength: Int): ByteArray {
        val newArray = ByteArray(newLength)
        if (dataSize > 0) {
            System.arraycopy(array, 0, newArray, 0, dataSize)
        }
        return newArray
    }

    @JvmStatic
    fun createCopy(array: CharArray, dataSize: Int, newLength: Int): CharArray {
        val newArray = CharArray(newLength)
        if (dataSize > 0) {
            System.arraycopy(array, 0, newArray, 0, dataSize)
        }
        return newArray
    }

    @JvmStatic
    fun createCopy(array: IntArray, dataSize: Int, newLength: Int): IntArray {
        val newArray = IntArray(newLength)
        if (dataSize > 0) {
            System.arraycopy(array, 0, newArray, 0, dataSize)
        }
        return newArray
    }

    @JvmStatic
    fun createCopy(array: Array<String?>, dataSize: Int, newLength: Int): Array<String?> {
        val newArray = arrayOfNulls<String>(newLength)
        if (dataSize > 0) {
            System.arraycopy(array, 0, newArray, 0, dataSize)
        }
        return newArray
    }
}
