/*
 * Copyright (C) 2011-2015 FBReader.ORG Limited <contact@fbreader.org>
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

import org.geometerplus.fbreader.book.AbstractBook
import org.geometerplus.fbreader.book.BookUtil
import org.geometerplus.fbreader.bookmodel.BookModel
import org.geometerplus.fbreader.formats.fb2.FB2NativePlugin
import org.geometerplus.fbreader.formats.oeb.OEBNativePlugin
import org.geometerplus.zlibrary.core.drm.FileEncryptionInfo
import org.geometerplus.zlibrary.core.encodings.EncodingCollection
import org.geometerplus.zlibrary.core.encodings.JavaEncodingCollection
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.image.ZLFileImage
import org.geometerplus.zlibrary.core.image.ZLFileImageProxy
import org.geometerplus.zlibrary.core.util.SystemInfo
import org.geometerplus.zlibrary.text.model.CachedCharStorageException

open class NativeFormatPlugin protected constructor(
    systemInfo: SystemInfo,
    fileType: String
) : BuiltinFormatPlugin(systemInfo, fileType) {

    companion object {
        private val nativeLock = Any()

        @JvmStatic
        fun create(systemInfo: SystemInfo, fileType: String): NativeFormatPlugin {
            return when (fileType) {
                "fb2" -> FB2NativePlugin(systemInfo)
                "ePub" -> OEBNativePlugin(systemInfo)
                else -> NativeFormatPlugin(systemInfo, fileType)
            }
        }
    }

    @Throws(BookReadingException::class)
    @Synchronized
    override fun readMetainfo(book: AbstractBook) {
        val code: Int
        synchronized(nativeLock) {
            code = readMetainfoNative(book)
        }
        if (code != 0) {
            throw BookReadingException(
                "nativeCodeFailure",
                BookUtil.fileByBook(book),
                listOf(code.toString(), book.getPath())
            )
        }
    }

    private external fun readMetainfoNative(book: AbstractBook): Int

    override fun readEncryptionInfos(book: AbstractBook): List<FileEncryptionInfo> {
        val infos: Array<FileEncryptionInfo>?
        synchronized(nativeLock) {
            infos = readEncryptionInfosNative(book)
        }
        return infos?.toList() ?: emptyList()
    }

    private external fun readEncryptionInfosNative(book: AbstractBook): Array<FileEncryptionInfo>?

    @Throws(BookReadingException::class)
    @Synchronized
    override fun readUids(book: AbstractBook) {
        synchronized(nativeLock) {
            readUidsNative(book)
        }
        if (book.uids().isEmpty()) {
            book.addUid(BookUtil.createUid(book, "SHA-256"))
        }
    }

    private external fun readUidsNative(book: AbstractBook): Boolean

    override fun detectLanguageAndEncoding(book: AbstractBook) {
        synchronized(nativeLock) {
            detectLanguageAndEncodingNative(book)
        }
    }

    private external fun detectLanguageAndEncodingNative(book: AbstractBook)

    @Throws(BookReadingException::class)
    @Synchronized
    override fun readModel(model: BookModel) {
        val code: Int
        val tempDirectory = systemInfo.tempDirectory() ?: ""
        synchronized(nativeLock) {
            code = readModelNative(model, tempDirectory)
        }
        when (code) {
            0 -> return
            3 -> throw CachedCharStorageException(
                "Cannot write file from native code to temp directory"
            )
            else -> throw BookReadingException(
                "nativeCodeFailure",
                BookUtil.fileByBook(model.book),
                listOf(code.toString(), model.book.getPath())
            )
        }
    }

    private external fun readModelNative(model: BookModel, cacheDir: String): Int

    override fun readCover(file: ZLFile): ZLFileImageProxy {
        return object : ZLFileImageProxy(file) {
            override fun retrieveRealImage(): ZLFileImage? {
                val box = arrayOfNulls<ZLFileImage>(1)
                synchronized(nativeLock) {
                    readCoverNative(file, box)
                }
                return box[0]
            }
        }
    }

    private external fun readCoverNative(file: ZLFile, box: Array<ZLFileImage?>)

    override fun readAnnotation(file: ZLFile): String? {
        synchronized(nativeLock) {
            return readAnnotationNative(file)
        }
    }

    private external fun readAnnotationNative(file: ZLFile): String?

    override fun priority(): Int = 5

    override fun supportedEncodings(): EncodingCollection = JavaEncodingCollection.Instance()

    override fun toString(): String = "NativeFormatPlugin [${supportedFileType()}]"
}
