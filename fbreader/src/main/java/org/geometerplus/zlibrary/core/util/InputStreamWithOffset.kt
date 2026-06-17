package org.geometerplus.zlibrary.core.util

import java.io.IOException
import java.io.InputStream

open class InputStreamWithOffset(private val myDecoratedStream: InputStream) : InputStream() {
    private var myOffset = 0

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
        myOffset += shift.toInt()
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
