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

package org.geometerplus.zlibrary.text.view

class ZLTextElementArea(
    paragraphIndex: Int,
    elementIndex: Int,
    charIndex: Int,
    @JvmField val Length: Int,
    lastInElement: Boolean,
    @JvmField val AddHyphenationSign: Boolean,
    @JvmField val ChangeStyle: Boolean,
    @JvmField val Style: ZLTextStyle,
    @JvmField val Element: ZLTextElement,
    @JvmField val XStart: Int,
    @JvmField val XEnd: Int,
    @JvmField val YStart: Int,
    @JvmField val YEnd: Int,
    @JvmField val ColumnIndex: Int,
) : ZLTextFixedPosition(paragraphIndex, elementIndex, charIndex) {

    private val myIsLastInElement: Boolean = lastInElement

    fun contains(x: Int, y: Int): Boolean =
        (y >= YStart) && (y <= YEnd) && (x >= XStart) && (x <= XEnd)

    fun isFirstInElement(): Boolean = CharIndex == 0

    fun isLastInElement(): Boolean = myIsLastInElement
}
