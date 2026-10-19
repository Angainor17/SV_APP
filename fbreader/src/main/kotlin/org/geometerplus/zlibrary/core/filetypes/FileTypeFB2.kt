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
import java.util.ArrayList
import java.util.Locale

class FileTypeFB2 : FileType("fb2") {

    private val myMimeTypes = ArrayList<MimeType>()

    override fun acceptsFile(file: ZLFile): Boolean {
        val lName = file.getShortName().lowercase(Locale.ROOT)
        return lName.endsWith(".fb2") || lName.endsWith(".fb2.zip")
    }

    override fun mimeTypes(): List<MimeType> {
        if (myMimeTypes.isEmpty()) {
            myMimeTypes.addAll(MimeType.TYPES_FB2)
            myMimeTypes.addAll(MimeType.TYPES_FB2_ZIP)
        }
        return myMimeTypes
    }

    override fun mimeType(file: ZLFile): MimeType {
        val lName = file.getShortName().lowercase(Locale.ROOT)
        return when {
            lName.endsWith(".fb2") -> MimeType.APP_FB2_XML
            lName.endsWith(".fb2.zip") -> MimeType.APP_FB2_ZIP
            else -> MimeType.NULL
        }
    }

    override fun rawMimeType(file: ZLFile): MimeType {
        val lName = file.getShortName().lowercase(Locale.ROOT)
        return when {
            lName.endsWith(".fb2") -> MimeType.TEXT_XML
            lName.endsWith(".fb2.zip") -> MimeType.APP_ZIP
            else -> MimeType.NULL
        }
    }

    override fun defaultExtension(mime: MimeType): String {
        val m = mime.clean()
        if (MimeType.TYPES_FB2.contains(m) || MimeType.TEXT_XML == m) {
            return "fb2"
        }
        if (MimeType.TYPES_FB2_ZIP.contains(m) || MimeType.APP_ZIP == m) {
            return "fb2.zip"
        }
        return "fb2"
    }
}
