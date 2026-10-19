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

class ZLTarHeader {
    var Name: String? = null
    var Size: Int = 0
    var IsRegularFile: Boolean = false

    @Throws(IOException::class)
    fun read(stream: InputStream): Boolean {
        val fileName = ByteArray(100)
        if (stream.read(fileName) != 100) {
            return false
        }
        if (fileName[0] == 0.toByte()) {
            return false
        }
        Name = getStringFromByteArray(fileName)

        if (stream.skip(24L) != 24L) {
            return false
        }

        val fileSizeString = ByteArray(12)
        if (stream.read(fileSizeString) != 12) {
            return false
        }
        Size = 0
        for (i in 0 until 12) {
            val digit = fileSizeString[i]
            if ((digit < '0'.code.toByte()) || (digit > '7'.code.toByte())) {
                break
            }
            Size *= 8
            Size += digit - '0'.code.toByte()
        }

        if (stream.skip(20L) != 20L) {
            return false
        }

        val linkFlag = stream.read().toByte()
        if (linkFlag == (-1).toByte()) {
            return false
        }
        IsRegularFile = linkFlag == 0.toByte() || linkFlag == '0'.code.toByte()

        stream.skip(355L)

        if ((linkFlag == 'L'.code.toByte() || linkFlag == 'K'.code.toByte()) &&
            "././@LongLink" == Name && Size < 10240
        ) {
            val nameBuffer = ByteArray(Size - 1)
            stream.read(nameBuffer)
            Name = getStringFromByteArray(nameBuffer)
            val skip = 512 - (Size and 0x1ff)
            stream.skip((skip + 1).toLong())
        }
        return true
    }

    fun erase() {
        Name = null
    }

    companion object {
        private fun getStringFromByteArray(buffer: ByteArray): String {
            val s = String(buffer)
            val indexOfZero = s.indexOf('\u0000')
            return if (indexOfZero != -1) s.substring(0, indexOfZero) else s
        }
    }
}
