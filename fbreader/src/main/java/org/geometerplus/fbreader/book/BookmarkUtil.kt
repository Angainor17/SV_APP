/*
 * Copyright (C) 2009-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.fbreader.book

import org.geometerplus.zlibrary.core.resources.ZLResource
import org.geometerplus.zlibrary.text.view.ZLTextView
import org.geometerplus.zlibrary.text.view.ZLTextWord
import org.geometerplus.zlibrary.text.view.ZLTextWordCursor

object BookmarkUtil {

    @JvmStatic
    fun getStyleName(style: HighlightingStyle): String {
        val name = style.getNameOrNull()
        return if (name != null && name.isNotEmpty()) name else defaultName(style)
    }

    @JvmStatic
    fun setStyleName(style: HighlightingStyle, name: String) {
        style.setName(if (defaultName(style) == name) null else name)
    }

    private fun defaultName(style: HighlightingStyle): String =
        ZLResource.resource("style").value.replace("%s", style.id.toString())

    @JvmStatic
    fun findEnd(bookmark: Bookmark, view: ZLTextView) {
        if (bookmark.getEnd() != null) {
            return
        }
        var cursor = view.getStartCursor()
        if (cursor.isNull) {
            cursor = view.getEndCursor()
        }
        if (cursor.isNull) {
            return
        }
        cursor = ZLTextWordCursor(cursor)
        cursor.moveTo(bookmark)

        var word: ZLTextWord? = null
        var count = bookmark.length
        mainLoop@ while (count > 0) {
            cursor.nextWord()
            while (cursor.isEndOfParagraph) {
                if (!cursor.nextParagraph()) {
                    break@mainLoop
                }
            }
            val element = cursor.getElement()
            if (element is ZLTextWord) {
                if (word != null) {
                    --count
                }
                word = element
                count -= word.length
            }
        }
        word?.let {
            bookmark.setEnd(cursor.paragraphIndex, cursor.elementIndex, it.length)
        }
    }
}
