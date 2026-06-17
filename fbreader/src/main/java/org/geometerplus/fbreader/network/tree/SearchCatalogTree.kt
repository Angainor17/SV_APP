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
import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.fbreader.network.SearchItem
import org.geometerplus.fbreader.network.urlInfo.UrlInfo
import org.geometerplus.zlibrary.core.image.ZLImage
import org.geometerplus.zlibrary.core.network.ZLNetworkContext
import org.geometerplus.zlibrary.core.util.MimeType

class SearchCatalogTree : NetworkCatalogTree {
    constructor(parent: RootTree, item: SearchItem) : super(parent, null, item, -1) {
        item.setPattern(null)
    }

    constructor(parent: NetworkCatalogTree, item: SearchItem) : super(parent, parent.getLink(), item, -1) {
        item.setPattern(null)
    }

    fun setPattern(pattern: String?) {
        (Item as SearchItem).setPattern(pattern)
    }

    override fun canUseParentCover(): Boolean = false

    override fun isContentValid(): Boolean = true

    override val name: String
        get() {
            val pattern = (Item as SearchItem).getPattern()
            if (pattern != null && library.getStoredLoader(this) == null) {
                return NetworkLibrary.resource().getResource("found").getValue()
            }
            return super.name ?: ""
        }

    override val treeTitle: Pair<String, String?>
        get() = Pair(summary, null)

    override val summary: String
        get() {
            val pattern = (Item as SearchItem).getPattern()
            if (pattern != null) {
                return NetworkLibrary.resource().getResource("found").getResource("summary").getValue().replace("%s", pattern)
            }
            if (library.getStoredLoader(this) != null) {
                return NetworkLibrary.resource().getResource("search").getResource("summaryInProgress").getValue()
            }
            return super.summary
        }

    fun getMimeType(): MimeType = (Item as SearchItem).mimeType

    fun getUrl(pattern: String): String? = (Item as SearchItem).getUrl(pattern)

    fun startItemsLoader(nc: ZLNetworkContext, pattern: String) {
        Searcher(nc, this, pattern).start()
    }

    override fun createCover(): ZLImage? {
        val link: INetworkLink? = getLink()
        if (link == null) {
            return null
        }
        val info = link.getUrlInfo(UrlInfo.Type.SearchIcon)
        return if (info != null) createCoverFromUrl(library, info.url, info.mime) else null
    }
}
