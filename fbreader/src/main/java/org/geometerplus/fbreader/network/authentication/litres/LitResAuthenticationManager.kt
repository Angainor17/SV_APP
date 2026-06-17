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

package org.geometerplus.fbreader.network.authentication.litres

import org.geometerplus.fbreader.network.AlreadyPurchasedException
import org.geometerplus.fbreader.network.NetworkBookItem
import org.geometerplus.fbreader.network.NetworkException
import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.fbreader.network.authentication.NetworkAuthenticationManager
import org.geometerplus.fbreader.network.opds.OPDSNetworkLink
import org.geometerplus.fbreader.network.urlInfo.BookUrlInfo
import org.geometerplus.fbreader.network.urlInfo.DecoratedBookUrlInfo
import org.geometerplus.fbreader.network.urlInfo.UrlInfo
import org.geometerplus.zlibrary.core.money.Money
import org.geometerplus.zlibrary.core.network.QuietNetworkContext
import org.geometerplus.zlibrary.core.network.ZLNetworkAuthenticationException
import org.geometerplus.zlibrary.core.network.ZLNetworkContext
import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.options.ZLBooleanOption
import org.geometerplus.zlibrary.core.options.ZLStringOption
import org.geometerplus.zlibrary.core.util.ZLNetworkUtil

