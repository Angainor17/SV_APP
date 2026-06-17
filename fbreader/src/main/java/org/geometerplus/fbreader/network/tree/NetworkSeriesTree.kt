/*
 * Copyright (C) 2010-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version)
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
import java.util.TreeSet

class NetworkSeriesTree internal constructor(
    parent: NetworkTree,
    val seriesTitle: String,
    position: Int,
    private val myShowAuthors: Boolean
) : NetworkTree(parent, position) {

    override val name: String
        get() = seriesTitle

    override val summary: String
        get() {
            if (!myShowAuthors) {
                return super.summary
            }

            val builder = StringBuilder()
            var count = 0

            val authorSet = TreeSet<NetworkBookItem.AuthorData>()
            for (tree in subtrees()) {
                if (tree !is NetworkBookTree) {
                    continue
                }
                val book = tree.book

                for (author in book.authors) {
                    if (!authorSet.contains(author)) {
                        authorSet.add(author)
                        if (count++ > 0) {
                            builder.append(",  ")
                        }
                        builder.append(author.displayName)
                        if (count == 5) {
                            return builder.toString()
                        }
                    }
                }
            }
            return builder.toString()
        }

    override fun createCover(): ZLImage? {
        for (tree in subtrees()) {
            if (tree is NetworkBookTree) {
                return tree.getCover()
            }
        }
        return null
    }

    override val stringId: String
        get() = "@Series:$seriesTitle"
}
