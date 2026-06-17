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

import org.geometerplus.fbreader.network.authentication.NetworkAuthenticationManager
import org.geometerplus.fbreader.network.tree.NetworkItemsLoader
import org.geometerplus.fbreader.network.urlInfo.UrlInfo
import org.geometerplus.fbreader.network.urlInfo.UrlInfoWithDate
import org.geometerplus.zlibrary.core.network.ZLNetworkRequest

interface INetworkLink : Comparable<INetworkLink> {

    companion object {
        const val INVALID_ID = -1
    }

    val id: Int
    fun setId(id: Int)
    val stringId: String
    val shortName: String
    val hostName: String?
    val title: String
    val summary: String?
    fun getUrl(type: UrlInfo.Type): String?
    fun getUrlInfo(type: UrlInfo.Type): UrlInfoWithDate?
    val urlKeys: Set<UrlInfo.Type>

    /**
     * @param force if local status is not checked then
     *              if force is set to false, NotChecked will be returned
     *              if force is set to true, network check will be performed;
     *              that will take some time and can return NotChecked (if network is not available)
     */
    //fun getAccountStatus(force: Boolean): AccountStatus

    val type: Type

    /**
     * @return 2-letters language code or special token "multi"
     */
    val language: String?

    /**
     * @param loader Network operation loader
     * @return instance, which represents the state of the network operation.
     */
    fun createOperationData(loader: NetworkItemsLoader?): NetworkOperationData

    val basketItem: BasketItem?

    fun simpleSearchRequest(pattern: String, data: NetworkOperationData): ZLNetworkRequest?

    fun resume(data: NetworkOperationData): ZLNetworkRequest?

    fun libraryItem(): NetworkCatalogItem?

    fun authenticationManager(): NetworkAuthenticationManager?

    fun rewriteUrl(url: String, isUrlExternal: Boolean): String

    enum class Type(val index: Int) {
        Predefined(0),
        Custom(1),
        Local(2),
        Sync(3);

        companion object {
            fun byIndex(index: Int): Type {
                for (t in entries) {
                    if (t.index == index) {
                        return t
                    }
                }
                return Custom
            }
        }
    }

    enum class AccountStatus {
        NotSupported,
        NoUserName,
        SignedIn,
        SignedOut,
        NotChecked
    }
}
