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
import org.geometerplus.fbreader.book.IBookCollection
import org.geometerplus.fbreader.formats.PluginCollection
import org.geometerplus.zlibrary.core.util.MiscUtil

abstract class FilteredTree : LibraryTree {

    private val filter: Filter

    internal constructor(collection: IBookCollection<Book>, pluginCollection: PluginCollection, filter: Filter) : super(collection, pluginCollection) {
        this.filter = filter
    }

    internal constructor(parent: LibraryTree, filter: Filter, position: Int) : super(parent, position) {
        this.filter = filter
    }

    override val summary: String get() =
        MiscUtil.join(collection.titles(BookQuery(filter, 5)), ", ")

    override fun containsBook(book: Book): Boolean =
        filter.matches(book)

    override val openingStatus: Status get() = Status.ALWAYS_RELOAD_BEFORE_OPENING

    protected fun createBookSubtrees() {
        var query = BookQuery(filter, 20)
        while (true) {
            val books = collection.books(query)
            if (books.isEmpty()) break
            for (b in books) {
                createSubtree(b)
            }
            query = query.next()
        }
    }

    override fun waitForOpening() {
        clear()
        createBookSubtrees()
    }

    override fun onBookEvent(event: BookEvent, book: Book): Boolean {
        return when (event) {
            BookEvent.Added -> containsBook(book) && createSubtree(book)
            BookEvent.Updated -> {
                var changed = removeBook(book)
                changed = changed or (containsBook(book) && createSubtree(book))
                changed
            }
            BookEvent.Removed -> super.onBookEvent(event, book)
            else -> super.onBookEvent(event, book)
        }
    }

    internal open abstract fun createSubtree(book: Book): Boolean
}
