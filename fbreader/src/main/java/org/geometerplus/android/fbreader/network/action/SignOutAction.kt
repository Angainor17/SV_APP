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
import org.geometerplus.android.util.UIUtil
import org.geometerplus.fbreader.network.ISyncNetworkLink
import org.geometerplus.fbreader.network.NetworkTree
import org.geometerplus.fbreader.network.sync.SyncUtil
import org.geometerplus.fbreader.network.tree.NetworkCatalogRootTree
import org.geometerplus.zlibrary.core.network.ZLNetworkContext

class SignOutAction(activity: Activity, private val myNetworkContext: ZLNetworkContext) : Action(activity, ActionCode.SIGNOUT, "signOut", -1) {

    override fun isVisible(tree: NetworkTree): Boolean {
        if (tree !is NetworkCatalogRootTree) {
            return false
        }

        val link = tree.getLink()
        if (link is ISyncNetworkLink) {
            return link.isLoggedIn(myNetworkContext)
        }

        val mgr = link?.authenticationManager()
        return mgr != null && mgr.mayBeAuthorised(false)
    }

    override fun run(tree: NetworkTree) {
        val link = tree.getLink()
        if (link is ISyncNetworkLink) {
            link.logout(myNetworkContext)
            (tree as NetworkCatalogRootTree).clearCatalog()
            return
        }

        val mgr = link?.authenticationManager()
        val runnable = Runnable {
            if (mgr != null && mgr.mayBeAuthorised(false)) {
                mgr.logOut()
                myActivity.runOnUiThread {
                    myLibrary.invalidateVisibility()
                    myLibrary.synchronize()
                }
            }
        }
        UIUtil.wait("signOut", runnable, myActivity)
    }

    private fun accountName(tree: NetworkTree): String? {
        val link = tree.getLink()
        if (link is ISyncNetworkLink) {
            return SyncUtil.getAccountName(myNetworkContext)
        }

        val mgr = link?.authenticationManager()
        return if (mgr != null && mgr.mayBeAuthorised(false)) mgr.getVisibleUserName() else null
    }

    override fun getOptionsLabel(tree: NetworkTree): String {
        val account = accountName(tree)
        return super.getOptionsLabel(tree).replace("%s", account ?: "")
    }

    override fun getContextLabel(tree: NetworkTree): String {
        val account = accountName(tree)
        return super.getContextLabel(tree).replace("%s", account ?: "")
    }
}
