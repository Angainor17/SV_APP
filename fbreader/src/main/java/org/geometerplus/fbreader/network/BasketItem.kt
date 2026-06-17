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

import org.geometerplus.fbreader.network.urlInfo.UrlInfo
import org.geometerplus.fbreader.network.urlInfo.UrlInfoCollection
import org.geometerplus.zlibrary.core.money.Money
import org.geometerplus.zlibrary.core.options.ZLStringListOption

abstract class BasketItem protected constructor(
    private val library: NetworkLibrary,
    link: INetworkLink
) : NetworkCatalogItem(
    link,
    NetworkLibrary.resource().getResource("basket").value,
    NetworkLibrary.resource().getResource("basketSummaryEmpty").value,
    UrlInfoCollection<UrlInfo>(),
    Accessibility.ALWAYS,
    FLAGS_DEFAULT and FLAGS_GROUP.inv()
) {

    private val booksInBasketOption = ZLStringListOption(link.stringId, "Basket", emptyList(), ",")
    private val books = mutableMapOf<String, NetworkBookItem>()
    private var generation = 0L

    open fun addItem(book: NetworkBookItem) {
        books[book.id] = book
    }

    fun getBasketSummary(): CharSequence {
        val size = bookIds().size
        if (size == 0) {
            return getSummary() ?: ""
        } else {
            val basketCost = cost()
            return if (basketCost != null) {
                NetworkLibrary.resource().getResource("basketSummary").getValue(size)
                    .replace("%0", size.toString()).replace("%1", basketCost.toString())
            } else {
                NetworkLibrary.resource().getResource("basketSummaryCountOnly").getValue(size)
                    .replace("%0", size.toString())
            }
        }
    }

    override fun canBeOpened(): Boolean = bookIds().isNotEmpty()

    override val stringId: String
        get() = "@Basket:${link?.stringId}"

    fun getGeneration(): Long = generation

    open fun add(book: NetworkBookItem) {
        var ids = bookIds()
        if (!ids.contains(book.id)) {
            ids = ArrayList(ids)
            ids.add(book.id)
            booksInBasketOption.value = ids
            addItem(book)
            ++generation
            library.fireModelChangedEvent(NetworkLibrary.ChangeListener.Code.SomeCode)
        }
    }

    open fun remove(book: NetworkBookItem) {
        var ids = bookIds()
        if (ids.contains(book.id)) {
            ids = ArrayList(ids)
            ids.remove(book.id)
            booksInBasketOption.value = ids
            books.remove(book.id)
            ++generation
            library.fireModelChangedEvent(NetworkLibrary.ChangeListener.Code.SomeCode)
        }
    }

    open fun clear() {
        booksInBasketOption.value = emptyList()
        books.clear()
        ++generation
        library.fireModelChangedEvent(NetworkLibrary.ChangeListener.Code.SomeCode)
    }

    open fun contains(book: NetworkBookItem): Boolean = bookIds().contains(book.id)

    fun bookIds(): List<String> = booksInBasketOption.value

    fun getBook(id: String): NetworkBookItem? = books[id]

    protected fun isFullyLoaded(): Boolean {
        synchronized(books) {
            for (id in bookIds()) {
                val b = books[id]
                if (b == null) {
                    return false
                }
            }
        }
        return true
    }

    private fun cost(): Money? {
        var sum = Money.ZERO
        synchronized(books) {
            for (id in bookIds()) {
                val b = books[id] ?: return null
                val info = b.buyInfo() ?: return null
                if (b.getStatus(null) == NetworkBookItem.Status.CanBePurchased) {
                    if (info.price == null) {
                        return null
                    }
                    sum = sum.add(info.price)
                }
            }
        }
        return sum
    }
}
