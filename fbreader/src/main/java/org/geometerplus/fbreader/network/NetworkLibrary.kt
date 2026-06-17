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

import org.geometerplus.fbreader.fbreader.options.SyncOptions
import org.geometerplus.fbreader.network.opds.OPDSLinkReader
import org.geometerplus.fbreader.network.opds.OPDSSyncNetworkLink
import org.geometerplus.fbreader.network.tree.AddCustomCatalogItemTree
import org.geometerplus.fbreader.network.tree.BasketCatalogTree
import org.geometerplus.fbreader.network.tree.ManageCatalogsItemTree
import org.geometerplus.fbreader.network.tree.NetworkBookTree
import org.geometerplus.fbreader.network.tree.NetworkCatalogRootTree
import org.geometerplus.fbreader.network.tree.NetworkCatalogTree
import org.geometerplus.fbreader.network.tree.NetworkItemsLoader
import org.geometerplus.fbreader.network.tree.RootTree
import org.geometerplus.fbreader.network.tree.SearchCatalogTree
import org.geometerplus.fbreader.network.urlInfo.UrlInfo
import org.geometerplus.fbreader.tree.FBTree.Key
import org.geometerplus.zlibrary.core.image.ZLImage
import org.geometerplus.zlibrary.core.library.ZLibrary
import org.geometerplus.zlibrary.core.network.QuietNetworkContext
import org.geometerplus.zlibrary.core.network.ZLNetworkContext
import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.options.ZLBooleanOption
import org.geometerplus.zlibrary.core.options.ZLStringListOption
import org.geometerplus.zlibrary.core.options.ZLStringOption
import org.geometerplus.zlibrary.core.resources.ZLResource
import org.geometerplus.zlibrary.core.util.MimeType
import org.geometerplus.zlibrary.core.util.SystemInfo
import org.geometerplus.zlibrary.core.util.ZLNetworkUtil
import java.io.File
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.LinkedList
import java.util.TreeMap

class NetworkLibrary private constructor(val systemInfo: SystemInfo) {

    @JvmField
    val networkSearchPatternOption = ZLStringOption("NetworkSearch", "Pattern", "")

    // that's important to keep this list synchronized
    // it can be used from background thread
    private val myLinks = Collections.synchronizedList(ArrayList<INetworkLink>())
    private val myListeners = Collections.synchronizedSet(HashSet<ChangeListener>())
    private val myLoaders = Collections.synchronizedMap(HashMap<NetworkTree, NetworkItemsLoader>())
    private val myImageMap = Collections.synchronizedMap(HashMap<String, WeakReference<ZLImage>>())
    private val myRootAllTree = RootTree(this, "@AllRoot", false)
    private val myRootTree = RootTree(this, "@Root", false)
    private val myFakeRootTree = RootTree(this, "@FakeRoot", true)
    private val mySearchItem = AllCatalogsSearchItem(this)
    private var myActiveIdsOption: ZLStringListOption? = null
    private var myChildrenAreInvalid = true
    private var myUpdateVisibility = false
    private var myIsInitialized = false
    private var myUpdateInProgress = false
    private var myUpdateLock = Any()

    interface ChangeListener {
        fun onLibraryChanged(code: Code, params: Array<Any?>)

        enum class Code {
            InitializationFinished,
            InitializationFailed,
            SomeCode,
            /*
            ItemAdded,
            ItemRemoved,
            StatusChanged,
            */
            SignedIn,
            Found,
            NotFound,
            EmptyCatalog,
            NetworkError
        }
    }

    interface OnNewLinkListener {
        fun onNewLink(link: INetworkLink)
    }

    companion object {
        private var ourInstance: NetworkLibrary? = null

        @JvmStatic
        fun Instance(systemInfo: SystemInfo): NetworkLibrary {
            if (ourInstance == null) {
                ourInstance = NetworkLibrary(systemInfo)
            }
            return ourInstance!!
        }

        @JvmStatic
        fun resource(): ZLResource = ZLResource.resource("networkLibrary")
    }

