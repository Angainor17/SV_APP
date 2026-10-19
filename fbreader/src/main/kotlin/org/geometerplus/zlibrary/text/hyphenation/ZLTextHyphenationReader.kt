/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.zlibrary.text.hyphenation

import org.geometerplus.zlibrary.core.util.ZLArrayUtils
import org.geometerplus.zlibrary.core.xml.ZLStringMap
import org.geometerplus.zlibrary.core.xml.ZLXMLReaderAdapter

internal class ZLTextHyphenationReader(
    private val myHyphenator: ZLTextTeXHyphenator,
) : ZLXMLReaderAdapter() {

    private var myReadPattern: Boolean = false
    private var myBuffer: CharArray = CharArray(10)
    private var myBufferLength: Int = 0

    override fun startElementHandler(tag: String, attributes: ZLStringMap): Boolean {
        if (PATTERN == tag) {
            myReadPattern = true
        }
        return false
    }

    override fun endElementHandler(tag: String): Boolean {
        if (PATTERN == tag) {
            myReadPattern = false
            val len = myBufferLength
            if (len != 0) {
                myHyphenator.addPattern(ZLTextTeXHyphenationPattern(myBuffer, 0, len, true))
            }
            myBufferLength = 0
        }
        return false
    }

    override fun characterDataHandler(ch: CharArray, start: Int, length: Int) {
        if (myReadPattern) {
            var buffer = myBuffer
            val oldLen = myBufferLength
            val newLen = oldLen + length
            if (newLen > buffer.size) {
                buffer = ZLArrayUtils.createCopy(buffer, oldLen, newLen + 10)
                myBuffer = buffer
            }
            System.arraycopy(ch, start, buffer, oldLen, length)
            myBufferLength = newLen
        }
    }

    companion object {
        private const val PATTERN = "pattern"
    }
}
