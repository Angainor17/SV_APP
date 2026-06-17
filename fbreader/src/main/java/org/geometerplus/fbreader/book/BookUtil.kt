package org.geometerplus.fbreader.book

import org.geometerplus.fbreader.formats.BookReadingException
import org.geometerplus.fbreader.formats.FormatPlugin
import org.geometerplus.fbreader.formats.PluginCollection
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.filesystem.ZLResourceFile

import java.io.IOException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
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
            "data/intro/intro-${locale.language}_${locale.country}.epub"
        )
        if (file.exists()) {
            return file
        }

        file = ZLResourceFile.createResourceFile(
            "data/intro/intro-${locale.language}.epub"
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
        var stream: java.io.InputStream? = null

        try {
            val hash = MessageDigest.getInstance(algorithm)
            stream = file.getInputStream() ?: return null

            val buffer = ByteArray(2048)
            while (true) {
                val nread = stream.read(buffer)
                if (nread == -1) {
                    break
                }
                hash.update(buffer, 0, nread)
            }

            val result = StringBuilder()
            for (b in hash.digest()) {
                result.append(String.format("%02X", b.toInt() and 0xFF))
            }
            return UID(algorithm, result.toString())
        } catch (e: IOException) {
            return null
        } catch (e: NoSuchAlgorithmException) {
            return null
        } finally {
            stream?.let {
                try {
                    it.close()
                } catch (e: IOException) {
                }
            }
        }
    }

    @JvmStatic
    @Throws(BookReadingException::class)
    fun getPlugin(pluginCollection: PluginCollection, book: AbstractBook): FormatPlugin {
        val file = fileByBook(book)
        val plugin = pluginCollection.getPlugin(file)
            ?: throw BookReadingException("pluginNotFound", file)
        return plugin
    }

    @JvmStatic
    fun getEncoding(book: AbstractBook, pluginCollection: PluginCollection): String? {
        if (book.getEncodingNoDetection() == null) {
            try {
                BookUtil.getPlugin(pluginCollection, book).detectLanguageAndEncoding(book)
            } catch (e: BookReadingException) {
            }
            if (book.getEncodingNoDetection() == null) {
                book.setEncoding("utf-8")
            }
        }
        return book.getEncodingNoDetection()
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
    internal fun readMetainfo(book: AbstractBook, pluginCollection: PluginCollection) {
        readMetainfo(book, getPlugin(pluginCollection, book))
    }

    @Throws(BookReadingException::class)
    internal fun readMetainfo(book: AbstractBook, plugin: FormatPlugin) {
        book.encoding = null
        book.setLanguage(null)
        book.setTitle(null)
        book.authors = null
        book.tags = null
        book.seriesInfo = null
        book.uids = null

        book.saveState = AbstractBook.SaveState.NotSaved

        plugin.readMetainfo(book)
        if (book.uids == null || book.uids!!.isEmpty()) {
            plugin.readUids(book)
        }

        if (book.isTitleEmpty) {
            val fileName = fileByBook(book).shortName
            val index = fileName.lastIndexOf('.')
            book.setTitle(if (index > 0) fileName.substring(0, index) else fileName)
        }
    }

    @JvmStatic
    fun fileByBook(book: AbstractBook): ZLFile = if (book is DbBook) {
        book.file
    } else {
        ZLFile.createFileByPath(book.getPath())!!
    }
}
