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

package org.geometerplus.android.fbreader.libraryService

import org.geometerplus.fbreader.book.AbstractBookCollection
import org.geometerplus.fbreader.book.Author
import org.geometerplus.fbreader.book.Book
import org.geometerplus.fbreader.book.BookQuery
import org.geometerplus.fbreader.book.Bookmark
import org.geometerplus.fbreader.book.BookmarkQuery
import org.geometerplus.fbreader.book.Filter
import org.geometerplus.fbreader.book.HighlightingStyle
import org.geometerplus.fbreader.book.IBookCollection
import org.geometerplus.fbreader.book.Tag
import org.geometerplus.fbreader.book.UID
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition
import org.geometerplus.zlibrary.text.view.ZLTextPosition

/**
 * A placeholder book collection.
 *
 * The original implementation proxied every call to a [LibraryService] via AIDL.
 * That service is no longer registered in the app, so at runtime every call already
 * returned an empty or default result. This class now returns those same results
 * directly, without the IPC layer.
 */
class BookCollectionShadow : AbstractBookCollection<Book>() {
    override fun status(): IBookCollection.Status = IBookCollection.Status.NotStarted

    override fun size(): Int = 0

    override fun books(query: BookQuery): List<Book> = emptyList()

    override fun hasBooks(filter: Filter): Boolean = false

    override fun titles(query: BookQuery): List<String> = emptyList()

    override fun recentlyOpenedBooks(count: Int): List<Book> = emptyList()

    override fun recentlyAddedBooks(count: Int): List<Book> = emptyList()

    override fun getRecentBook(index: Int): Book? = null

    override fun addToRecentlyOpened(book: Book) {
    }

    override fun removeFromRecentlyOpened(book: Book) {
    }

    override fun getBookByFile(path: String): Book? = null

    override fun getBookById(id: Long): Book? = null

    override fun getBookByUid(uid: UID): Book? = null

    override fun getBookByHash(hash: String): Book? = null

    override fun labels(): List<String> = emptyList()

    override fun authors(): List<Author> = emptyList()

    override fun hasSeries(): Boolean = false

    override fun series(): List<String> = emptyList()

    override fun tags(): List<Tag> = emptyList()

    override fun firstTitleLetters(): List<String> = emptyList()

    override fun saveBook(book: Book): Boolean = false

    override fun canRemoveBook(book: Book, deleteFromDisk: Boolean): Boolean = false

    override fun removeBook(book: Book, deleteFromDisk: Boolean) {
    }

    override fun getHash(book: Book, force: Boolean): String? = null

    override fun setHash(book: Book, hash: String) {
    }

    override fun getStoredPosition(bookId: Long): ZLTextFixedPosition.WithTimestamp? = null

    override fun storePosition(bookId: Long, position: ZLTextPosition) {
    }

    override fun isHyperlinkVisited(book: Book, linkId: String): Boolean = false

    override fun markHyperlinkAsVisited(book: Book, linkId: String) {
    }

    override fun getCoverUrl(book: Book): String? = null

    override fun getDescription(book: Book): String? = null

    override fun bookmarks(query: BookmarkQuery): List<Bookmark> = emptyList()

    override fun saveBookmark(bookmark: Bookmark) {
    }

    override fun deleteBookmark(bookmark: Bookmark) {
    }

    override fun deletedBookmarkUids(): List<String> = emptyList()

    override fun purgeBookmarks(uids: List<String>) {
    }

    override fun getHighlightingStyle(styleId: Int): HighlightingStyle? = null

    override fun highlightingStyles(): List<HighlightingStyle> = emptyList()

    override fun saveHighlightingStyle(style: HighlightingStyle) {
    }

    override fun getDefaultHighlightingStyleId(): Int = 1

    override fun setDefaultHighlightingStyleId(styleId: Int) {
    }

    override fun formats(): List<IBookCollection.FormatDescriptor> = emptyList()

    override fun setActiveFormats(formatIds: List<String>): Boolean = false

    override fun rescan(path: String) {
    }

    override fun createBook(id: Long, url: String?, title: String?, encoding: String?, language: String?): Book {
        return Book(id, url!!.substring("file://".length), title, encoding, language)
    }
}
