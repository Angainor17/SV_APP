/*
 * Copyright (C) 2011-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.fbreader.formats.oeb

import org.geometerplus.fbreader.book.AbstractBook
import org.geometerplus.fbreader.book.BookUtil
import org.geometerplus.fbreader.bookmodel.BookModel
import org.geometerplus.fbreader.formats.BookReadingException
import org.geometerplus.fbreader.formats.NativeFormatPlugin
import org.geometerplus.zlibrary.core.encodings.AutoEncodingCollection
import org.geometerplus.zlibrary.core.encodings.EncodingCollection
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.util.SystemInfo

class OEBNativePlugin(systemInfo: SystemInfo) : NativeFormatPlugin(systemInfo, "ePub") {

    override fun readModel(model: BookModel) {
        val file = BookUtil.fileByBook(model.book)
        file.setCached(true)
        try {
            super.readModel(model)
            model.setLabelResolver(object : BookModel.LabelResolver {
                override fun getCandidates(id: String): List<String> {
                    val index = id.indexOf("#")
                    return if (index > 0) {
                        listOf(id.substring(0, index))
                    } else {
                        emptyList()
                    }
                }
            })
        } finally {
            file.setCached(false)
        }
    }

    override fun supportedEncodings(): EncodingCollection = AutoEncodingCollection()

    override fun detectLanguageAndEncoding(book: AbstractBook) {
        book.encoding = "auto"
    }

    override fun readAnnotation(file: ZLFile): String? {
        file.setCached(true)
        return try {
            OEBAnnotationReader().readAnnotation(getOpfFile(file))
        } catch (e: BookReadingException) {
            null
        } finally {
            file.setCached(false)
        }
    }

    @Throws(BookReadingException::class)
    private fun getOpfFile(oebFile: ZLFile): ZLFile {
        if ("opf" == oebFile.extension) {
            return oebFile
        }

        val containerInfoFile = ZLFile.createFile(oebFile, "META-INF/container.xml")
        if (containerInfoFile.exists()) {
            val reader = ContainerFileReader()
            reader.readQuietly(containerInfoFile)
            val opfPath = reader.rootPath
            if (opfPath != null) {
                return ZLFile.createFile(oebFile, opfPath)
            }
        }

        for (child in oebFile.children()) {
            if (child.extension == "opf") {
                return child
            }
        }
        throw BookReadingException("opfFileNotFound", oebFile)
    }

    override fun priority(): Int = 0
}
