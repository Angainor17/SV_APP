package org.geometerplus.zlibrary.core.util

import java.io.IOException
import java.io.InputStream

class SliceInputStream @Throws(IOException::class)
constructor(base: InputStream, private val myStart: Int, private val myLength: Int) : InputStreamWithOffset(base) {

    init {
        baseSkip(myStart.toLong())
    }

    @Throws(IOException::class)
    override fun read(): Int {
        if (offset() >= myLength) {
            return -1
        }
        return super.read()
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val maxbytes = myLength - offset()
        if (maxbytes <= 0) {
            return -1
        }
        return super.read(b, off, Math.min(len, maxbytes))
    }

    @Throws(IOException::class)
    override fun skip(n: Long): Long = super.skip(Math.min(n, Math.max(myLength - offset(), 0).toLong()))

    @Throws(IOException::class)
    override fun available(): Int = Math.min(super.available(), Math.max(myLength - offset(), 0))

    override fun offset(): Int = super.offset() - myStart
}
