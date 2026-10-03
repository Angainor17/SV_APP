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

class FileTypeEpub : FileType("ePub") {

    override fun acceptsFile(file: ZLFile): Boolean {
        val extension = file.getExtension()
        return "epub".equals(extension, ignoreCase = true) ||
            "oebzip".equals(extension, ignoreCase = true) ||
            ("opf".equals(extension, ignoreCase = true) && file !== file.getPhysicalFile())
    }

    override fun mimeTypes(): List<MimeType> = MimeType.TYPES_EPUB

    override fun mimeType(file: ZLFile): MimeType {
        val extension = file.getExtension()
        if ("epub".equals(extension, ignoreCase = true)) {
            return MimeType.APP_EPUB_ZIP
        }
        // TODO: process other extensions (?)
        return MimeType.NULL
    }

    override fun rawMimeType(file: ZLFile): MimeType {
        val extension = file.getExtension()
        if ("epub".equals(extension, ignoreCase = true)) {
            return MimeType.APP_ZIP
        }
        // TODO: process other extensions (?)
        return MimeType.NULL
    }

    override fun defaultExtension(mime: MimeType): String = "epub"
}
