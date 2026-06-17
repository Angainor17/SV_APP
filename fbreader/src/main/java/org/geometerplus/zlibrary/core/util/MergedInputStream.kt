package org.geometerplus.zlibrary.core.util

import java.io.IOException
import java.io.InputStream

class MergedInputStream @Throws(IOException::class)
constructor(private val myStreams: Array<InputStream>) : InputStream() {

    private var myCurrentStream: InputStream = myStreams[0]
    private var myCurrentStreamNumber = 0

    @Throws(IOException::class)
    override fun read(): Int {
        var readed = -1
        var streamIsAvailable = true
        while (readed == -1 && streamIsAvailable) {
            readed = myCurrentStream.read()
            if (readed == -1) {
                streamIsAvailable = nextStream()
            }
        }
        return readed
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        var bytesToRead = len
        var off = off
        var bytesReaded = 0
        var streamIsAvailable = true
        while (bytesToRead > 0 && streamIsAvailable) {
            val readed = myCurrentStream.read(b, off, bytesToRead)
            if (readed != -1) {
                bytesToRead -= readed
                off += readed
                bytesReaded += readed
            }
            if (bytesToRead != 0) {
                streamIsAvailable = nextStream()
            }
        }
        return if (bytesReaded == 0) -1 else bytesReaded
    }

    @Throws(IOException::class)
    override fun skip(n: Long): Long {
        var skipped = myCurrentStream.skip(n)
        var streamIsAvailable = true
        while (skipped < n && streamIsAvailable) {
            streamIsAvailable = nextStream()
            if (streamIsAvailable) {
                skipped += myCurrentStream.skip(n - skipped)
            }
        }
        return skipped
    }

    @Throws(IOException::class)
    override fun available(): Int {
        var total = 0
        for (i in myCurrentStreamNumber until myStreams.size) {
            total += myStreams[i].available()
        }
        return total
    }

    private fun nextStream(): Boolean {
        if (myCurrentStreamNumber + 1 >= myStreams.size) {
            return false
        }
        ++myCurrentStreamNumber
        myCurrentStream = myStreams[myCurrentStreamNumber]
        return true
    }
}
