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
import org.geometerplus.fbreader.book.Filter
import org.geometerplus.fbreader.book.IBookCollection
import org.geometerplus.fbreader.formats.PluginCollection

class TitleTree : FilteredTree {

    @JvmField
    val prefix: String

    internal constructor(collection: IBookCollection<Book>, pluginCollection: PluginCollection, prefix: String) : super(collection, pluginCollection, Filter.ByTitlePrefix(prefix)) {
        this.prefix = prefix
    }

    internal constructor(parent: LibraryTree, prefix: String, position: Int) : super(parent, Filter.ByTitlePrefix(prefix), position) {
        this.prefix = prefix
    }

    override val name: String get() = prefix

    override val stringId: String get() = "@PrefixTree $name"

    override fun createSubtree(book: Book): Boolean = createBookWithAuthorsSubtree(book)
}
