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

package org.geometerplus.zlibrary.text.view

import android.graphics.Rect

import org.geometerplus.zlibrary.core.view.HorizontalConvexHull
import org.geometerplus.zlibrary.core.view.Hull
import org.geometerplus.zlibrary.core.view.UnionHull

object HullUtil {
    fun hull(areas: Array<ZLTextElementArea>): Hull = hull(areas.toList())

    fun hull(areas: List<ZLTextElementArea>): Hull {
        val rectangles0 = mutableListOf<Rect>()
        val rectangles1 = mutableListOf<Rect>()
        for (a in areas) {
            val rect = Rect(a.xStart, a.yStart, a.xEnd, a.yEnd)
            if (a.columnIndex == 0) {
                rectangles0.add(rect)
            } else {
                rectangles1.add(rect)
            }
        }
        return when {
            rectangles0.isEmpty() -> HorizontalConvexHull(rectangles1)
            rectangles1.isEmpty() -> HorizontalConvexHull(rectangles0)
            else -> UnionHull(
                HorizontalConvexHull(rectangles0),
                HorizontalConvexHull(rectangles1)
            )
        }
    }
}
