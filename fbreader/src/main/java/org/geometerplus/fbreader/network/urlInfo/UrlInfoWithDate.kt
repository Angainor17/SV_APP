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
import java.util.Date

class UrlInfoWithDate(
    type: Type?,
    url: String?,
    mime: MimeType,
    @JvmField val updated: Date = Date()
) : UrlInfo(type ?: Type.Catalog, url, mime) {

    companion object {
        @JvmField
        val NULL = UrlInfoWithDate(null, null, MimeType.NULL)
        private const val serialVersionUID: Long = -896768978957787222L
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UrlInfoWithDate) return false

        return infoType == other.infoType &&
                ComparisonUtil.equal(url, other.url) &&
                ComparisonUtil.equal(mime, other.mime) &&
                ComparisonUtil.equal(updated, other.updated)
    }

    override fun hashCode(): Int =
        infoType.hashCode() + ComparisonUtil.hashCode(url) + ComparisonUtil.hashCode(mime) + ComparisonUtil.hashCode(updated)
}
