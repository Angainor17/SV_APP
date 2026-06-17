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
import android.app.AlertDialog
import org.geometerplus.android.fbreader.network.Util
import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.fbreader.network.NetworkTree
import org.geometerplus.fbreader.network.NetworkURLCatalogItem
import org.geometerplus.fbreader.network.tree.NetworkCatalogTree
import org.geometerplus.fbreader.network.urlInfo.UrlInfo
import org.geometerplus.zlibrary.core.resources.ZLResource

internal class OpenInBrowserAction(activity: Activity) : CatalogAction(activity, ActionCode.OPEN_IN_BROWSER, "openInBrowser") {

    override fun isVisible(tree: NetworkTree): Boolean {
        if (!super.isVisible(tree)) {
            return false
        }

        val item = (tree as NetworkCatalogTree).Item
        if (item !is NetworkURLCatalogItem) {
            return false
        }

        return item.getUrl(UrlInfo.Type.HtmlPage) != null
    }

    override fun run(tree: NetworkTree) {
        val url = ((tree as NetworkCatalogTree).Item as NetworkURLCatalogItem).getUrl(UrlInfo.Type.HtmlPage)

        if (Util.isOurLink(url!!)) {
            Util.openInBrowser(myActivity, url)
        } else {
            val buttonResource = ZLResource.resource("dialog").getResource("button")
            val message = NetworkLibrary.resource().getResource("confirmQuestions").getResource("openInBrowser").value
            AlertDialog.Builder(myActivity)
                .setTitle(tree.name)
                .setMessage(message)
                .setIcon(0)
                .setPositiveButton(buttonResource.getResource("yes").value) { dialog, which ->
                    Util.openInBrowser(myActivity, url)
                }
                .setNegativeButton(buttonResource.getResource("no").value, null)
                .create().show()
        }
    }
}
