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

import org.geometerplus.fbreader.network.NetworkItem
import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.fbreader.network.SearchItem
import org.geometerplus.zlibrary.core.network.ZLNetworkContext
import org.geometerplus.zlibrary.core.network.ZLNetworkException

internal class Searcher(
    nc: ZLNetworkContext,
    tree: SearchCatalogTree,
    private val pattern: String
) : NetworkItemsLoader(nc, tree) {

    @Volatile
    private var itemFound = false

    override fun doBefore() {
        tree.library.networkSearchPatternOption.value = pattern
    }

    @Throws(ZLNetworkException::class)
    override fun load() {
        val item = tree.Item as SearchItem
        if (pattern == item.getPattern()) {
            if (tree.hasChildren()) {
                itemFound = true
                tree.library.fireModelChangedEvent(
                    NetworkLibrary.ChangeListener.Code.Found, tree
                )
            } else {
                tree.library.fireModelChangedEvent(
                    NetworkLibrary.ChangeListener.Code.NotFound
                )
            }
        } else {
            item.runSearch(networkContext, this, pattern)
        }
    }

    @Synchronized
    override fun onNewItem(item: NetworkItem) {
        if (!itemFound) {
            (tree as SearchCatalogTree).setPattern(pattern)
            tree.clearCatalog()
            tree.library.fireModelChangedEvent(
                NetworkLibrary.ChangeListener.Code.Found, tree
            )
            itemFound = true
        }
        super.onNewItem(item)
    }

    override fun onFinish(exception: ZLNetworkException?, interrupted: Boolean) {
        if (!interrupted && !itemFound) {
            tree.library.fireModelChangedEvent(NetworkLibrary.ChangeListener.Code.NotFound)
        }
    }
}
