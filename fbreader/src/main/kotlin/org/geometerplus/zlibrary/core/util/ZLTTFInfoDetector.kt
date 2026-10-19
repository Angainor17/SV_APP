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

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.Locale

class ZLTTFInfoDetector {
    private var myPosition: Int = 0

    fun collectFonts(files: Iterable<File>?): HashMap<String, Array<File?>> {
        val fonts = HashMap<String, Array<File?>>()
        if (files == null) {
            return fonts
        }

        for (f in files) {
            var stream: InputStream? = null
            try {
                stream = FileInputStream(f)
                val info = detectInfo(stream)
                if (info == null) {
                    continue
                }

                var family = info.FamilyName ?: continue
                var subfamily = info.SubfamilyName
                if (subfamily == null || !STYLES.contains(subfamily.lowercase(Locale.ROOT))) {
                    val full = if (subfamily != null) "$family $subfamily" else family
                    val lower = full.lowercase(Locale.ROOT)
                    family = full
                    subfamily = ""
                    for (style in STYLES) {
                        if (lower.endsWith(" $style")) {
                            family = full.substring(0, lower.length - style.length - 1)
                            subfamily = full.substring(lower.length - style.length)
                            break
                        }
                    }
                }

                var table = fonts[family]
                if (table == null) {
                    table = arrayOfNulls(4)
                    fonts[family] = table
                }
                when {
                    "bold".equals(subfamily, ignoreCase = true) -> table[1] = f
                    "italic".equals(subfamily, ignoreCase = true) ||
                        "oblique".equals(subfamily, ignoreCase = true) -> table[2] = f
                    "bold italic".equals(subfamily, ignoreCase = true) ||
                        "bold oblique".equals(subfamily, ignoreCase = true) -> table[3] = f
                    else -> table[0] = f
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                if (stream != null) {
                    try {
                        stream.close()
                    } catch (e1: IOException) {
                    }
                }
            }
        }
        return fonts
    }

    @Throws(IOException::class)
    fun detectInfo(stream: InputStream): ZLTTFInfo? {
        myPosition = 0

        val subtable = ByteArray(12)
        myPosition += stream.read(subtable)

        val numTables = getInt16(subtable, 4)
        val tables = ByteArray(16 * numTables)
        myPosition += stream.read(tables)

        var nameInfo: TableInfo? = null
        for (i in 0 until numTables) {
            if ("name" == String(tables, i * 16, 4, Charsets.US_ASCII)) {
                nameInfo = TableInfo(tables, i * 16)
                break
            }
        }
        if (nameInfo == null) {
            return null
        }
        return readFontInfo(stream, nameInfo)
    }

    @Throws(IOException::class)
    private fun readTable(stream: InputStream, info: TableInfo): ByteArray {
        myPosition += stream.skip((info.Offset - myPosition).toLong()).toInt()
        val buffer = ByteArray(info.Length)
        while (myPosition < info.Offset) {
            val len = stream.read(buffer, 0, minOf(info.Offset - myPosition, info.Length))
            if (len <= 0) {
                throw IOException("Table ${info.Name} not found in TTF file")
            }
            myPosition += len
        }
        myPosition += stream.read(buffer)
        return buffer
    }

    @Throws(IOException::class)
    private fun readFontInfo(stream: InputStream, nameInfo: TableInfo): ZLTTFInfo? {
        if (nameInfo.Offset < myPosition || nameInfo.Length <= 0) {
            return null
        }
        val buffer: ByteArray
        try {
            buffer = readTable(stream, nameInfo)
        } catch (e: Throwable) {
            return null
        }
        if (getInt16(buffer, 0) != 0) {
            throw IOException("Name table format is invalid")
        }
        val count = minOf(getInt16(buffer, 2), (buffer.size - 6) / 12)
        val stringOffset = getInt16(buffer, 4)
        var family: String? = null
        var subfamily: String? = null
        for (i in 0 until count) {
            val platformId = getInt16(buffer, 12 * i + 6)
            val languageId = getInt16(buffer, 12 * i + 10)
            val nameId = getInt16(buffer, 12 * i + 12)
            val length = getInt16(buffer, 12 * i + 14)
            val offset = getInt16(buffer, 12 * i + 16)
            when (nameId) {
                1 -> if ((family == null || languageId == 1033) &&
                    stringOffset + offset + length <= buffer.size
                ) {
                    family = String(
                        buffer, stringOffset + offset, length,
                        if (platformId == 1) Charsets.ISO_8859_1 else Charsets.UTF_16BE
                    )
                }
                2 -> if ((subfamily == null || languageId == 1033) &&
                    stringOffset + offset + length <= buffer.size
                ) {
                    subfamily = String(
                        buffer, stringOffset + offset, length,
                        if (platformId == 1) Charsets.ISO_8859_1 else Charsets.UTF_16BE
                    )
                }
            }
        }
        return if (family != null) ZLTTFInfo(family, subfamily) else null
    }

    private class TableInfo(buffer: ByteArray, off: Int) {
        val Name: String = String(buffer, off, 4, Charsets.US_ASCII)
        val Offset: Int = getInt32(buffer, off + 8)
        val Length: Int = getInt32(buffer, off + 12)
    }

    private companion object {
        private val STYLES = listOf(
            "bold italic",
            "bold oblique",
            "roman",
            "regular",
            "bold",
            "italic",
            "oblique"
        )

        private fun getInt16(buffer: ByteArray, offset: Int): Int =
            ((buffer[offset].toInt() and 0xFF) shl 8) + (buffer[offset + 1].toInt() and 0xFF)

        private fun getInt32(buffer: ByteArray, offset: Int): Int {
            if (offset <= buffer.size - 4) {
                var o = offset
                return ((buffer[o++].toInt() and 0xFF) shl 24) +
                        ((buffer[o++].toInt() and 0xFF) shl 16) +
                        ((buffer[o++].toInt() and 0xFF) shl 8) +
                        (buffer[o++].toInt() and 0xFF)
            } else {
                var result = 0
                var o = offset
                for (i in 0 until 4) {
                    result += if (o < buffer.size) (buffer[o++].toInt() and 0xFF) else 0
                    result = result shl 8
                }
                return result
            }
        }
    }
}
