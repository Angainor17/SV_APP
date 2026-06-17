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

import org.geometerplus.fbreader.network.INetworkLink
import org.geometerplus.fbreader.network.NetworkBookItem
import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.fbreader.network.urlInfo.BookBuyUrlInfo
import org.geometerplus.fbreader.network.urlInfo.BookUrlInfo
import org.geometerplus.fbreader.network.urlInfo.RelatedUrlInfo
import org.geometerplus.fbreader.network.urlInfo.UrlInfo
import org.geometerplus.fbreader.network.urlInfo.UrlInfoCollection
import org.geometerplus.zlibrary.core.money.Money
import org.geometerplus.zlibrary.core.network.ZLNetworkContext
import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.network.ZLNetworkRequest
import org.geometerplus.zlibrary.core.util.MimeType
import org.geometerplus.zlibrary.core.util.ZLNetworkUtil
import java.io.InputStream

open class OPDSBookItem : NetworkBookItem {
    private val library: NetworkLibrary
    @Volatile
    private var informationIsFull = false

    constructor(
        library: NetworkLibrary,
        link: OPDSNetworkLink,
        id: String,
        index: Int,
        title: CharSequence,
        summary: CharSequence?,
        authors: List<AuthorData>,
        tags: List<String>,
        seriesTitle: String?,
        indexInSeries: Float,
        urls: UrlInfoCollection<*>
    ) : super(
        link, id, index,
        title, summary,
        authors, tags.toMutableList(),
        seriesTitle, indexInSeries,
        urls
    ) {
        this.library = library
    }

    internal constructor(
        library: NetworkLibrary,
        networkLink: OPDSNetworkLink,
        entry: OPDSEntry,
        baseUrl: String,
        index: Int
    ) : this(
        library,
        networkLink, entry.id?.uri ?: "", index,
        entry.title ?: "", getAnnotation(entry),
        getAuthors(entry), getTags(entry),
        entry.seriesTitle, entry.seriesIndex,
        getUrls(library, networkLink, entry, baseUrl)
    )

    companion object {
        @Throws(ZLNetworkException::class)
        fun create(library: NetworkLibrary, nc: ZLNetworkContext, link: INetworkLink?, url: String?): OPDSBookItem? {
            if (link == null || url == null) {
                return null
            }

            val handler = CreateBookHandler(library, link, url)
            nc.perform(object : ZLNetworkRequest.Get(url) {
                override fun handleStream(inputStream: InputStream, length: Int) {
                    OPDSXMLReader(library, handler, true).read(inputStream)
                }
            })
            return handler.book
        }

        private fun getAnnotation(entry: OPDSEntry): CharSequence? {
            return entry.content ?: entry.summary
        }

        private fun getAuthors(entry: OPDSEntry): List<AuthorData> {
            val authorPrefix = "author:"
            val authorsPrefix = "authors:"

            val authors = mutableListOf<AuthorData>()
            for (author in entry.authors) {
                var name = author.name ?: continue
                val lowerCased = name.lowercase()
                var index = lowerCased.indexOf(authorPrefix)
                if (index != -1) {
                    name = name.substring(index + authorPrefix.length)
                } else {
                    index = lowerCased.indexOf(authorsPrefix)
                    if (index != -1) {
                        name = name.substring(index + authorsPrefix.length)
                    }
                }
                index = name.indexOf(',')
                val authorData = if (index != -1) {
                    val before = name.substring(0, index).trim()
                    val after = name.substring(index + 1).trim()
                    AuthorData("$after $before", before)
                } else {
                    name = name.trim()
                    index = name.lastIndexOf(' ')
                    AuthorData(name, name.substring(index + 1))
                }
                authors.add(authorData)
            }
            return authors
        }

        private fun getTags(entry: OPDSEntry): List<String> {
            val tags = mutableListOf<String>()
            for (category in entry.categories) {
                var label = category.label
                if (label == null) {
                    label = category.term
                }
                if (label != null) {
                    tags.add(label)
                }
            }
            return tags
        }

        private fun getUrls(
            library: NetworkLibrary,
            networkLink: OPDSNetworkLink?,
            entry: OPDSEntry,
            baseUrl: String
        ): UrlInfoCollection<UrlInfo> {
            val urls = UrlInfoCollection<UrlInfo>()
            for (link in entry.links) {
                val href = ZLNetworkUtil.url(baseUrl, link.href ?: "") ?: continue
                val mime = MimeType.get(link.type)
                var rel = link.rel
                if (networkLink != null) {
                    rel = networkLink.relation(rel, mime)
                }
                val referenceType = typeByRelation(rel)
                when {
                    OPDSConstants.REL_IMAGE_THUMBNAIL == rel || OPDSConstants.REL_THUMBNAIL == rel -> {
                        urls.addInfo(UrlInfo(UrlInfo.Type.Thumbnail, href, mime))
                    }
                    rel != null && rel.startsWith(OPDSConstants.REL_IMAGE_PREFIX) || OPDSConstants.REL_COVER == rel -> {
                        urls.addInfo(UrlInfo(UrlInfo.Type.Image, href, mime))
                    }
                    MimeType.APP_ATOM_XML.weakEquals(mime) && "entry" == mime.getParameter("type") -> {
                        urls.addInfo(UrlInfo(UrlInfo.Type.SingleEntry, href, mime))
                    }
                    UrlInfo.Type.BookBuy == referenceType -> {
                        val opdsLink = link as OPDSLink
                        var price = opdsLink.selectBestPrice()
                        if (price == null) {
                            // FIXME: HACK: price handling must be implemented not through attributes!!!
                            val priceAttribute = entry.getAttribute(OPDSXMLReader.KEY_PRICE)
                            if (priceAttribute != null) {
                                price = Money(priceAttribute)
                            }
                        }
                        // If price is still null, use ZERO as default
                        val finalPrice = price ?: Money.ZERO
                        if (MimeType.TEXT_HTML == mime) {
                            collectReferences(
                                library,
                                urls, opdsLink, href,
                                UrlInfo.Type.BookBuyInBrowser, finalPrice, true
                            )
                        } else {
                            collectReferences(
                                library,
                                urls, opdsLink, href,
                                UrlInfo.Type.BookBuy, finalPrice, false
                            )
                        }
                    }
                    referenceType == UrlInfo.Type.Related -> {
                        urls.addInfo(RelatedUrlInfo(referenceType, link.title ?: "", href, mime))
                    }
                    referenceType == UrlInfo.Type.Comments -> {
                        urls.addInfo(RelatedUrlInfo(referenceType, link.title ?: "", href, mime))
                    }
                    referenceType == UrlInfo.Type.TOC -> {
                        urls.addInfo(UrlInfo(referenceType, href, mime))
                    }
                    referenceType != null -> {
                        if (BookUrlInfo.isMimeSupported(mime, library.systemInfo)) {
                            urls.addInfo(BookUrlInfo(referenceType, href, mime))
                        }
                    }
                }
            }
            return urls
        }

        private fun typeByRelation(rel: String?): UrlInfo.Type? {
            return when {
                rel == null || OPDSConstants.REL_ACQUISITION_SAMPLE_OR_FULL == rel -> UrlInfo.Type.BookFullOrDemo
                OPDSConstants.REL_ACQUISITION == rel || OPDSConstants.REL_ACQUISITION_OPEN == rel -> UrlInfo.Type.Book
                OPDSConstants.REL_ACQUISITION_SAMPLE == rel -> UrlInfo.Type.BookDemo
                OPDSConstants.REL_ACQUISITION_CONDITIONAL == rel -> UrlInfo.Type.BookConditional
                OPDSConstants.REL_ACQUISITION_BUY == rel -> UrlInfo.Type.BookBuy
                OPDSConstants.REL_RELATED == rel -> UrlInfo.Type.Related
                OPDSConstants.REL_CONTENTS == rel -> UrlInfo.Type.TOC
                OPDSConstants.REL_REPLIES == rel -> UrlInfo.Type.Comments
                else -> null
            }
        }

        private fun collectReferences(
            library: NetworkLibrary,
            urls: UrlInfoCollection<UrlInfo>,
            opdsLink: OPDSLink,
            href: String,
            type: UrlInfo.Type,
            price: Money,
            addWithoutFormat: Boolean
        ) {
            var added = false
            for (f in opdsLink.formats) {
                val mime = MimeType.get(f)
                if (BookUrlInfo.isMimeSupported(mime, library.systemInfo)) {
                    urls.addInfo(BookBuyUrlInfo(type, href, mime, price))
                    added = true
                }
            }
            if (!added && addWithoutFormat) {
                urls.addInfo(BookBuyUrlInfo(type, href, MimeType.NULL, price))
            }
        }
    }

