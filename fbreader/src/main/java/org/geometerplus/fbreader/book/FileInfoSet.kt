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

package org.geometerplus.fbreader.book

import org.fbreader.util.Pair
import org.geometerplus.zlibrary.core.filesystem.ZLArchiveEntryFile
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.filesystem.ZLPhysicalFile

class FileInfoSet private constructor(
    private val database: BooksDatabase,
    infos: Collection<FileInfo>
) {
    private val infosByFile: MutableMap<ZLFile, FileInfo> = HashMap()
    private val filesByInfo: MutableMap<FileInfo, ZLFile> = HashMap()
    private val infosByPair: MutableMap<Pair<String, FileInfo?>, FileInfo> = HashMap()
    private val infosById: MutableMap<Long, FileInfo> = HashMap()

    private val infosToSave: MutableSet<FileInfo> = LinkedHashSet()
    private val infosToRemove: MutableSet<FileInfo> = LinkedHashSet()

    constructor(database: BooksDatabase) : this(database, database.loadFileInfos())

    constructor(database: BooksDatabase, file: ZLFile) : this(database, database.loadFileInfos(file))

    internal constructor(database: BooksDatabase, fileId: Long) : this(database, database.loadFileInfos(fileId))

    init {
        for (info in infos) {
            infosByPair[Pair(info.name, info.parent)] = info
            infosById[info.id] = info
        }
    }

    fun save() {
        database.executeAsTransaction {
            for (info in infosToRemove) {
                database.removeFileInfo(info.id)
                infosByPair.remove(Pair(info.name, info.parent))
            }
            infosToRemove.clear()
            for (info in infosToSave) {
                database.saveFileInfo(info)
            }
            infosToSave.clear()
        }
    }

    fun check(file: ZLPhysicalFile?, processChildren: Boolean): Boolean {
        if (file == null) {
            return true
        }
        val fileSize = file.size()
        val info = get(file) ?: return true
        if (info.fileSize == fileSize) {
            return true
        } else {
            info.fileSize = fileSize
            if (processChildren && file.extension != "epub") {
                removeChildren(info)
                infosToSave.add(info)
                addChildren(file)
            } else {
                infosToSave.add(info)
            }
            return false
        }
    }

    fun archiveEntries(file: ZLFile): List<ZLFile> {
        val info = get(file) ?: return emptyList()
        if (!info.hasChildren()) {
            return emptyList()
        }
        val entries = mutableListOf<ZLFile>()
        for (child in info.subtrees()) {
            if (!infosToRemove.contains(child)) {
                ZLArchiveEntryFile.createArchiveEntryFile(file, child.name)?.let {
                    entries.add(it)
                }
            }
        }
        return entries
    }

    private fun get(name: String, parent: FileInfo?): FileInfo {
        val pair = Pair(name, parent)
        var info = infosByPair[pair]
        if (info == null) {
            info = FileInfo(name, parent)
            infosByPair[pair] = info
            infosToSave.add(info)
        }
        return info
    }

    private fun get(file: ZLFile?): FileInfo? {
        if (file == null) {
            return null
        }
        var info = infosByFile[file]
        if (info == null) {
            info = get(file.longName, get(file.parent))
            infosByFile[file] = info
        }
        return info
    }

    fun getId(file: ZLFile?): Long {
        val info = get(file) ?: return -1
        if (info.id == -1L) {
            save()
        }
        return info.id
    }

    private fun getFile(info: FileInfo?): ZLFile? {
        if (info == null) {
            return null
        }
        var file = filesByInfo[info]
        if (file == null) {
            file = ZLFile.createFile(getFile(info.parent), info.name)
            if (file != null) {
                filesByInfo[info] = file
            }
        }
        return file
    }

    fun getFile(id: Long): ZLFile? = getFile(infosById[id])

    private fun removeChildren(info: FileInfo) {
        for (child in info.subtrees()) {
            if (infosToSave.contains(child)) {
                infosToSave.remove(child)
            } else {
                infosToRemove.add(child)
            }
            removeChildren(child)
        }
    }

    private fun addChildren(file: ZLFile) {
        for (child in file.children()) {
            val info = get(child) ?: continue
            if (infosToRemove.contains(info)) {
                infosToRemove.remove(info)
            } else {
                infosToSave.add(info)
            }
            addChildren(child)
        }
    }
}
