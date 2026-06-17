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

package org.geometerplus.fbreader.network.opds

import org.geometerplus.fbreader.network.atom.ATOMLink
import org.geometerplus.zlibrary.core.money.Money
import org.geometerplus.zlibrary.core.xml.ZLStringMap

import java.util.Currency
import java.util.Locale

open class OPDSLink(attributes: ZLStringMap) : ATOMLink(attributes) {
    val prices = mutableListOf<Money>()
    val formats = mutableListOf<String>()

    private fun getPrice(currency: String): Money? {
        for (p in prices) {
            if (currency == p.Currency) {
                return p
            }
        }
        return null
    }

    fun selectBestPrice(): Money? {
        if (prices.isEmpty()) {
            return null
        } else if (prices.size == 1) {
            return prices[0]
        }
        val locale = Locale.getDefault()
        if (locale.country.length == 2) {
            val bestCode = Currency.getInstance(locale).currencyCode
            if (bestCode != null) {
                getPrice(bestCode)?.let { return it }
            }
        }
        getPrice("USD")?.let { return it }
        getPrice("EUR")?.let { return it }
        return prices[0]
    }
}