    @Synchronized
    override fun isFullyLoaded(): Boolean {
        return informationIsFull || getUrl(UrlInfo.Type.SingleEntry) == null
    }

    @Synchronized
    override fun loadFullInformation(nc: ZLNetworkContext): Boolean {
        if (informationIsFull) {
            return true
        }

        val url = getUrl(UrlInfo.Type.SingleEntry)
        if (url == null) {
            informationIsFull = true
            return true
        }

        return nc.performQuietly(object : ZLNetworkRequest.Get(url) {
            override fun handleStream(inputStream: InputStream, length: Int) {
                OPDSXMLReader(library, LoadInfoHandler(url), true).read(inputStream)
                informationIsFull = true
            }
        })
    }

    override fun createRelatedCatalogItem(info: RelatedUrlInfo): OPDSCatalogItem? {
        return if (MimeType.APP_ATOM_XML.weakEquals(info.mime)) {
            OPDSCatalogItem(link as OPDSNetworkLink, info)
        } else null
    }

    private abstract class SingleEntryFeedHandler(protected val url: String) : AbstractOPDSFeedHandler() {
        override fun processFeedStart() {}
        override fun processFeedMetadata(feed: OPDSFeedMetadata, beforeEntries: Boolean): Boolean = false
        override fun processFeedEnd() {}
    }

    private class CreateBookHandler(
        private val library: NetworkLibrary,
        private val link: INetworkLink,
        url: String
    ) : SingleEntryFeedHandler(url) {
        var book: OPDSBookItem? = null
            private set

        override fun processFeedEntry(entry: OPDSEntry): Boolean {
            book = OPDSBookItem(library, link as OPDSNetworkLink, entry, url, 0)
            return false
        }
    }

    private inner class LoadInfoHandler(url: String) : SingleEntryFeedHandler(url) {
        override fun processFeedEntry(entry: OPDSEntry): Boolean {
            addUrls(getUrls(library, link as OPDSNetworkLink, entry, url))
            val summary = getAnnotation(entry)
            if (summary != null) {
                setSummary(summary)
            }
            return false
        }
    }
}
