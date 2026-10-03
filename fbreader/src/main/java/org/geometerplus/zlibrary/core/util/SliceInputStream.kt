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

import java.io.InputStream

class SliceInputStream(base: InputStream, start: Int, length: Int) : InputStreamWithOffset(base) {
    private val myStart = start
    private val myLength = length

    init {
        baseSkip(start.toLong())
    }

    override fun read(): Int {
        if (offset() >= myLength) {
            return -1
        }
        return super.read()
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val maxbytes = myLength - offset()
        if (maxbytes <= 0) {
            return -1
        }
        return super.read(b, off, minOf(len, maxbytes))
    }

    override fun skip(n: Long): Long =
        super.skip(minOf(n, maxOf(myLength - offset(), 0).toLong()))

    override fun available(): Int =
        minOf(super.available(), maxOf(myLength - offset(), 0))

    override fun offset(): Int = super.offset() - myStart
}
