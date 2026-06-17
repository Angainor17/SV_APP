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

package org.geometerplus.fbreader.library

import org.geometerplus.fbreader.book.Book
import org.geometerplus.fbreader.book.BookEvent
import org.geometerplus.fbreader.book.BookQuery
import org.geometerplus.fbreader.book.Filter

import java.util.Collections

class TitleListTree internal constructor(root: RootTree) : FirstLevelTree(root, ROOT_BY_TITLE) {

    private var groupByFirstLetter = false

    override val openingStatus: Status get() = Status.ALWAYS_RELOAD_BEFORE_OPENING

    override fun waitForOpening() {
        clear()

        groupByFirstLetter = false

        var letters: List<String>? = null
        if (collection.size() > 9) {
            letters = collection.firstTitleLetters()
            groupByFirstLetter = collection.size() > letters.size * 5 / 4
        }

        if (groupByFirstLetter) {
            for (l in letters!!) {
                createTitleSubtree(l)
            }
        } else {
            var query = BookQuery(Filter.Empty(), 20)
            while (true) {
                val books = collection.books(query)
                if (books.isEmpty()) break
                for (b in books) {
                    createBookWithAuthorsSubtree(b)
                }
                query = query.next()
            }
        }
    }

    override fun onBookEvent(event: BookEvent, book: Book): Boolean {
        return when (event) {
            BookEvent.Added -> {
                if (groupByFirstLetter) {
                    createTitleSubtree(book.firstTitleLetter())
                } else {
                    createBookWithAuthorsSubtree(book)
                }
            }
            BookEvent.Removed -> {
                if (groupByFirstLetter) {
                    false
                } else {
                    super.onBookEvent(event, book)
                }
            }
            BookEvent.Updated -> {
                if (groupByFirstLetter) {
                    createTitleSubtree(book.firstTitleLetter())
                } else {
                    var changed = removeBook(book)
                    changed = changed or createBookWithAuthorsSubtree(book)
                    changed
                }
            }
            else -> super.onBookEvent(event, book)
        }
    }

    private fun createTitleSubtree(prefix: String?): Boolean {
        if (prefix == null) return false
        val temp = TitleTree(collection, pluginCollection, prefix)
        val position = Collections.binarySearch(subtrees(), temp)
        return if (position >= 0) {
            false
        } else {
            TitleTree(this, prefix, -position - 1)
            true
        }
    }
}
