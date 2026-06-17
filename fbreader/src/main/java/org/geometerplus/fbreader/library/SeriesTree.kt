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

import org.geometerplus.fbreader.book.Author
import org.geometerplus.fbreader.book.Book
import org.geometerplus.fbreader.book.Filter
import org.geometerplus.fbreader.book.IBookCollection
import org.geometerplus.fbreader.book.Series
import org.geometerplus.fbreader.formats.PluginCollection

import java.util.Collections

class SeriesTree : FilteredTree {

    @JvmField
    val series: Series

    internal constructor(collection: IBookCollection<Book>, pluginCollection: PluginCollection, series: Series, author: Author?) : super(collection, pluginCollection, filter(series, author)) {
        this.series = series
    }

    internal constructor(parent: LibraryTree, series: Series, author: Author?, position: Int) : super(parent, filter(series, author), position) {
        this.series = series
    }

    companion object {
        private fun filter(series: Series, author: Author?): Filter {
            val f = Filter.BySeries(series)
            return if (author != null) Filter.And(f, Filter.ByAuthor(author)) else f
        }
    }

    override val name: String get() = series.getTitle()

    override val stringId: String get() = "@SeriesTree $name"

    override val sortKey: String get() = series.getSortKey()

    override fun createSubtree(book: Book): Boolean {
        val temp = BookInSeriesTree(collection, pluginCollection, book)
        val position = Collections.binarySearch(subtrees(), temp)
        return if (position >= 0) {
            false
        } else {
            BookInSeriesTree(this, book, -position - 1)
            true
        }
    }
}
