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
import org.geometerplus.fbreader.book.IBookCollection
import org.geometerplus.fbreader.formats.PluginCollection
import org.geometerplus.fbreader.tree.FBTree

class BookInSeriesTree : BookTree {

    internal constructor(collection: IBookCollection<Book>, pluginCollection: PluginCollection, book: Book) : super(collection, pluginCollection, book)

    internal constructor(parent: LibraryTree, book: Book, position: Int) : super(parent, book, position)

    override fun compareTo(tree: FBTree): Int {
        if (tree is BookInSeriesTree) {
            val index0 = book.getSeriesInfo()?.index
            val index1 = tree.book.getSeriesInfo()?.index
            val cmp = when {
                index0 == null -> if (index1 == null) 0 else 1
                index1 == null -> -1
                else -> index0.compareTo(index1)
            }
            if (cmp != 0) return cmp
        }
        return super.compareTo(tree)
    }
}
