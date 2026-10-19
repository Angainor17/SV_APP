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

package org.geometerplus.zlibrary.core.filesystem.tar

import java.io.IOException
import java.io.InputStream

class ZLTarInputStream @Throws(IOException::class) constructor(
    private val myBase: InputStream,
    fileName: String,
) : InputStream() {
    init {
        val header = ZLTarHeader()
        var found = false
        while (!found && header.read(myBase)) {
            if (header.IsRegularFile && fileName == header.Name) {
                found = true
            } else {
                val sizeToSkip = (header.Size + 0x1ff) and -0x200
                if (sizeToSkip < 0) {
                    throw IOException("Bad tar archive")
                }
                if (myBase.skip(sizeToSkip.toLong()) != sizeToSkip.toLong()) {
                    break
                }
                header.erase()
            }
        }
        if (!found) {
            throw IOException("Item $fileName not found in tar archive")
        }
    }

    @Throws(IOException::class)
    override fun read(): Int = myBase.read()

    @Throws(IOException::class)
    override fun read(b: ByteArray): Int = myBase.read(b)

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int = myBase.read(b, off, len)

    @Throws(IOException::class)
    override fun skip(n: Long): Long = myBase.skip(n)

    @Throws(IOException::class)
    override fun available(): Int = myBase.available()
}
