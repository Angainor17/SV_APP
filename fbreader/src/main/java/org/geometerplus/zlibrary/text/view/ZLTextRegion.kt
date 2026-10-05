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

import org.geometerplus.zlibrary.core.view.Hull
import java.util.ArrayList

class ZLTextRegion(
    private val mySoul: Soul,
    private val myAreaList: List<ZLTextElementArea>,
    private val myFromIndex: Int,
) {
    private var myAreas: Array<ZLTextElementArea>? = null
    private var myToIndex: Int = myFromIndex + 1
    private var myHull: Hull? = null
    private var myHull0: Hull? = null

    fun extend() {
        ++myToIndex
        myHull = null
    }

    val soul: Soul
        get() = mySoul

    fun textAreas(): Array<ZLTextElementArea> {
        val cached = myAreas
        if (cached == null || cached.size != myToIndex - myFromIndex) {
            return synchronized(myAreaList) {
                val result = Array(myToIndex - myFromIndex) { i -> myAreaList[i + myFromIndex] }
                myAreas = result
                result
            }
        }
        return cached
    }

    fun hull(): Hull {
        var hull = myHull
        if (hull == null) {
            hull = HullUtil.hull(textAreas())
            myHull = hull
        }
        return hull
    }

    fun hull0(): Hull {
        var hull0 = myHull0
        if (hull0 == null) {
            val column0 = ArrayList<ZLTextElementArea>()
            for (a in textAreas()) {
                if (a.ColumnIndex == 0) {
                    column0.add(a)
                }
            }
            hull0 = HullUtil.hull(column0)
            myHull0 = hull0
        }
        return hull0
    }

    fun getFirstArea(): ZLTextElementArea = textAreas()[0]

    fun getLastArea(): ZLTextElementArea {
        val areas = textAreas()
        return areas[areas.size - 1]
    }

    val left: Int
        get() {
            var left = Int.MAX_VALUE
            for (area in textAreas()) {
                left = minOf(area.XStart, left)
            }
            return left
        }

    val right: Int
        get() {
            var right = Int.MIN_VALUE
            for (area in textAreas()) {
                right = maxOf(area.XEnd, right)
            }
            return right
        }

    val top: Int
        get() = getFirstArea().YStart

    val bottom: Int
        get() = getLastArea().YEnd

    fun distanceTo(x: Int, y: Int): Int = hull().distanceTo(x, y)

    fun isBefore(x: Int, y: Int, columnIndex: Int): Boolean =
        when (columnIndex) {
            0 -> {
                var count0 = 0
                var count1 = 0
                for (area in textAreas()) {
                    if (area.ColumnIndex == 0) {
                        ++count0
                    } else {
                        ++count1
                    }
                }
                when {
                    count0 == 0 -> false
                    count1 == 0 -> hull().isBefore(x, y)
                    else -> hull0().isBefore(x, y)
                }
            }
            1 -> if (textAreas().any { it.ColumnIndex == 0 }) true else hull().isBefore(x, y)
            else -> hull().isBefore(x, y)
        }

    fun isAtRightOf(other: ZLTextRegion?): Boolean =
        other == null || getFirstArea().XStart >= other.getLastArea().XEnd

    fun isAtLeftOf(other: ZLTextRegion?): Boolean = other == null || other.isAtRightOf(this)

    fun isUnder(other: ZLTextRegion?): Boolean =
        other == null || getFirstArea().YStart >= other.getLastArea().YEnd

    fun isOver(other: ZLTextRegion?): Boolean = other == null || other.isUnder(this)

    fun isExactlyUnder(other: ZLTextRegion?): Boolean {
        if (other == null) {
            return true
        }
        if (!isUnder(other)) {
            return false
        }
        val areas0 = textAreas()
        val areas1 = other.textAreas()
        for (i in areas0) {
            for (j in areas1) {
                if (i.XStart <= j.XEnd && j.XStart <= i.XEnd) {
                    return true
                }
            }
        }
        return false
    }

    fun isExactlyOver(other: ZLTextRegion?): Boolean = other == null || other.isExactlyUnder(this)

    fun isVerticallyAligned(): Boolean {
        for (area in textAreas()) {
            if (!area.Style.isVerticallyAligned()) {
                return false
            }
        }
        return true
    }

    interface Filter {
        fun accepts(region: ZLTextRegion): Boolean
    }

    abstract class Soul protected constructor(
        @JvmField val ParagraphIndex: Int,
        @JvmField val StartElementIndex: Int,
        @JvmField val EndElementIndex: Int,
    ) : Comparable<Soul> {
        fun accepts(area: ZLTextElementArea): Boolean = compareTo(area) == 0

        final override fun equals(other: Any?): Boolean {
            if (other === this) {
                return true
            }
            if (other !is Soul) {
                return false
            }
            return ParagraphIndex == other.ParagraphIndex &&
                StartElementIndex == other.StartElementIndex &&
                EndElementIndex == other.EndElementIndex
        }

        final override fun compareTo(soul: Soul): Int {
            if (ParagraphIndex != soul.ParagraphIndex) {
                return if (ParagraphIndex < soul.ParagraphIndex) -1 else 1
            }
            if (EndElementIndex < soul.StartElementIndex) {
                return -1
            }
            if (StartElementIndex > soul.EndElementIndex) {
                return 1
            }
            return 0
        }

        fun compareTo(area: ZLTextElementArea): Int {
            if (ParagraphIndex != area.ParagraphIndex) {
                return if (ParagraphIndex < area.ParagraphIndex) -1 else 1
            }
            if (EndElementIndex < area.ElementIndex) {
                return -1
            }
            if (StartElementIndex > area.ElementIndex) {
                return 1
            }
            return 0
        }

        fun compareTo(position: ZLTextPosition): Int {
            val ppi = position.paragraphIndex
            if (ParagraphIndex != ppi) {
                return if (ParagraphIndex < ppi) -1 else 1
            }
            val pei = position.elementIndex
            if (EndElementIndex < pei) {
                return -1
            }
            if (StartElementIndex > pei) {
                return 1
            }
            return 0
        }
    }

    companion object {
        @JvmField
        val AnyRegionFilter: Filter = object : Filter {
            override fun accepts(region: ZLTextRegion): Boolean = true
        }

        @JvmField
        val HyperlinkFilter: Filter = object : Filter {
            override fun accepts(region: ZLTextRegion): Boolean =
                region.soul is ZLTextHyperlinkRegionSoul
        }

        @JvmField
        val VideoFilter: Filter = object : Filter {
            override fun accepts(region: ZLTextRegion): Boolean =
                region.soul is ZLTextVideoRegionSoul
        }

        @JvmField
        val ExtensionFilter: Filter = object : Filter {
            override fun accepts(region: ZLTextRegion): Boolean =
                region.soul is ExtensionRegionSoul
        }

        @JvmField
        val ImageOrHyperlinkFilter: Filter = object : Filter {
            override fun accepts(region: ZLTextRegion): Boolean {
                val soul = region.soul
                return soul is ZLTextImageRegionSoul || soul is ZLTextHyperlinkRegionSoul
            }
        }
    }
}
