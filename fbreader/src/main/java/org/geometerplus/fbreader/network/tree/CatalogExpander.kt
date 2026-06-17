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

import org.geometerplus.fbreader.network.INetworkLink
import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.zlibrary.core.network.ZLNetworkContext
import org.geometerplus.zlibrary.core.network.ZLNetworkException

class CatalogExpander(
    nc: ZLNetworkContext,
    tree: NetworkCatalogTree,
    private val myAuthenticate: Boolean,
    private val myResumeNotLoad: Boolean
) : NetworkItemsLoader(nc, tree) {

    @Throws(ZLNetworkException::class)
    override fun doBefore() {
        val link: INetworkLink? = tree.getLink()
        if (myAuthenticate && link != null) {
            val mgr = link.authenticationManager()
            if (mgr != null) {
                try {
                    if (mgr.isAuthorised(true) && mgr.needsInitialization()) {
                        object : Thread() {
                            override fun run() {
                                try {
                                    mgr.initialize()
                                } catch (e: ZLNetworkException) {
                                    e.printStackTrace()
                                }
                            }
                        }.start()
                    }
                } catch (e: ZLNetworkException) {
                    mgr.logOut()
                }
            }
        }
    }

    @Throws(ZLNetworkException::class)
    override fun load() {
        if (myResumeNotLoad) {
            tree.Item.resumeLoading(this)
        } else {
            tree.Item.loadChildren(this)
        }
    }

    override fun onFinish(exception: ZLNetworkException?, interrupted: Boolean) {
        if (interrupted && (!tree.Item.supportsResumeLoading() || exception != null)) {
            tree.clearCatalog()
        } else {
            tree.removeUnconfirmedItems()
            if (!interrupted) {
                if (exception != null) {
                    tree.library.fireModelChangedEvent(
                        NetworkLibrary.ChangeListener.Code.NetworkError, exception.message
                    )
                } else {
                    tree.updateLoadedTime()
                    if (tree.subtrees().isEmpty()) {
                        tree.library.fireModelChangedEvent(
                            NetworkLibrary.ChangeListener.Code.EmptyCatalog
                        )
                    }
                }
            }
            tree.library.invalidateVisibility()
            tree.library.synchronize()
        }
    }
}
