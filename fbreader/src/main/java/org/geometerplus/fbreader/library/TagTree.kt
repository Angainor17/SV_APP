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
import org.geometerplus.fbreader.book.Filter
import org.geometerplus.fbreader.book.IBookCollection
import org.geometerplus.fbreader.book.Tag
import org.geometerplus.fbreader.formats.PluginCollection

class TagTree : FilteredTree {

    @JvmField
    val tag: Tag

    internal constructor(collection: IBookCollection<Book>, pluginCollection: PluginCollection, tag: Tag) : super(collection, pluginCollection, Filter.ByTag(tag)) {
        this.tag = tag
    }

    internal constructor(parent: LibraryTree, tag: Tag, position: Int) : super(parent, Filter.ByTag(tag), position) {
        this.tag = tag
    }

    override val name: String get() =
        if (Tag.NULL == tag) resource().getResource("booksWithNoTags").value else tag.Name

    override val stringId: String get() = "@TagTree $name"

    override val sortKey: String? get() = if (Tag.NULL == tag) null else tag.Name

    override fun containsBook(book: Book): Boolean {
        if (Tag.NULL == tag) return book.tags().isEmpty()
        for (t in book.tags()) {
            var current: Tag? = t
            while (current != null) {
                if (current == tag) return true
                current = current.parent
            }
        }
        return false
    }

    override fun waitForOpening() {
        clear()
        if (Tag.NULL != tag) {
            for (t in collection.tags()) {
                if (tag == t.parent) {
                    createTagSubtree(t)
                }
            }
        }
        createBookSubtrees()
    }

    override fun onBookEvent(event: BookEvent, book: Book): Boolean {
        return when (event) {
            BookEvent.Added -> {
                var changed = false
                val bookTags = book.tags()
                if (bookTags.isEmpty()) {
                    if (Tag.NULL == tag) {
                        changed = createBookWithAuthorsSubtree(book)
                    }
                } else {
                    for (t in bookTags) {
                        if (tag == t) {
                            changed = createBookWithAuthorsSubtree(book)
                        } else if (tag == t.parent) {
                            changed = createTagSubtree(t)
                        }
                    }
                }
                changed
            }
            BookEvent.Removed -> super.onBookEvent(event, book)
            BookEvent.Updated -> {
                var changed = removeBook(book)
                val bookTags = book.tags()
                if (bookTags.isEmpty()) {
                    if (Tag.NULL == tag) {
                        changed = createBookWithAuthorsSubtree(book)
                    }
                } else {
                    for (t in bookTags) {
                        if (tag == t) {
                            changed = createBookWithAuthorsSubtree(book)
                        } else if (tag == t.parent) {
                            changed = createTagSubtree(t)
                        }
                    }
                }
                changed
            }
            else -> super.onBookEvent(event, book)
        }
    }

    override fun createSubtree(book: Book): Boolean = createBookWithAuthorsSubtree(book)
}
