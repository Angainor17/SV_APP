/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.zlibrary.core.filesystem

import org.amse.ys.zip.ZipFile
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList
import java.util.Collections

internal class ZLZipEntryFile internal constructor(
    parent: ZLFile,
    name: String,
) : ZLArchiveEntryFile(parent, name) {
    override fun exists(): Boolean =
        try {
            myParent.exists() && getZipFile(myParent).entryExists(myName)
        } catch (e: IOException) {
            false
        }

    override fun size(): Long =
        try {
            getZipFile(myParent).getEntrySize(myName).toLong()
        } catch (e: IOException) {
            0L
        }

    @Throws(IOException::class)
    override fun getInputStream(): InputStream =
        getZipFile(myParent).getInputStream(myName)

    companion object {
        private val ourZipFileMap = HashMap<ZLFile, ZipFile>()

        @JvmStatic
        fun archiveEntries(archive: ZLFile): List<ZLFile> {
            try {
                val zf = getZipFile(archive)
                val headers = zf.headers()
                if (!headers.isEmpty()) {
                    val entries = ArrayList<ZLFile>(headers.size)
                    for (h in headers) {
                        entries.add(ZLZipEntryFile(archive, h.FileName!!))
                    }
                    return entries
                }
            } catch (e: IOException) {
            }
            return Collections.emptyList()
        }

        @Throws(IOException::class)
        private fun getZipFile(file: ZLFile): ZipFile =
            synchronized(ourZipFileMap) {
                val cached = if (file.isCached()) ourZipFileMap[file] else null
                cached ?: ZipFile(file).also { zf ->
                    if (file.isCached()) {
                        ourZipFileMap[file] = zf
                    }
                }
            }

        @JvmStatic
        fun removeFromCache(file: ZLFile) {
            ourZipFileMap.remove(file)
        }
    }
}
