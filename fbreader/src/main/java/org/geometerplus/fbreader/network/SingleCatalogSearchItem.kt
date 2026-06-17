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

package org.geometerplus.fbreader.network

import org.geometerplus.fbreader.network.tree.NetworkItemsLoader
import org.geometerplus.fbreader.network.urlInfo.UrlInfo
import org.geometerplus.zlibrary.core.network.ZLNetworkContext
import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.util.MimeType

class SingleCatalogSearchItem(link: INetworkLink) : SearchItem(
    link,
    NetworkLibrary.resource().getResource("search").getResource("summary").value.replace("%s", link.shortName)
) {

    @Throws(ZLNetworkException::class)
    override fun runSearch(nc: ZLNetworkContext, loader: NetworkItemsLoader, pattern: String) {
        val data = link?.createOperationData(loader)
        var request = link?.simpleSearchRequest(pattern, data!!)
        // TODO: possible infinite loop, use "continue link" instead
        while (request != null) {
            nc.perform(request)
            if (loader.confirmInterruption()) {
                return
            }
            request = data?.resume()
        }
    }

    override val mimeType: MimeType
        get() = link?.getUrlInfo(UrlInfo.Type.Search)?.mime ?: MimeType.NULL

    override fun getUrl(pattern: String): String? {
        val info = link?.getUrlInfo(UrlInfo.Type.Search)
        return if (info != null && info.url != null) info.url.replace("%s", pattern) else null
    }
}