    fun allIds(): List<String> {
        val ids = ArrayList<String>()
        synchronized(myLinks) {
            for (link in myLinks) {
                link.getUrl(UrlInfo.Type.Catalog)?.let { ids.add(it) }
            }
        }
        return ids
    }

    private fun activeIdsOption(): ZLStringListOption {
        if (myActiveIdsOption == null) {
            myActiveIdsOption = ZLStringListOption(
                "Options",
                "ActiveIds",
                "",
                ","
            )
        }
        return myActiveIdsOption!!
    }

    fun activeIds(): List<String> = activeIdsOption().value

    fun setLinkActive(link: INetworkLink?, active: Boolean) {
        if (link == null) {
            return
        }
        setLinkActive(link.getUrl(UrlInfo.Type.Catalog), active)
        myChildrenAreInvalid = true
    }

    fun setLinkActive(id: String?, active: Boolean) {
        if (id == null) {
            return
        }
        val oldIds = activeIds()
        if (oldIds.contains(id) == active) {
            return
        }

        val newIds: MutableList<String>
        if (active) {
            newIds = ArrayList(oldIds.size + 1)
            newIds.add(id)
            newIds.addAll(oldIds)
        } else {
            newIds = ArrayList(oldIds)
            newIds.remove(id)
        }
        activeIdsOption().value = newIds
        invalidateChildren()
    }

    fun setActiveIds(ids: List<String>) {
        activeIdsOption().value = ids
        invalidateChildren()
    }

    fun activeLinks(): List<INetworkLink> {
        val linksById = TreeMap<String, INetworkLink>()
        synchronized(myLinks) {
            for (link in myLinks) {
                val id = link.getUrl(UrlInfo.Type.Catalog)
                if (id != null) {
                    linksById[id] = link
                }
            }
        }

        val result = LinkedList<INetworkLink>()
        var syncLink = linksById[SyncOptions.DOMAIN]
        if (syncLink == null) {
            syncLink = OPDSSyncNetworkLink(this)
        }
        result.add(syncLink)
        for (id in activeIds()) {
            val link = linksById[id]
            if (link != null) {
                result.add(link)
            }
        }
        return result
    }

    fun getLinkByUrl(url: String?): INetworkLink? {
        if (url == null) {
            return null
        }
        synchronized(myLinks) {
            for (link in myLinks) {
                if (url == link.getUrlInfo(UrlInfo.Type.Catalog)?.url) {
                    return link
                }
            }
        }
        return null
    }

    fun getCatalogTreeByUrl(url: String): NetworkTree? {
        for (tree in getRootTree().subtrees()) {
            if (tree is NetworkCatalogRootTree) {
                val link = (tree as NetworkCatalogTree).getLink()
                val cUrl = link?.getUrlInfo(UrlInfo.Type.Catalog)?.url
                if (url == cUrl) {
                    return tree as NetworkTree
                }
            }
        }
        return null
    }

    fun getCatalogTreeByUrlAll(url: String): NetworkTree? {
        for (tree in getRootAllTree().subtrees()) {
            if (tree is NetworkCatalogRootTree) {
                val link = (tree as NetworkCatalogTree).getLink()
                val cUrl = link?.getUrlInfo(UrlInfo.Type.Catalog)?.url
                if (url == cUrl) {
                    return tree as NetworkTree
                }
            }
        }
        return null
    }

    fun getLinkByStringId(stringId: String): INetworkLink? {
        synchronized(myLinks) {
            for (link in myLinks) {
                if (stringId == link.stringId) {
                    return link
                }
            }
        }
        return null
    }

    fun clearExpiredCache(hours: Int) {
        val toVisit = LinkedList<File>()
        val processedDirs = HashSet<File>()
        val root = File(systemInfo.networkCacheDirectory())
        toVisit.add(root)
        processedDirs.add(root)

        while (toVisit.isNotEmpty()) {
            val children = toVisit.remove().listFiles()
            if (children == null) {
                continue
            }
            for (child in children) {
                if (child.isDirectory) {
                    if (!processedDirs.contains(child)) {
                        toVisit.add(child)
                        processedDirs.add(child)
                    }
                } else {
                    val age = System.currentTimeMillis() - child.lastModified()
                    if (age / 1000 / 60 / 60 >= hours) {
                        child.delete()
                    }
                }
            }
        }
    }

