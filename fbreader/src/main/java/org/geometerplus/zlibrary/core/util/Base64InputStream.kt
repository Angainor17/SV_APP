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

class Base64InputStream(
    private val myBaseStream: InputStream,
) : InputStream() {
    private val myBuffer = ByteArray(32768)
    private var myDecoded0: Int = -1
    private var myDecoded1: Int = -1
    private var myDecoded2: Int = -1
    private var myBufferOffset: Int = 0
    private var myBufferLength: Int = 0

    @Throws(IOException::class)
    override fun available(): Int =
        (myBufferLength + myBaseStream.available()) * 3 / 4

    @Throws(IOException::class)
    override fun skip(n: Long): Long {
        var skipped = 0L
        while (skipped < n) {
            if (read() == -1) {
                return skipped
            }
            ++skipped
        }
        return n
    }

    @Throws(IOException::class)
    override fun read(): Int {
        var result = myDecoded0
        if (result != -1) {
            myDecoded0 = -1
            return result
        }
        result = myDecoded1
        if (result != -1) {
            myDecoded1 = -1
            return result
        }
        result = myDecoded2
        if (result != -1) {
            myDecoded2 = -1
            return result
        }

        fillDecodedBuffer()
        result = myDecoded0
        myDecoded0 = -1
        return result
    }

    @Throws(IOException::class)
    override fun close() {
        myBaseStream.close()
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len == 0) {
            return 0
        }
        var ready = 0
        if (myDecoded0 != -1) {
            b[off] = myDecoded0.toByte()
            myDecoded0 = -1
            if (len == 1) {
                return 1
            }
            b[off + 1] = myDecoded1.toByte()
            myDecoded1 = -1
            if (len == 2) {
                return 2
            }
            b[off + 2] = myDecoded2.toByte()
            myDecoded2 = -1
            ready = 3
        } else if (myDecoded1 != -1) {
            b[off] = myDecoded1.toByte()
            myDecoded1 = -1
            if (len == 1) {
                return 1
            }
            b[off + 1] = myDecoded2.toByte()
            myDecoded2 = -1
            ready = 2
        } else if (myDecoded2 != -1) {
            b[off] = myDecoded2.toByte()
            myDecoded2 = -1
            ready = 1
        }
        while (ready < len - 2) {
            var first = -1
            var second = -1
            var third = -1
            var fourth = -1
            mainLoop@ while (myBufferLength >= 0) {
                while (myBufferLength > 0) {
                    myBufferLength--
                    val digit = decode(myBuffer[myBufferOffset++])
                    if (digit != -1) {
                        when {
                            first == -1 -> first = digit
                            second == -1 -> second = digit
                            third == -1 -> third = digit
                            else -> {
                                fourth = digit
                                break@mainLoop
                            }
                        }
                    }
                }
                fillBuffer()
            }
            if (first == -1) {
                return if (ready > 0) ready else -1
            }
            b[off + ready] = ((first shl 2) or (second shr 4)).toByte()
            b[off + ready + 1] = ((second shl 4) or (third shr 2)).toByte()
            b[off + ready + 2] = ((third shl 6) or fourth).toByte()
            ready += 3
        }
        fillDecodedBuffer()
        while (ready < len) {
            val num = read()
            if (num == -1) {
                return if (ready > 0) ready else -1
            }
            b[off + ready] = num.toByte()
            ++ready
        }
        return len
    }

    @Throws(IOException::class)
    private fun fillDecodedBuffer() {
        var first = -1
        var second = -1
        var third = -1
        var fourth = -1
        mainLoop@ while (myBufferLength >= 0) {
            while (myBufferLength > 0) {
                myBufferLength--
                val digit = decode(myBuffer[myBufferOffset++])
                if (digit != -1) {
                    when {
                        first == -1 -> first = digit
                        second == -1 -> second = digit
                        third == -1 -> third = digit
                        else -> {
                            fourth = digit
                            break@mainLoop
                        }
                    }
                }
            }
            fillBuffer()
        }
        if (first != -1) {
            myDecoded0 = (first shl 2) or (second shr 4)
            myDecoded1 = 0xFF and ((second shl 4) or (third shr 2))
            myDecoded2 = 0xFF and ((third shl 6) or fourth)
        }
    }

    @Throws(IOException::class)
    private fun fillBuffer() {
        myBufferLength = myBaseStream.read(myBuffer)
        myBufferOffset = 0
    }

    private companion object {
        private fun decode(b: Byte): Int {
            val value = b.toInt()
            return when (value) {
                in 'A'.code..'Z'.code -> value - 'A'.code
                in 'a'.code..'z'.code -> value - 'a'.code + 26
                in '0'.code..'9'.code -> value - '0'.code + 52
                '+'.code -> 62
                '/'.code -> 63
                '='.code -> 64
                else -> -1
            }
        }
    }
}
