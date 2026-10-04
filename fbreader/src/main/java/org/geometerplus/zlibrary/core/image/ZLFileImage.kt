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

package org.geometerplus.zlibrary.core.image

import org.geometerplus.zlibrary.core.drm.FileEncryptionInfo
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.util.Base64InputStream
import org.geometerplus.zlibrary.core.util.HexInputStream
import org.geometerplus.zlibrary.core.util.MergedInputStream
import org.geometerplus.zlibrary.core.util.SliceInputStream
import java.io.IOException
import java.io.InputStream

class ZLFileImage(
    private val myFile: ZLFile,
    encoding: String?,
    private val myOffsets: IntArray,
    private val myLengths: IntArray,
    private val myEncryptionInfo: FileEncryptionInfo?,
) : ZLStreamImage {

    private val myEncoding: String = encoding ?: ENCODING_NONE

    constructor(file: ZLFile, encoding: String?, offset: Int, length: Int) :
            this(file, encoding, intArrayOf(offset), intArrayOf(length), null)

    constructor(file: ZLFile) :
            this(file, ENCODING_NONE, 0, file.size().toInt())

    override fun getURI(): String {
        var result = "$SCHEME://${myFile.getPath()}\u0000$myEncoding\u0000${myOffsets.size}"
        for (offset in myOffsets) {
            result += "\u0000$offset"
        }
        for (length in myLengths) {
            result += "\u0000$length"
        }
        return result
    }

    @Throws(IOException::class)
    private fun baseInputStream(): InputStream {
        if (myOffsets.size == 1) {
            val offset = myOffsets[0]
            val length = myLengths[0]
            return SliceInputStream(
                myFile.getInputStream()!!,
                offset,
                if (length != 0) length else Int.MAX_VALUE
            )
        } else {
            val streams = Array<InputStream>(myOffsets.size) { i ->
                val offset = myOffsets[i]
                val length = myLengths[i]
                SliceInputStream(
                    myFile.getInputStream()!!,
                    offset,
                    if (length != 0) length else Int.MAX_VALUE
                )
            }
            return MergedInputStream(streams)
        }
    }

    override fun inputStream(): InputStream? {
        try {
            if (myEncryptionInfo != null) {
                return null
            }

            var stream = baseInputStream()
            if (ENCODING_NONE == myEncoding) {
                return stream
            } else if (ENCODING_HEX == myEncoding) {
                return HexInputStream(stream)
            } else if (ENCODING_BASE64 == myEncoding) {
                stream = Base64InputStream(stream)
                val len = stream.skip(stream.available().toLong()).toInt()
                stream.close()
                return SliceInputStream(Base64InputStream(baseInputStream()), 0, len)
            } else {
                System.err.println("unsupported encoding: $myEncoding")
                return null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    companion object {
        const val SCHEME = "imagefile"
        const val ENCODING_NONE = ""
        const val ENCODING_HEX = "hex"
        const val ENCODING_BASE64 = "base64"

        @JvmStatic
        fun byUrlPath(urlPath: String?): ZLFileImage? {
            if (urlPath == null) {
                return null
            }
            try {
                val data = urlPath.split("\u0000")
                val count = data[2].toInt()
                val offsets = IntArray(count)
                val lengths = IntArray(count)
                for (i in 0 until count) {
                    offsets[i] = data[3 + i].toInt()
                    lengths[i] = data[3 + count + i].toInt()
                }
                return ZLFileImage(
                    ZLFile.createFileByPath(data[0]),
                    data[1],
                    offsets,
                    lengths,
                    null
                )
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
    }
}
