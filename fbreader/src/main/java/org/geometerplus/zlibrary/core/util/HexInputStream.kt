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

class HexInputStream(
    private val myBaseStream: InputStream,
) : InputStream() {
    private val myBuffer = ByteArray(32768)
    private var myBufferOffset: Int = 0
    private var myBufferLength: Int = 0

    @Throws(IOException::class)
    override fun available(): Int =
        (myBufferLength + myBaseStream.available()) / 2

    @Throws(IOException::class)
    override fun skip(n: Long): Long {
        var offset = myBufferOffset
        var available = myBufferLength
        var skipped = 0L
        while (skipped < 2 * n) {
            while (skipped < 2 * n && available > 0) {
                available--
                if (decode(myBuffer[offset++]) != -1) {
                    ++skipped
                }
            }
            if (skipped < 2 * n) {
                fillBuffer()
                available = myBufferLength
                if (available == -1) {
                    return skipped / 2
                }
                offset = 0
            }
        }
        myBufferLength = available
        myBufferOffset = offset
        return n
    }

    @Throws(IOException::class)
    override fun read(): Int {
        var first = -1
        while (myBufferLength >= 0) {
            while (myBufferLength > 0) {
                myBufferLength--
                val digit = decode(myBuffer[myBufferOffset++])
                if (digit != -1) {
                    if (first == -1) {
                        first = digit
                    } else {
                        return (first shl 4) + digit
                    }
                }
            }
            fillBuffer()
        }
        return -1
    }

    @Throws(IOException::class)
    override fun close() {
        myBaseStream.close()
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        var offset = myBufferOffset
        var available = myBufferLength
        var first = -1
        var ready = 0
        while (ready < len) {
            while (ready < len && available > 0) {
                available--
                val digit = decode(myBuffer[offset++])
                if (digit != -1) {
                    if (first == -1) {
                        first = digit
                    } else {
                        b[off + ready++] = ((first shl 4) + digit).toByte()
                        first = -1
                    }
                }
            }
            if (ready < len) {
                fillBuffer()
                available = myBufferLength
                if (available == -1) {
                    return if (ready == 0) -1 else ready
                }
                offset = 0
            }
        }
        myBufferLength = available
        myBufferOffset = offset
        return len
    }

    @Throws(IOException::class)
    private fun fillBuffer() {
        myBufferLength = myBaseStream.read(myBuffer)
        myBufferOffset = 0
    }

    companion object {
        private fun decode(b: Byte): Int {
            val value = b.toInt()
            return when (value) {
                in '0'.code..'9'.code -> value - '0'.code
                in 'A'.code..'F'.code -> value - 'A'.code + 10
                in 'a'.code..'f'.code -> value - 'a'.code + 10
                else -> -1
            }
        }
    }
}
