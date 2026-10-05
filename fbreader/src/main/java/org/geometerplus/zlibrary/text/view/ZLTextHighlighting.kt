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

import org.geometerplus.zlibrary.core.util.ZLColor
import org.geometerplus.zlibrary.core.view.Hull

abstract class ZLTextHighlighting : Comparable<ZLTextHighlighting> {
    abstract fun isEmpty(): Boolean

    abstract fun getStartPosition(): ZLTextPosition

    abstract fun getEndPosition(): ZLTextPosition

    abstract fun getStartArea(page: ZLTextPage): ZLTextElementArea

    abstract fun getEndArea(page: ZLTextPage): ZLTextElementArea

    abstract fun getForegroundColor(): ZLColor?

    abstract fun getBackgroundColor(): ZLColor?

    abstract fun getOutlineColor(): ZLColor?

    fun intersects(page: ZLTextPage): Boolean =
        !isEmpty() &&
            !page.StartCursor.isNull() && !page.EndCursor.isNull() &&
            page.StartCursor.compareTo(getEndPosition()) < 0 &&
            page.EndCursor.compareTo(getStartPosition()) > 0

    fun intersects(region: ZLTextRegion): Boolean {
        val soul = region.soul
        return !isEmpty() &&
            soul.compareTo(getStartPosition()) >= 0 &&
            soul.compareTo(getEndPosition()) <= 0
    }

    fun hull(page: ZLTextPage): Hull {
        val startPosition = getStartPosition()
        val endPosition = getEndPosition()
        val areas = page.TextElementMap.areas()
        var startIndex = 0
        var endIndex = 0
        for (i in areas.indices) {
            val a = areas[i]
            if (i == startIndex && startPosition.compareTo(a) > 0) {
                ++startIndex
            } else if (endPosition.compareTo(a) < 0) {
                break
            }
            ++endIndex
        }
        return HullUtil.hull(areas.subList(startIndex, endIndex))
    }

    override fun compareTo(highlighting: ZLTextHighlighting): Int {
        val cmp = getStartPosition().compareTo(highlighting.getStartPosition())
        return if (cmp != 0) cmp else getEndPosition().compareTo(highlighting.getEndPosition())
    }
}
