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
    val file: ZLFile?

    constructor(resourceId: String, file: ZLFile?, params: List<String>) : super(
        getResourceText(resourceId, params)
    ) {
        this.file = file
    }

    constructor(resourceId: String, file: ZLFile) : this(
        resourceId,
        file,
        listOf(file.path)
    )

    constructor(e: IOException, file: ZLFile) : super(
        getResourceText(
            if (e is ZipException) "errorReadingZip" else "errorReadingFile",
            listOf(file.path)
        ),
        e
    ) {
        this.file = file
    }

    companion object {
        private fun getResourceText(resourceId: String, params: List<String>): String {
            var message = ZLResource.resource("errorMessage").getResource(resourceId).value
            for (p in params) {
                message = message.replaceFirst("%s", p)
            }
            return message
        }
    }
}
