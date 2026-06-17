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

package org.geometerplus.fbreader.network.opds

import org.geometerplus.fbreader.network.BasketItem
import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.fbreader.network.tree.NetworkItemsLoader
import org.geometerplus.fbreader.network.urlInfo.UrlInfo
import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.util.MiscUtil

internal class OPDSBasketItem(library: NetworkLibrary, link: OPDSNetworkLink) : BasketItem(library, link) {

    @Throws(ZLNetworkException::class)
    override fun loadChildren(loader: NetworkItemsLoader) {
        val ids = bookIds()
        if (ids.isEmpty()) {
            return
        }

        if (isFullyLoaded()) {
            for (id in ids) {
                getBook(id)?.let { loader.onNewItem(it) }
            }
            loader.tree.confirmAllItems()
            return
        }

        val opdsLink = link as OPDSNetworkLink
        var url = opdsLink.getUrl(UrlInfo.Type.ListBooks)
        if (url == null) {
            return
        }
        url = url.replace("{ids}", MiscUtil.join(ids, ","))

        val state = opdsLink.createOperationData(loader)
        doLoadChildren(state, opdsLink.createNetworkData(url, state))
    }
}