    fun isInitialized(): Boolean = myIsInitialized

    @Synchronized
    @Throws(ZLNetworkException::class)
    fun initialize(nc: ZLNetworkContext) {
        if (myIsInitialized) {
            return
        }

        try {
            myLinks.addAll(OPDSLinkReader.loadOPDSLinks(this, nc, OPDSLinkReader.CacheMode.LOAD))
        } catch (e: ZLNetworkException) {
            removeAllLoadedLinks()
            fireModelChangedEvent(ChangeListener.Code.InitializationFailed, e.message)
            throw e
        }

        val db = NetworkDatabase.Instance()
        if (db != null) {
            myLinks.addAll(db.listLinks())
        }

        synchronize()

        myIsInitialized = true
        fireModelChangedEvent(ChangeListener.Code.InitializationFinished)
    }

    private fun removeAllLoadedLinks() {
        val toRemove = LinkedList<INetworkLink>()
        synchronized(myLinks) {
            for (link in myLinks) {
                if (link !is ICustomNetworkLink) {
                    toRemove.add(link)
                }
            }
        }
        myLinks.removeAll(toRemove)
    }

    fun runBackgroundUpdate(force: Boolean) {
        if (!myIsInitialized) {
            return
        }

        val thread = Thread {
            try {
                myUpdateInProgress = true
                fireModelChangedEvent(ChangeListener.Code.SomeCode)
                runBackgroundUpdateInternal(force)
            } catch (e: ZLNetworkException) {
                fireModelChangedEvent(ChangeListener.Code.NetworkError, e.message)
            } finally {
                myUpdateInProgress = false
                fireModelChangedEvent(ChangeListener.Code.SomeCode)
            }
        }
        thread.priority = Thread.MIN_PRIORITY
        thread.start()
    }

    @Throws(ZLNetworkException::class)
    private fun runBackgroundUpdateInternal(force: Boolean) {
        val quietContext = QuietNetworkContext()
        synchronized(myUpdateLock) {
            val mode = if (force) OPDSLinkReader.CacheMode.CLEAR else OPDSLinkReader.CacheMode.UPDATE
            val loadedLinks = OPDSLinkReader.loadOPDSLinks(this, quietContext, mode)
            if (loadedLinks.isNotEmpty()) {
                removeAllLoadedLinks()
                myLinks.addAll(loadedLinks)
            }
            invalidateChildren()

            // we create this copy to prevent long operations on synchronized list
            val linksCopy = ArrayList(myLinks)
            for (link in linksCopy) {
                if (link.type == INetworkLink.Type.Custom) {
                    val customLink = link as ICustomNetworkLink
                    if (force || customLink.isObsolete(12 * 60 * 60 * 1000)) { // 12 hours
                        try {
                            customLink.reloadInfo(quietContext, true, true)
                            NetworkDatabase.Instance()?.saveLink(customLink)
                        } catch (t: Throwable) {
                            // ignore
                        }
                    }
                }
            }

            synchronize()
        }
    }

    fun rewriteUrl(url: String, externalUrl: Boolean): String {
        var url = url
        val host = ZLNetworkUtil.hostFromUrl(url).lowercase()
        synchronized(myLinks) {
            for (link in myLinks) {
                if (link is IPredefinedNetworkLink && link.servesHost(host)) {
                    url = link.rewriteUrl(url, externalUrl)
                }
            }
        }
        return url
    }

    private fun invalidateChildren() {
        myChildrenAreInvalid = true
    }

    fun invalidateVisibility() {
        myUpdateVisibility = true
    }

