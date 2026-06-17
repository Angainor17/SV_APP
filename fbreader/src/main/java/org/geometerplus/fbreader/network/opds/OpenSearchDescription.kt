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

import org.geometerplus.zlibrary.core.util.MimeType
import java.util.regex.Pattern

class OpenSearchDescription(
    @JvmField val template: String,
    @JvmField val indexOffset: Int,
    @JvmField val pageOffset: Int,
    @JvmField val mime: MimeType
) {

    val itemsPerPage: Int = 20

    companion object {
        @JvmStatic
        fun createDefault(tmpl: String, mime: MimeType): OpenSearchDescription =
            OpenSearchDescription(tmpl, -1, -1, mime)
    }

    fun isValid(): Boolean = makeQuery("") != null

    // searchTerms -- an HTML-encoded string
    fun makeQuery(searchTerms: String): String? {
        val query = StringBuffer()
        val m = Pattern.compile("\\{([^}]*)\\}").matcher(template)
        while (m.find()) {
            var name = m.group(1)
            if (name.isNullOrEmpty() || name.contains(":")) {
                return null
            }
            val optional = name.endsWith("?")
            if (optional) {
                name = name.substring(0, name.length - 1)
            }
            when (name) {
                "searchTerms" -> m.appendReplacement(query, searchTerms)
                "count" -> m.appendReplacement(query, itemsPerPage.toString())
                "startIndex" -> {
                    if (indexOffset > 0) {
                        m.appendReplacement(query, indexOffset.toString())
                    } else if (optional) {
                        m.appendReplacement(query, "")
                    } else {
                        return null
                    }
                }
                "startPage" -> {
                    if (pageOffset > 0) {
                        m.appendReplacement(query, pageOffset.toString())
                    } else if (optional) {
                        m.appendReplacement(query, "")
                    } else {
                        return null
                    }
                }
                "language" -> m.appendReplacement(query, "*")
                "inputEncoding", "outputEncoding" -> m.appendReplacement(query, "UTF-8")
                else -> if (optional) m.appendReplacement(query, "") else return null
            }
        }
        m.appendTail(query)
        return query.toString()
    }
}
