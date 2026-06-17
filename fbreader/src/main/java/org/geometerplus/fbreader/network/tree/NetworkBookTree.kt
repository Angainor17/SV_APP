/*
 * Copyright (C) 2010-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.fbreader.network.tree

import org.geometerplus.fbreader.network.NetworkBookItem
import org.geometerplus.fbreader.network.NetworkTree
import org.geometerplus.zlibrary.core.image.ZLImage

open class NetworkBookTree : NetworkTree {
    @JvmField
    val book: NetworkBookItem

    private val myShowAuthors: Boolean

    constructor(parent: NetworkTree, book: NetworkBookItem, showAuthors: Boolean) : super(parent) {
        this.book = book
        myShowAuthors = showAuthors
    }

    internal constructor(parent: NetworkTree, book: NetworkBookItem, position: Int, showAuthors: Boolean) : super(parent, position) {
        this.book = book
        myShowAuthors = showAuthors
    }

    override fun canUseParentCover(): Boolean = false

    override val name: String
        get() = book.title.toString()

    override val summary: String
        get() {
            if (!myShowAuthors && book.authors.size < 2) {
                return ""
            }
            val builder = StringBuilder()
            var count = 0
            for (author in book.authors) {
                if (count++ > 0) {
                    builder.append(",  ")
                }
                builder.append(author.displayName)
            }
            return builder.toString()
        }

    override fun createCover(): ZLImage? = createCoverForItem(library, book, true)

    override val stringId: String
        get() = book.stringId
}
