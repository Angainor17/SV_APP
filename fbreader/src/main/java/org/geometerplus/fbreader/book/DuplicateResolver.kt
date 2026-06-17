package org.geometerplus.fbreader.book

import org.fbreader.util.ComparisonUtil
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import java.util.Collections
import java.util.LinkedList

internal class DuplicateResolver {
    private val map: MutableMap<String, MutableList<ZLFile>> =
        Collections.synchronizedMap(HashMap())

    fun addFile(file: ZLFile) {
        val key = file.shortName
        val list: MutableList<ZLFile>
        synchronized(map) {
            list = map.getOrPut(key) { LinkedList() }
        }
        synchronized(list) {
            if (!list.contains(file)) {
                list.add(file)
            }
        }
    }

    fun removeFile(file: ZLFile) {
        val list = map[file.shortName]
        if (list != null) {
            synchronized(list) {
                list.remove(file)
            }
        }
    }

    private fun entryName(file: ZLFile): String? {
        val path = file.path
        val index = path.indexOf(":")
        return if (index == -1) null else path.substring(index + 1)
    }

    fun findDuplicate(file: ZLFile): ZLFile? {
        val pFile = file.physicalFile ?: return null
        val list = map[file.shortName] ?: return null
        if (list.isEmpty()) return null

        val copy: List<ZLFile>
        synchronized(list) {
            copy = ArrayList(list)
        }

        val entryName = entryName(file)
        val shortName = pFile.shortName
        val size = pFile.size()
        val lastModified = pFile.javaFile().lastModified()

        for (candidate in copy) {
            if (file == candidate) {
                return candidate
            }
            val pCandidate = candidate.physicalFile
            if (pCandidate != null &&
                ComparisonUtil.equal(entryName, entryName(candidate)) &&
                shortName == pCandidate.shortName &&
                size == pCandidate.size() &&
                lastModified == pCandidate.javaFile().lastModified()
            ) {
                return candidate
            }
        }
        return null
    }
}
