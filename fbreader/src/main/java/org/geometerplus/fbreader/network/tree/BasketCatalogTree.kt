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

import org.geometerplus.fbreader.network.BasketItem
import org.geometerplus.fbreader.network.NetworkBookItem
import org.geometerplus.fbreader.network.NetworkItem
import org.geometerplus.fbreader.tree.FBTree
import org.geometerplus.zlibrary.core.network.QuietNetworkContext
import java.util.TreeSet

class BasketCatalogTree : NetworkCatalogTree {
    private var myGeneration: Long = -1

    constructor(parent: NetworkCatalogTree, item: BasketItem, position: Int) : super(parent, parent.getLink(), item, position) {
        if (item.bookIds().isNotEmpty()) {
            startItemsLoader(QuietNetworkContext(), false, false)
        }
    }

    constructor(parent: RootTree, item: BasketItem) : super(parent, item.link, item, 0) {
        if (item.bookIds().isNotEmpty()) {
            startItemsLoader(QuietNetworkContext(), false, false)
        }
    }

    override fun canUseParentCover(): Boolean = false

    @Synchronized
    override fun subtrees(): List<FBTree> {
        val basketItem = Item as BasketItem
        val generation = basketItem.getGeneration()
        if (generation != myGeneration) {
            myGeneration = generation
            val toRemove = ArrayList<FBTree>()
            val idsToAdd = TreeSet(basketItem.bookIds())
            for (t in super.subtrees()) {
                if (t !is NetworkBookTree) {
                    continue
                }
                val bookTree = t
                if (basketItem.contains(bookTree.book)) {
                    idsToAdd.remove(bookTree.book.id)
                } else {
                    toRemove.add(bookTree)
                }
            }
            for (t in toRemove) {
                t.removeSelf()
            }
            for (id in idsToAdd) {
                val book = basketItem.getBook(id)
                if (book != null) {
                    NetworkTreeFactory.createNetworkTree(this, book)
                }
            }
        }
        return super.subtrees()
    }

    @Synchronized
    override fun addItem(i: NetworkItem) {
        if (i !is NetworkBookItem) {
            return
        }
        val bookItem = i
        val id = bookItem.stringId
        for (t in subtrees()) {
            if (t is NetworkBookTree && id == t.book.stringId) {
                return
            }
        }

        val basketItem = Item as BasketItem
        if (basketItem.contains(bookItem)) {
            super.addItem(bookItem)
            basketItem.addItem(bookItem)
        }
    }
}
