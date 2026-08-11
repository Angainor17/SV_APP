package com.github.axet.bookreader.screens.viewmodel

import com.github.axet.bookreader.app.Storage
import com.github.axet.bookreader.widgets.FBReaderView
import timber.log.Timber

/**
 * Делегат для работы с закладками в читалке.
 *
 * Отвечает за создание, редактирование, удаление закладок,
 * синхронизацию с FBReaderView и миграцию контекста старых заметок.
 *
 * Выделен из [ReaderViewModel] для уменьшения размера God-класса.
 */
class ReaderBookmarksDelegate(
    private val getState: () -> ReaderState.Content?,
    private val updateState: (ReaderState.Content) -> Unit,
    private val getFBReaderView: () -> FBReaderView?,
    private val getCurrentBook: () -> Storage.Book?,
    private val getStorage: () -> Storage,
    private val onSavePosition: () -> Unit,
) {

    fun syncFromFBook() {
        val book = getCurrentBook() ?: return
        val fbookBookmarks = getFBReaderView()?.book?.info?.bookmarks
        if (fbookBookmarks != null) {
            book.info.bookmarks = fbookBookmarks
        }
        val currentState = getState() ?: return
        updateState(currentState.copy(book = book))
        getStorage().save(book)
    }

    fun edit(bookmark: Storage.Bookmark) {
        val currentState = getState() ?: return
        updateState(currentState.copy(
            showBookmarkEdit = true,
            editingBookmark = bookmark,
            showToc = false,
            showBookmarks = false,
            showFontSettings = false
        ))
    }

    fun saveEdit(bookmark: Storage.Bookmark, name: String, color: Int) {
        bookmark.name = name.ifBlank { null }
        bookmark.color = color
        bookmark.last = System.currentTimeMillis()

        val fbBookmark = getFBReaderView()?.book?.info?.bookmarks?.find {
            it.start.samePositionAs(bookmark.start)
        }
        if (fbBookmark != null) {
            fbBookmark.name = bookmark.name
            fbBookmark.color = bookmark.color
            fbBookmark.last = bookmark.last
        }

        getCurrentBook()?.let { getStorage().save(it) }
        getFBReaderView()?.bookmarksUpdate()
        syncFromFBook()
        hideEditDialog()
    }

    fun add(bookmark: Storage.Bookmark) {
        val book = getCurrentBook() ?: return

        Timber.tag("voronin2").d("========== СОЗДАНИЕ ЗАМЕТКИ ==========")
        Timber.tag("voronin2").d("start: paragraph=${bookmark.start.paragraphIndex}, element=${bookmark.start.elementIndex}")
        Timber.tag("voronin2").d("end: paragraph=${bookmark.end.paragraphIndex}, element=${bookmark.end.elementIndex}")

        bookmark.coverUrl = book.info?.coverUrl
        bookmark.bookFileUri = book.url?.toString()

        val context = extractSentenceContext(bookmark)
        bookmark.sentenceBefore = context?.first
        bookmark.sentenceAfter = context?.second

        book.info.bookmarks.add(bookmark)
        getStorage().save(book)
        getFBReaderView()?.bookmarksUpdate()
        onSavePosition()
    }

    fun delete(bookmark: Storage.Bookmark) {
        val book = getCurrentBook() ?: return

        val index = book.info.bookmarks.indexOfFirst {
            it.start.samePositionAs(bookmark.start) && it.end.samePositionAs(bookmark.end)
        }
        if (index >= 0) {
            book.info.bookmarks.removeAt(index)
        }
        val newBookmarks = Storage.Bookmarks()
        newBookmarks.addAll(book.info.bookmarks)
        book.info.bookmarks = newBookmarks

        val fbBookmarks = getFBReaderView()?.book?.info?.bookmarks
        if (fbBookmarks != null) {
            val fbIndex = fbBookmarks.indexOfFirst {
                it.start.samePositionAs(bookmark.start) && it.end.samePositionAs(bookmark.end)
            }
            if (fbIndex >= 0) {
                fbBookmarks.removeAt(fbIndex)
            }
        }

        getFBReaderView()?.bookmarksUpdate()
        getStorage().save(book)

        val currentState = getState() ?: return
        updateState(currentState.copy(book = book))
    }

    /**
     * Мигрировать контекст старых заметок (без sentenceBefore/sentenceAfter).
     * Должен вызываться ПОСЛЕ инициализации FBReaderView.
     */
    fun migrateContext() {
        val book = getCurrentBook() ?: return
        val fbookBookmarks = getFBReaderView()?.book?.info?.bookmarks ?: return
        if (fbookBookmarks.isEmpty()) return

        Timber.tag("voronin2").d("=== migrateBookmarksContext START: ${fbookBookmarks.size} bookmarks ===")
        var needSave = false

        fbookBookmarks.forEach { bm ->
            if (bm.sentenceBefore != null || bm.sentenceAfter != null) return@forEach
            try {
                val context = getFBReaderView()?.extractSentenceContext(bm)
                if (context != null) {
                    bm.sentenceBefore = context.first
                    bm.sentenceAfter = context.second
                    needSave = true
                }
            } catch (e: Exception) {
                Timber.tag("voronin2").e(e, "Error migrating bookmark context")
            }
        }

        if (needSave) {
            book.info.bookmarks = Storage.Bookmarks().apply { addAll(fbookBookmarks) }
            getStorage().save(book)
            getFBReaderView()?.bookmarksUpdate()
        }
        Timber.tag("voronin2").d("=== migrateBookmarksContext END ===")
    }

    private fun extractSentenceContext(bookmark: Storage.Bookmark): Pair<String, String?>? {
        return try {
            getFBReaderView()?.extractSentenceContext(bookmark)
        } catch (e: Exception) {
            Timber.tag("voronin").e(e, "Error extracting sentence context")
            null
        }
    }

    private fun hideEditDialog() {
        val currentState = getState() ?: return
        updateState(currentState.copy(
            showBookmarkEdit = false,
            editingBookmark = null,
            showToc = false,
            showBookmarks = false,
            showFontSettings = false
        ))
    }
}
