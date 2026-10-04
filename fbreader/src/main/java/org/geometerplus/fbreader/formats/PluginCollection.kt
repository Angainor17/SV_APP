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

package org.geometerplus.fbreader.formats

import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.filetypes.FileType
import org.geometerplus.zlibrary.core.filetypes.FileTypeCollection
import org.geometerplus.zlibrary.core.util.SystemInfo
import java.util.ArrayList
import java.util.Collections
import java.util.LinkedList

class PluginCollection private constructor(systemInfo: SystemInfo) : IFormatPluginCollection {

    private val myBuiltinPlugins = LinkedList<BuiltinFormatPlugin>()
    private val myExternalPlugins = LinkedList<ExternalFormatPlugin>()

    init {
        myExternalPlugins.add(DjVuPlugin(systemInfo))
        myExternalPlugins.add(PDFPlugin(systemInfo))
        myExternalPlugins.add(ComicBookPlugin(systemInfo))
    }

    override fun getPlugin(file: ZLFile): FormatPlugin? {
        val fileType = FileTypeCollection.Instance.typeForFile(file)
        val plugin = getPlugin(fileType)
        if (plugin is ExternalFormatPlugin) {
            return if (file === file.getPhysicalFile()) plugin else null
        }
        return plugin
    }

    fun getPlugin(fileType: FileType?): FormatPlugin? {
        if (fileType == null) {
            return null
        }

        for (p in myBuiltinPlugins) {
            if (fileType.Id.equals(p.supportedFileType(), ignoreCase = true)) {
                return p
            }
        }
        for (p in myExternalPlugins) {
            if (fileType.Id.equals(p.supportedFileType(), ignoreCase = true)) {
                return p
            }
        }
        return null
    }

    fun plugins(): List<FormatPlugin> {
        val all = ArrayList<FormatPlugin>()
        all.addAll(myBuiltinPlugins)
        all.addAll(myExternalPlugins)
        Collections.sort(all) { p0, p1 ->
            val diff = p0.priority() - p1.priority()
            if (diff != 0) diff else p0.supportedFileType().compareTo(p1.supportedFileType())
        }
        return all
    }

    private external fun nativePlugins(systemInfo: SystemInfo): Array<NativeFormatPlugin>

    private external fun free()

    @Suppress("DEPRECATION")
    @Throws(Throwable::class)
    protected fun finalize() {
        free()
    }

    companion object {
        @Volatile
        private var ourInstance: PluginCollection? = null

        init {
            System.loadLibrary("NativeFormats-v4")
        }

        @JvmStatic
        fun Instance(systemInfo: SystemInfo): PluginCollection {
            if (ourInstance == null) {
                createInstance(systemInfo)
            }
            return ourInstance!!
        }

        @Synchronized
        private fun createInstance(systemInfo: SystemInfo) {
            if (ourInstance == null) {
                val instance = PluginCollection(systemInfo)
                ourInstance = instance

                // This code cannot be moved to constructor
                // because nativePlugins() is a native method
                for (p in instance.nativePlugins(systemInfo)) {
                    instance.myBuiltinPlugins.add(p)
                    System.err.println("native plugin: $p")
                }
            }
        }

        @JvmStatic
        fun deleteInstance() {
            if (ourInstance != null) {
                ourInstance = null
            }
        }
    }
}
