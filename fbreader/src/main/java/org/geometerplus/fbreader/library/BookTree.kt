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
import org.geometerplus.fbreader.book.CoverUtil
import org.geometerplus.fbreader.book.IBookCollection
import org.geometerplus.fbreader.formats.PluginCollection
import org.geometerplus.fbreader.tree.FBTree
import org.geometerplus.zlibrary.core.image.ZLImage

open class BookTree : LibraryTree {

    @JvmField
    val book: Book

    internal constructor(collection: IBookCollection<Book>, pluginCollection: PluginCollection, book: Book) : super(collection, pluginCollection) {
        this.book = book
    }

    internal constructor(parent: LibraryTree, book: Book) : super(parent) {
        this.book = book
    }

    internal constructor(parent: LibraryTree, book: Book, position: Int) : super(parent, position) {
        this.book = book
    }

    override val name: String get() = book.getTitle()

    override val summary: String get() = ""

    override fun getBook(): Book = book

    override val stringId: String get() = "@BookTree $name"

    override fun createCover(): ZLImage? = CoverUtil.getCover(book, pluginCollection)

    override fun containsBook(book: Book): Boolean = collection.sameBook(book, this.book)

    override val sortKey: String get() = book.getSortKey()

    override fun compareTo(tree: FBTree): Int {
        val cmp = super.compareTo(tree)
        if (cmp == 0 && tree is BookTree) {
            return this.book.getPath().compareTo(tree.book.getPath())
        }
        return cmp
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is BookTree) return false
        return book == other.book
    }

    override fun hashCode(): Int = book.hashCode()
}
