package org.amse.ys.zip

import org.geometerplus.zlibrary.core.util.InputStreamHolder
import java.io.InputStream

class MyBufferedInputStream(
    private val myStreamHolder: InputStreamHolder,
    bufferSize: Int
) : InputStream() {
    private lateinit var myFileInputStream: InputStream
    private val myBuffer = ByteArray(bufferSize)
    private var myBytesReady = 0
    private var myPositionInBuffer = 0
    private var myCurrentPosition = 0

    init {
        myFileInputStream = myStreamHolder.getInputStream()!!
    }

    constructor(streamHolder: InputStreamHolder) : this(streamHolder, 1 shl 10)

    override fun available(): Int = myFileInputStream.available() + myBytesReady

    internal fun offset(): Int = myCurrentPosition

    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        var ready = if (len < myBytesReady) len else myBytesReady
        var remaining = len
        var offset = off
        if (ready > 0) {
            if (b != null) {
                System.arraycopy(myBuffer, myPositionInBuffer, b, offset, ready)
            }
            remaining -= ready
            myBytesReady -= ready
            myPositionInBuffer += ready
            offset += ready
        }
        if (remaining > 0) {
            val ready2 = myFileInputStream.read(b, offset, remaining)
            if (ready2 >= 0) {
                ready += ready2
            }
        }
        myCurrentPosition += ready
        return if (ready > 0) ready else -1
    }

    override fun read(): Int {
        myCurrentPosition++
        if (myBytesReady <= 0) {
            myPositionInBuffer = 0
            myBytesReady = myFileInputStream.read(myBuffer)
            if (myBytesReady <= 0) {
                return -1
            }
        }
        myBytesReady--
        return myBuffer[myPositionInBuffer++].toInt() and 255
    }

    internal fun read2Bytes(): Int {
        val low = read()
        val high = read()
        if (high < 0) {
            throw ZipException("unexpected end of file at position " + offset())
        }
        return (high shl 8) + low
    }

    internal fun read4Bytes(): Int {
        val firstByte = read()
        val secondByte = read()
        val thirdByte = read()
        val fourthByte = read()
        if (fourthByte < 0) {
            throw ZipException("unexpected end of file at position " + offset())
        }
        return (fourthByte shl 24) + (thirdByte shl 16) + (secondByte shl 8) + firstByte
    }

    internal fun readString(stringLength: Int): String {
        val array = ByteArray(stringLength)
        read(array)
        if (isUtf8String(array)) {
            return String(array, Charsets.UTF_8)
        }
        val chars = CharArray(stringLength)
        for (i in 0 until stringLength) {
            chars[i] = (array[i].toInt() and 0xFF).toChar()
        }
        return String(chars)
    }

    override fun skip(n: Long): Long {
        if (myBytesReady.toLong() >= n) {
            myBytesReady = (myBytesReady.toLong() - n).toInt()
            myPositionInBuffer = (myPositionInBuffer.toLong() + n).toInt()
            myCurrentPosition = (myCurrentPosition.toLong() + n).toInt()
            return n
        } else {
            var left = n - myBytesReady.toLong()
            myBytesReady = 0
            left -= myFileInputStream.skip(left)
            while (left > 0) {
                val skipped = myFileInputStream.read(myBuffer, 0, minOf(left.toInt(), myBuffer.size))
                if (skipped <= 0) {
                    break
                }
                left -= skipped.toLong()
            }
            myCurrentPosition = (myCurrentPosition.toLong() + (n - left)).toInt()
            return n - left
        }
    }

    fun backSkip(n: Int) {
        if (n <= 0) {
            return
        }
        myFileInputStream.close()
        myFileInputStream = myStreamHolder.getInputStream()!!
        myBytesReady = 0
        myPositionInBuffer = 0
        val position = myCurrentPosition - n
        myCurrentPosition = 0
        skip(position.toLong())
    }

    fun setPosition(position: Int) {
        if (myCurrentPosition < position) {
            skip((position - myCurrentPosition).toLong())
        } else {
            backSkip(myCurrentPosition - position)
        }
    }

    override fun close() {
        myFileInputStream.close()
        myBytesReady = 0
    }
}

private fun isUtf8String(array: ByteArray): Boolean {
    var nonLeadingCharsCounter = 0
    for (b in array) {
        if (nonLeadingCharsCounter == 0) {
            if ((b.toInt() and 0x80) != 0) {
                if ((b.toInt() and 0xE0) == 0xC0) {
                    nonLeadingCharsCounter = 1
                } else if ((b.toInt() and 0xF0) == 0xE0) {
                    nonLeadingCharsCounter = 2
                } else if ((b.toInt() and 0xF8) == 0xF0) {
                    nonLeadingCharsCounter = 3
                } else {
                    return false
                }
            }
        } else {
            if ((b.toInt() and 0xC0) != 0x80) {
                return false
            }
            --nonLeadingCharsCounter
        }
    }
    return nonLeadingCharsCounter == 0
}
