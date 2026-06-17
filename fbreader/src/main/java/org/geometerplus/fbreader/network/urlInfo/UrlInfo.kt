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

package org.geometerplus.fbreader.network.urlInfo

import org.fbreader.util.ComparisonUtil
import org.geometerplus.zlibrary.core.util.MimeType
import java.io.Serializable

open class UrlInfo(
    @JvmField val infoType: Type,
    @JvmField val url: String?,
    @JvmField val mime: MimeType
) : Serializable {

    companion object {
        private const val serialVersionUID: Long = -893514485257788222L
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UrlInfo) return false
        return infoType == other.infoType && ComparisonUtil.equal(url, other.url)
    }

    override fun hashCode(): Int = infoType.hashCode() + ComparisonUtil.hashCode(url)

    enum class Type {
        // Never rename elements of this enum; these names are used in network database
        Catalog,
        HtmlPage,
        SingleEntry,
        Related,
        Image,
        Thumbnail,
        SearchIcon,
        Search,
        ListBooks,
        SignIn,
        SignOut,
        SignUp,
        TopUp,
        RecoverPassword,
        Book,
        BookConditional,
        BookDemo,
        BookFullOrDemo,
        BookBuy,
        BookBuyInBrowser,
        TOC,
        Comments
    }
}
