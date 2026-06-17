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

import org.geometerplus.fbreader.network.HtmlUtil
import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.zlibrary.core.xml.ZLStringMap

class FormattedBuffer(private val library: NetworkLibrary, private var type: Type = Type.Text) {

    private val buffer = StringBuilder()

    enum class Type {
        Text,
        Html,
        XHtml
    }

    fun appendText(text: CharSequence?) {
        if (text != null) {
            buffer.append(text)
        }
    }

    fun appendText(data: CharArray, start: Int, length: Int) {
        buffer.append(data, start, length)
    }

    fun appendStartTag(tag: String, attributes: ZLStringMap) {
        buffer.append("<").append(tag)
        for (i in 0 until attributes.getSize()) {
            val key = attributes.getKey(i)
            val value = attributes.getValue(key)
            buffer.append(" ").append(key).append("=\"")
            if (value != null) {
                buffer.append(value)
            }
            buffer.append("\"")
        }
        buffer.append(">")
    }

    fun appendEndTag(tag: String) {
        buffer.append("</").append(tag).append(">")
    }

    fun reset(type: Type) {
        this.type = type
        reset()
    }

    fun reset() {
        buffer.clear()
    }

    fun getText(): CharSequence {
        val text = buffer.toString()

        return when (type) {
            Type.Html, Type.XHtml -> HtmlUtil.getHtmlText(library, text)
            else -> text
        }
    }

    override fun toString(): String = buffer.toString()
}
