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

package org.geometerplus.fbreader.book

import org.geometerplus.fbreader.formats.BookReadingException
import org.geometerplus.zlibrary.core.util.MiscUtil
import org.geometerplus.zlibrary.text.view.ZLTextPosition
import java.util.ArrayList

class BookMergeHelper(private val myCollection: BookCollection) {

    fun merge(base: DbBook, duplicate: DbBook): Boolean {
        var result = false
        // Каждый mergeX-метод должен выполниться всегда (аналог Java `result |= ...`,
        // где `|` не короткозамкнут), поэтому `||` здесь неприменим.
        if (mergeMetainfo(base, duplicate)) result = true
        if (mergeBookmarks(base, duplicate, true)) result = true
        if (mergeBookmarks(base, duplicate, false)) result = true
        if (mergeLabels(base, duplicate)) result = true
        if (mergePositions(base, duplicate)) result = true
        if (mergeProgress(base, duplicate)) result = true
        if (result) {
            myCollection.saveBook(base)
        }
        myCollection.removeBook(duplicate, false)
        return result
    }

    private fun mergeMetainfo(base: DbBook, duplicate: DbBook): Boolean {
        if (base.hasSameMetainfoAs(duplicate)) {
            return false
        }
        val vanilla: DbBook = try {
            DbBook(base.File, BookUtil.getPlugin(myCollection.PluginCollection, base))
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
            base.addNewLabel(l.Name)
        }
        return true
    }

    private fun mergePositions(base: DbBook, duplicate: DbBook): Boolean {
        if (myCollection.getStoredPosition(base.getId()) != null) {
            return false
        }
        val position: ZLTextPosition? = myCollection.getStoredPosition(duplicate.getId())
        if (position == null) {
            return false
        }
        myCollection.storePosition(base.getId(), position)
        return true
    }

    private fun mergeProgress(base: DbBook, duplicate: DbBook): Boolean {
        if (base.getProgress() != null) {
            return false
        }
        val progress = duplicate.getProgress()
        if (progress == null) {
            return false
        }
        base.setProgress(progress)
        return true
    }

    private fun allBookmarks(book: DbBook, visible: Boolean): List<Bookmark> {
        var result: MutableList<Bookmark>? = null
        var query = BookmarkQuery(book, visible, 20)
        while (true) {
            val portion = myCollection.bookmarks(query)
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
                    myCollection.saveBookmark(clone)
                }
                result = true
            }
        }
        return result
    }
}
