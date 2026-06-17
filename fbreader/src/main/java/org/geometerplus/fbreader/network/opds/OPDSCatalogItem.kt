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

import org.geometerplus.fbreader.network.NetworkOperationData
import org.geometerplus.fbreader.network.NetworkURLCatalogItem
import org.geometerplus.fbreader.network.tree.NetworkItemsLoader
import org.geometerplus.fbreader.network.urlInfo.RelatedUrlInfo
import org.geometerplus.fbreader.network.urlInfo.UrlInfo
import org.geometerplus.fbreader.network.urlInfo.UrlInfoCollection
import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.network.ZLNetworkRequest
import org.geometerplus.zlibrary.core.util.MimeType

open class OPDSCatalogItem : NetworkURLCatalogItem {
    private val extraData: Map<String, String>?
    private var loadingState: State? = null

    internal constructor(link: OPDSNetworkLink, info: RelatedUrlInfo) : this(
        link, info.Title ?: "", null, createSimpleCollection(info.url ?: "")
    )

    internal constructor(
        link: OPDSNetworkLink,
        title: CharSequence,
        summary: CharSequence?,
        urls: UrlInfoCollection<*>
    ) : this(link, title, summary, urls, Accessibility.ALWAYS, FLAGS_DEFAULT, null)

    protected constructor(
        link: OPDSNetworkLink,
        title: CharSequence,
        summary: CharSequence?,
        urls: UrlInfoCollection<*>,
        accessibility: Accessibility,
        flags: Int,
        extraData: Map<String, String>?
    ) : super(link, title, summary, urls, accessibility, flags) {
        this.extraData = extraData
    }

    companion object {
        private fun createSimpleCollection(url: String): UrlInfoCollection<UrlInfo> {
            val collection = UrlInfoCollection<UrlInfo>()
            collection.addInfo(UrlInfo(UrlInfo.Type.Catalog, url, MimeType.APP_ATOM_XML))
            return collection
        }
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

    override fun extraData(): Map<String, String> = extraData ?: emptyMap()

    @Throws(ZLNetworkException::class)
    override fun loadChildren(loader: NetworkItemsLoader) {
        val opdsLink = link as OPDSNetworkLink

        loadingState = opdsLink.createOperationData(loader)

        doLoadChildren(opdsLink.createNetworkData(catalogUrl, loadingState!!))
    }

    override fun supportsResumeLoading(): Boolean = true

    override fun canResumeLoading(): Boolean = loadingState != null && loadingState!!.resumeURI != null

    @Throws(ZLNetworkException::class)
    override fun resumeLoading(loader: NetworkItemsLoader) {
        if (canResumeLoading()) {
            loadingState!!.loader = loader
            val networkRequest = loadingState!!.resume()
            doLoadChildren(networkRequest)
        }
    }

    class State(link: OPDSNetworkLink, loader: NetworkItemsLoader) : NetworkOperationData(link, loader) {
        val loadedIds = mutableSetOf<String>()
        var lastLoadedId: String? = null
    }
}
