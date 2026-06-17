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

import org.geometerplus.fbreader.network.NetworkTree
import org.geometerplus.fbreader.network.TopUpItem
import org.geometerplus.zlibrary.core.image.ZLImage
import org.geometerplus.zlibrary.core.network.ZLNetworkException

class TopUpTree internal constructor(
    parentTree: NetworkCatalogTree,
    @JvmField val item: TopUpItem
) : NetworkTree(parentTree) {

    override val name: String
        get() = item.title.toString()

    override val summary: String
        get() {
            val mgr = getLink()?.authenticationManager()
            return try {
                if (mgr != null && mgr.isAuthorised(false)) {
                    val account = mgr.currentAccount()
                    val summary = item.getSummary()
                    if (account != null && summary != null) {
                        summary.toString().replace("%s", account.toString())
                    } else {
                        ""
                    }
                } else {
                    ""
                }
            } catch (e: ZLNetworkException) {
                ""
            }
        }

    override fun createCover(): ZLImage? = createCoverForItem(library, item, true)

    override val stringId: String
        get() = "@TopUp Account"
}
