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
import android.os.Bundle

import org.geometerplus.R
import org.geometerplus.android.fbreader.network.NetworkSearchActivity
import org.geometerplus.android.fbreader.tree.TreeActivity
import org.geometerplus.android.util.DeviceType
import org.geometerplus.android.util.SearchDialogUtil
import org.geometerplus.fbreader.network.NetworkTree
import org.geometerplus.fbreader.network.tree.SearchCatalogTree
import org.geometerplus.fbreader.tree.FBTree

class RunSearchAction(activity: Activity, private val myFromContextMenu: Boolean) : Action(activity, ActionCode.SEARCH, "networkSearch", R.drawable.ic_menu_search) {

    override fun isVisible(tree: NetworkTree): Boolean {
        return if (myFromContextMenu) {
            tree is SearchCatalogTree
        } else {
            getSearchTree(tree) != null
        }
    }

    override fun isEnabled(tree: NetworkTree): Boolean = myLibrary.getStoredLoader(getSearchTree(tree)) == null

    override fun run(tree: NetworkTree) {
        val bundle = Bundle()
        bundle.putSerializable(
            TreeActivity.TREE_KEY_KEY,
            getSearchTree(tree)!!.getUniqueKey()
        )
        val library = myLibrary
        if (DeviceType.Instance().hasStandardSearchDialog()) {
            myActivity.startSearch(library.networkSearchPatternOption.value, true, bundle, false)
        } else {
            SearchDialogUtil.showDialog(myActivity, NetworkSearchActivity::class.java, library.networkSearchPatternOption.value, null, bundle)
        }
    }

    companion object {
        fun getSearchTree(tree: FBTree?): SearchCatalogTree? {
            var t = tree
            while (t != null) {
                for (sub in t.subtrees()) {
                    if (sub is SearchCatalogTree) {
                        return sub
                    }
                }
                t = t.parent
            }
            return null
        }
    }
}
