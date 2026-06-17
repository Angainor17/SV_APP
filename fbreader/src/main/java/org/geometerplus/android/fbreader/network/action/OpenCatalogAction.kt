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

package org.geometerplus.android.fbreader.network.action

import android.app.Activity
import android.content.Intent
import org.geometerplus.android.fbreader.network.NetworkLibraryActivity
import org.geometerplus.android.fbreader.network.NetworkLibrarySecondaryActivity
import org.geometerplus.android.fbreader.tree.TreeActivity
import org.geometerplus.android.util.OrientationUtil
import org.geometerplus.fbreader.network.NetworkTree
import org.geometerplus.fbreader.network.tree.NetworkAuthorTree
import org.geometerplus.fbreader.network.tree.NetworkCatalogTree
import org.geometerplus.fbreader.network.tree.NetworkSeriesTree
import org.geometerplus.zlibrary.core.network.ZLNetworkContext

class OpenCatalogAction(activity: Activity, private val myNetworkContext: ZLNetworkContext) : Action(activity, ActionCode.OPEN_CATALOG, "openCatalog", -1) {

    override fun isVisible(tree: NetworkTree): Boolean {
        return when (tree) {
            is NetworkAuthorTree, is NetworkSeriesTree -> true
            is NetworkCatalogTree -> tree.canBeOpened()
            else -> false
        }
    }

    override fun run(tree: NetworkTree) {
        if (tree is NetworkCatalogTree) {
            doExpandCatalog(tree)
        } else {
            doOpenTree(tree)
        }
    }

    private fun doOpenTree(tree: NetworkTree) {
        if (myActivity is NetworkLibraryActivity) {
            (myActivity as NetworkLibraryActivity).openTree(tree)
        } else {
            OrientationUtil.startActivity(
                myActivity,
                Intent(myActivity.applicationContext, NetworkLibrarySecondaryActivity::class.java)
                    .putExtra(TreeActivity.TREE_KEY_KEY, tree.getUniqueKey())
            )
        }
    }

    private fun doExpandCatalog(tree: NetworkCatalogTree) {
        val loader = myLibrary.getStoredLoader(tree)
        if (loader != null && loader.canResumeLoading()) {
            doOpenTree(tree)
        } else if (loader != null) {
            loader.setPostRunnable { doLoadCatalog(tree) }
        } else {
            doLoadCatalog(tree)
        }
    }

    private fun doLoadCatalog(tree: NetworkCatalogTree) {
        var resumeNotLoad = false
        if (tree.hasChildren()) {
            if (tree.isContentValid()) {
                if (tree.Item.supportsResumeLoading()) {
                    resumeNotLoad = true
                } else {
                    doOpenTree(tree)
                    return
                }
            } else {
                tree.clearCatalog()
            }
        }

        tree.startItemsLoader(myNetworkContext, true, resumeNotLoad)
        doOpenTree(tree)
    }
}
