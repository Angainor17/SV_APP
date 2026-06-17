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
import org.geometerplus.fbreader.book.IBookCollection
import org.geometerplus.fbreader.book.Tag
import org.geometerplus.fbreader.formats.PluginCollection
import org.geometerplus.fbreader.tree.FBTree
import org.geometerplus.zlibrary.core.resources.ZLResource

abstract class LibraryTree : FBTree {

    companion object {
        const val ROOT_EXTERNAL_VIEW = "bookshelfView"
        const val ROOT_FOUND = "found"
        const val ROOT_FAVORITES = "favorites"
        const val ROOT_RECENT = "recent"
        const val ROOT_BY_AUTHOR = "byAuthor"
        const val ROOT_BY_TITLE = "byTitle"
        const val ROOT_BY_SERIES = "bySeries"
        const val ROOT_BY_TAG = "byTag"
        const val ROOT_SYNC = "sync"
        const val ROOT_FILE = "fileTree"

        @JvmStatic
        fun resource(): ZLResource = ZLResource.resource("library")
    }

    @JvmField
    val collection: IBookCollection<Book>
    @JvmField
    val pluginCollection: PluginCollection

    protected constructor(collection: IBookCollection<Book>, pluginCollection: PluginCollection) : super() {
        this.collection = collection
        this.pluginCollection = pluginCollection
    }

    protected constructor(parent: LibraryTree) : super(parent) {
        this.collection = parent.collection
        this.pluginCollection = parent.pluginCollection
    }

    protected constructor(parent: LibraryTree, position: Int) : super(parent, position) {
        this.collection = parent.collection
        this.pluginCollection = parent.pluginCollection
    }

    open fun getBook(): Book? = null

    open fun containsBook(book: Book): Boolean = false

    open fun isSelectable(): Boolean = true

    internal fun createTagSubtree(tag: Tag): Boolean {
        val temp = TagTree(collection, pluginCollection, tag)
        val position = java.util.Collections.binarySearch(subtrees(), temp)
        return if (position >= 0) {
            false
        } else {
            TagTree(this, tag, -position - 1)
            true
        }
    }

    internal fun createBookWithAuthorsSubtree(book: Book): Boolean {
        val temp = BookWithAuthorsTree(collection, pluginCollection, book)
        val position = java.util.Collections.binarySearch(subtrees(), temp)
        return if (position >= 0) {
            false
        } else {
            BookWithAuthorsTree(this, book, -position - 1)
            true
        }
    }

    open fun removeBook(book: Book): Boolean {
        val toRemove = mutableListOf<FBTree>()
        for (tree in this) {
            if (tree is BookTree && tree.book == book) {
                toRemove.add(tree)
            }
        }
        for (tree in toRemove) {
            tree.removeSelf()
        }
        return toRemove.isNotEmpty()
    }

    open fun onBookEvent(event: BookEvent, book: Book): Boolean {
        return when (event) {
            BookEvent.Added -> false
            BookEvent.Removed -> removeBook(book)
            BookEvent.Updated -> {
                var changed = false
                for (tree in this) {
                    if (tree is BookTree) {
                        val b = tree.book
                        if (collection.sameBook(b, book)) {
                            b.updateFrom(book)
                            changed = true
                        }
                    }
                }
                changed
            }
            else -> false
        }
    }

    override fun compareTo(tree: FBTree): Int {
        val cmp = super.compareTo(tree)
        return if (cmp == 0) {
            javaClass.simpleName.compareTo(tree.javaClass.simpleName)
        } else cmp
    }
}
