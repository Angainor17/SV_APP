/*
 * Copyright (C) 2009-2015 FBReader.ORG Limited <contact@fbreader.org>
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

import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.util.RationalNumber
import org.geometerplus.zlibrary.core.util.ZLColor
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition
import org.geometerplus.zlibrary.text.view.ZLTextPosition

abstract class BooksDatabase {

    protected fun createBook(id: Long, fileId: Long, title: String?, encoding: String?, language: String?): DbBook? {
        val infos = FileInfoSet(this, fileId)
        return createBook(id, infos.getFile(fileId), title, encoding, language)
    }

    protected open fun createBook(id: Long, file: ZLFile?, title: String?, encoding: String?, language: String?): DbBook? =
        file?.let { DbBook(id, it, title, encoding, language) }

    protected fun addAuthor(book: DbBook, author: Author) {
        book.addAuthorWithNoCheck(author)
    }

    protected fun addTag(book: DbBook, tag: Tag) {
        book.addTagWithNoCheck(tag)
    }

    protected fun setSeriesInfo(book: DbBook, series: String?, index: String?) {
        book.setSeriesInfoWithNoCheck(series, index)
    }

    internal abstract fun executeAsTransaction(actions: Runnable)

    // returns map fileId -> book
    internal abstract fun loadBooks(infos: FileInfoSet, existing: Boolean): Map<Long, DbBook>

    internal abstract fun setExistingFlag(books: Collection<DbBook>, flag: Boolean)

    internal abstract fun loadBook(bookId: Long): DbBook?

    internal abstract fun loadBookByFile(fileId: Long, file: ZLFile): DbBook?

    internal abstract fun deleteBook(bookId: Long)

    internal abstract fun listLabels(): List<String>

    internal abstract fun listAuthors(bookId: Long): MutableList<Author>?

    internal abstract fun listTags(bookId: Long): MutableList<Tag>?

    internal abstract fun listLabels(bookId: Long): MutableList<Label>?

    internal abstract fun getSeriesInfo(bookId: Long): SeriesInfo?

    internal abstract fun listUids(bookId: Long): MutableList<UID>?

    internal abstract fun hasVisibleBookmark(bookId: Long): Boolean

    internal abstract fun getProgress(bookId: Long): RationalNumber?

    internal abstract fun bookIdByUid(uid: UID): Long?

    internal abstract fun updateBookInfo(bookId: Long, fileId: Long, encoding: String?, language: String?, title: String?)

    internal abstract fun insertBookInfo(file: ZLFile, encoding: String?, language: String?, title: String?): Long

    internal abstract fun deleteAllBookAuthors(bookId: Long)

    internal abstract fun saveBookAuthorInfo(bookId: Long, index: Long, author: Author)

    internal abstract fun deleteAllBookTags(bookId: Long)

    internal abstract fun saveBookTagInfo(bookId: Long, tag: Tag)

    internal abstract fun saveBookSeriesInfo(bookId: Long, seriesInfo: SeriesInfo?)

    internal abstract fun deleteAllBookUids(bookId: Long)

    internal abstract fun saveBookUid(bookId: Long, uid: UID)

    internal abstract fun saveBookProgress(bookId: Long, progress: RationalNumber)

    protected fun createFileInfo(id: Long, name: String, parent: FileInfo?): FileInfo =
        FileInfo(name, parent, id)

    internal abstract fun loadFileInfos(): Collection<FileInfo>

    internal abstract fun loadFileInfos(file: ZLFile): Collection<FileInfo>

    internal abstract fun loadFileInfos(fileId: Long): Collection<FileInfo>

    internal abstract fun removeFileInfo(fileId: Long)

    internal abstract fun saveFileInfo(fileInfo: FileInfo)

    internal abstract fun addBookHistoryEvent(bookId: Long, event: Int)

    internal abstract fun removeBookHistoryEvents(bookId: Long, event: Int)

    internal abstract fun loadRecentBookIds(event: Int, limit: Int): List<Long>

    internal abstract fun addLabel(bookId: Long, label: Label)

    internal abstract fun removeLabel(bookId: Long, label: Label)

    protected fun createBookmark(
        id: Long,
        uid: String?,
        versionUid: String?,
        bookId: Long,
        bookTitle: String?,
        text: String?,
        originalText: String?,
        creationTimestamp: Long,
        modificationTimestamp: Long?,
        accessTimestamp: Long?,
        modelId: String?,
        startParagraphIndex: Int,
        startWordIndex: Int,
        startCharIndex: Int,
        endParagraphIndex: Int,
        endWordIndex: Int,
        endCharIndex: Int,
        isVisible: Boolean,
        styleId: Int
    ): Bookmark = Bookmark(
        id, uid, versionUid,
        bookId, bookTitle, text, originalText,
        creationTimestamp, modificationTimestamp, accessTimestamp,
        modelId,
        startParagraphIndex, startWordIndex, startCharIndex,
        endParagraphIndex, endWordIndex, endCharIndex,
        isVisible,
        styleId
    )

    internal abstract fun loadBookmarks(query: BookmarkQuery): List<Bookmark>

    internal abstract fun saveBookmark(bookmark: Bookmark): Long

    internal abstract fun deleteBookmark(bookmark: Bookmark)

    internal abstract fun deletedBookmarkUids(): List<String>

    internal abstract fun purgeBookmarks(uids: List<String>)

    protected fun createStyle(id: Int, timestamp: Long, name: String?, bgColor: ZLColor?, fgColor: ZLColor?): HighlightingStyle =
        HighlightingStyle(id, timestamp, name ?: "", bgColor, fgColor)

    internal abstract fun loadStyles(): List<HighlightingStyle>

    internal abstract fun saveStyle(style: HighlightingStyle)

    internal abstract fun getStoredPosition(bookId: Long): ZLTextFixedPosition.WithTimestamp?

    internal abstract fun storePosition(bookId: Long, position: ZLTextPosition)

    internal abstract fun loadVisitedHyperlinks(bookId: Long): Collection<String>

    internal abstract fun addVisitedHyperlink(bookId: Long, hyperlinkId: String)

    @Throws(NotAvailable::class)
    internal abstract fun getHash(bookId: Long, lastModified: Long): String?

    @Throws(NotAvailable::class)
    internal abstract fun setHash(bookId: Long, hash: String)

    internal abstract fun bookIdsByHash(hash: String): List<Long>

    internal abstract fun getOptionValue(name: String): String?

    internal abstract fun setOptionValue(name: String, value: String)

    interface HistoryEvent {
        companion object {
            const val Added = 0
            const val Opened = 1
        }
    }

    class NotAvailable : Exception()
}
