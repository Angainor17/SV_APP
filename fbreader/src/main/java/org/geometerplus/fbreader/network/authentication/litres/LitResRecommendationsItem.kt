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

package org.geometerplus.fbreader.network.authentication.litres

import org.geometerplus.fbreader.network.opds.OPDSCatalogItem
import org.geometerplus.fbreader.network.opds.OPDSNetworkLink
import org.geometerplus.fbreader.network.urlInfo.UrlInfo
import org.geometerplus.fbreader.network.urlInfo.UrlInfoCollection
import org.geometerplus.zlibrary.core.util.ZLNetworkUtil

class LitResRecommendationsItem(
    link: OPDSNetworkLink,
    title: CharSequence,
    summary: CharSequence?,
    urls: UrlInfoCollection<*>
) : OPDSCatalogItem(link, title, summary, urls, Accessibility.HAS_BOOKS, FLAGS_DEFAULT and FLAGS_GROUP.inv(), null) {

    override val catalogUrl: String
        get() {
            val networkLink = link ?: return ""
            val mgr = networkLink.authenticationManager() as? LitResAuthenticationManager
            val builder = StringBuilder()
            var flag = false
            if (mgr != null) {
                for (book in mgr.purchasedBooks()) {
                    if (flag) {
                        builder.append(',')
                    } else {
                        flag = true
                    }
                    builder.append(book.id)
                }
            }
            val basketItem = networkLink.basketItem
            if (basketItem != null) {
                for (bookId in basketItem.bookIds()) {
                    if (flag) {
                        builder.append(',')
                    } else {
                        flag = true
                    }
                    builder.append(bookId)
                }
            }

            return ZLNetworkUtil.appendParameter(getUrl(UrlInfo.Type.Catalog) ?: "", "ids", builder.toString())
        }
}
