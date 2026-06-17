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

import org.geometerplus.zlibrary.core.util.MimeType
import org.geometerplus.zlibrary.core.util.ZLNetworkUtil

internal class OPDSCatalogInfoHandler(
    private val baseURL: String,
    private val link: OPDSNetworkLink,
    private val opensearchDescriptionURLs: MutableList<String>
) : AbstractOPDSFeedHandler() {

    var feedStarted = false
    var icon: String? = null
    var title: CharSequence? = null
    var summary: CharSequence? = null
    var directOpenSearchDescription: OpenSearchDescription? = null

    override fun processFeedMetadata(feed: OPDSFeedMetadata, beforeEntries: Boolean): Boolean {
        icon = feed.icon?.uri?.let { ZLNetworkUtil.url(baseURL, it) }
        title = feed.title
        summary = feed.subtitle

        for (linkItem in feed.links) {
            val href = linkItem.href ?: continue
            val mime = MimeType.get(linkItem.type)
            val rel = link.relation(linkItem.rel, mime)
            if ("search" == rel) {
                val fullUrl = ZLNetworkUtil.url(baseURL, href)
                if (MimeType.APP_OPENSEARCHDESCRIPTION == mime) {
                    fullUrl?.let { opensearchDescriptionURLs.add(it) }
                } else if (MimeType.APP_ATOM_XML.weakEquals(mime) || MimeType.TEXT_HTML.weakEquals(mime)) {
                    fullUrl?.let { tmpl ->
                        val descr = OpenSearchDescription.createDefault(tmpl, mime)
                        if (descr.isValid()) {
                            directOpenSearchDescription = descr
                        }
                    }
                }
            }
        }
        return true
    }

    override fun processFeedStart() {
        feedStarted = true
    }

    override fun processFeedEnd() {}

    override fun processFeedEntry(entry: OPDSEntry): Boolean = true
}
