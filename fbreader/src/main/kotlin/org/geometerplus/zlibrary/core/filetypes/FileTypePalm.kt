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
import org.geometerplus.zlibrary.core.options.ZLStringOption
import org.geometerplus.zlibrary.core.util.MimeType
import java.io.IOException

abstract class FileTypePalm(id: String, private val myPalmId: String) : FileType(id) {

    protected fun palmFileType(file: ZLFile): String? {
        // TODO: use database instead of option (?)
        val palmTypeOption = ZLStringOption(file.getPath(), "PalmType", "")
        var palmType = palmTypeOption.getValue()
        if (palmType.length != 8) {
            val id = ByteArray(8)
            try {
                val stream = file.getInputStream()
                if (stream == null) {
                    return null
                }
                stream.skip(60)
                stream.read(id)
                stream.close()
            } catch (e: IOException) {
            }
            palmType = String(id).intern()
            palmTypeOption.setValue(palmType)
        }
        return palmType.intern()
    }

    override fun acceptsFile(file: ZLFile): Boolean {
        val extension = file.getExtension()
        return ("pdb".equals(extension, ignoreCase = true) || "prc".equals(extension, ignoreCase = true)) &&
            myPalmId == palmFileType(file)
    }

    override fun defaultExtension(mime: MimeType): String = "pdb"
}
