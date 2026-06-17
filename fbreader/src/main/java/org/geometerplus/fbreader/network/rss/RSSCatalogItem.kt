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

package org.geometerplus.fbreader.network.rss

import org.geometerplus.fbreader.network.INetworkLink
import org.geometerplus.fbreader.network.NetworkCatalogItem
import org.geometerplus.fbreader.network.NetworkOperationData
import org.geometerplus.fbreader.network.NetworkURLCatalogItem
import org.geometerplus.fbreader.network.tree.NetworkItemsLoader
import org.geometerplus.fbreader.network.urlInfo.UrlInfoCollection
import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.network.ZLNetworkRequest

open class RSSCatalogItem protected constructor(
    link: INetworkLink,
    title: CharSequence,
    summary: CharSequence?,
    urls: UrlInfoCollection<*>?,
    accessibility: NetworkCatalogItem.Accessibility,
    flags: Int
) : NetworkURLCatalogItem(link, title, summary, urls, accessibility, flags) {

    private var loadingState: State? = null

    @Throws(ZLNetworkException::class)
    override fun loadChildren(loader: NetworkItemsLoader) {
        val rssLink = link as RSSNetworkLink
        loadingState = rssLink.createOperationData(loader) as State
        doLoadChildren(loadingState!!, rssLink.createNetworkData(catalogUrl, loadingState!!))
    }

    @Throws(ZLNetworkException::class)
    private fun doLoadChildren(networkRequest: ZLNetworkRequest?) {
        try {
            super.doLoadChildren(loadingState!!, networkRequest)
        } catch (e: ZLNetworkException) {
            loadingState = null
            throw e
        }
    }

    open class State(link: RSSNetworkLink, loader: NetworkItemsLoader) : NetworkOperationData(link, loader) {
        val loadedIds: HashSet<String> = HashSet()
        var lastLoadedId: String? = null
    }
}
