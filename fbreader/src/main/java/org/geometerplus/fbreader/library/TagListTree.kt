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
import org.geometerplus.fbreader.book.Tag

class TagListTree internal constructor(root: RootTree) : FirstLevelTree(root, ROOT_BY_TAG) {

    override val openingStatus: Status get() = Status.ALWAYS_RELOAD_BEFORE_OPENING

    override fun waitForOpening() {
        clear()
        for (t in collection.tags()) {
            if (t.parent == null) {
                createTagSubtree(t)
            }
        }
    }

    override fun onBookEvent(event: BookEvent, book: Book): Boolean {
        return when (event) {
            BookEvent.Added, BookEvent.Updated -> {
                val bookTags = book.tags()
                var changed = false
                if (bookTags.isEmpty()) {
                    changed = createTagSubtree(Tag.NULL)
                } else {
                    for (t in bookTags) {
                        if (t.parent == null) {
                            changed = createTagSubtree(t) || changed
                        }
                    }
                }
                changed
            }
            BookEvent.Removed -> false
            else -> false
        }
    }
}
