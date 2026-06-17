package org.geometerplus.fbreader.formats

import org.geometerplus.fbreader.bookmodel.BookModel
import org.geometerplus.zlibrary.core.util.SystemInfo

abstract class BuiltinFormatPlugin(
    systemInfo: SystemInfo,
    fileType: String
) : FormatPlugin(systemInfo, fileType) {
    @Throws(BookReadingException::class)
    abstract fun readModel(model: BookModel)
}
