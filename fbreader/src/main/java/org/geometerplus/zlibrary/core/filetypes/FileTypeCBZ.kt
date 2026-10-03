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

class FileTypeCBZ : FileType("CBZ") {

    override fun acceptsFile(file: ZLFile): Boolean {
        val extension = file.getExtension()
        return "cbz".equals(extension, ignoreCase = true) || "cbr".equals(extension, ignoreCase = true)
    }

    override fun mimeTypes(): List<MimeType> = MimeType.TYPES_COMIC_BOOK

    override fun mimeType(file: ZLFile): MimeType {
        val lName = file.getShortName().lowercase(Locale.ROOT)
        return when {
            lName.endsWith(".cbz") -> MimeType.APP_CBZ
            lName.endsWith(".cbr") -> MimeType.APP_CBR
            else -> MimeType.NULL
        }
    }

    override fun rawMimeType(file: ZLFile): MimeType {
        val lName = file.getShortName().lowercase(Locale.ROOT)
        return when {
            lName.endsWith(".cbz") -> MimeType.APP_ZIP
            lName.endsWith(".cbr") -> MimeType.APP_RAR
            else -> MimeType.NULL
        }
    }

    override fun defaultExtension(mime: MimeType): String {
        val m = mime.clean()
        if (MimeType.APP_CBZ == m || MimeType.APP_ZIP == m) {
            return "cbz"
        }
        if (MimeType.APP_CBR == m || MimeType.APP_RAR == m) {
            return "cbr"
        }
        return "cbz"
    }
}
