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

import org.fbreader.util.Boolean3
import org.fbreader.util.Pair
import org.geometerplus.fbreader.network.INetworkLink
import org.geometerplus.fbreader.network.ISyncNetworkLink
import org.geometerplus.fbreader.network.NetworkBookItem
import org.geometerplus.fbreader.network.NetworkCatalogItem
import org.geometerplus.fbreader.network.NetworkItem
import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.fbreader.network.NetworkTree
import org.geometerplus.fbreader.network.SearchItem
import org.geometerplus.fbreader.network.SingleCatalogSearchItem
import org.geometerplus.fbreader.network.urlInfo.UrlInfo
import org.geometerplus.fbreader.tree.FBTree
import org.geometerplus.zlibrary.core.image.ZLImage
import org.geometerplus.zlibrary.core.network.QuietNetworkContext
import org.geometerplus.zlibrary.core.network.ZLNetworkContext
import java.util.Collections
import java.util.LinkedList

open class NetworkCatalogTree : NetworkTree {
    @JvmField
    val Item: NetworkCatalogItem

    @JvmField
    val myChildrenItems: ArrayList<NetworkCatalogItem> = ArrayList()

    private val myLink: INetworkLink?
    private val myUnconfirmedTrees: MutableSet<NetworkTree> = Collections.synchronizedSet(HashSet())
    private var myLastTotalChildren: Int = -1
    private var myLoadedTime: Long = -1
    private var mySearchItem: SearchItem? = null

    constructor(parent: NetworkTree, link: INetworkLink?, item: NetworkCatalogItem, position: Int) : super(parent, position) {
        myLink = link
        if (item == null) {
            throw IllegalArgumentException("item cannot be null")
        }
        Item = item
    }

    override fun getLink(): INetworkLink? = myLink

    fun getVisibility(): Boolean3 = Item.getVisibility()

    fun canBeOpened(): Boolean = Item.canBeOpened()

    protected open fun addSpecialTrees() {
        if ((Item.flags and NetworkCatalogItem.FLAG_ADD_SEARCH_ITEM) != 0) {
            val link = getLink()
            if (link != null && link.getUrl(UrlInfo.Type.Search) != null) {
                if (mySearchItem == null) {
                    mySearchItem = SingleCatalogSearchItem(link)
                }
                myChildrenItems.add(mySearchItem!!)
                SearchCatalogTree(this, mySearchItem!!)
            }
        }
    }

    @Synchronized
    open fun addItem(item: NetworkItem) {
        if (!hasChildren() && !isSingleSyncItem(item)) {
            addSpecialTrees()
        }
        if (item is NetworkCatalogItem) {
            myChildrenItems.add(item)
        }
        myUnconfirmedTrees.add(NetworkTreeFactory.createNetworkTree(this, item)!!)
        library.fireModelChangedEvent(NetworkLibrary.ChangeListener.Code.SomeCode)
    }

    override val name: String
        get() {
            val title = Item.title
            return title?.toString() ?: ""
        }

    override val summary: String
        get() {
            val summary = Item.getSummary()
            return summary?.toString() ?: ""
        }

    override val treeTitle: Pair<String, String?>
        get() {
            val link = getLink()
            return Pair(name, link?.title)
        }

    override fun createCover(): ZLImage? = createCoverForItem(library, Item, true)

    open fun isContentValid(): Boolean {
        if (myLoadedTime < 0) {
            return false
        }
        val reloadTime = 15 * 60 * 1000 // 15 minutes in milliseconds
        return System.currentTimeMillis() - myLoadedTime < reloadTime
    }

    fun updateLoadedTime() {
        myLoadedTime = System.currentTimeMillis()
    }

    fun updateVisibility() {
        val toRemove = LinkedList<FBTree>()

        var nodeIterator = subtrees().listIterator()
        var currentTree: FBTree? = null
        var nodeCount = 0

        for (i in myChildrenItems.indices) {
            val currentItem = myChildrenItems[i]
            var processed = false
            while (currentTree != null || nodeIterator.hasNext()) {
                if (currentTree == null) {
                    currentTree = nodeIterator.next()
                }
                if (currentTree !is NetworkCatalogTree) {
                    currentTree = null
                    ++nodeCount
                    continue
                }
                val child = currentTree
                if (child.Item == currentItem) {
                    when (child.Item.getVisibility()) {
                        Boolean3.TRUE -> child.updateVisibility()
                        Boolean3.FALSE -> toRemove.add(child)
                        Boolean3.UNDEFINED -> child.clearCatalog()
                    }
                    currentTree = null
                    ++nodeCount
                    processed = true
                    break
                } else {
                    var found = false
                    for (j in i + 1 until myChildrenItems.size) {
                        if (child.Item == myChildrenItems[j]) {
                            found = true
                            break
                        }
                    }
                    if (!found) {
                        toRemove.add(currentTree)
                        currentTree = null
                        ++nodeCount
                    } else {
                        break
                    }
                }
            }
            val nextIndex = nodeIterator.nextIndex()
            if (!processed && NetworkTreeFactory.createNetworkTree(this, currentItem, nodeCount) != null) {
                ++nodeCount
                nodeIterator = subtrees().listIterator(nextIndex + 1)
            }
        }

        while (currentTree != null || nodeIterator.hasNext()) {
            if (currentTree == null) {
                currentTree = nodeIterator.next()
            }
            if (currentTree is NetworkCatalogTree) {
                toRemove.add(currentTree)
            }
            currentTree = null
        }

        for (tree in toRemove) {
            tree.removeSelf()
        }
    }

    @Synchronized
    fun confirmAllItems() {
        myUnconfirmedTrees.clear()
    }

    @Synchronized
    fun removeUnconfirmedItems() {
        synchronized(myUnconfirmedTrees) {
            removeTrees(myUnconfirmedTrees)
        }
    }

    override val stringId: String
        get() = Item.stringId

    fun startItemsLoader(nc: ZLNetworkContext, authenticate: Boolean, resumeNotLoad: Boolean) {
        CatalogExpander(nc, this, authenticate, resumeNotLoad).start()
    }

    @Synchronized
    fun clearCatalog() {
        myChildrenItems.clear()
        myLastTotalChildren = -1
        clear()
        library.fireModelChangedEvent(NetworkLibrary.ChangeListener.Code.SomeCode)
    }

    @Synchronized
    fun loadMoreChildren(currentTotal: Int) {
        if (currentTotal == subtrees().size
            && myLastTotalChildren < currentTotal
            && !library.isLoadingInProgress(this)
            && Item.canResumeLoading()
        ) {
            myLastTotalChildren = currentTotal
            startItemsLoader(QuietNetworkContext(), false, true)
        }
    }

    private fun isSingleSyncItem(item: NetworkItem): Boolean {
        if (item !is NetworkBookItem) {
            return false
        }
        val link = getLink()
        if (link !is ISyncNetworkLink) {
            return false
        }
        return "fbreader:book:network:description" == item.id
    }
}
