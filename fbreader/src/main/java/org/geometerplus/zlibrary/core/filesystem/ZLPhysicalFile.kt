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

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList
import java.util.Collections

class ZLPhysicalFile(private val myFile: File) : ZLFile() {
    private var myIsDirectory: Boolean? = null
    private var myPath: String? = null

    internal constructor(path: String) : this(File(path))

    init {
        init()
    }

    override fun exists(): Boolean = myFile.exists()

    override fun size(): Long = myFile.length()

    override fun lastModified(): Long = myFile.lastModified()

    override fun isDirectory(): Boolean {
        if (myIsDirectory == null) {
            myIsDirectory = myFile.isDirectory
        }
        return myIsDirectory!!
    }

    override fun isReadable(): Boolean = myFile.canRead()

    fun delete(): Boolean = myFile.delete()

    fun javaFile(): File = myFile

    override fun getPath(): String {
        var path = myPath
        if (path == null) {
            path = try {
                myFile.canonicalPath
            } catch (e: Exception) {
                // should be never thrown
                myFile.path
            }
            myPath = path
        }
        return path
    }

    override fun getLongName(): String = if (isDirectory()) getPath() else myFile.name

    override fun getParent(): ZLFile? =
        if (isDirectory()) null else ZLPhysicalFile(myFile.parent!!)

    override fun getPhysicalFile(): ZLPhysicalFile = this

    @Throws(IOException::class)
    override fun getInputStream(): InputStream = FileInputStream(myFile)

    protected override fun directoryEntries(): List<ZLFile> {
        val subFiles = myFile.listFiles()
        if (subFiles == null || subFiles.isEmpty()) {
            return Collections.emptyList()
        }

        val entries = ArrayList<ZLFile>(subFiles.size)
        for (f in subFiles) {
            if (!f.name.startsWith(".")) {
                entries.add(ZLPhysicalFile(f))
            }
        }
        return entries
    }
}
