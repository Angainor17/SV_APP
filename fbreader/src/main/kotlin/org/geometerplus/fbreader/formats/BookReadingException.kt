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

package org.geometerplus.fbreader.formats

import org.amse.ys.zip.ZipException
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.resources.ZLResource
import java.io.IOException

class BookReadingException : Exception {
    @JvmField
    val File: ZLFile

    constructor(resourceId: String, file: ZLFile, params: Array<String>) : super(getResourceText(resourceId, *params)) {
        File = file
    }

    constructor(resourceId: String, file: ZLFile) : this(resourceId, file, arrayOf(file.getPath()))

    constructor(e: IOException, file: ZLFile) : super(
        getResourceText(if (e is ZipException) "errorReadingZip" else "errorReadingFile", file.getPath()),
        e
    ) {
        File = file
    }

    companion object {
        private fun getResourceText(resourceId: String, vararg params: String): String {
            var message = ZLResource.resource("errorMessage").getResource(resourceId).getValue()
            for (p in params) {
                message = message.replaceFirst("%s", p)
            }
            return message
        }
    }
}
