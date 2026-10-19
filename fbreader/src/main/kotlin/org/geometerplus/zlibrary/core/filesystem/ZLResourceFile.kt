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

import org.geometerplus.zlibrary.core.library.ZLibrary
import java.util.Collections
import java.util.HashMap

abstract class ZLResourceFile protected constructor(private val myPath: String) : ZLFile() {
    init {
        `init`()
    }

    override fun getPath(): String = myPath

    override fun getLongName(): String =
        myPath.substring(myPath.lastIndexOf('/') + 1)

    override fun getPhysicalFile(): ZLPhysicalFile? = null

    companion object {
        private val ourCache =
            Collections.synchronizedMap(HashMap<String, ZLResourceFile>())

        @JvmStatic
        fun createResourceFile(path: String): ZLResourceFile {
            var file = ourCache[path]
            if (file == null) {
                file = ZLibrary.Instance().createResourceFile(path)
                ourCache[path] = file
            }
            return file
        }

        @JvmStatic
        fun createResourceFile(parent: ZLResourceFile, name: String): ZLResourceFile =
            ZLibrary.Instance().createResourceFile(parent, name)
    }
}
