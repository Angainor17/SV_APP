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

class FileTypeHtml : FileType("HTML") {

    override fun acceptsFile(file: ZLFile): Boolean {
        val extension = file.getExtension().lowercase(Locale.ROOT)
        return extension.endsWith("html") || "htm" == extension
    }

    override fun mimeTypes(): List<MimeType> = MimeType.TYPES_HTML

    override fun mimeType(file: ZLFile): MimeType =
        if (acceptsFile(file)) MimeType.TEXT_HTML else MimeType.NULL

    override fun defaultExtension(mime: MimeType): String = "html"
}
