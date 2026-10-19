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

import org.geometerplus.zlibrary.core.drm.EncryptionMethod
import org.geometerplus.zlibrary.core.drm.FileEncryptionInfo
import org.geometerplus.zlibrary.core.drm.embedding.EmbeddingInputStream
import org.geometerplus.zlibrary.core.util.InputStreamHolder
import java.io.IOException
import java.io.InputStream
import java.util.Collections
import java.util.HashMap
import java.util.Locale

abstract class ZLFile : InputStreamHolder {
    internal var myArchiveType: Int = 0
    private var myExtension: String = ""
    private var myShortName: String = ""
    private var myIsCached: Boolean = false

    companion object {
        private val ourCachedFiles = HashMap<String, ZLFile>()

        @JvmStatic
        fun createFile(parent: ZLFile?, name: String): ZLFile? {
            var file: ZLFile? = null
            if (parent == null) {
                val cached = ourCachedFiles[name]
                if (cached != null) {
                    return cached
                }
                return if (name.isEmpty() || name[0] != '/') {
                    ZLResourceFile.createResourceFile(name)
                } else {
                    ZLPhysicalFile(name)
                }
            } else if (parent is ZLPhysicalFile && parent.getParent() == null) {
                // parent is a directory
                file = ZLPhysicalFile(parent.getPath() + '/' + name)
            } else if (parent is ZLResourceFile) {
                file = ZLResourceFile.createResourceFile(parent, name)
            } else {
                file = ZLArchiveEntryFile.createArchiveEntryFile(parent, name)
            }

            if (ourCachedFiles.isNotEmpty() && file != null) {
                val cached = ourCachedFiles[file.getPath()]
                if (cached != null) {
                    return cached
                }
            }
            return file
        }

        @JvmStatic
        fun createFileByUrl(url: String?): ZLFile? =
            if (url == null || !url.startsWith("file://")) {
                null
            } else {
                createFileByPath(url.substring("file://".length))
            }

        @JvmStatic
        fun createFileByPath(path: String?): ZLFile? {
            if (path == null) {
                return null
            }
            var current: String = path
            val cached = ourCachedFiles[current]
            if (cached != null) {
                return cached
            }

            var len = current.length
            var first = if (len == 0) '*' else current[0]
            if (first != '/') {
                while (len > 1 && first == '.' && current[1] == '/') {
                    current = current.substring(2)
                    len -= 2
                    first = if (len == 0) '*' else current[0]
                }
                return ZLResourceFile.createResourceFile(current)
            }
            val index = current.lastIndexOf(':')
            if (index > 1) {
                val archive = createFileByPath(current.substring(0, index))
                if (archive != null && archive.myArchiveType != 0) {
                    return ZLArchiveEntryFile.createArchiveEntryFile(archive, current.substring(index + 1))
                }
            }
            return ZLPhysicalFile(current)
        }
    }

    protected fun init() {
        val name = getLongName()
        val index = name.lastIndexOf('.')
        myExtension =
            if (index > 0) name.substring(index + 1).lowercase(Locale.ROOT).intern() else ""
        myShortName = name.substring(name.lastIndexOf('/') + 1)

        /*
        if (lowerCaseName.endsWith(".gz")) {
            myNameWithoutExtension = myNameWithoutExtension.substring(0, myNameWithoutExtension.length() - 3);
            lowerCaseName = lowerCaseName.substring(0, lowerCaseName.length() - 3);
            myArchiveType = myArchiveType | ArchiveType.GZIP;
        }
        if (lowerCaseName.endsWith(".bz2")) {
            myNameWithoutExtension = myNameWithoutExtension.substring(0, myNameWithoutExtension.length() - 4);
            lowerCaseName = lowerCaseName.substring(0, lowerCaseName.length() - 4);
            myArchiveType = myArchiveType | ArchiveType.BZIP2;
        }
        */
        var archiveType = ArchiveType.NONE
        if (myExtension === "zip") {
            archiveType = archiveType or ArchiveType.ZIP
        } else if (myExtension === "oebzip") {
            archiveType = archiveType or ArchiveType.ZIP
        } else if (myExtension === "epub") {
            archiveType = archiveType or ArchiveType.ZIP
        } else if (myExtension === "tar") {
            archiveType = archiveType or ArchiveType.TAR
            //} else if (lowerCaseName.endsWith(".tgz")) {
            //nothing to-do myNameWithoutExtension = myNameWithoutExtension.substr(0, myNameWithoutExtension.length() - 3) + "tar";
            //myArchiveType = myArchiveType | ArchiveType.TAR | ArchiveType.GZIP;
        }
        myArchiveType = archiveType
    }

    abstract fun size(): Long

    abstract fun exists(): Boolean

    abstract fun isDirectory(): Boolean

    abstract fun getPath(): String

    abstract fun getParent(): ZLFile?

    abstract fun getPhysicalFile(): ZLPhysicalFile?

    open fun lastModified(): Long {
        val physicalFile = getPhysicalFile()
        return if (physicalFile != null) physicalFile.lastModified() else 0L
    }

    @Throws(IOException::class)
    fun getInputStream(encryptionInfo: FileEncryptionInfo?): InputStream? {
        if (encryptionInfo == null) {
            return getInputStream()
        }

        if (EncryptionMethod.EMBEDDING == encryptionInfo.Method) {
            return EmbeddingInputStream(getInputStream()!!, encryptionInfo.ContentId!!)
        }

        throw IOException("Encryption method " + encryptionInfo.Method + " is not supported")
    }

    fun getUrl(): String = "file://" + getPath()

    open fun isReadable(): Boolean = true

    fun isCompressed(): Boolean = 0 != (myArchiveType and ArchiveType.COMPRESSED)

    fun isArchive(): Boolean = 0 != (myArchiveType and ArchiveType.ARCHIVE)

    abstract fun getLongName(): String

    fun getShortName(): String = myShortName

    fun getExtension(): String = myExtension

    protected open fun directoryEntries(): List<ZLFile> = Collections.emptyList()

    fun children(): List<ZLFile> {
        if (exists()) {
            if (isDirectory()) {
                return directoryEntries()
            } else if (isArchive()) {
                return ZLArchiveEntryFile.archiveEntries(this)
            }
        }
        return Collections.emptyList()
    }

    override fun hashCode(): Int = getPath().hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is ZLFile) {
            return false
        }
        return getPath() == other.getPath()
    }

    override fun toString(): String = "ZLFile [" + getPath() + "]"

    internal fun isCached(): Boolean = myIsCached

    fun setCached(cached: Boolean) {
        myIsCached = cached
        if (cached) {
            ourCachedFiles[getPath()] = this
        } else {
            ourCachedFiles.remove(getPath())
            if (0 != (myArchiveType and ArchiveType.ZIP)) {
                ZLZipEntryFile.removeFromCache(this)
            }
        }
    }

    object ArchiveType {
        const val NONE = 0
        const val GZIP = 0x0001
        const val BZIP2 = 0x0002
        const val COMPRESSED = 0x00ff
        const val ZIP = 0x0100
        const val TAR = 0x0200
        const val ARCHIVE = 0xff00
    }
}
