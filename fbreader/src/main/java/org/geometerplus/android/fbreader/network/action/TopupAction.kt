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
import org.geometerplus.android.fbreader.network.TopupMenuActivity
import org.geometerplus.fbreader.network.NetworkTree
import org.geometerplus.fbreader.network.tree.NetworkCatalogRootTree
import org.geometerplus.fbreader.network.tree.TopUpTree
import org.geometerplus.zlibrary.core.money.Money

class TopupAction(activity: Activity) : Action(activity, ActionCode.TOPUP, "topup", -1) {

    override fun isVisible(tree: NetworkTree): Boolean {
        return when (tree) {
            is TopUpTree -> true
            is NetworkCatalogRootTree -> {
                val link = tree.getLink()
                val mgr = link?.authenticationManager()
                mgr != null &&
                    mgr.mayBeAuthorised(false) &&
                    mgr.currentAccount() != null &&
                    TopupMenuActivity.isTopupSupported(link)
            }
            else -> false
        }
    }

    override fun run(tree: NetworkTree) {
        val link = tree.getLink()
        if (link != null) {
            TopupMenuActivity.runMenu(myActivity, link, null)
        }
    }

    override fun getContextLabel(tree: NetworkTree): String {
        val link = tree.getLink()
        var account: Money? = null
        if (link != null) {
            val mgr = link.authenticationManager()
            if (mgr != null && mgr.mayBeAuthorised(false)) {
                account = mgr.currentAccount()
            }
        }
        return super.getContextLabel(tree).replace("%s", account?.toString() ?: "")
    }
}
