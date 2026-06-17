package org.geometerplus.zlibrary.core.util

import java.io.IOException
import java.io.InputStream

class HexInputStream(private val myBaseStream: InputStream) : InputStream() {
    private val myBuffer = ByteArray(32768)
    private var myBufferOffset = 0
    private var myBufferLength = 0

    companion object {
        private fun decode(b: Byte): Int = when (b.toInt().toChar()) {
            in '0'..'9' -> b - '0'.code
            in 'A'..'F' -> b - 'A'.code + 10
            in 'a'..'f' -> b - 'a'.code + 10
            else -> -1
        }
    }

    @Throws(IOException::class)
    override fun available(): Int = (myBufferLength + myBaseStream.available()) / 2

    @Throws(IOException::class)
    override fun skip(n: Long): Long {
        var offset = myBufferOffset
        var available = myBufferLength
        for (skipped in 0L until 2 * n) {
            while (skipped < 2 * n && available-- > 0) {
                if (decode(myBuffer[offset++]) != -1) {
                    // skipped++
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
            while (myBufferLength-- > 0) {
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
            while (ready < len && available-- > 0) {
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
}
