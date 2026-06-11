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

abstract class ATOMCommonAttributes protected constructor(source: ZLStringMap) {

    companion object {
        const val XML_BASE = "xml:base"
        const val XML_LANG = "xml:lang"
    }

    private var myAttributes: ZLStringMap? = null

    init {
        readAttribute(XML_BASE, source)
        readAttribute(XML_LANG, source)
    }

    protected fun readAttribute(name: String, source: ZLStringMap) {
        var value = source.getValue(name) ?: return
        value = value.trim().intern()
        if (value.isNotEmpty()) {
            if (myAttributes == null) {
                myAttributes = ZLStringMap()
            }
            myAttributes!!.put(name, value)
        }
    }

    fun getAttribute(name: String): String? = myAttributes?.getValue(name)

    val lang: String?
        get() = getAttribute(XML_LANG)

    val base: String?
        get() = getAttribute(XML_BASE)

    // FIXME: HACK: addAttribute is used ONLY to add OPDSPrice to the ATOMLink... Must be killed + SEE NetworkOPDSFeedReader
    // name and value MUST BE not null AND MUST BE INTERNED String objects
    fun addAttribute(name: String, value: String) {
        var v = value.trim().intern()
        if (v.isNotEmpty()) {
            if (myAttributes == null) {
                myAttributes = ZLStringMap()
            }
            myAttributes!!.put(name, v)
        }
    }

    override fun toString(): String {
        val buf = StringBuilder("[Attributes:\n")
        myAttributes?.let { attrs ->
            for (i in 0 until attrs.getSize()) {
                val key = attrs.getKey(i)
                val value = attrs.getValue(key)
                if (i != 0) {
                    buf.append(",\n")
                }
                buf.append(key).append("=").append(value)
            }
        }
        buf.append("]")
        return buf.toString()
    }
}
