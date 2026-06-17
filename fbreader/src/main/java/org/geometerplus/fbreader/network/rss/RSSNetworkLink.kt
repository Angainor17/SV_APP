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

import org.geometerplus.fbreader.network.AbstractNetworkLink
import org.geometerplus.fbreader.network.INetworkLink
import org.geometerplus.fbreader.network.IPredefinedNetworkLink
import org.geometerplus.fbreader.network.NetworkCatalogItem
import org.geometerplus.fbreader.network.NetworkOperationData
import org.geometerplus.fbreader.network.authentication.NetworkAuthenticationManager
import org.geometerplus.fbreader.network.tree.NetworkItemsLoader
import org.geometerplus.fbreader.network.urlInfo.UrlInfo
import org.geometerplus.fbreader.network.urlInfo.UrlInfoCollection
import org.geometerplus.fbreader.network.urlInfo.UrlInfoWithDate
import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.network.ZLNetworkRequest
import java.io.IOException
import java.io.InputStream

open class RSSNetworkLink(
    id: Int,
    private val myPredefinedId: String?,
    title: String,
    summary: String?,
    language: String?,
    infos: UrlInfoCollection<UrlInfoWithDate>
) : AbstractNetworkLink(id, title, summary, language, infos), IPredefinedNetworkLink {

    private val myExtraData: MutableMap<String, String> = HashMap()

    fun createNetworkData(url: String?, result: RSSCatalogItem.State): ZLNetworkRequest? {
        if (url == null) {
            return null
        }

        val library = result.loader?.tree?.library ?: return null
        val catalogItem = result.loader?.tree?.Item ?: return null
        library.startLoading(catalogItem)

        return object : ZLNetworkRequest.Get(url, false) {
            @Throws(IOException::class, ZLNetworkException::class)
            override fun handleStream(inputStream: InputStream, length: Int) {
                if (result.loader?.confirmInterruption() == true) {
                    return
                }

                RSSXMLReader<RSSChannelMetadata, RSSItem>(library, RSSChannelHandler(url, result), false).read(inputStream)

                if (result.loader?.confirmInterruption() == true && result.lastLoadedId != null) {
                    result.lastLoadedId = null
                } else {
                    result.loader?.tree?.confirmAllItems()
                }
            }

            override fun doAfter(success: Boolean) {
                library.stopLoading(catalogItem)
            }
        }
    }

    override fun createOperationData(loader: NetworkItemsLoader?): NetworkOperationData =
        RSSCatalogItem.State(this, loader!!)

    fun setExtraData(extraData: Map<String, String>) {
        myExtraData.clear()
        myExtraData.putAll(extraData)
    }

    override val type: INetworkLink.Type
        get() = INetworkLink.Type.Predefined

    override fun getPredefinedId(): String? = myPredefinedId

    override fun simpleSearchRequest(pattern: String, data: NetworkOperationData): ZLNetworkRequest {
        throw UnsupportedOperationException("simpleSearchRequest is not supported")
    }

    override fun resume(data: NetworkOperationData): ZLNetworkRequest {
        throw UnsupportedOperationException("resume is not supported")
    }

    override fun libraryItem(): NetworkCatalogItem? {
        val urlMap = UrlInfoCollection<UrlInfo>()
        urlMap.addInfo(getUrlInfo(UrlInfo.Type.Catalog))
        urlMap.addInfo(getUrlInfo(UrlInfo.Type.Image))
        urlMap.addInfo(getUrlInfo(UrlInfo.Type.Thumbnail))
        return createRSSCatalogItem(
            this,
            title,
            summary,
            urlMap,
            NetworkCatalogItem.Accessibility.ALWAYS,
            NetworkCatalogItem.FLAGS_DEFAULT or NetworkCatalogItem.FLAG_ADD_SEARCH_ITEM
        )
    }

    protected open fun createRSSCatalogItem(
        link: RSSNetworkLink,
        title: String,
        summary: String?,
        urls: UrlInfoCollection<UrlInfo>,
        accessibility: NetworkCatalogItem.Accessibility,
        flags: Int
    ): RSSCatalogItem = RSSCatalogItemInternal(link, title, summary, urls, accessibility, flags)

    private class RSSCatalogItemInternal(
        link: RSSNetworkLink,
        title: String,
        summary: String?,
        urls: UrlInfoCollection<UrlInfo>,
        accessibility: NetworkCatalogItem.Accessibility,
        flags: Int
    ) : RSSCatalogItem(link, title, summary, urls, accessibility, flags)

    override fun authenticationManager(): NetworkAuthenticationManager? = null

    override fun rewriteUrl(url: String, isUrlExternal: Boolean): String = url

    override fun servesHost(hostname: String?): Boolean = false
}
