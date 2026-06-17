package org.geometerplus.fbreader.formats

import org.geometerplus.fbreader.book.AbstractBook
import org.geometerplus.zlibrary.core.encodings.AutoEncodingCollection
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.util.SystemInfo

abstract class ExternalFormatPlugin protected constructor(
    systemInfo: SystemInfo,
    fileType: String
) : FormatPlugin(systemInfo, fileType) {

    override fun priority(): Int = 10

    abstract fun packageName(): String

    override fun readCover(file: ZLFile): PluginImage = PluginImage(file, this)

    override fun supportedEncodings(): AutoEncodingCollection = AutoEncodingCollection()

    override fun detectLanguageAndEncoding(book: AbstractBook) {}

    override fun readAnnotation(file: ZLFile): String? = null

    override fun toString(): String = "ExternalFormatPlugin [${supportedFileType()}]"
}
