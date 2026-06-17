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
import org.geometerplus.fbreader.book.SeriesInfo
import org.geometerplus.fbreader.formats.PluginCollection
import java.util.Collections

class AuthorTree : FilteredTree {

    @JvmField
    val author: Author

    internal constructor(collection: IBookCollection<Book>, pluginCollection: PluginCollection, author: Author) : super(collection, pluginCollection, Filter.ByAuthor(author)) {
        this.author = author
    }

    internal constructor(parent: AuthorListTree, author: Author, position: Int) : super(parent, Filter.ByAuthor(author), position) {
        this.author = author
    }

    override val name: String get() =
        if (Author.NULL == author) resource().getResource("unknownAuthor").value else author.displayName

    override val stringId: String get() = "@AuthorTree$sortKey"

    override val sortKey: String? get() {
        if (Author.NULL == author) return null
        return " Author:${author.sortKey}:${author.displayName}"
    }

    private fun getSeriesSubtree(series: Series): SeriesTree {
        val temp = SeriesTree(collection, pluginCollection, series, author)
        val position = Collections.binarySearch(subtrees(), temp)
        return if (position >= 0) {
            subtrees()[position] as SeriesTree
        } else {
            SeriesTree(this, series, author, -position - 1)
        }
    }

    override fun createSubtree(book: Book): Boolean {
        val seriesInfo: SeriesInfo? = book.getSeriesInfo()
        if (seriesInfo != null) {
            return getSeriesSubtree(seriesInfo.series).createSubtree(book)
        }

        val temp = BookTree(collection, pluginCollection, book)
        val position = Collections.binarySearch(subtrees(), temp)
        return if (position >= 0) {
            false
        } else {
            BookTree(this, book, -position - 1)
            true
        }
    }
}
