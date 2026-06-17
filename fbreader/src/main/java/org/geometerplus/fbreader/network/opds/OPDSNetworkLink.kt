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

import org.geometerplus.fbreader.network.AbstractNetworkLink
import org.geometerplus.fbreader.network.BasketItem
import org.geometerplus.fbreader.network.NetworkCatalogItem
import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.fbreader.network.NetworkOperationData
import org.geometerplus.fbreader.network.authentication.NetworkAuthenticationManager
import org.geometerplus.fbreader.network.tree.NetworkItemsLoader
import org.geometerplus.fbreader.network.urlInfo.UrlInfo
import org.geometerplus.fbreader.network.urlInfo.UrlInfoCollection
import org.geometerplus.fbreader.network.urlInfo.UrlInfoWithDate
import org.geometerplus.zlibrary.core.network.ZLNetworkRequest
import org.geometerplus.zlibrary.core.util.MimeType
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

abstract class OPDSNetworkLink(
    protected val library: NetworkLibrary,
    id: Int,
    title: String,
    summary: String?,
    language: String?,
    infos: UrlInfoCollection<UrlInfoWithDate>
) : AbstractNetworkLink(id, title, summary, language, infos) {

    private val urlRewritingRules = mutableListOf<URLRewritingRule>()
    private val extraData = mutableMapOf<String, String>()
    private var relationAliases: Map<RelationAlias, String>? = null
    private var authenticationManager: NetworkAuthenticationManager? = null
    private var basketItemValue: BasketItem? = null

    internal fun setRelationAliases(relationAliases: Map<RelationAlias, String>?) {
        if (relationAliases != null && relationAliases.isNotEmpty()) {
            this.relationAliases = relationAliases.toMap()
        } else {
            this.relationAliases = null
        }
    }

    internal fun setUrlRewritingRules(rules: List<URLRewritingRule>) {
        urlRewritingRules.clear()
        urlRewritingRules.addAll(rules)
    }

    internal fun setExtraData(extraData: Map<String, String>) {
        this.extraData.clear()
        this.extraData.putAll(extraData)
    }

    internal fun setAuthenticationManager(mgr: NetworkAuthenticationManager?) {
        authenticationManager = mgr
    }

    internal fun createNetworkData(url: String?, state: OPDSCatalogItem.State): ZLNetworkRequest? {
        if (url == null) {
            return null
        }
        val catalogItem = state.loader?.tree?.Item ?: return null
        library.startLoading(catalogItem)
        val processedUrl = rewriteUrl(url, false)
        return object : ZLNetworkRequest.Get(processedUrl) {
            override fun handleStream(inputStream: InputStream, length: Int) {
                if (state.loader?.confirmInterruption() == true) {
                    return
                }

                OPDSXMLReader(
                    library, OPDSFeedHandler(library, url, state), false
                ).read(inputStream)

                if (state.loader?.confirmInterruption() == true && state.lastLoadedId != null) {
                    // reset state to load current page from the beginning
                    state.lastLoadedId = null
                } else {
                    state.loader?.tree?.confirmAllItems()
                }
            }

            override fun doAfter(success: Boolean) {
                library.stopLoading(catalogItem)
            }
        }
    }

    override fun createOperationData(loader: NetworkItemsLoader?): OPDSCatalogItem.State {
        return OPDSCatalogItem.State(this, loader!!)
    }

    override fun simpleSearchRequest(pattern: String, data: NetworkOperationData): ZLNetworkRequest? {
        val info = getUrlInfo(UrlInfo.Type.Search)
        if (info == null || info.url == null || !MimeType.APP_ATOM_XML.weakEquals(info.mime)) {
            return null
        }
        var encodedPattern = pattern
        try {
            encodedPattern = URLEncoder.encode(pattern, "utf-8")
        } catch (e: UnsupportedEncodingException) {
        }
        return createNetworkData(info.url.replace("%s", encodedPattern), data as OPDSCatalogItem.State)
    }

    override fun resume(data: NetworkOperationData): ZLNetworkRequest? {
        return createNetworkData(data.resumeURI, data as OPDSCatalogItem.State)
    }

    override fun libraryItem(): NetworkCatalogItem? {
        val urlMap = UrlInfoCollection<UrlInfo>()
        urlMap.addInfo(getUrlInfo(UrlInfo.Type.Catalog))
        urlMap.addInfo(getUrlInfo(UrlInfo.Type.Image))
        urlMap.addInfo(getUrlInfo(UrlInfo.Type.Thumbnail))
        return LibraryCatalogItem(
            this,
            title,
            summary,
            urlMap,
            NetworkCatalogItem.Accessibility.ALWAYS,
            NetworkCatalogItem.FLAGS_DEFAULT or NetworkCatalogItem.FLAG_ADD_SEARCH_ITEM,
            extraData
        )
    }

    private class LibraryCatalogItem(
        link: OPDSNetworkLink,
        title: String,
        summary: String?,
        urls: UrlInfoCollection<*>,
        accessibility: Accessibility,
        flags: Int,
        extraData: Map<String, String>?
    ) : OPDSCatalogItem(link, title, summary, urls, accessibility, flags, extraData) {
        private val linkSummary: String? = summary

        override fun getSummary(): String? = linkSummary
    }

    override fun authenticationManager(): NetworkAuthenticationManager? = authenticationManager

    override fun rewriteUrl(url: String, isUrlExternal: Boolean): String {
        val apply = if (isUrlExternal)
            URLRewritingRule.APPLY_EXTERNAL
        else
            URLRewritingRule.APPLY_INTERNAL
        var result = url
        for (rule in urlRewritingRules) {
            if ((rule.whereToApply() and apply) != 0) {
                result = rule.apply(result)
            }
        }
        return result
    }

    // rel and type must be either null or interned String objects.
    internal fun relation(rel: String?, type: MimeType?): String? {
        if (relationAliases == null) {
            return rel
        }
        var alias = RelationAlias(rel, type?.name)
        var mapped = relationAliases!![alias]
        if (mapped != null) {
            return mapped
        }
        if (type != null) {
            alias = RelationAlias(rel, null)
            mapped = relationAliases!![alias]
            if (mapped != null) {
                return mapped
            }
        }
        return rel
    }

    override val basketItem: BasketItem?
        get() {
            val url = getUrl(UrlInfo.Type.ListBooks)
            if (url != null && basketItemValue == null) {
                basketItemValue = OPDSBasketItem(library, this)
            }
            return basketItemValue
        }

    override fun toString(): String {
        return "OPDSNetworkLink: {super=${super.toString()}" +
                "; authManager=${authenticationManager?.javaClass?.name}" +
                "; relationAliases=$relationAliases" +
                "; rewritingRules=$urlRewritingRules" +
                "}"
    }

    companion object {
        const val INVALID_ID = -1
    }
}
