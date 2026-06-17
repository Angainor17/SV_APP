package org.geometerplus.fbreader.formats

import org.geometerplus.fbreader.book.AbstractBook
import org.geometerplus.zlibrary.core.drm.FileEncryptionInfo
import org.geometerplus.zlibrary.core.encodings.EncodingCollection
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.image.ZLImage
import org.geometerplus.zlibrary.core.resources.ZLResource
import org.geometerplus.zlibrary.core.util.SystemInfo

abstract class FormatPlugin protected constructor(
    val systemInfo: SystemInfo,
    private val fileType: String
) {
    fun supportedFileType(): String = fileType

    fun name(): String = ZLResource.resource("format").getResource(fileType).value

    @Throws(BookReadingException::class)
    open fun realBookFile(file: ZLFile): ZLFile = file

    open fun readEncryptionInfos(book: AbstractBook): List<FileEncryptionInfo> = emptyList()

    @Throws(BookReadingException::class)
    abstract fun readMetainfo(book: AbstractBook)

    @Throws(BookReadingException::class)
    abstract fun readUids(book: AbstractBook)

    @Throws(BookReadingException::class)
    abstract fun detectLanguageAndEncoding(book: AbstractBook)

    abstract fun readCover(file: ZLFile): ZLImage?

    abstract fun readAnnotation(file: ZLFile): String?

    /* lesser is higher: 0 for ePub/fb2, 5 for other native, 10 for external */
    abstract fun priority(): Int

    abstract fun supportedEncodings(): EncodingCollection
}
