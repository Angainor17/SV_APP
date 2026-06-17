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

import org.geometerplus.fbreader.network.INetworkLink
import org.geometerplus.fbreader.network.NetworkBookItem
import org.geometerplus.fbreader.network.NetworkBookItemComparator
import org.geometerplus.fbreader.network.NetworkCatalogItem
import org.geometerplus.fbreader.network.NetworkItem
import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.fbreader.network.NetworkURLCatalogItem
import org.geometerplus.fbreader.network.tree.NetworkItemsLoader
import org.geometerplus.fbreader.network.urlInfo.UrlInfoCollection
import org.geometerplus.zlibrary.core.network.ZLNetworkAuthenticationException
import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.resources.ZLResource

import java.util.Collections

private abstract class SortedCatalogItem(
    parent: NetworkCatalogItem,
    resource: ZLResource,
    children: List<NetworkItem>,
    flags: Int
) : NetworkCatalogItem(
    parent.link,
    resource.value,
    resource.getResource("summary").value,
    null,
    Accessibility.ALWAYS,
    flags
) {
    private val myChildren = mutableListOf<NetworkItem>()

    init {
        for (child in children) {
            if (accepts(child)) {
                myChildren.add(child)
            }
        }
        comparator?.let {
            Collections.sort(myChildren, it)
        }
    }

    internal constructor(parent: NetworkCatalogItem, resourceKey: String, children: List<NetworkItem>, flags: Int) :
            this(parent, NetworkLibrary.resource().getResource(resourceKey), children, flags)

    override fun canBeOpened(): Boolean = true

    val isEmpty: Boolean
        get() = myChildren.isEmpty()

    protected open val comparator: Comparator<NetworkItem>?
        get() = null

    protected open fun accepts(item: NetworkItem): Boolean = item is NetworkBookItem

    @Throws(ZLNetworkException::class)
    override fun loadChildren(loader: NetworkItemsLoader) {
        for (child in myChildren) {
            loader.onNewItem(child)
        }
        loader.tree.confirmAllItems()
    }
}

private class ByAuthorCatalogItem(parent: NetworkCatalogItem, children: List<NetworkItem>) :
    SortedCatalogItem(parent, "byAuthor", children, FLAG_GROUP_BY_AUTHOR) {

    override val comparator: Comparator<NetworkItem>
        get() = NetworkBookItemComparator()

    override val stringId: String
        get() = "@ByAuthor"
}

private class ByTitleCatalogItem(parent: NetworkCatalogItem, children: List<NetworkItem>) :
    SortedCatalogItem(parent, "byTitle", children, FLAG_SHOW_AUTHOR) {

    override val comparator: Comparator<NetworkItem>
        get() = Comparator { item0, item1 -> item0.title.toString().compareTo(item1.title.toString()) }

    override val stringId: String
        get() = "@ByTitle"
}

private class ByDateCatalogItem(parent: NetworkCatalogItem, children: List<NetworkItem>) :
    SortedCatalogItem(parent, "byDate", children, FLAG_SHOW_AUTHOR) {

    override val stringId: String
        get() = "@ByDate"
}

private class BySeriesCatalogItem(parent: NetworkCatalogItem, children: List<NetworkItem>) :
    SortedCatalogItem(parent, "bySeries", children, FLAG_SHOW_AUTHOR or FLAG_GROUP_BY_SERIES) {

    override val comparator: Comparator<NetworkItem>
        get() = Comparator { item0, item1 ->
            val book0 = item0 as NetworkBookItem
            val book1 = item1 as NetworkBookItem
            val diff = book0.seriesTitle!!.compareTo(book1.seriesTitle!!)
            if (diff != 0) return@Comparator diff
            val fdiff = book0.indexInSeries - book1.indexInSeries
            if (fdiff != 0f) return@Comparator if (fdiff > 0) 1 else -1
            book0.title.toString().compareTo(book1.title.toString())
        }

    override fun accepts(item: NetworkItem): Boolean =
        item is NetworkBookItem && item.seriesTitle != null

    override val stringId: String
        get() = "@BySeries"
}

class LitResBookshelfItem(
    link: INetworkLink,
    title: CharSequence,
    summary: CharSequence?,
    urls: UrlInfoCollection<*>
) : NetworkURLCatalogItem(link, title, summary, urls, Accessibility.SIGNED_IN, FLAGS_DEFAULT) {

    private var forceReload = false

    @Throws(ZLNetworkException::class)
    override fun loadChildren(loader: NetworkItemsLoader) {
        val networkLink = link ?: throw ZLNetworkAuthenticationException()
        val mgr = networkLink.authenticationManager() as? LitResAuthenticationManager
            ?: throw ZLNetworkAuthenticationException()

        // TODO: Maybe it's better to call isAuthorised(true) directly
        // and let exception fly through???
        if (!mgr.mayBeAuthorised(true)) {
            throw ZLNetworkAuthenticationException()
        }
        try {
            if (forceReload) {
                mgr.reloadPurchasedBooks()
            }
        } finally {
            forceReload = true
            // TODO: implement asynchronous loading
            val children = ArrayList<NetworkItem>(mgr.purchasedBooks())
            if (children.size <= 5) {
                Collections.sort(children, NetworkBookItemComparator())
                for (item in children) {
                    loader.onNewItem(item)
                }
            } else {
                loader.onNewItem(ByDateCatalogItem(this, children))
                loader.onNewItem(ByAuthorCatalogItem(this, children))
                loader.onNewItem(ByTitleCatalogItem(this, children))
                val bySeries = BySeriesCatalogItem(this, children)
                if (!bySeries.isEmpty) {
                    loader.onNewItem(bySeries)
                }
            }
            loader.tree.confirmAllItems()
        }
    }
}
