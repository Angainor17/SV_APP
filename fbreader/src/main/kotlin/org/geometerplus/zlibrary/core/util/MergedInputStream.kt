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

class MergedInputStream @Throws(IOException::class) constructor(
    private val myStreams: Array<InputStream>,
) : InputStream() {
    private var myCurrentStream: InputStream = myStreams[0]
    private var myCurrentStreamNumber: Int = 0

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
        var bytesReaded = 0
        var streamIsAvailable = true
        var offset = off
        while (bytesToRead > 0 && streamIsAvailable) {
            val readed = myCurrentStream.read(b, offset, bytesToRead)
            if (readed != -1) {
                bytesToRead -= readed
                offset += readed
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
