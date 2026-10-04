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

import org.geometerplus.fbreader.formats.BookReadingException
import org.geometerplus.fbreader.formats.FormatPlugin
import org.geometerplus.fbreader.formats.PluginCollection
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.filesystem.ZLResourceFile
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Formatter
import java.util.Locale

object BookUtil {
    @JvmStatic
    fun getAnnotation(book: AbstractBook, pluginCollection: PluginCollection): String? {
        return try {
            getPlugin(pluginCollection, book).readAnnotation(fileByBook(book))
        } catch (e: BookReadingException) {
            null
        }
    }

    @JvmStatic
    fun getHelpFile(): ZLResourceFile {
        val locale = Locale.getDefault()

        var file = ZLResourceFile.createResourceFile(
            "data/intro/intro-" + locale.language + "_" + locale.country + ".epub"
        )
        if (file.exists()) {
            return file
        }

        file = ZLResourceFile.createResourceFile(
            "data/intro/intro-" + locale.language + ".epub"
        )
        if (file.exists()) {
            return file
        }

        return ZLResourceFile.createResourceFile("data/intro/intro-en.epub")
    }

    @JvmStatic
    fun createUid(book: AbstractBook, algorithm: String): UID? =
        createUid(fileByBook(book), algorithm)

    @JvmStatic
    fun createUid(file: ZLFile, algorithm: String): UID? {
        var stream: InputStream? = null

        try {
            val hash = MessageDigest.getInstance(algorithm)
            stream = file.getInputStream()

            val buffer = ByteArray(2048)
            while (true) {
                val nread = stream!!.read(buffer)
                if (nread == -1) {
                    break
                }
                hash.update(buffer, 0, nread)
            }

            val f = Formatter()
            for (b in hash.digest()) {
                f.format("%02X", b.toInt() and 0xFF)
            }
            return UID(algorithm, f.toString())
        } catch (e: IOException) {
            return null
        } catch (e: NoSuchAlgorithmException) {
            return null
        } finally {
            if (stream != null) {
                try {
                    stream!!.close()
                } catch (e: IOException) {
                }
            }
        }
    }

    @Throws(BookReadingException::class)
    @JvmStatic
    fun getPlugin(pluginCollection: PluginCollection, book: AbstractBook): FormatPlugin {
        val file = fileByBook(book)
        val plugin = pluginCollection.getPlugin(file)
        if (plugin == null) {
            throw BookReadingException("pluginNotFound", file)
        }
        return plugin
    }

    @JvmStatic
    fun getEncoding(book: AbstractBook, pluginCollection: PluginCollection): String {
        if (book.getEncodingNoDetection() == null) {
            try {
                getPlugin(pluginCollection, book).detectLanguageAndEncoding(book)
            } catch (e: BookReadingException) {
            }
            if (book.getEncodingNoDetection() == null) {
                book.setEncoding("utf-8")
            }
        }
        return book.getEncodingNoDetection()!!
    }

    @JvmStatic
    fun reloadInfoFromFile(book: AbstractBook, pluginCollection: PluginCollection) {
        try {
            readMetainfo(book, pluginCollection)
        } catch (e: BookReadingException) {
            // ignore
        }
    }

    @Throws(BookReadingException::class)
    @JvmStatic
    fun readMetainfo(book: AbstractBook, pluginCollection: PluginCollection) {
        readMetainfo(book, getPlugin(pluginCollection, book))
    }

    @Throws(BookReadingException::class)
    @JvmStatic
    fun readMetainfo(book: AbstractBook, plugin: FormatPlugin) {
        book.myEncoding = null
        book.myLanguage = null
        book.setTitle(null)
        book.myAuthors = null
        book.myTags = null
        book.mySeriesInfo = null
        book.myUids = null

        book.mySaveState = AbstractBook.SaveState.NotSaved

        plugin.readMetainfo(book)
        if (book.myUids == null || book.myUids.isEmpty()) {
            plugin.readUids(book)
        }

        if (book.isTitleEmpty()) {
            val fileName = fileByBook(book).getShortName()
            val index = fileName.lastIndexOf('.')
            book.setTitle(if (index > 0) fileName.substring(0, index) else fileName)
        }
    }

    @JvmStatic
    fun fileByBook(book: AbstractBook?): ZLFile {
        return if (book is DbBook) {
            book.File
        } else {
            ZLFile.createFileByPath(book!!.getPath())!!
        }
    }
}
