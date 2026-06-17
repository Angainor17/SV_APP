/*
 * Copyright (C) 2009-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.android.fbreader.httpd

import org.geometerplus.zlibrary.core.filesystem.ZLFile

object DataUtil {
    internal fun fileFromEncodedPath(encodedPath: String): ZLFile {
        val path = StringBuilder()
        for (item in encodedPath.split("X")) {
            if (item.isEmpty()) continue
            path.append(item.toShort(16).toInt().toChar())
        }
        return ZLFile.createFileByPath(path.toString())
    }

    @JvmStatic
    fun buildUrl(connection: DataService.Connection, prefix: String, path: String): String? {
        val port = connection.getPort()
        if (port == -1) return null
        val url = StringBuilder("http://127.0.0.1:$port/$prefix/")
        for (i in path.indices) {
            url.append(String.format("X%X", path[i].code.toShort()))
        }
        return url.toString()
    }
}
