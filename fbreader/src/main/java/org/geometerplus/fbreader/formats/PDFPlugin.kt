package org.geometerplus.fbreader.formats

import org.geometerplus.fbreader.book.AbstractBook
import org.geometerplus.fbreader.book.BookUtil
import org.geometerplus.zlibrary.core.util.SystemInfo
import org.pdfparse.model.PDFDocInfo
import org.pdfparse.model.PDFDocument

class PDFPlugin(systemInfo: SystemInfo) : ExternalFormatPlugin(systemInfo, "PDF") {

    override fun packageName(): String = "org.geometerplus.fbreader.plugin.pdf"

    override fun readMetainfo(book: AbstractBook) {
        val file = BookUtil.fileByBook(book)
        if (file != file.physicalFile) {
            // TODO: throw BookReadingException
            System.err.println("Only physical PDF files are supported")
            return
        }
        try {
            val doc = PDFDocument(book.getPath())
            // TODO: solution for rc4 encryption
            if (!doc.isEncrypted) {
                val info: PDFDocInfo = doc.documentInfo
                book.setTitle(info.title)
                book.addAuthor(info.author)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun readUids(book: AbstractBook) {
        if (book.uids().isEmpty()) {
            book.addUid(BookUtil.createUid(book, "SHA-256"))
        }
    }
}
