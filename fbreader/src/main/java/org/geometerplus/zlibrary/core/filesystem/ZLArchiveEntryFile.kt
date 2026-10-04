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

import org.geometerplus.zlibrary.core.filesystem.tar.ZLTarEntryFile
import java.util.Collections

abstract class ZLArchiveEntryFile protected constructor(
    protected val myParent: ZLFile,
    protected val myName: String,
) : ZLFile() {
    init {
        init()
    }

    override fun isDirectory(): Boolean = false

    override fun getPath(): String = myParent.getPath() + ":" + myName

    override fun getLongName(): String = myName

    override fun getParent(): ZLFile = myParent

    override fun getPhysicalFile(): ZLPhysicalFile? {
        var ancestor: ZLFile? = myParent
        while (ancestor != null && ancestor !is ZLPhysicalFile) {
            ancestor = ancestor.getParent()
        }
        return ancestor as ZLPhysicalFile?
    }

    companion object {
        @JvmStatic
        fun normalizeEntryName(entryName: String): String {
            var entryName = entryName
            while (entryName.startsWith("./")) {
                entryName = entryName.substring(2)
            }
            while (true) {
                val index = entryName.lastIndexOf("/./")
                if (index == -1) {
                    break
                }
                entryName = entryName.substring(0, index) + entryName.substring(index + 2)
            }
            while (true) {
                val index = entryName.indexOf("/../")
                if (index <= 0) {
                    break
                }
                val prevIndex = entryName.lastIndexOf('/', index - 1)
                if (prevIndex == -1) {
                    entryName = entryName.substring(index + 4)
                    break
                }
                entryName = entryName.substring(0, prevIndex) + entryName.substring(index + 3)
            }
            return entryName
        }

        @JvmStatic
        fun createArchiveEntryFile(archive: ZLFile?, entryName: String): ZLArchiveEntryFile? {
            if (archive == null) {
                return null
            }
            val name = normalizeEntryName(entryName)
            return when (archive.myArchiveType and ZLFile.ArchiveType.ARCHIVE) {
                ZLFile.ArchiveType.ZIP -> ZLZipEntryFile(archive, name)
                ZLFile.ArchiveType.TAR -> ZLTarEntryFile(archive, name)
                else -> null
            }
        }

        @JvmStatic
        fun archiveEntries(archive: ZLFile): List<ZLFile> =
            when (archive.myArchiveType and ZLFile.ArchiveType.ARCHIVE) {
                ZLFile.ArchiveType.ZIP -> ZLZipEntryFile.archiveEntries(archive)
                ZLFile.ArchiveType.TAR -> ZLTarEntryFile.archiveEntries(archive)
                else -> Collections.emptyList()
            }
    }
}
