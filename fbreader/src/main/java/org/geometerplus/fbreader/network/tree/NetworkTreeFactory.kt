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

import org.fbreader.util.Boolean3
import org.geometerplus.fbreader.network.NetworkBookItem
import org.geometerplus.fbreader.network.NetworkCatalogItem
import org.geometerplus.fbreader.network.NetworkItem
import org.geometerplus.fbreader.network.NetworkTree
import org.geometerplus.fbreader.network.TopUpItem

object NetworkTreeFactory {
    @JvmStatic
    fun createNetworkTree(parent: NetworkCatalogTree, item: NetworkItem): NetworkTree? =
        createNetworkTree(parent, item, -1)

    @JvmStatic
    fun createNetworkTree(parent: NetworkCatalogTree, item: NetworkItem, position: Int): NetworkTree? {
        val subtreesSize = parent.subtrees().size
        val pos = when {
            position == -1 -> subtreesSize
            position < 0 || position > subtreesSize ->
                throw IndexOutOfBoundsException("`position` value equals $position but must be in range [0; $subtreesSize]")
            else -> position
        }

        when (item) {
            is NetworkCatalogItem -> {
                if (item.getVisibility() == Boolean3.FALSE) {
                    return null
                }
                return NetworkCatalogTree(parent, parent.getLink(), item, pos)
            }
            is NetworkBookItem -> {
                if (pos != subtreesSize) {
                    throw RuntimeException("Unable to insert NetworkBookItem to the middle of the catalog")
                }

                val flags = parent.Item.flags
                val showAuthors = (flags and NetworkCatalogItem.FLAG_SHOW_AUTHOR) != 0

                when (flags and NetworkCatalogItem.FLAGS_GROUP) {
                    NetworkCatalogItem.FLAG_GROUP_BY_SERIES -> {
                        if (item.seriesTitle == null) {
                            return NetworkBookTree(parent, item, pos, showAuthors)
                        } else {
                            val previous = if (pos > 0) parent.subtrees()[pos - 1] as? NetworkTree else null
                            var seriesTree: NetworkSeriesTree? = null
                            if (previous is NetworkSeriesTree) {
                                seriesTree = previous
                                if (item.seriesTitle != seriesTree.seriesTitle) {
                                    seriesTree = null
                                }
                            }
                            if (seriesTree == null) {
                                seriesTree = NetworkSeriesTree(parent, item.seriesTitle!!, pos, showAuthors)
                            }
                            return NetworkBookTree(seriesTree, item, showAuthors)
                        }
                    }
                    NetworkCatalogItem.FLAG_GROUP_MORE_THAN_1_BOOK_BY_SERIES -> {
                        if (pos > 0 && item.seriesTitle != null) {
                            val previous = parent.subtrees()[pos - 1] as? NetworkTree
                            if (previous is NetworkSeriesTree) {
                                val seriesTree = previous
                                if (item.seriesTitle == seriesTree.seriesTitle) {
                                    return NetworkBookTree(seriesTree, item, showAuthors)
                                }
                            } else if (previous is NetworkBookTree) {
                                val previousBook = previous.book
                                if (item.seriesTitle == previousBook.seriesTitle) {
                                    previous.removeSelf()
                                    val seriesTree = NetworkSeriesTree(parent, item.seriesTitle!!, pos - 1, showAuthors)
                                    NetworkBookTree(seriesTree, previousBook, showAuthors)
                                    return NetworkBookTree(seriesTree, item, showAuthors)
                                }
                            }
                        }
                        return NetworkBookTree(parent, item, pos, showAuthors)
                    }
                    NetworkCatalogItem.FLAG_GROUP_BY_AUTHOR -> {
                        if (item.authors.isEmpty()) {
                            return NetworkBookTree(parent, item, pos, showAuthors)
                        } else {
                            val author = item.authors[0]
                            val previous = if (pos > 0) parent.subtrees()[pos - 1] as? NetworkTree else null
                            var authorTree: NetworkAuthorTree? = null
                            if (previous is NetworkAuthorTree) {
                                authorTree = previous
                                if (author != authorTree.author) {
                                    authorTree = null
                                }
                            }
                            if (authorTree == null) {
                                authorTree = NetworkAuthorTree(parent, author)
                            }
                            return NetworkBookTree(authorTree, item, showAuthors)
                        }
                    }
                    else -> return NetworkBookTree(parent, item, pos, showAuthors)
                }
            }
            is TopUpItem -> return TopUpTree(parent, item)
            else -> return null
        }
    }
}
