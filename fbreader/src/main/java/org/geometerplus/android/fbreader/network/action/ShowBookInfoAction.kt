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

import org.geometerplus.android.fbreader.network.NetworkBookInfoActivity
import org.geometerplus.android.fbreader.tree.TreeActivity
import org.geometerplus.android.util.OrientationUtil
import org.geometerplus.android.util.UIUtil
import org.geometerplus.fbreader.network.NetworkTree
import org.geometerplus.zlibrary.core.network.ZLNetworkContext

internal class ShowBookInfoAction(activity: Activity, private val myNetworkContext: ZLNetworkContext) : BookAction(activity, ActionCode.SHOW_BOOK_ACTIVITY, "bookInfo") {

    override fun run(tree: NetworkTree) {
        if (getBook(tree).isFullyLoaded()) {
            showBookInfo(tree)
        } else {
            UIUtil.wait("loadInfo", {
                getBook(tree).loadFullInformation(myNetworkContext)
                myActivity.runOnUiThread {
                    showBookInfo(tree)
                }
            }, myActivity)
        }
    }

    private fun showBookInfo(tree: NetworkTree) {
        OrientationUtil.startActivityForResult(
            myActivity,
            Intent(myActivity, NetworkBookInfoActivity::class.java)
                .putExtra(TreeActivity.TREE_KEY_KEY, tree.getUniqueKey()),
            1
        )
    }
}
