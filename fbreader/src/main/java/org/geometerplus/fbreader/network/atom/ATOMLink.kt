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

package org.geometerplus.fbreader.network.atom

import org.geometerplus.zlibrary.core.xml.ZLStringMap

open class ATOMLink(source: ZLStringMap) : ATOMCommonAttributes(source) {

    companion object {
        const val HREF = "href"
        const val REL = "rel"
        const val TYPE = "type"
        const val HREFLANG = "hreflang"
        const val TITLE = "title"
        const val LENGTH = "length"
    }

    init {
        readAttribute(HREF, source)
        readAttribute(REL, source)
        readAttribute(TYPE, source)
        readAttribute(HREFLANG, source)
        readAttribute(TITLE, source)
        readAttribute(LENGTH, source)
    }

    val href: String?
        get() = getAttribute(HREF)

    val rel: String?
        get() = getAttribute(REL)

    val type: String?
        get() = getAttribute(TYPE)

    val hrefLang: String?
        get() = getAttribute(HREFLANG)

    val title: String?
        get() = getAttribute(TITLE)

    val length: String?
        get() = getAttribute(LENGTH)
}
