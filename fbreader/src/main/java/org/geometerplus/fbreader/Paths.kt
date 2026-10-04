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

package org.geometerplus.fbreader

import android.content.Context
import android.os.Environment
import org.geometerplus.zlibrary.core.options.ZLStringListOption
import org.geometerplus.zlibrary.core.options.ZLStringOption
import org.geometerplus.zlibrary.core.util.SystemInfo
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.ArrayList
import java.util.LinkedList
import java.util.Locale

object Paths {
    @JvmField
    val BookPathOption: ZLStringListOption =
        pathOption("BooksDirectory", defaultBookDirectory())

    @JvmField
    val FontPathOption: ZLStringListOption =
        pathOption("FontPathOption", cardDirectory() + "/Fonts")

    @JvmField
    val WallpaperPathOption: ZLStringListOption =
        pathOption("WallpapersDirectory", cardDirectory() + "/Wallpapers")

    @JvmField
    val DownloadsDirectoryOption: ZLStringOption =
        ZLStringOption("Files", "DownloadsDirectory", "")

    private val ourTempDirectoryOption =
        ZLStringOption("Files", "TemporaryDirectory", "")

    init {
        if ("" == DownloadsDirectoryOption.getValue()) {
            DownloadsDirectoryOption.setValue(mainBookDirectory())
        }
    }

    @JvmStatic
    fun TempDirectoryOption(context: Context?): ZLStringOption {
        if ("" == ourTempDirectoryOption.getValue()) {
            ourTempDirectoryOption.setValue(internalTempDirectoryValue(context))
        }
        return ourTempDirectoryOption
    }

    private fun internalTempDirectoryValue(context: Context?): String {
        val d = if (context != null) context.getExternalCacheDir() else null
        if (d != null) {
            d.mkdirs()
            if (d.exists() && d.isDirectory()) {
                return d.path
            }
        }
        return mainBookDirectory() + "/.FBReader"
    }

    private fun addDirToList(list: MutableList<String>, candidate: String?) {
        val dir = candidate ?: return
        if (!dir.startsWith("/")) {
            return
        }
        var c = dir
        for (count in 0 until 5) {
            while (c.endsWith("/")) {
                c = c.substring(0, c.length - 1)
            }
            val f = File(c)
            try {
                val canonical = f.canonicalPath
                if (canonical == c) {
                    break
                }
                c = canonical
            } catch (t: Throwable) {
                return
            }
        }
        while (c.endsWith("/")) {
            c = c.substring(0, c.length - 1)
        }
        if ("" != c && !list.contains(c) && File(c).canRead()) {
            list.add(c)
        }
    }

    @JvmStatic
    fun allCardDirectories(): List<String> {
        val dirs = LinkedList<String>()
        dirs.add(cardDirectory())
        addDirToList(dirs, System.getenv("SECONDARY_STORAGE"))
        return dirs
    }

    @JvmStatic
    fun cardDirectory(): String {
        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            return Environment.getExternalStorageDirectory().path
        }

        val dirNames = LinkedList<String>()
        var reader: BufferedReader? = null
        try {
            reader = BufferedReader(FileReader("/proc/self/mounts"))
            var line = reader.readLine()
            while (line != null) {
                val parts = line.split(Regex("\\s+"))
                if (parts.size >= 4 &&
                    parts[2].lowercase(Locale.ROOT).indexOf("fat") >= 0 &&
                    parts[3].indexOf("rw") >= 0
                ) {
                    val fsDir = File(parts[1])
                    if (fsDir.isDirectory() && fsDir.canWrite()) {
                        dirNames.add(fsDir.path)
                    }
                }
                line = reader.readLine()
            }
        } catch (e: Throwable) {
        } finally {
            try {
                reader?.close()
            } catch (t: Throwable) {
            }
        }

        for (dir in dirNames) {
            if (dir.lowercase(Locale.ROOT).indexOf("media") > 0) {
                return dir
            }
        }
        if (dirNames.isNotEmpty()) {
            return dirNames[0]
        }
        return Environment.getExternalStorageDirectory().path
    }

    private fun defaultBookDirectory(): String = cardDirectory() + "/Books"

    private fun pathOption(key: String, defaultDirectory: String): ZLStringListOption {
        val option = ZLStringListOption(
            "Files", key, emptyList<String>(), "\n"
        )
        if (option.getValue().isEmpty()) {
            option.setValue(listOf(defaultDirectory))
        }
        return option
    }

    @JvmStatic
    fun bookPath(): List<String> {
        val path = ArrayList<String>(BookPathOption.getValue())
        val downloadsDirectory = DownloadsDirectoryOption.getValue()
        if ("" != downloadsDirectory && !path.contains(downloadsDirectory)) {
            path.add(downloadsDirectory)
        }
        return path
    }

    @JvmStatic
    fun mainBookDirectory(): String {
        val bookPath = BookPathOption.getValue()
        return if (bookPath.isEmpty()) defaultBookDirectory() else bookPath[0]
    }

    @JvmStatic
    fun systemInfo(context: Context): SystemInfo {
        val appContext = context.applicationContext
        return object : SystemInfo {
            override fun tempDirectory(): String? {
                val value = ourTempDirectoryOption.getValue()
                if ("" != value) {
                    return value
                }
                return internalTempDirectoryValue(appContext)
            }

            override fun networkCacheDirectory(): String? = tempDirectory() + "/cache"
        }
    }

    @JvmStatic
    fun systemShareDirectory(): String = "/system/usr/share/FBReader"
}