    private fun makeUpToDateRootAll() {
        myRootAllTree.clear()
        synchronized(myLinks) {
            for (link in myLinks) {
                for (t in myRootAllTree.subtrees()) {
                    val l = (t as NetworkTree).getLink()
                    if (l != null && link.compareTo(l) <= 0) {
                        break
                    }
                }
                NetworkCatalogRootTree(myRootAllTree, link)
            }
        }
    }

    private fun makeUpToDate() {
        firstTimeComputeActiveIds()

        val linkToTreeMap = HashMap<INetworkLink, MutableList<NetworkCatalogTree>>()
        for (tree in myRootTree.subtrees()) {
            if (tree is NetworkCatalogTree) {
                val nTree = tree
                val link = nTree.getLink()
                if (link != null) {
                    var list = linkToTreeMap[link]
                    if (list == null) {
                        list = LinkedList()
                        linkToTreeMap[link] = list
                    }
                    list.add(nTree)
                }
            }
        }

        if (!myRootTree.hasChildren()) {
            //new RecentCatalogListTree(
            //	myRootTree, new RecentCatalogListItem(resource().getResource("recent"))
            //);
            SearchCatalogTree(myRootTree, mySearchItem)
            // normal catalog items to be inserted here
            ManageCatalogsItemTree(myRootTree)
            AddCustomCatalogItemTree(myRootTree)
        }

        var changedCatalogsList = false
        var index = 1
        for (link in activeLinks()) {
            val trees = linkToTreeMap.remove(link)
            if (trees != null) {
                for (t in trees) {
                    myRootTree.moveSubtree(t, index++)
                }
            } else {
                NetworkCatalogRootTree(myRootTree, link, index++)
                changedCatalogsList = true
            }
        }

        for (trees in linkToTreeMap.values) {
            for (t in trees) {
                t.removeSelf()
                changedCatalogsList = true
            }
        }

        if (changedCatalogsList) {
            mySearchItem.setPattern(null)
        }

        fireModelChangedEvent(ChangeListener.Code.SomeCode)
    }

    private fun firstTimeComputeActiveIds() {
        val firstLaunchOption = ZLBooleanOption(
            "Options",
            "firstLaunch",
            true
        )
        if (!firstLaunchOption.value) {
            return
        }

        val ids = ArrayList<String>()
        // language codes were saved in this options in versions before 1.9
        val codes = ZLStringListOption(
            "Options",
            "ActiveLanguages",
            ZLibrary.Instance().defaultLanguageCodes(),
            ","
        ).value
        synchronized(myLinks) {
            for (link in myLinks) {
                if (link is ICustomNetworkLink || codes.contains(link.language)) {
                    link.getUrl(UrlInfo.Type.Catalog)?.let { ids.add(it) }
                }
            }
        }
        setActiveIds(ids)

        firstLaunchOption.value = false
    }

    private fun updateVisibility() {
        for (tree in myRootTree.subtrees()) {
            if (tree is NetworkCatalogTree) {
                tree.updateVisibility()
            }
        }
        fireModelChangedEvent(ChangeListener.Code.SomeCode)
    }

    fun synchronize() {
        if (myChildrenAreInvalid) {
            myChildrenAreInvalid = false
            makeUpToDate()
            makeUpToDateRootAll()
        }
        if (myUpdateVisibility) {
            myUpdateVisibility = false
            updateVisibility()
        }
        fireModelChangedEvent(ChangeListener.Code.SomeCode)
    }

    fun getRootTree(): NetworkTree = myRootTree

    fun getRootAllTree(): NetworkTree = myRootAllTree

    fun getFakeBookTree(book: NetworkBookItem): NetworkBookTree {
        val id = book.stringId
        for (tree in myFakeRootTree.subtrees()) {
            if (tree is NetworkBookTree && id == tree.getUniqueKey().id) {
                return tree
            }
        }
        return NetworkBookTree(myFakeRootTree, book, true)
    }

    fun getFakeBasketTree(item: BasketItem): BasketCatalogTree {
        val id = item.stringId
        for (tree in myFakeRootTree.subtrees()) {
            if (tree is BasketCatalogTree && id == tree.getUniqueKey().id) {
                return tree
            }
        }
        return BasketCatalogTree(myFakeRootTree, item)
    }

