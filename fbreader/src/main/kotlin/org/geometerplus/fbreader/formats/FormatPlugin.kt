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

import org.geometerplus.fbreader.book.AbstractBook
import org.geometerplus.zlibrary.core.drm.FileEncryptionInfo
import org.geometerplus.zlibrary.core.encodings.EncodingCollection
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.image.ZLImage
import org.geometerplus.zlibrary.core.resources.ZLResource
import org.geometerplus.zlibrary.core.util.SystemInfo
import java.util.Collections

abstract class FormatPlugin protected constructor(
    systemInfo: SystemInfo,
    fileType: String,
) {
    @JvmField
    val SystemInfo: SystemInfo = systemInfo

    private val myFileType: String = fileType

    fun supportedFileType(): String = myFileType

    fun name(): String = ZLResource.resource("format").getResource(myFileType).getValue()

    @Throws(BookReadingException::class)
    open fun realBookFile(file: ZLFile): ZLFile = file

    open fun readEncryptionInfos(book: AbstractBook): List<FileEncryptionInfo> =
        Collections.emptyList()

    @Throws(BookReadingException::class)
    abstract fun readMetainfo(book: AbstractBook)

    @Throws(BookReadingException::class)
    abstract fun readUids(book: AbstractBook)

    @Throws(BookReadingException::class)
    abstract fun detectLanguageAndEncoding(book: AbstractBook)

    abstract fun readCover(file: ZLFile): ZLImage

    abstract fun readAnnotation(file: ZLFile): String?

    /* lesser is higher: 0 for ePub/fb2, 5 for other native, 10 for external */
    abstract fun priority(): Int

    abstract fun supportedEncodings(): EncodingCollection?
}
