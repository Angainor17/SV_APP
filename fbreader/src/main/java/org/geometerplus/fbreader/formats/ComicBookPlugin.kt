package org.geometerplus.fbreader.formats

import org.geometerplus.fbreader.book.AbstractBook
import org.geometerplus.fbreader.book.BookUtil
import org.geometerplus.zlibrary.core.util.SystemInfo

class ComicBookPlugin(systemInfo: SystemInfo) : ExternalFormatPlugin(systemInfo, "CBZ") {

    override fun packageName(): String = "org.geometerplus.fbreader.plugin.comicbook"

    override fun readMetainfo(book: AbstractBook) {
        // TODO: implement
    }

    override fun readUids(book: AbstractBook) {
        if (book.uids().isEmpty()) {
            book.addUid(BookUtil.createUid(book, "SHA-256"))
        }
    }
}
