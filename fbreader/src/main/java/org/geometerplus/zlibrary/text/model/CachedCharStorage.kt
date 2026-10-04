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

package org.geometerplus.zlibrary.text.model

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.Collections

class CachedCharStorage(directoryName: String, fileExtension: String, blocksNumber: Int) {
    private val myArray = ArrayList<WeakReference<CharArray>>()
    private val myDirectoryName: String
    private val myFileExtension: String

    init {
        myDirectoryName = "$directoryName/"
        myFileExtension = ".$fileExtension"
        myArray.addAll(Collections.nCopies(blocksNumber, WeakReference<CharArray>(null)))
    }

    private fun fileName(index: Int): String = "$myDirectoryName$index$myFileExtension"

    fun size(): Int = myArray.size

    private fun exceptionMessage(index: Int, extra: String?): String {
        val buffer = StringBuilder("Cannot read ${fileName(index)}")
        if (extra != null) {
            buffer.append("; ").append(extra)
        }
        buffer.append("\n")
        try {
            val dir = File(myDirectoryName)
            buffer.append("ts = ").append(System.currentTimeMillis()).append("\n")
            buffer.append("dir exists = ").append(dir.exists()).append("\n")
            for (f in dir.listFiles()) {
                buffer.append(f.name).append(" :: ")
                buffer.append(f.length()).append(" :: ")
                buffer.append(f.lastModified()).append("\n")
            }
        } catch (t: Throwable) {
            buffer.append(t.javaClass.name)
            buffer.append("\n")
            buffer.append(t.message)
        }
        return buffer.toString()
    }

    fun block(index: Int): CharArray? {
        if (index < 0 || index >= myArray.size) {
            return null
        }
        val cached = myArray[index].get()
        if (cached != null) {
            return cached
        }

        val block: CharArray
        try {
            val file = File(fileName(index))
            val size = file.length().toInt()
            if (size < 0) {
                throw CachedCharStorageException(exceptionMessage(index, "size = $size"))
            }
            block = CharArray(size / 2)
            val reader = InputStreamReader(FileInputStream(file), "UTF-16LE")
            val rd = reader.read(block)
            if (rd != block.size) {
                throw CachedCharStorageException(
                    exceptionMessage(index, "; $rd != ${block.size}"),
                )
            }
            reader.close()
        } catch (e: IOException) {
            throw CachedCharStorageException(exceptionMessage(index, null), e)
        }
        myArray[index] = WeakReference(block)
        return block
    }
}