    fun getFakeCatalogTree(item: NetworkCatalogItem): NetworkCatalogTree {
        val id = item.stringId
        for (tree in myFakeRootTree.subtrees()) {
            if (tree is NetworkCatalogTree && id == tree.getUniqueKey().id) {
                return tree
            }
        }
        return NetworkCatalogTree(myFakeRootTree, item.link, item, 0)
    }

    fun getTreeByKey(key: Key?): NetworkTree? {
        if (key == null) {
            return null
        }
        if (key.parent == null) {
            if (key == myRootTree.getUniqueKey()) {
                return myRootTree
            }
            if (key == myFakeRootTree.getUniqueKey()) {
                return myFakeRootTree
            }
            return null
        }
        val parentTree = getTreeByKey(key.parent) ?: return null
        return parentTree.getSubtree(key.id) as? NetworkTree
    }

    fun addCustomLink(link: ICustomNetworkLink) {
        val id = link.id
        if (id == INetworkLink.INVALID_ID) {
            synchronized(myLinks) {
                val existing = getLinkByUrl(link.getUrl(UrlInfo.Type.Catalog))
                if (existing == null) {
                    myLinks.add(link)
                } else {
                    setLinkActive(existing, true)
                    fireModelChangedEvent(ChangeListener.Code.SomeCode)
                    return
                }
            }
        } else {
            synchronized(myLinks) {
                for (i in myLinks.size - 1 downTo 0) {
                    val l = myLinks[i]
                    if (l is ICustomNetworkLink && l.id == id) {
                        myLinks[i] = link
                        break
                    }
                }
            }
        }
        NetworkDatabase.Instance()?.saveLink(link)
        setLinkActive(link, true)
        fireModelChangedEvent(ChangeListener.Code.SomeCode)
    }

    fun removeCustomLink(link: ICustomNetworkLink) {
        myLinks.remove(link)
        NetworkDatabase.Instance()?.deleteLink(link)
        invalidateChildren()
    }

    fun addChangeListener(listener: ChangeListener) {
        myListeners.add(listener)
    }

    fun removeChangeListener(listener: ChangeListener) {
        myListeners.remove(listener)
    }

    // TODO: change to private
    fun fireModelChangedEvent(code: ChangeListener.Code, vararg params: Any?) {
        synchronized(myListeners) {
            for (l in myListeners) {
                l.onLibraryChanged(code, arrayOf(*params))
            }
        }
    }

    fun storeLoader(tree: NetworkTree, loader: NetworkItemsLoader) {
        myLoaders[tree] = loader
    }

    fun getStoredLoader(tree: NetworkTree?): NetworkItemsLoader? = if (tree != null) myLoaders[tree] else null

    fun isUpdateInProgress(): Boolean = myUpdateInProgress

    fun startLoading(item: NetworkCatalogItem?) {
        if (item != null) {
            item.updatingInProgress = true
            fireModelChangedEvent(ChangeListener.Code.SomeCode)
        }
    }

    fun stopLoading(item: NetworkCatalogItem?) {
        if (item != null) {
            item.updatingInProgress = false
            fireModelChangedEvent(ChangeListener.Code.SomeCode)
        }
    }

    fun isLoadingInProgress(tree: NetworkTree): Boolean {
        return (tree is NetworkCatalogTree && tree.Item.updatingInProgress) || getStoredLoader(tree) != null
    }

    fun removeStoredLoader(tree: NetworkTree) {
        myLoaders.remove(tree)
    }

    fun getImageByUrl(url: String, mimeType: MimeType): ZLImage {
        synchronized(myImageMap) {
            val ref = myImageMap[url]
            if (ref != null) {
                val image = ref.get()
                if (image != null) {
                    return image
                }
            }
            val image = NetworkImage(url, systemInfo)
            myImageMap[url] = WeakReference(image)
            return image
        }
    }
}
