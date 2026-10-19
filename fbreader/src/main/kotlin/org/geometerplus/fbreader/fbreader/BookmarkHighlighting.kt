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

package org.geometerplus.fbreader.fbreader

import org.geometerplus.fbreader.book.Bookmark
import org.geometerplus.fbreader.book.IBookCollection
import org.geometerplus.zlibrary.core.util.ZLColor
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition
import org.geometerplus.zlibrary.text.view.ZLTextPosition
import org.geometerplus.zlibrary.text.view.ZLTextSimpleHighlighting
import org.geometerplus.zlibrary.text.view.ZLTextView

class BookmarkHighlighting internal constructor(
    view: ZLTextView,
    collection: IBookCollection<*>,
    bookmark: Bookmark
) : ZLTextSimpleHighlighting(view, startPosition(bookmark), endPosition(bookmark)) {
    @JvmField
    val Collection: IBookCollection<*> = collection

    @JvmField
    val Bookmark: Bookmark = bookmark

    override fun getBackgroundColor(): ZLColor? =
        Collection.getHighlightingStyle(Bookmark.getStyleId())?.getBackgroundColor()

    override fun getForegroundColor(): ZLColor? =
        Collection.getHighlightingStyle(Bookmark.getStyleId())?.getForegroundColor()

    override fun getOutlineColor(): ZLColor? = null

    private companion object {
        private fun startPosition(bookmark: Bookmark): ZLTextPosition =
            ZLTextFixedPosition(bookmark.paragraphIndex, bookmark.elementIndex, 0)

        private fun endPosition(bookmark: Bookmark): ZLTextPosition {
            val end: ZLTextPosition? = bookmark.getEnd()
            return end ?: bookmark
        }
    }
}
