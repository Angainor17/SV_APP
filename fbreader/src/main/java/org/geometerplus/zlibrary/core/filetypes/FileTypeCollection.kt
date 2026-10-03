/*
 * Copyright (C) 2012-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.zlibrary.core.filetypes

import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.util.MimeType
import java.util.Locale
import java.util.TreeMap

class FileTypeCollection private constructor() {

    private val myTypes = TreeMap<String, FileType>()

    init {
        addType(FileTypeFB2())
        addType(FileTypeEpub())
        addType(FileTypeMobipocket())
        addType(FileTypeHtml())
        addType(SimpleFileType("txt", "txt", MimeType.TYPES_TXT))
        addType(SimpleFileType("RTF", "rtf", MimeType.TYPES_RTF))
        addType(SimpleFileType("PDF", "pdf", MimeType.TYPES_PDF))
        addType(FileTypeDjVu())
        addType(FileTypeCBZ())
        addType(SimpleFileType("ZIP archive", "zip", listOf(MimeType.APP_ZIP)))
        addType(SimpleFileType("msdoc", "doc", MimeType.TYPES_DOC))
    }

    private fun addType(type: FileType) {
        myTypes[type.Id.lowercase(Locale.ROOT)] = type
    }

    fun types(): Collection<FileType> = myTypes.values

    fun typeById(id: String): FileType? = myTypes[id.lowercase(Locale.ROOT)]

    fun typeForFile(file: ZLFile): FileType? {
        for (type in types()) {
            if (type.acceptsFile(file)) {
                return type
            }
        }
        return null
    }

    fun typeForMime(mime: MimeType?): FileType? {
        if (mime == null) {
            return null
        }
        val m = mime.clean()
        for (type in types()) {
            if (type.mimeTypes().contains(m)) {
                return type
            }
        }
        return null
    }

    fun mimeType(file: ZLFile): MimeType {
        for (type in types()) {
            val mime = type.mimeType(file)
            if (mime !== MimeType.NULL) {
                return mime
            }
        }
        return MimeType.UNKNOWN
    }

    fun rawMimeType(file: ZLFile): MimeType {
        for (type in types()) {
            val mime = type.rawMimeType(file)
            if (mime !== MimeType.NULL) {
                return mime
            }
        }
        return MimeType.UNKNOWN
    }

    companion object {
        @JvmField
        val Instance: FileTypeCollection = FileTypeCollection()
    }
}
