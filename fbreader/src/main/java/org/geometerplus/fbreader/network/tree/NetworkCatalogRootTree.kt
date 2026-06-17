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

import org.fbreader.util.Pair
import org.geometerplus.fbreader.network.INetworkLink
import org.geometerplus.fbreader.network.NetworkCatalogItem
import org.geometerplus.fbreader.tree.FBTree

class NetworkCatalogRootTree : NetworkCatalogTree {

    constructor(parent: RootTree, link: INetworkLink, position: Int) : super(
        parent, link, link.libraryItem() as NetworkCatalogItem, position
    )

    constructor(parent: RootTree, link: INetworkLink) : this(parent, link, -1)

    override val treeTitle: Pair<String, String?>
        get() = Pair(name, null)

    override fun addSpecialTrees() {
        super.addSpecialTrees()
        val basketItem = getLink()?.basketItem
        if (basketItem != null) {
            myChildrenItems.add(basketItem)
            BasketCatalogTree(this, basketItem, -1)
        }
    }

    override fun compareTo(tree: FBTree): Int {
        if (tree !is NetworkCatalogRootTree) {
            return 1
        }
        val thisLink = getLink()
        val otherLink = tree.getLink()
        return if (thisLink != null && otherLink != null) {
            thisLink.compareTo(otherLink)
        } else {
            0
        }
    }
}
