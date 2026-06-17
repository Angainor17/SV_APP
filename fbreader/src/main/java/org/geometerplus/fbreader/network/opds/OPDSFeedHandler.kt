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

import org.geometerplus.fbreader.network.NetworkCatalogItem
import org.geometerplus.fbreader.network.NetworkItem
import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.fbreader.network.TopUpItem
import org.geometerplus.fbreader.network.atom.ATOMId
import org.geometerplus.fbreader.network.authentication.litres.LitResBookshelfItem
import org.geometerplus.fbreader.network.authentication.litres.LitResRecommendationsItem
import org.geometerplus.fbreader.network.urlInfo.BookUrlInfo
import org.geometerplus.fbreader.network.urlInfo.UrlInfo
import org.geometerplus.fbreader.network.urlInfo.UrlInfoCollection
import org.geometerplus.zlibrary.core.util.MimeType
import org.geometerplus.zlibrary.core.util.ZLNetworkUtil

internal class OPDSFeedHandler(
    private val library: NetworkLibrary,
    private val baseURL: String,
    private val data: OPDSCatalogItem.State
) : AbstractOPDSFeedHandler() {

    companion object {
        private const val REL_BOOKSHELF = OPDSConstants.REL_BOOKSHELF
        private const val REL_RECOMMENDATIONS = OPDSConstants.REL_RECOMMENDATIONS
        private const val REL_TOPUP = OPDSConstants.REL_TOPUP
        private const val REL_SUBSECTION = OPDSConstants.REL_SUBSECTION
        private const val REL_ACQUISITION_PREFIX = OPDSConstants.REL_ACQUISITION_PREFIX
        private const val REL_FBREADER_ACQUISITION_PREFIX = OPDSConstants.REL_FBREADER_ACQUISITION_PREFIX
        private const val REL_ACQUISITION = OPDSConstants.REL_ACQUISITION
        private const val REL_ACQUISITION_OPEN = OPDSConstants.REL_ACQUISITION_OPEN
        private const val REL_ALTERNATE = OPDSConstants.REL_ALTERNATE
        private const val REL_RELATED = OPDSConstants.REL_RELATED
        private const val REL_IMAGE_PREFIX = OPDSConstants.REL_IMAGE_PREFIX
        private const val REL_IMAGE_THUMBNAIL = OPDSConstants.REL_IMAGE_THUMBNAIL
        private const val REL_COVER = OPDSConstants.REL_COVER
        private const val REL_THUMBNAIL = OPDSConstants.REL_THUMBNAIL
    }

    private val catalog: NetworkCatalogItem? = data.loader?.tree?.Item
    private var index = 0

    private var nextURL: String? = null
    private var skipUntilId: String? = data.lastLoadedId
    private var foundNewIds = skipUntilId != null

    private var itemsToLoad = -1

    init {
        if (data.link !is OPDSNetworkLink) {
            throw IllegalArgumentException("${data.link} is not an instance of OPDSNetworkLink class")
        }
    }

    override fun processFeedStart() {
        data.resumeURI = baseURL
    }

    override fun processFeedMetadata(feed: OPDSFeedMetadata, beforeEntries: Boolean): Boolean {
        if (beforeEntries) {
            index = feed.opensearchStartIndex - 1
            if (feed.opensearchItemsPerPage > 0) {
                itemsToLoad = feed.opensearchItemsPerPage
                if (feed.opensearchTotalResults >= 0) {
                    itemsToLoad = minOf(maxOf(0, feed.opensearchTotalResults - index), itemsToLoad)
                }
                if (itemsToLoad == 0) {
                    data.resumeURI = null
                    return true
                }
            }
            if ("series" == feed.viewType) {
                catalog?.setFlags((catalog.flags) and NetworkCatalogItem.FLAG_GROUP_BY_AUTHOR.inv() and NetworkCatalogItem.FLAG_GROUP_BY_SERIES.inv())
            } else if ("authors" == feed.viewType) {
                catalog?.setFlags((catalog.flags) and NetworkCatalogItem.FLAG_SHOW_AUTHOR.inv())
            }
        } else {
            val opdsLink = data.link as OPDSNetworkLink
            for (link in feed.links) {
                val mime = MimeType.get(link.type)
                val rel = opdsLink.relation(link.rel, mime)
                if (MimeType.APP_ATOM_XML.weakEquals(mime) && "next" == rel) {
                    nextURL = ZLNetworkUtil.url(baseURL, link.href ?: "")
                }
            }
        }
        return false
    }

    override fun processFeedEnd() {
        if (skipUntilId != null) {
            // Last loaded element was not found => resume error => DO NOT RESUME
            // TODO: notify user about error???
            // TODO: do reload???
            nextURL = null
        }
        data.resumeURI = if (foundNewIds) nextURL else null
        data.lastLoadedId = null
    }

    private fun tryInterrupt(): Boolean {
        val noninterruptableRemainder = 10
        return (itemsToLoad < 0 || itemsToLoad > noninterruptableRemainder) &&
                data.loader?.confirmInterruption() == true
    }

    private fun calculateEntryId(entry: OPDSEntry): String? {
        if (entry.id != null) {
            return entry.id?.uri
        }

        var id: String? = null
        var idMime = MimeType.NULL

        val opdsLink = data.link as OPDSNetworkLink
        for (link in entry.links) {
            val mime = MimeType.get(link.type)
            val rel = opdsLink.relation(link.rel, mime)

            if (rel == null && MimeType.APP_ATOM_XML.weakEquals(mime)) {
                return ZLNetworkUtil.url(baseURL, link.href ?: "")
            }
            if (!BookUrlInfo.isMimeSupported(mime, library.systemInfo)) {
                continue
            }
            if (rel != null &&
                !rel.startsWith(REL_ACQUISITION_PREFIX) &&
                !rel.startsWith(REL_FBREADER_ACQUISITION_PREFIX)
            ) {
                continue
            }
            if (id == null ||
                BookUrlInfo.isMimeBetterThan(mime, idMime) ||
                (idMime == mime && REL_ACQUISITION == rel)
            ) {
                id = ZLNetworkUtil.url(baseURL, link.href ?: "")
                idMime = mime
            }
        }
        return id
    }

    override fun processFeedEntry(entry: OPDSEntry): Boolean {
        if (itemsToLoad >= 0) {
            --itemsToLoad
        }

        if (entry.id == null) {
            val id = calculateEntryId(entry)
            if (id == null) {
                return tryInterrupt()
            }
            entry.id = ATOMId().apply { uri = id }
        }

        val entryId = entry.id?.uri ?: return tryInterrupt()

        if (skipUntilId != null) {
            if (skipUntilId == entryId) {
                skipUntilId = null
            }
            return tryInterrupt()
        }
        data.lastLoadedId = entryId
        if (!foundNewIds && entryId !in data.loadedIds) {
            foundNewIds = true
        }
        data.loadedIds.add(entryId)

        val opdsLink = data.link as OPDSNetworkLink
        var hasBookLink = false
        for (link in entry.links) {
            val mime = MimeType.get(link.type)
            val rel = opdsLink.relation(link.rel, mime)
            if (if (rel == null) {
                    BookUrlInfo.isMimeSupported(mime, library.systemInfo)
                } else {
                    rel == REL_RELATED ||
                    rel.startsWith(REL_ACQUISITION_PREFIX) ||
                    rel.startsWith(REL_FBREADER_ACQUISITION_PREFIX)
                }) {
                hasBookLink = true
                break
            }
        }

        val item: NetworkItem? = if (hasBookLink) {
            OPDSBookItem(library, data.link as OPDSNetworkLink, entry, baseURL, index++).also {
                for (identifier in entry.dcIdentifiers) {
                    it.identifiers.add(identifier)
                }
            }
        } else {
            readCatalogItem(entry)
        }
        item?.let { data.loader?.onNewItem(it) }
        return tryInterrupt()
    }

    private fun readCatalogItem(entry: OPDSEntry): NetworkItem? {
        val opdsLink = data.link as OPDSNetworkLink
        val urlMap = UrlInfoCollection<UrlInfo>()

        var urlIsAlternate = false
        var litresRel: String? = null
        for (link in entry.links) {
            val href = ZLNetworkUtil.url(baseURL, link.href ?: "")
            val mime = MimeType.get(link.type)
            val rel = opdsLink.relation(link.rel, mime)
            if (MimeType.IMAGE_PNG.weakEquals(mime) || MimeType.IMAGE_JPEG.weakEquals(mime)) {
                when {
                    REL_IMAGE_THUMBNAIL == rel || REL_THUMBNAIL == rel -> {
                        urlMap.addInfo(UrlInfo(UrlInfo.Type.Thumbnail, href, mime))
                    }
                    REL_COVER == rel || (rel != null && rel.startsWith(REL_IMAGE_PREFIX)) -> {
                        urlMap.addInfo(UrlInfo(UrlInfo.Type.Image, href, mime))
                    }
                }
            } else if (MimeType.APP_ATOM_XML.weakEquals(mime)) {
                val hasCatalogUrl = urlMap.getInfo(UrlInfo.Type.Catalog) != null
                if (REL_ALTERNATE == rel) {
                    if (!hasCatalogUrl) {
                        urlMap.addInfo(UrlInfo(UrlInfo.Type.Catalog, href, mime))
                        urlIsAlternate = true
                    }
                } else if (!hasCatalogUrl || rel == null || REL_SUBSECTION == rel) {
                    urlMap.addInfo(UrlInfo(UrlInfo.Type.Catalog, href, mime))
                    urlIsAlternate = false
                }
            } else if (MimeType.TEXT_HTML.weakEquals(mime)) {
                if (REL_ACQUISITION == rel ||
                    REL_ACQUISITION_OPEN == rel ||
                    REL_ALTERNATE == rel ||
                    rel == null
                ) {
                    urlMap.addInfo(UrlInfo(UrlInfo.Type.HtmlPage, href, mime))
                }
            } else if (MimeType.APP_LITRES.weakEquals(mime)) {
                urlMap.addInfo(UrlInfo(UrlInfo.Type.Catalog, href, mime))
                litresRel = rel
            }
        }

        if (urlMap.getInfo(UrlInfo.Type.Catalog) == null &&
            urlMap.getInfo(UrlInfo.Type.HtmlPage) == null) {
            return null
        }

        if (urlMap.getInfo(UrlInfo.Type.Catalog) != null && !urlIsAlternate) {
            urlMap.removeAllInfos(UrlInfo.Type.HtmlPage)
        }

        val annotation = entry.summary ?: entry.content

        return when (litresRel) {
            REL_BOOKSHELF -> LitResBookshelfItem(opdsLink, entry.title ?: "", annotation, urlMap)
            REL_RECOMMENDATIONS -> LitResRecommendationsItem(opdsLink, entry.title ?: "", annotation, urlMap)
            REL_TOPUP -> TopUpItem(opdsLink, urlMap)
            null -> OPDSCatalogItem(opdsLink, entry.title ?: "", annotation, urlMap)
            else -> null
        }
    }
}
