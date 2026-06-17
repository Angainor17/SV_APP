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

import org.geometerplus.fbreader.book.Book
import org.geometerplus.fbreader.book.BookUtil
import org.geometerplus.fbreader.book.IBookCollection
import org.geometerplus.fbreader.network.urlInfo.BookBuyUrlInfo
import org.geometerplus.fbreader.network.urlInfo.BookUrlInfo
import org.geometerplus.fbreader.network.urlInfo.RelatedUrlInfo
import org.geometerplus.fbreader.network.urlInfo.UrlInfo
import org.geometerplus.fbreader.network.urlInfo.UrlInfoCollection
import org.geometerplus.zlibrary.core.network.ZLNetworkContext
import java.io.File
import java.util.LinkedList

open class NetworkBookItem(
    link: INetworkLink,
    @JvmField val id: String,
    @JvmField val index: Int,
    title: CharSequence,
    summary: CharSequence?,
    authors: List<AuthorData>,
    @JvmField val tags: MutableList<String>,
    @JvmField val seriesTitle: String?,
    @JvmField val indexInSeries: Float,
    urls: UrlInfoCollection<*>?
) : NetworkItem(link, title, summary, urls) {

    companion object {
        private const val HASH_PREFIX = "sha1:"
    }

    @JvmField
    val authors: LinkedList<AuthorData> = LinkedList(authors)
    @JvmField
    val identifiers: MutableList<String> = mutableListOf()

    open fun isFullyLoaded(): Boolean = true

    open fun loadFullInformation(nc: ZLNetworkContext): Boolean = true

    open fun createRelatedCatalogItem(info: RelatedUrlInfo): NetworkCatalogItem? = null

    fun getStatus(collection: IBookCollection<Book>?): Status {
        return when {
            localCopyFileName(collection) != null -> Status.Downloaded
            reference(UrlInfo.Type.Book) != null -> Status.ReadyForDownload
            buyInfo() != null -> Status.CanBePurchased
            else -> Status.NotAvailable
        }
    }

    private fun getReferenceInternal(type: UrlInfo.Type): BookUrlInfo? {
        var reference: BookUrlInfo? = null
        for (r in getAllInfos(type)) {
            if (r !is BookUrlInfo) continue
            if (reference == null || BookUrlInfo.isMimeBetterThan(r.mime, reference.mime)) {
                reference = r
            }
        }
        return reference
    }

    fun reference(type: UrlInfo.Type): BookUrlInfo? {
        val reference = getReferenceInternal(type)
        if (reference != null) {
            return reference
        }

        when (type) {
            UrlInfo.Type.Book -> {
                if (getReferenceInternal(UrlInfo.Type.BookConditional) != null) {
                    val authManager = link?.authenticationManager()
                    if (authManager == null || authManager.needPurchase(this)) {
                        return null
                    }
                    return authManager.downloadReference(this)
                } else if (buyInfo() == null) {
                    return getReferenceInternal(UrlInfo.Type.BookFullOrDemo)
                }
            }
            UrlInfo.Type.BookDemo -> {
                if (buyInfo() != null) {
                    return getReferenceInternal(UrlInfo.Type.BookFullOrDemo)
                }
            }
            else -> {}
        }

        return null
    }

    fun buyInfo(): BookBuyUrlInfo? {
        val info = reference(UrlInfo.Type.BookBuy)
        if (info != null) {
            return info as BookBuyUrlInfo
        }
        return reference(UrlInfo.Type.BookBuyInBrowser) as? BookBuyUrlInfo
    }

    fun localCopyFileName(collection: IBookCollection<Book>?): String? {
        if (collection != null) {
            for (identifier in identifiers) {
                if (identifier.startsWith(HASH_PREFIX)) {
                    val hash = identifier.substring(HASH_PREFIX.length)
                    val book = collection.getBookByHash(hash)
                    if (book != null) {
                        val file = BookUtil.fileByBook(book).physicalFile
                        if (file != null) {
                            return file.path
                        }
                    }
                }
            }
        }

        val hasBuyReference = buyInfo() != null
        var reference: BookUrlInfo? = null
        var fileName: String? = null
        for (r in getAllInfos()) {
            if (r !is BookUrlInfo) continue
            val type = r.infoType
            if ((type == UrlInfo.Type.Book ||
                        type == UrlInfo.Type.BookConditional ||
                        (!hasBuyReference && type == UrlInfo.Type.BookFullOrDemo)) &&
                (reference == null || BookUrlInfo.isMimeBetterThan(r.mime, reference.mime))
            ) {
                val name = r.localCopyFileName(UrlInfo.Type.Book)
                if (name != null) {
                    reference = r
                    fileName = name
                }
            }
        }
        return fileName
    }

    fun removeLocalFiles() {
        val hasBuyReference = buyInfo() != null
        for (r in getAllInfos()) {
            if (r !is BookUrlInfo) continue
            val type = r.infoType
            if (type == UrlInfo.Type.Book ||
                type == UrlInfo.Type.BookConditional ||
                (!hasBuyReference && type == UrlInfo.Type.BookFullOrDemo)
            ) {
                val fileName = r.localCopyFileName(UrlInfo.Type.Book)
                if (fileName != null) {
                    // TODO: remove a book from the library
                    // TODO: remove a record from the database
                    File(fileName).delete()
                }
            }
        }
    }

    val stringId: String
        get() = "@Book:$id:$title"

    enum class Status {
        NotAvailable,
        Downloaded,
        ReadyForDownload,
        CanBePurchased
    }

    class AuthorData(
        @JvmField val displayName: String,
        @JvmField val sortKey: String
    ) : Comparable<AuthorData> {

        constructor(displayName: String) : this(displayName, displayName.lowercase())

        override fun compareTo(other: AuthorData): Int {
            val key = sortKey.compareTo(other.sortKey)
            if (key != 0) {
                return key
            }
            return displayName.compareTo(other.displayName)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is AuthorData) return false
            return sortKey == other.sortKey && displayName == other.displayName
        }

        override fun hashCode(): Int = sortKey.hashCode() + displayName.hashCode()
    }
}
