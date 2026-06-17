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

import org.geometerplus.fbreader.network.NetworkCatalogItem
import org.geometerplus.fbreader.network.NetworkItem
import org.geometerplus.fbreader.network.atom.ATOMId

class RSSChannelHandler(
    private val baseURL: String,
    result: RSSCatalogItem.State
) : AbstractRSSChannelHandler() {
    private val catalog: NetworkCatalogItem = result.loader!!.tree.Item
    private val data: RSSCatalogItem.State = result
    private var index: Int = 0
    private var skipUntilId: String? = data.lastLoadedId
    private var foundNewIds: Boolean = skipUntilId != null

    init {
        if (result.link !is RSSNetworkLink) {
            throw IllegalArgumentException("Parameter `result` has invalid `Link` field value: result.Link must be an instance of RSSNetworkLink class.")
        }
    }

    override fun processFeedStart() {}

    override fun processFeedMetadata(feed: RSSChannelMetadata, beforeEntries: Boolean): Boolean = false

    override fun processFeedEntry(entry: RSSItem): Boolean {
        if (entry.id == null) {
            entry.id = ATOMId()
            entry.id?.uri = "id_$index"
        }

        data.lastLoadedId = entry.id?.uri
        if (!foundNewIds && !data.loadedIds.contains(entry.id?.uri)) {
            foundNewIds = true
        }
        entry.id?.uri?.let { data.loadedIds.add(it) }

        val item: NetworkItem? = RSSBookItem(data.link as RSSNetworkLink, entry, baseURL, index++)

        item?.let { data.loader?.onNewItem(it) }
        return false
    }

    override fun processFeedEnd() {}
}
