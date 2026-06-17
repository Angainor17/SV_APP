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

class ZLTextRegion internal constructor(
    val soul: Soul,
    private val areaList: List<ZLTextElementArea>,
    private val fromIndex: Int
) {
    private var areas: Array<ZLTextElementArea>? = null
    private var toIndex: Int = fromIndex + 1
    private var hull: Hull? = null
    private var hull0: Hull? = null

    fun extend() {
        ++toIndex
        hull = null
    }

    fun textAreas(): Array<ZLTextElementArea> {
        if (areas == null || areas!!.size != toIndex - fromIndex) {
            synchronized(areaList) {
                areas = Array(toIndex - fromIndex) { i ->
                    areaList[i + fromIndex]
                }
            }
        }
        return areas!!
    }

    fun hull(): Hull {
        if (hull == null) {
            hull = HullUtil.hull(textAreas())
        }
        return hull!!
    }

    fun hull0(): Hull {
        if (hull0 == null) {
            val column0 = mutableListOf<ZLTextElementArea>()
            for (a in textAreas()) {
                if (a.columnIndex == 0) {
                    column0.add(a)
                }
            }
            hull0 = HullUtil.hull(column0)
        }
        return hull0!!
    }

    fun getFirstArea(): ZLTextElementArea = textAreas()[0]

    fun getLastArea(): ZLTextElementArea {
        val areas = textAreas()
        return areas[areas.size - 1]
    }

    fun getLeft(): Int {
        var left = Int.MAX_VALUE
        for (area in textAreas()) {
            left = minOf(area.xStart, left)
        }
        return left
    }

    fun getRight(): Int {
        var right = Int.MIN_VALUE
        for (area in textAreas()) {
            right = maxOf(area.xEnd, right)
        }
        return right
    }

    fun getTop(): Int = getFirstArea().yStart

    fun getBottom(): Int = getLastArea().yEnd

    fun distanceTo(x: Int, y: Int): Int = hull().distanceTo(x, y)

    fun isBefore(x: Int, y: Int, columnIndex: Int): Boolean {
        return when (columnIndex) {
            -1 -> hull().isBefore(x, y)
            0 -> {
                var count0 = 0
                var count1 = 0
                for (area in textAreas()) {
                    if (area.columnIndex == 0) {
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
            1 -> {
                for (area in textAreas()) {
                    if (area.columnIndex == 0) {
                        return true
                    }
                }
                hull().isBefore(x, y)
            }
            else -> hull().isBefore(x, y)
        }
    }

    fun isAtRightOf(other: ZLTextRegion?): Boolean =
        other == null || getFirstArea().xStart >= other.getLastArea().xEnd

    fun isAtLeftOf(other: ZLTextRegion?): Boolean =
        other == null || other.isAtRightOf(this)

    fun isUnder(other: ZLTextRegion?): Boolean =
        other == null || getFirstArea().yStart >= other.getLastArea().yEnd

    fun isOver(other: ZLTextRegion?): Boolean =
        other == null || other.isUnder(this)

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
                if (i.xStart <= j.xEnd && j.xStart <= i.xEnd) {
                    return true
                }
            }
        }
        return false
    }

    fun isExactlyOver(other: ZLTextRegion?): Boolean =
        other == null || other.isExactlyUnder(this)

    fun isVerticallyAligned(): Boolean {
        for (area in textAreas()) {
            if (!area.style.isVerticallyAligned()) {
                return false
            }
        }
        return true
    }

    interface Filter {
        fun accepts(region: ZLTextRegion): Boolean
    }

    abstract class Soul(
        val paragraphIndex: Int,
        val startElementIndex: Int,
        val endElementIndex: Int
    ) : Comparable<Soul> {

        fun accepts(area: ZLTextElementArea): Boolean = compareTo(area) == 0

        override fun equals(other: Any?): Boolean {
            if (other === this) {
                return true
            }
            if (other !is Soul) {
                return false
            }
            return paragraphIndex == other.paragraphIndex &&
                   startElementIndex == other.startElementIndex &&
                   endElementIndex == other.endElementIndex
        }

        override fun compareTo(soul: Soul): Int {
            if (paragraphIndex != soul.paragraphIndex) {
                return if (paragraphIndex < soul.paragraphIndex) -1 else 1
            }
            if (endElementIndex < soul.startElementIndex) {
                return -1
            }
            if (startElementIndex > soul.endElementIndex) {
                return 1
            }
            return 0
        }

        fun compareTo(area: ZLTextElementArea): Int {
            if (paragraphIndex != area.paragraphIndex) {
                return if (paragraphIndex < area.paragraphIndex) -1 else 1
            }
            if (endElementIndex < area.elementIndex) {
                return -1
            }
            if (startElementIndex > area.elementIndex) {
                return 1
            }
            return 0
        }

        fun compareTo(position: ZLTextPosition): Int {
            val ppi = position.paragraphIndex
            if (paragraphIndex != ppi) {
                return if (paragraphIndex < ppi) -1 else 1
            }
            val pei = position.elementIndex
            if (endElementIndex < pei) {
                return -1
            }
            if (startElementIndex > pei) {
                return 1
            }
            return 0
        }
    }

    companion object {
        @JvmField
        val AnyRegionFilter = object : Filter {
            override fun accepts(region: ZLTextRegion): Boolean = true
        }

        @JvmField
        val HyperlinkFilter = object : Filter {
            override fun accepts(region: ZLTextRegion): Boolean = region.soul is ZLTextHyperlinkRegionSoul
        }

        @JvmField
        val VideoFilter = object : Filter {
            override fun accepts(region: ZLTextRegion): Boolean = region.soul is ZLTextVideoRegionSoul
        }

        @JvmField
        val ExtensionFilter = object : Filter {
            override fun accepts(region: ZLTextRegion): Boolean = region.soul is ExtensionRegionSoul
        }

        @JvmField
        val ImageOrHyperlinkFilter = object : Filter {
            override fun accepts(region: ZLTextRegion): Boolean =
                region.soul is ZLTextImageRegionSoul || region.soul is ZLTextHyperlinkRegionSoul
        }
    }
}
