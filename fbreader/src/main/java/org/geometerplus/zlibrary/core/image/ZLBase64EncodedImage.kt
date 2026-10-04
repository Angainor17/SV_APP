/*
 * Copyright (C) 2010-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.zlibrary.core.image

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

abstract class ZLBase64EncodedImage protected constructor() : ZLStreamImage {
    private var myIsDecoded: Boolean = false

    private fun decodeByte(encodedByte: Byte): Int {
        val value = encodedByte.toInt()
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

    override fun getURI(): String? {
        return try {
            decode()
            val file = File(decodedFileName())
            "${ZLFileImage.SCHEME}://${decodedFileName()}\u0000\u00000\u0000${file.length().toInt()}"
        } catch (e: Exception) {
            null
        }
    }

    protected abstract fun encodedFileName(): String

    protected abstract fun decodedFileName(): String

    protected open fun isCacheValid(file: File): Boolean = false

    @Throws(IOException::class)
    private fun decode() {
        if (myIsDecoded) {
            return
        }
        myIsDecoded = true

        val outputFile = File(decodedFileName())
        if (isCacheValid(outputFile)) {
            return
        }

        val outputStream = FileOutputStream(outputFile)
        try {
            var dataLength: Int
            val encodedData: ByteArray

            val file = File(encodedFileName())
            val inputStream = FileInputStream(file)
            try {
                dataLength = file.length().toInt()
                encodedData = ByteArray(dataLength)
                inputStream.read(encodedData)
            } finally {
                inputStream.close()
            }
            file.delete()

            val data = ByteArray(dataLength * 3 / 4 + 4)
            var dataPos = 0
            var pos = 0
            while (pos < dataLength) {
                var n0 = -1
                var n1 = -1
                var n2 = -1
                var n3 = -1
                while (pos < dataLength && n0 == -1) {
                    n0 = decodeByte(encodedData[pos++])
                }
                while (pos < dataLength && n1 == -1) {
                    n1 = decodeByte(encodedData[pos++])
                }
                while (pos < dataLength && n2 == -1) {
                    n2 = decodeByte(encodedData[pos++])
                }
                while (pos < dataLength && n3 == -1) {
                    n3 = decodeByte(encodedData[pos++])
                }
                data[dataPos++] = (n0 shl 2 or (n1 shr 4)).toByte()
                data[dataPos++] = ((n1 and 0xf) shl 4 or ((n2 shr 2) and 0xf)).toByte()
                data[dataPos++] = (n2 shl 6 or n3).toByte()
            }
            outputStream.write(data, 0, dataPos)
        } finally {
            outputStream.close()
        }
    }

    override fun inputStream(): InputStream? {
        return try {
            decode()
            FileInputStream(File(decodedFileName()))
        } catch (e: IOException) {
            null
        }
    }
}
