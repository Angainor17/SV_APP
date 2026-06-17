package org.geometerplus.fbreader.book

import org.geometerplus.fbreader.formats.BookReadingException
import org.geometerplus.zlibrary.core.util.MiscUtil

internal class BookMergeHelper(private val collection: BookCollection) {

    fun merge(base: DbBook, duplicate: DbBook): Boolean {
        var result = false
        result = result or mergeMetainfo(base, duplicate)
        result = result or mergeBookmarks(base, duplicate, true)
        result = result or mergeBookmarks(base, duplicate, false)
        result = result or mergeLabels(base, duplicate)
        result = result or mergePositions(base, duplicate)
        result = result or mergeProgress(base, duplicate)
        if (result) {
            collection.saveBook(base)
        }
        collection.removeBook(duplicate, false)
        return result
    }

    private fun mergeMetainfo(base: DbBook, duplicate: DbBook): Boolean {
        if (base.hasSameMetainfoAs(duplicate)) {
            return false
        }
        val vanilla: DbBook
        try {
            vanilla = DbBook(base.file, BookUtil.getPlugin(collection.pluginCollection, base))
        } catch (e: BookReadingException) {
            return false
        }
        base.merge(duplicate, vanilla)
        return true
    }

    private fun mergeLabels(base: DbBook, duplicate: DbBook): Boolean {
        val labels = duplicate.labels()
        if (MiscUtil.listsEquals(labels, base.labels())) {
            return false
        }
        for (l in labels) {
            base.addNewLabel(l.name)
        }
        return true
    }

    private fun mergePositions(base: DbBook, duplicate: DbBook): Boolean {
        if (collection.getStoredPosition(base.getId()) != null) {
            return false
        }
        val position = collection.getStoredPosition(duplicate.getId()) ?: return false
        collection.storePosition(base.getId(), position)
        return true
    }

    private fun mergeProgress(base: DbBook, duplicate: DbBook): Boolean {
        if (base.getProgress() != null) {
            return false
        }
        val progress = duplicate.getProgress() ?: return false
        base.setProgress(progress)
        return true
    }

    private fun allBookmarks(book: DbBook, visible: Boolean): List<Bookmark> {
        var result: MutableList<Bookmark>? = null
        var query = BookmarkQuery(book, 20, visible)
        while (true) {
            val portion = collection.bookmarks(query)
            if (portion.isEmpty()) {
                break
            }
            if (result == null) {
                result = ArrayList(portion)
            } else {
                result.addAll(portion)
            }
            query = query.next()
        }
        return result ?: emptyList()
    }

    private fun hasSameBookmark(original: List<Bookmark>, bookmark: Bookmark): Boolean {
        for (b in original) {
            if (b.sameAs(bookmark)) {
                return true
            }
        }
        return false
    }

    private fun mergeBookmarks(base: DbBook, duplicate: DbBook, visible: Boolean): Boolean {
        val duplicateBookmarks = allBookmarks(duplicate, visible)
        if (duplicateBookmarks.isEmpty()) {
            return false
        }
        val baseBookmarks = allBookmarks(base, visible)
        var result = false
        for (b in duplicateBookmarks) {
            if (!hasSameBookmark(baseBookmarks, b)) {
                val clone = b.transferToBook(base)
                if (clone != null) {
                    collection.saveBookmark(clone)
                }
                result = true
            }
        }
        return result
    }
}