class LitResAuthenticationManager(
    private val library: NetworkLibrary,
    link: OPDSNetworkLink
) : NetworkAuthenticationManager(link) {

    private val networkContext: ZLNetworkContext = QuietNetworkContext()
    private val sidOption = ZLStringOption(LitResUtil.HOST_NAME, "sid", "")
    private val userIdOption = ZLStringOption(LitResUtil.HOST_NAME, "userId", "")
    private val canRebillOption = ZLBooleanOption(LitResUtil.HOST_NAME, "canRebill", false)
    private val purchasedBooks = BookCollection()
    private val initialisationLock = Any()
    @Volatile
    private var fullyInitialized = false
    @Volatile
    private var initializedDataSid: String? = null
    @Volatile
    private var account: Money? = null

    @Synchronized
    fun initUser(username: String?, sid: String, userId: String, canRebill: Boolean): Boolean {
        var changed = false
        var actualUsername = username
        if (actualUsername == null) {
            actualUsername = userNameOption.value
        } else if (actualUsername != userNameOption.value) {
            changed = true
            userNameOption.value = actualUsername
        }
        changed = changed || sid != sidOption.value
        sidOption.value = sid
        changed = changed || userId != userIdOption.value
        userIdOption.value = userId
        changed = changed || canRebill != canRebillOption.value
        canRebillOption.value = canRebill
        val newFullyInitialized = actualUsername.isNotEmpty() && sid.isNotEmpty() && userId.isNotEmpty()
        if (newFullyInitialized != fullyInitialized) {
            changed = true
            fullyInitialized = newFullyInitialized
        }
        return changed
    }

    override fun logOut() {
        logOut(true)
    }

    @Synchronized
    private fun logOut(full: Boolean) {
        var changed = false
        if (full) {
            changed = initUser(null, "", "", false)
        } else {
            changed = fullyInitialized
            fullyInitialized = false
        }
        changed = changed || initializedDataSid != null
        initializedDataSid = null
        changed = changed || !purchasedBooks.isEmpty
        purchasedBooks.clear()
        if (changed) {
            library.fireModelChangedEvent(NetworkLibrary.ChangeListener.Code.SignedIn)
        }
    }

    @Throws(ZLNetworkException::class)
    override fun isAuthorised(useNetwork: Boolean): Boolean {
        val sid: String
        synchronized(this) {
            val authState = userNameOption.value.isNotEmpty() && sidOption.value.isNotEmpty()

            if (fullyInitialized || !useNetwork) {
                return authState
            }

            if (!authState) {
                logOut(false)
                return false
            }
            sid = sidOption.value
        }

        return try {
            val xmlReader = LitResLoginXMLReader()
            val params = mutableMapOf<String, String>()
            val url = parseUrl(link.getUrl(UrlInfo.Type.SignIn), params)
                ?: throw ZLNetworkException.forCode(NetworkException.ERROR_UNSUPPORTED_OPERATION)
            val request = LitResNetworkRequest(url, xmlReader)
            for ((key, value) in params) {
                request.addPostParameter(key, value)
            }
            request.addPostParameter("sid", sid)
            networkContext.perform(request)
            initUser(null, xmlReader.sid ?: "", xmlReader.userId ?: "", xmlReader.canRebill)
            true
        } catch (e: ZLNetworkAuthenticationException) {
            throw e
        } catch (e: ZLNetworkException) {
            logOut(false)
            false
        }
    }

    @Throws(ZLNetworkException::class)
    override fun authorise(username: String, password: String) {
        val params = mutableMapOf<String, String>()
        val url = parseUrl(link.getUrl(UrlInfo.Type.SignIn), params)
            ?: throw ZLNetworkException.forCode(NetworkException.ERROR_UNSUPPORTED_OPERATION)
        synchronized(this) {
            userNameOption.value = username
        }

        try {
            val xmlReader = LitResLoginXMLReader()
            val request = LitResNetworkRequest(url, xmlReader)
            for ((key, value) in params) {
                request.addPostParameter(key, value)
            }
            request.addPostParameter("login", username)
            request.addPostParameter("pwd", password)
            networkContext.perform(request)
            library.fireModelChangedEvent(NetworkLibrary.ChangeListener.Code.SignedIn)
            initUser(null, xmlReader.sid ?: "", xmlReader.userId ?: "", xmlReader.canRebill)
        } catch (e: ZLNetworkException) {
            logOut(false)
            throw e
        }
    }

    override fun downloadReference(book: NetworkBookItem): BookUrlInfo? {
        val sid: String
        synchronized(this) {
            sid = sidOption.value
        }
        if (sid.isEmpty()) {
            return null
        }
        val reference = book.reference(UrlInfo.Type.BookConditional) ?: return null
        val url = reference.url ?: return null
        return DecoratedBookUrlInfo(reference, ZLNetworkUtil.appendParameter(url, "sid", sid))
    }

    @Synchronized
    override fun needPurchase(book: NetworkBookItem): Boolean = !purchasedBooks.contains(book)

    @Throws(ZLNetworkException::class)
    override fun purchaseBook(book: NetworkBookItem) {
        val sid: String
        synchronized(this) {
            sid = sidOption.value
        }
        if (sid.isEmpty()) {
            throw ZLNetworkAuthenticationException()
        }

        val reference = book.reference(UrlInfo.Type.BookBuy)
            ?: throw ZLNetworkException.forCode(NetworkException.ERROR_BOOK_NOT_PURCHASED)
        val referenceUrl = reference.url
            ?: throw ZLNetworkException.forCode(NetworkException.ERROR_BOOK_NOT_PURCHASED)

        val xmlReader = LitResPurchaseXMLReader()

        var exception: ZLNetworkException? = null
        try {
            val request = LitResNetworkRequest(referenceUrl, xmlReader)
            request.addPostParameter("sid", sid)
            networkContext.perform(request)
        } catch (e: ZLNetworkException) {
            exception = e
        }

        synchronized(this) {
            if (xmlReader.account != null) {
                account = Money(xmlReader.account!!, "RUB")
            }
            if (exception != null) {
                if (exception is ZLNetworkAuthenticationException) {
                    logOut(false)
                } else if (exception is AlreadyPurchasedException) {
                    purchasedBooks.addToStart(book)
                }
                throw exception
            }
            if (xmlReader.bookId == null || xmlReader.bookId != book.id) {
                throw ZLNetworkException.forCode(ZLNetworkException.ERROR_SOMETHING_WRONG, LitResUtil.HOST_NAME)
            }
            purchasedBooks.addToStart(book)
            val basket = book.link?.basketItem
            basket?.remove(book)
        }
    }

    override fun topupLink(sum: Money): String? {
        val sid: String
        synchronized(this) {
            sid = sidOption.value
        }
        if (sid.isEmpty()) {
            return null
        }
        var url = link.getUrl(UrlInfo.Type.TopUp) ?: return null
        url = ZLNetworkUtil.appendParameter(url, "sid", sid)
        if (sum != null) {
            url = ZLNetworkUtil.appendParameter(url, "summ", sum.Amount.toString())
        }
        return url
    }

    @Synchronized
    override fun currentAccount(): Money? = account

    @Throws(ZLNetworkException::class)
    internal fun reloadPurchasedBooks() {
        var page = 0
        while (true) {
            var networkRequest: LitResNetworkRequest
            synchronized(this) {
                val sid = sidOption.value
                if (sid.isEmpty()) {
                    throw ZLNetworkAuthenticationException()
                }
                if (sid != initializedDataSid) {
                    logOut(false)
                    throw ZLNetworkAuthenticationException()
                }
                networkRequest = loadPurchasedBooksRequest(sid, page)
            }

            var exception: ZLNetworkException? = null
            try {
                networkContext.perform(networkRequest)
            } catch (e: ZLNetworkException) {
                exception = e
            }

            synchronized(this) {
                if (exception != null) {
                    if (exception is ZLNetworkAuthenticationException) {
                        logOut(false)
                    }
                    throw exception
                }
                if (loadPurchasedBooksOnSuccess(networkRequest, page == 0) < BOOKS_PER_PAGE) {
                    break
                }
            }
            page++
        }
    }

    override fun purchasedBooks(): List<NetworkBookItem> {
        synchronized(initialisationLock) {
            return purchasedBooks.list()
        }
    }

    @Synchronized
    override fun needsInitialization(): Boolean {
        val sid = sidOption.value
        if (sid.isEmpty()) {
            return false
        }
        return sid != initializedDataSid
    }

    @Throws(ZLNetworkException::class)
    override fun initialize() {
        synchronized(initialisationLock) {
            initializeInternal()
        }
    }

    @Throws(ZLNetworkException::class)
    private fun initializeInternal() {
        val sid: String
        val purchasedBooksRequest: LitResNetworkRequest
        val accountRequest: LitResNetworkRequest
        synchronized(this) {
            sid = sidOption.value
            if (sid.isEmpty()) {
                throw ZLNetworkAuthenticationException()
            }
            if (sid == initializedDataSid || !isAuthorised(true)) {
                return
            }

            purchasedBooksRequest = loadPurchasedBooksRequest(sid, 0)
            accountRequest = loadAccountRequest(sid)
        }

        val requests = mutableListOf<LitResNetworkRequest>()
        requests.add(purchasedBooksRequest)
        requests.add(accountRequest)

        try {
            networkContext.perform(requests)
            val hasMorePages: Boolean
            synchronized(this) {
                initializedDataSid = sid
                hasMorePages = loadPurchasedBooksOnSuccess(purchasedBooksRequest, true) == BOOKS_PER_PAGE
                account = Money((accountRequest.reader as LitResPurchaseXMLReader).account!!, "RUB")
            }
            if (hasMorePages) {
                var page = 1
                while (true) {
                    val r = loadPurchasedBooksRequest(sid, page)
                    networkContext.perform(r)
                    synchronized(this) {
                        if (loadPurchasedBooksOnSuccess(r, false) < BOOKS_PER_PAGE) {
                            break
                        }
                    }
                    page++
                }
            }
        } catch (e: ZLNetworkException) {
            synchronized(this) {
                initializedDataSid = null
                loadPurchasedBooksOnError()
                account = null
            }
            throw e
        }
    }

    @Throws(ZLNetworkException::class)
    override fun refreshAccountInformation() {
        val accountRequest = loadAccountRequest(sidOption.value)
        networkContext.perform(accountRequest)
        synchronized(this) {
            account = Money((accountRequest.reader as LitResPurchaseXMLReader).account!!, "RUB")
        }
    }

    private fun loadPurchasedBooksRequest(sid: String, page: Int): LitResNetworkRequest {
        val query = "pages/catalit_browser/"

        val request = LitResNetworkRequest(
            LitResUtil.url(link, query),
            LitResXMLReader(library, link as OPDSNetworkLink)
        )
        request.addPostParameter("my", "1")
        request.addPostParameter("sid", sid)
        request.addPostParameter("limit", "${page * BOOKS_PER_PAGE},$BOOKS_PER_PAGE")
        return request
    }

    private fun loadPurchasedBooksOnError() {
        purchasedBooks.clear()
    }

    private fun loadPurchasedBooksOnSuccess(purchasedBooksRequest: LitResNetworkRequest, clear: Boolean): Int {
        if (clear) {
            purchasedBooks.clear()
        }
        val reader = purchasedBooksRequest.reader as LitResXMLReader
        for (book in reader.books) {
            System.err.println("TITLE: ${book.title}")
            purchasedBooks.addToEnd(book)
        }
        return reader.books.size
    }

    private fun loadAccountRequest(sid: String): LitResNetworkRequest {
        val query = "pages/purchase_book/"

        val request = LitResNetworkRequest(
            LitResUtil.url(link, query),
            LitResPurchaseXMLReader()
        )
        request.addPostParameter("sid", sid)
        request.addPostParameter("art", "0")
        return request
    }

    override fun passwordRecoverySupported(): Boolean = true

    @Throws(ZLNetworkException::class)
    override fun recoverPassword(email: String) {
        val url = link.getUrl(UrlInfo.Type.RecoverPassword)
            ?: throw ZLNetworkException.forCode(NetworkException.ERROR_UNSUPPORTED_OPERATION)
        val xmlReader = LitResPasswordRecoveryXMLReader()
        val request = LitResNetworkRequest(url, xmlReader)
        request.addPostParameter("mail", email)
        networkContext.perform(request)
    }

    override fun getTopupData(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        map["litres:userId"] = userIdOption.value
        map["litres:canRebill"] = if (canRebillOption.value) "true" else "false"
        map["litres:sid"] = sidOption.value
        return map
    }

    private class BookCollection {
        private val map = mutableMapOf<String, NetworkBookItem>()
        private val list = mutableListOf<NetworkBookItem>()

        fun clear() {
            map.clear()
            list.clear()
        }

        val isEmpty: Boolean
            get() = list.isEmpty()

        fun addToStart(book: NetworkBookItem) {
            map[book.id] = book
            list.add(0, book)
        }

        fun addToEnd(book: NetworkBookItem) {
            map[book.id] = book
            list.add(book)
        }

        fun contains(book: NetworkBookItem): Boolean = map.containsKey(book.id)

        fun list(): List<NetworkBookItem> = list.toList()
    }

    companion object {
        private const val BOOKS_PER_PAGE = 40

        private fun parseUrl(url: String?, params: MutableMap<String, String>): String? {
            if (url == null) {
                return null
            }
            val parts = url.split("\\?".toRegex())
            if (parts.size != 2) {
                return url
            }
            for (s in parts[1].split("&")) {
                val pair = s.split("=".toRegex())
                if (pair.size == 2) {
                    params[pair[0]] = pair[1]
                }
            }
            return parts[0]
        }
    }
}
