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

package org.geometerplus.zlibrary.core.filesystem.tar

import org.geometerplus.zlibrary.core.filesystem.ZLArchiveEntryFile
import org.geometerplus.zlibrary.core.filesystem.ZLFile

import java.io.IOException
import java.io.InputStream
import java.util.LinkedList

class ZLTarEntryFile(
    parent: ZLFile,
    name: String,
) : ZLArchiveEntryFile(parent, name) {
    override fun exists(): Boolean =
        myParent.exists() && ZLTarEntryFile.archiveEntries(myParent).contains(this)

    override fun size(): Long =
        throw RuntimeException("Not implemented yet.")

    @Throws(IOException::class)
    override fun getInputStream(): InputStream =
        ZLTarInputStream(myParent.getInputStream()!!, myName)

    companion object {
        @JvmStatic
        fun archiveEntries(archive: ZLFile): List<ZLFile> {
            try {
                val stream = archive.getInputStream()
                if (stream != null) {
                    val entries = LinkedList<ZLFile>()
                    val header = ZLTarHeader()
                    while (header.read(stream)) {
                        if (header.IsRegularFile) {
                            entries.add(ZLTarEntryFile(archive, header.Name!!))
                        }
                        val lenToSkip = (header.Size + 0x1ff) and -0x200
                        if (lenToSkip < 0) {
                            break
                        }
                        if (stream.skip(lenToSkip.toLong()) != lenToSkip.toLong()) {
                            break
                        }
                        header.erase()
                    }
                    stream.close()
                    return entries
                }
            } catch (e: IOException) {
            }
            return emptyList()
        }
    }
}
