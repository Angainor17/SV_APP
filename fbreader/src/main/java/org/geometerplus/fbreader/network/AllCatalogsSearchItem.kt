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

import org.geometerplus.fbreader.network.tree.NetworkItemsLoader
import org.geometerplus.zlibrary.core.network.ZLNetworkContext
import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.network.ZLNetworkRequest
import org.geometerplus.zlibrary.core.util.MimeType

class AllCatalogsSearchItem(private val library: NetworkLibrary) : SearchItem(
    null,
    NetworkLibrary.resource().getResource("search").getResource("summaryAllCatalogs").value
) {

    @Throws(ZLNetworkException::class)
    override fun runSearch(nc: ZLNetworkContext, loader: NetworkItemsLoader, pattern: String) {
        val requestList = mutableListOf<ZLNetworkRequest>()
        val dataList = mutableListOf<NetworkOperationData>()

        var containsCyrillicLetters = false
        for (c in pattern.lowercase().toCharArray()) {
            if ("абвгдеёжзийклмнопрстуфхцчшщъыьэюя".indexOf(c) >= 0) {
                containsCyrillicLetters = true
                break
            }
        }
        for (link in library.activeLinks()) {
            if (containsCyrillicLetters) {
                if ("ebooks.qumran.org" == link.hostName) {
                    continue
                }
            }
            val data = link.createOperationData(loader)
            val request = link.simpleSearchRequest(pattern, data)
            if (request != null) {
                dataList.add(data)
                requestList.add(request)
            }
        }

        while (requestList.isNotEmpty()) {
            nc.perform(requestList)

            requestList.clear()

            if (loader.confirmInterruption()) {
                return
            }
            for (data in dataList) {
                val request = data.resume()
                if (request != null) {
                    requestList.add(request)
                }
            }
        }
    }

    override val mimeType: MimeType
        get() = MimeType.APP_ATOM_XML

    override fun getUrl(pattern: String): String? = null
}
