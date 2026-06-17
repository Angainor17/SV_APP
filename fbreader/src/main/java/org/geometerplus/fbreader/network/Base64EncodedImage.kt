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

package org.geometerplus.fbreader.network

import org.geometerplus.zlibrary.core.image.ZLBase64EncodedImage
import org.geometerplus.zlibrary.core.util.MimeType
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter

internal class Base64EncodedImage(
    library: NetworkLibrary,
    data: String,
    private val mimeType: MimeType
) : ZLBase64EncodedImage() {

    companion object {
        private const val ENCODED_SUFFIX = ".base64"
    }

    private var decodedFileName: String

    init {
        val dir = library.systemInfo.networkCacheDirectory() + "/base64"
        File(dir).mkdirs()

        decodedFileName = "$dir${File.separator}${Integer.toHexString(data.hashCode())}"
        when {
            MimeType.IMAGE_PNG == mimeType -> decodedFileName += ".png"
            MimeType.IMAGE_JPEG == mimeType -> decodedFileName += ".jpg"
        }

        if (isCacheValid(File(decodedFileName))) {
            // cache is valid, nothing to do
        } else {
            val file = File(encodedFileName())
            if (!isCacheValid(file)) {
                try {
                    val writer = OutputStreamWriter(FileOutputStream(file), "UTF-8")
                    try {
                        writer.write(data, 0, data.length)
                    } finally {
                        writer.close()
                    }
                } catch (e: IOException) {
                }
            }
        }
    }

    override fun isCacheValid(file: File): Boolean {
        if (file.exists()) {
            val diff = System.currentTimeMillis() - file.lastModified()
            val valid = 24 * 60 * 60 * 1000L // one day in milliseconds; FIXME: hardcoded const
            if (diff in 0..valid) {
                return true
            }
            file.delete()
        }
        return false
    }

    override fun encodedFileName(): String = decodedFileName + ENCODED_SUFFIX

    override fun decodedFileName(): String = decodedFileName
}
