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

import org.fbreader.util.Boolean3
import org.geometerplus.fbreader.network.authentication.NetworkAuthenticationManager
import org.geometerplus.fbreader.network.tree.NetworkItemsLoader
import org.geometerplus.fbreader.network.urlInfo.UrlInfoCollection
import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.network.ZLNetworkRequest

abstract class NetworkCatalogItem(
    link: INetworkLink?,
    title: CharSequence,
    summary: CharSequence?,
    urls: UrlInfoCollection<*>?,
    private val accessibility: Accessibility,
    private var myFlags: Int
) : NetworkItem(link, title, summary, urls) {

    val flags: Int
        get() = myFlags

    // bit mask for flags parameter
    companion object {
        const val FLAG_SHOW_AUTHOR = 1 shl 0
        const val FLAG_GROUP_BY_AUTHOR = 1 shl 1
        const val FLAG_GROUP_BY_SERIES = 1 shl 2
        const val FLAG_GROUP_MORE_THAN_1_BOOK_BY_SERIES = 1 shl 3
        const val FLAG_ADD_SEARCH_ITEM = 1 shl 4

        const val FLAGS_DEFAULT = FLAG_SHOW_AUTHOR or FLAG_GROUP_MORE_THAN_1_BOOK_BY_SERIES
        const val FLAGS_GROUP = FLAG_GROUP_BY_AUTHOR or FLAG_GROUP_BY_SERIES or FLAG_GROUP_MORE_THAN_1_BOOK_BY_SERIES
    }

    var updatingInProgress = false

    open fun extraData(): Map<String, String> = emptyMap()

    abstract fun canBeOpened(): Boolean

    @Throws(ZLNetworkException::class)
    abstract fun loadChildren(loader: NetworkItemsLoader)

    open fun supportsResumeLoading(): Boolean = false

    open fun canResumeLoading(): Boolean = false

    @Throws(ZLNetworkException::class)
    open fun resumeLoading(loader: NetworkItemsLoader) {}

    fun setFlags(flags: Int) {
        myFlags = flags
    }

    fun getVisibility(): Boolean3 {
        if (link == null) {
            return Boolean3.TRUE
        }

        val mgr = link.authenticationManager()
        return when (accessibility) {
            Accessibility.ALWAYS -> Boolean3.TRUE
            Accessibility.HAS_BOOKS -> {
                if (link.basketItem != null && link.basketItem!!.bookIds().isNotEmpty()) {
                    Boolean3.TRUE
                } else {
                    // go through!
                    checkSignedIn(mgr)
                }
            }
            Accessibility.SIGNED_IN -> checkSignedIn(mgr)
            Accessibility.NEVER -> Boolean3.FALSE
        }
    }

    private fun checkSignedIn(mgr: NetworkAuthenticationManager?): Boolean3 {
        if (mgr == null) {
            return Boolean3.FALSE
        }
        return try {
            if (mgr.isAuthorised(false)) Boolean3.TRUE else Boolean3.UNDEFINED
        } catch (e: ZLNetworkException) {
            Boolean3.UNDEFINED
        }
    }

    abstract val stringId: String

    /**
     * Performs all necessary operations with NetworkOperationData and NetworkRequest
     * to complete loading children items.
     *
     * @param data           Network operation data instance
     * @param networkRequest initial network request
     * @throws ZLNetworkException when network operation couldn't be completed
     */
    @Throws(ZLNetworkException::class)
    protected fun doLoadChildren(data: NetworkOperationData, networkRequest: ZLNetworkRequest?) {
        if (networkRequest != null) {
            data.loader!!.networkContext.perform(networkRequest)
            data.loader!!.confirmInterruption()
        }
    }

    // catalog accessibility types:
    enum class Accessibility {
        NEVER,
        ALWAYS,
        SIGNED_IN,
        HAS_BOOKS
    }
}
