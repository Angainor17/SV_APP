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

import java.io.IOException
import java.io.InputStream

open class InputStreamWithOffset(
    private val myDecoratedStream: InputStream,
) : InputStream() {
    private var myOffset: Int = 0

    @Throws(IOException::class)
    override fun available(): Int = myDecoratedStream.available()

    @Throws(IOException::class)
    override fun skip(n: Long): Long {
        var shift = myDecoratedStream.skip(n)
        if (shift > 0) {
            myOffset += shift.toInt()
        }
        while (shift < n && read() != -1) {
            ++shift
        }
        return shift
    }

    // does not call virtual methods
    @Throws(IOException::class)
    protected fun baseSkip(n: Long): Long {
        var shift = myDecoratedStream.skip(n)
        while (shift < n && myDecoratedStream.read() != -1) {
            ++shift
        }
        myOffset = (myOffset + shift).toInt()
        return shift
    }

    @Throws(IOException::class)
    override fun read(): Int {
        val result = myDecoratedStream.read()
        if (result != -1) {
            ++myOffset
        }
        return result
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray): Int = read(b, 0, b.size)

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val shift = myDecoratedStream.read(b, off, len)
        if (shift > 0) {
            myOffset += shift
        }
        return shift
    }

    open fun offset(): Int = myOffset

    @Throws(IOException::class)
    override fun close() {
        myOffset = 0
        myDecoratedStream.close()
    }
}
