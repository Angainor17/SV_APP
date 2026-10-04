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
import java.util.Collections
import java.util.HashMap
import java.util.LinkedHashSet
import java.util.LinkedList

class FileInfoSet private constructor(
    private val myDatabase: BooksDatabase,
    infos: MutableCollection<FileInfo>
) {
    private val myInfosByFile = HashMap<ZLFile, FileInfo>()
    private val myFilesByInfo = HashMap<FileInfo, ZLFile>()
    private val myInfosByPair = HashMap<Pair<String, FileInfo?>, FileInfo>()
    private val myInfosById = HashMap<Long, FileInfo>()

    private val myInfosToSave = LinkedHashSet<FileInfo>()
    private val myInfosToRemove = LinkedHashSet<FileInfo>()

    init {
        load(infos)
    }

    constructor(database: BooksDatabase) : this(database, database.loadFileInfos())

    constructor(database: BooksDatabase, file: ZLFile) : this(database, database.loadFileInfos(file))

    constructor(database: BooksDatabase, fileId: Long) : this(database, database.loadFileInfos(fileId))

    private fun load(infos: MutableCollection<FileInfo>) {
        for (info in infos) {
            myInfosByPair[Pair(info.Name, info.Parent)] = info
            myInfosById[info.Id] = info
        }
    }

    fun save() {
        myDatabase.executeAsTransaction {
            for (info in myInfosToRemove) {
                myDatabase.removeFileInfo(info.Id)
                myInfosByPair.remove(Pair(info.Name, info.Parent))
            }
            myInfosToRemove.clear()
            for (info in myInfosToSave) {
                myDatabase.saveFileInfo(info)
            }
            myInfosToSave.clear()
        }
    }

    fun check(file: ZLPhysicalFile?, processChildren: Boolean): Boolean {
        if (file == null) {
            return true
        }
        val fileSize = file.size()
        val info = get(file)!!
        if (info.FileSize == fileSize) {
            return true
        } else {
            info.FileSize = fileSize
            if (processChildren && "epub" != file.getExtension()) {
                removeChildren(info)
                myInfosToSave.add(info)
                addChildren(file)
            } else {
                myInfosToSave.add(info)
            }
            return false
        }
    }

    fun archiveEntries(file: ZLFile): List<ZLFile> {
        val info = get(file)!!
        if (!info.hasChildren()) {
            return Collections.emptyList()
        }
        val entries = LinkedList<ZLFile>()
        for (child in info.subtrees()) {
            if (!myInfosToRemove.contains(child)) {
                entries.add(ZLArchiveEntryFile.createArchiveEntryFile(file, child.Name)!!)
            }
        }
        return entries
    }

    private fun get(name: String, parent: FileInfo?): FileInfo {
        val pair = Pair(name, parent)
        var info = myInfosByPair[pair]
        if (info == null) {
            info = FileInfo(name, parent)
            myInfosByPair[pair] = info
            myInfosToSave.add(info)
        }
        return info
    }

    private fun get(file: ZLFile?): FileInfo? {
        if (file == null) {
            return null
        }
        var info = myInfosByFile[file]
        if (info == null) {
            info = get(file.getLongName(), get(file.getParent()))
            myInfosByFile[file] = info
        }
        return info
    }

    fun getId(file: ZLFile): Long {
        val info = get(file)
        if (info == null) {
            return -1
        }
        if (info.Id == -1L) {
            save()
        }
        return info.Id
    }

    private fun getFile(info: FileInfo?): ZLFile? {
        if (info == null) {
            return null
        }
        var file = myFilesByInfo[info]
        if (file == null) {
            file = ZLFile.createFile(getFile(info.Parent), info.Name)!!
            myFilesByInfo[info] = file
        }
        return file
    }

    fun getFile(id: Long): ZLFile? = getFile(myInfosById[id])

    private fun removeChildren(info: FileInfo) {
        for (child in info.subtrees()) {
            if (myInfosToSave.contains(child)) {
                myInfosToSave.remove(child)
            } else {
                myInfosToRemove.add(child)
            }
            removeChildren(child)
        }
    }

    private fun addChildren(file: ZLFile) {
        for (child in file.children()) {
            val info = get(child)!!
            if (myInfosToRemove.contains(info)) {
                myInfosToRemove.remove(info)
            } else {
                myInfosToSave.add(info)
            }
            addChildren(child)
        }
    }
}
