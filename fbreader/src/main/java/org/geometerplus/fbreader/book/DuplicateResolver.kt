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

package org.geometerplus.fbreader.book

import org.fbreader.util.ComparisonUtil
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.filesystem.ZLPhysicalFile
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.LinkedList

class DuplicateResolver {
    private val myMap: MutableMap<String, MutableList<ZLFile>> =
        Collections.synchronizedMap(HashMap<String, MutableList<ZLFile>>())

    fun addFile(file: ZLFile) {
        val key = file.getShortName()
        val list: MutableList<ZLFile> = synchronized(myMap) {
            val existing = myMap[key]
            if (existing == null) {
                val created = LinkedList<ZLFile>()
                myMap[key] = created
                created
            } else {
                existing
            }
        }
        synchronized(list) {
            if (!list.contains(file)) {
                list.add(file)
            }
        }
    }

    fun removeFile(file: ZLFile) {
        val list = myMap[file.getShortName()]
        if (list != null) {
            synchronized(list) {
                list.remove(file)
            }
        }
    }

    private fun entryName(file: ZLFile): String? {
        val path = file.getPath()
        val index = path.indexOf(":")
        return if (index == -1) null else path.substring(index + 1)
    }

    fun findDuplicate(file: ZLFile): ZLFile? {
        val pFile = file.getPhysicalFile()
        if (pFile == null) {
            return null
        }
        val list = myMap[file.getShortName()]
        if (list == null || list.isEmpty()) {
            return null
        }
        val copy: MutableList<ZLFile> = synchronized(list) {
            ArrayList(list)
        }

        val entry = entryName(file)
        val shortName = pFile.getShortName()
        val size = pFile.size()
        val lastModified = pFile.javaFile().lastModified()
        for (candidate in copy) {
            if (file == candidate) {
                return candidate
            }
            val pCandidate = candidate.getPhysicalFile()
            if (pCandidate != null &&
                ComparisonUtil.equal(entry, entryName(candidate)) &&
                shortName == pCandidate.getShortName() &&
                size == pCandidate.size() &&
                lastModified == pCandidate.javaFile().lastModified()
            ) {
                return candidate
            }
        }
        return null
    }
}
