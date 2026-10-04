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

class ZLTextLineInfo(
    @JvmField val ParagraphCursor: ZLTextParagraphCursor,
    elementIndex: Int,
    charIndex: Int,
    @JvmField var StartStyle: ZLTextStyle,
) {
    @JvmField val ParagraphCursorLength: Int = ParagraphCursor.getParagraphLength()

    @JvmField val StartElementIndex: Int = elementIndex
    @JvmField val StartCharIndex: Int = charIndex
    @JvmField var RealStartElementIndex: Int = elementIndex
    @JvmField var RealStartCharIndex: Int = charIndex
    @JvmField var EndElementIndex: Int = elementIndex
    @JvmField var EndCharIndex: Int = charIndex

    @JvmField var IsVisible: Boolean = false
    @JvmField var LeftIndent: Int = 0
    @JvmField var Width: Int = 0
    @JvmField var Height: Int = 0
    @JvmField var Descent: Int = 0
    @JvmField var VSpaceBefore: Int = 0
    @JvmField var VSpaceAfter: Int = 0
    @JvmField var PreviousInfoUsed: Boolean = false
    @JvmField var SpaceCounter: Int = 0

    fun isEndOfParagraph(): Boolean = EndElementIndex == ParagraphCursorLength

    fun adjust(previous: ZLTextLineInfo?) {
        if (!PreviousInfoUsed && previous != null) {
            Height -= minOf(previous.VSpaceAfter, VSpaceBefore)
            PreviousInfoUsed = true
        }
    }

    override fun equals(other: Any?): Boolean {
        val info = other as ZLTextLineInfo
        return (ParagraphCursor === info.ParagraphCursor) &&
                (StartElementIndex == info.StartElementIndex) &&
                (StartCharIndex == info.StartCharIndex)
    }

    override fun hashCode(): Int =
        ParagraphCursor.hashCode() + StartElementIndex + 239 * StartCharIndex
}
