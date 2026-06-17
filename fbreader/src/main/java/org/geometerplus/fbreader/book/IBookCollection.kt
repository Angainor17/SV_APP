package org.geometerplus.fbreader.book

import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition
import org.geometerplus.zlibrary.text.view.ZLTextPosition

interface IBookCollection<B : AbstractBook> : AbstractSerializer.BookCreator<B> {

    fun addListener(listener: Listener<B>)
    fun removeListener(listener: Listener<B>)

    fun status(): Status

    fun size(): Int

    fun books(query: BookQuery): List<B>

    fun hasBooks(filter: Filter): Boolean

    fun titles(query: BookQuery): List<String>

    fun recentlyOpenedBooks(count: Int): List<B>

    fun recentlyAddedBooks(count: Int): List<B>

    fun getRecentBook(index: Int): B?

    fun addToRecentlyOpened(book: B)

    fun removeFromRecentlyOpened(book: B)

    fun getBookByFile(path: String): B?

    fun getBookById(id: Long): B?

    fun getBookByUid(uid: UID): B?

    fun getBookByHash(hash: String): B?

    fun labels(): List<String>

    fun authors(): List<Author>

    fun hasSeries(): Boolean

    fun series(): List<String>

    fun tags(): List<Tag>

    fun firstTitleLetters(): List<String>

    fun saveBook(book: B): Boolean

    fun canRemoveBook(book: B, deleteFromDisk: Boolean): Boolean

    fun removeBook(book: B, deleteFromDisk: Boolean)

    fun getHash(book: B, force: Boolean): String?

    fun setHash(book: B, hash: String)

    fun sameBook(book0: B, book1: B): Boolean

    fun getStoredPosition(bookId: Long): ZLTextFixedPosition.WithTimestamp?

    fun storePosition(bookId: Long, position: ZLTextPosition)

    fun isHyperlinkVisited(book: B, linkId: String): Boolean

    fun markHyperlinkAsVisited(book: B, linkId: String)

    fun getCoverUrl(book: B): String?

    fun getDescription(book: B): String?

    fun bookmarks(query: BookmarkQuery): List<Bookmark>

    fun saveBookmark(bookmark: Bookmark)

    fun deleteBookmark(bookmark: Bookmark)

    fun deletedBookmarkUids(): List<String>

    fun purgeBookmarks(uids: List<String>)

    fun getHighlightingStyle(styleId: Int): HighlightingStyle?

    fun highlightingStyles(): List<HighlightingStyle>

    fun saveHighlightingStyle(style: HighlightingStyle)

    fun getDefaultHighlightingStyleId(): Int

    fun setDefaultHighlightingStyleId(styleId: Int)

    fun formats(): List<FormatDescriptor>

    // returns true iff active format set is changed
    fun setActiveFormats(formatIds: List<String>): Boolean

    fun rescan(path: String)

    enum class Status(val isComplete: Boolean) {
        NotStarted(false),
        Started(false),
        Succeeded(true),
        Failed(true)
    }

    interface Listener<B : AbstractBook> {
        fun onBookEvent(event: BookEvent, book: B?)
        fun onBuildEvent(status: Status)
    }

    class FormatDescriptor {
        @JvmField
        var id: String? = null
        @JvmField
        var name: String? = null
        @JvmField
        var isActive: Boolean = false
    }
}
