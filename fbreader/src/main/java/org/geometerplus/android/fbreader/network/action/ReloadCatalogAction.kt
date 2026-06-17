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

package org.geometerplus.android.fbreader.network.action

import org.geometerplus.R
import org.geometerplus.android.fbreader.network.NetworkLibraryActivity
import org.geometerplus.fbreader.network.NetworkTree
import org.geometerplus.fbreader.network.NetworkURLCatalogItem
import org.geometerplus.fbreader.network.tree.NetworkCatalogTree
import org.geometerplus.fbreader.network.urlInfo.UrlInfo
import org.geometerplus.zlibrary.core.network.ZLNetworkContext

internal class ReloadCatalogAction(activity: NetworkLibraryActivity, private val myNetworkContext: ZLNetworkContext) : CatalogAction(activity, ActionCode.RELOAD_CATALOG, "reload", R.drawable.ic_menu_refresh) {

    override fun isVisible(tree: NetworkTree): Boolean {
        if (!super.isVisible(tree)) {
            return false
        }
        val item = (tree as NetworkCatalogTree).Item
        if (item !is NetworkURLCatalogItem) {
            return false
        }
        return item.getUrl(UrlInfo.Type.Catalog) != null
    }

    override fun isEnabled(tree: NetworkTree): Boolean = myLibrary.getStoredLoader(tree) == null

    override fun run(tree: NetworkTree) {
        if (myLibrary.getStoredLoader(tree) != null) {
            return
        }
        (tree as NetworkCatalogTree).clearCatalog()
        (tree as NetworkCatalogTree).startItemsLoader(myNetworkContext, false, false)
    }
}
