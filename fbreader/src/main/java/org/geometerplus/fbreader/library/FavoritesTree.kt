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

import org.fbreader.util.Pair
import org.geometerplus.fbreader.book.AbstractBook
import org.geometerplus.fbreader.book.Book
import org.geometerplus.fbreader.book.Filter
import org.geometerplus.zlibrary.core.resources.ZLResource

class FavoritesTree internal constructor(root: RootTree) : FilteredTree(root, Filter.ByLabel(AbstractBook.FAVORITE_LABEL), -1) {

    private val resource: ZLResource = resource().getResource(ROOT_FAVORITES)

    override val name: String get() = resource.value

    override val treeTitle: Pair<String, String?> get() = Pair(summary, null)

    override val summary: String get() = resource.getResource("summary").value

    override val stringId: String get() = ROOT_FAVORITES

    override fun isSelectable(): Boolean = false

    override val openingStatus: Status get() {
        return if (collection.hasBooks(Filter.ByLabel(AbstractBook.FAVORITE_LABEL))) {
            Status.ALWAYS_RELOAD_BEFORE_OPENING
        } else {
            Status.CANNOT_OPEN
        }
    }

    override val openingStatusMessage: String? get() =
        if (openingStatus == Status.CANNOT_OPEN) "noFavorites" else super.openingStatusMessage

    override fun createSubtree(book: Book): Boolean = createBookWithAuthorsSubtree(book)
}
