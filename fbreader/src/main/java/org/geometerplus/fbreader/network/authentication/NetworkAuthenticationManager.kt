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

package org.geometerplus.fbreader.network.authentication

import org.geometerplus.fbreader.network.INetworkLink
import org.geometerplus.fbreader.network.NetworkBookItem
import org.geometerplus.fbreader.network.NetworkException
import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.fbreader.network.authentication.litres.LitResAuthenticationManager
import org.geometerplus.fbreader.network.opds.OPDSNetworkLink
import org.geometerplus.fbreader.network.urlInfo.BookUrlInfo
import org.geometerplus.zlibrary.core.money.Money
import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.options.ZLStringOption

abstract class NetworkAuthenticationManager protected constructor(
    @JvmField val link: INetworkLink
) {

    companion object {
        private val managers = mutableMapOf<String, NetworkAuthenticationManager>()

        @JvmStatic
        fun createManager(
            library: NetworkLibrary,
            link: INetworkLink,
            managerClass: Class<out NetworkAuthenticationManager>?
        ): NetworkAuthenticationManager? {
            var mgr = managers[link.stringId]
            if (mgr == null) {
                if (managerClass == LitResAuthenticationManager::class.java) {
                    mgr = LitResAuthenticationManager(library, link as OPDSNetworkLink)
                }
                if (mgr != null) {
                    managers[link.stringId] = mgr
                }
            }
            return mgr
        }
    }

    protected val userNameOption = ZLStringOption(link.stringId, "userName", "")

    open fun getUserName(): String = userNameOption.value

    open fun getVisibleUserName(): String {
        val username = getUserName()
        return if (username.startsWith("fbreader-auto-")) "auto" else username
    }

    /*
     * Common manager methods
     */
    @Throws(ZLNetworkException::class)
    abstract fun isAuthorised(useNetwork: Boolean = true): Boolean

    @Throws(ZLNetworkException::class)
    abstract fun authorise(username: String, password: String)

    abstract fun logOut()

    abstract fun downloadReference(book: NetworkBookItem): BookUrlInfo?

    @Throws(ZLNetworkException::class)
    abstract fun refreshAccountInformation()

    fun mayBeAuthorised(useNetwork: Boolean): Boolean {
        return try {
            isAuthorised(useNetwork)
        } catch (e: ZLNetworkException) {
            true
        }
    }

    open fun needsInitialization(): Boolean = false

    @Throws(ZLNetworkException::class)
    open fun initialize() {
        throw ZLNetworkException.forCode(NetworkException.ERROR_UNSUPPORTED_OPERATION)
    }

    // returns true if link must be purchased before downloading
    open fun needPurchase(book: NetworkBookItem): Boolean = true

    @Throws(ZLNetworkException::class)
    open fun purchaseBook(book: NetworkBookItem) {
        throw ZLNetworkException.forCode(NetworkException.ERROR_UNSUPPORTED_OPERATION)
    }

    open fun purchasedBooks(): List<NetworkBookItem> = emptyList()

    open fun currentAccount(): Money? = null

    /*
     * topup account
     */
    open fun topupLink(sum: Money): String? = null

    open fun getTopupData(): Map<String, String> = emptyMap()

    /*
     * Password Recovery
     */
    open fun passwordRecoverySupported(): Boolean = false

    @Throws(ZLNetworkException::class)
    open fun recoverPassword(email: String) {
        throw ZLNetworkException.forCode(NetworkException.ERROR_UNSUPPORTED_OPERATION)
    }
}
