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

import org.geometerplus.zlibrary.core.view.ZLViewEnums
import java.util.ArrayList
import java.util.Collections

class ZLTextElementAreaVector {
    private val myAreas: MutableList<ZLTextElementArea> =
        Collections.synchronizedList(ArrayList<ZLTextElementArea>())
    private val myElementRegions = ArrayList<ZLTextRegion>()
    private var myCurrentElementRegion: ZLTextRegion? = null

    fun clear() {
        synchronized(myAreas) {
            myElementRegions.clear()
            myCurrentElementRegion = null
            myAreas.clear()
        }
    }

    fun size(): Int = myAreas.size

    fun areas(): List<ZLTextElementArea> =
        synchronized(myAreas) {
            ArrayList(myAreas)
        }

    fun getFirstArea(): ZLTextElementArea? =
        synchronized(myAreas) {
            if (myAreas.isEmpty()) null else myAreas[0]
        }

    fun getLastArea(): ZLTextElementArea? =
        synchronized(myAreas) {
            if (myAreas.isEmpty()) null else myAreas[myAreas.size - 1]
        }

    fun add(area: ZLTextElementArea): Boolean =
        synchronized(myAreas) {
            val current = myCurrentElementRegion
            if (current != null && current.soul.accepts(area)) {
                current.extend()
            } else {
                var soul: ZLTextRegion.Soul? = null
                val element = area.Element
                val hyperlink = area.Style.Hyperlink
                if (hyperlink?.Id != null) {
                    soul = ZLTextHyperlinkRegionSoul(area, hyperlink)
                } else if (element is ZLTextImageElement) {
                    soul = ZLTextImageRegionSoul(area, element)
                } else if (element is ZLTextVideoElement) {
                    soul = ZLTextVideoRegionSoul(area, element)
                } else if (element is ZLTextWord && !element.isASpace()) {
                    soul = ZLTextWordRegionSoul(area, element)
                } else if (element is ExtensionElement) {
                    soul = ExtensionRegionSoul(area, element)
                }
                if (soul != null) {
                    myCurrentElementRegion = ZLTextRegion(soul, myAreas, myAreas.size)
                    myElementRegions.add(myCurrentElementRegion!!)
                } else {
                    myCurrentElementRegion = null
                }
            }
            myAreas.add(area)
        }

    fun getFirstAfter(position: ZLTextPosition?): ZLTextElementArea? {
        if (position == null) {
            return null
        }
        synchronized(myAreas) {
            for (area in myAreas) {
                if (position.compareTo(area) <= 0) {
                    return area
                }
            }
        }
        return null
    }

    fun getLastBefore(position: ZLTextPosition?): ZLTextElementArea? {
        if (position == null) {
            return null
        }
        synchronized(myAreas) {
            for (i in myAreas.size - 1 downTo 0) {
                val area = myAreas[i]
                if (position.compareTo(area) > 0) {
                    return area
                }
            }
        }
        return null
    }

    fun binarySearch(x: Int, y: Int): ZLTextElementArea? {
        synchronized(myAreas) {
            var left = 0
            var right = myAreas.size
            while (left < right) {
                val middle = (left + right) / 2
                val candidate = myAreas[middle]
                if (candidate.YStart > y) {
                    right = middle
                } else if (candidate.YEnd < y) {
                    left = middle + 1
                } else if (candidate.XStart > x) {
                    right = middle
                } else if (candidate.XEnd < x) {
                    left = middle + 1
                } else {
                    return candidate
                }
            }
            return null
        }
    }

    fun getRegion(soul: ZLTextRegion.Soul?): ZLTextRegion? {
        if (soul == null) {
            return null
        }
        synchronized(myAreas) {
            for (region in myElementRegions) {
                if (soul == region.soul) {
                    return region
                }
            }
        }
        return null
    }

    fun findRegion(x: Int, y: Int, maxDistance: Int, filter: ZLTextRegion.Filter): ZLTextRegion? {
        var bestRegion: ZLTextRegion? = null
        var distance = maxDistance + 1
        synchronized(myAreas) {
            for (region in myElementRegions) {
                if (filter.accepts(region)) {
                    val d = region.distanceTo(x, y)
                    if (d < distance) {
                        bestRegion = region
                        distance = d
                    }
                }
            }
        }
        return bestRegion
    }

    fun findRegionsPair(
        x: Int,
        y: Int,
        columnIndex: Int,
        filter: ZLTextRegion.Filter,
    ): RegionPair {
        val pair = RegionPair()
        synchronized(myAreas) {
            for (region in myElementRegions) {
                if (filter.accepts(region)) {
                    if (region.isBefore(x, y, columnIndex)) {
                        pair.Before = region
                    } else {
                        pair.After = region
                        break
                    }
                }
            }
        }
        return pair
    }

    protected fun nextRegion(
        currentRegion: ZLTextRegion?,
        direction: ZLViewEnums.Direction,
        filter: ZLTextRegion.Filter,
    ): ZLTextRegion? {
        synchronized(myAreas) {
            if (myElementRegions.isEmpty()) {
                return null
            }

            var index = if (currentRegion != null) myElementRegions.indexOf(currentRegion) else -1

            when (direction) {
                ZLViewEnums.Direction.rightToLeft, ZLViewEnums.Direction.up -> {
                    if (index == -1) {
                        index = myElementRegions.size - 1
                    } else if (index == 0) {
                        return null
                    } else {
                        --index
                    }
                }
                ZLViewEnums.Direction.leftToRight, ZLViewEnums.Direction.down -> {
                    if (index == myElementRegions.size - 1) {
                        return null
                    } else {
                        ++index
                    }
                }
            }

            when (direction) {
                ZLViewEnums.Direction.rightToLeft -> {
                    while (index >= 0) {
                        val candidate = myElementRegions[index]
                        if (filter.accepts(candidate) && candidate.isAtLeftOf(currentRegion)) {
                            return candidate
                        }
                        --index
                    }
                }
                ZLViewEnums.Direction.leftToRight -> {
                    while (index < myElementRegions.size) {
                        val candidate = myElementRegions[index]
                        if (filter.accepts(candidate) && candidate.isAtRightOf(currentRegion)) {
                            return candidate
                        }
                        ++index
                    }
                }
                ZLViewEnums.Direction.down -> {
                    var firstCandidate: ZLTextRegion? = null
                    while (index < myElementRegions.size) {
                        val candidate = myElementRegions[index]
                        if (!filter.accepts(candidate)) {
                            ++index
                            continue
                        }
                        if (candidate.isExactlyUnder(currentRegion)) {
                            return candidate
                        }
                        if (firstCandidate == null && candidate.isUnder(currentRegion)) {
                            firstCandidate = candidate
                        }
                        ++index
                    }
                    if (firstCandidate != null) {
                        return firstCandidate
                    }
                }
                ZLViewEnums.Direction.up -> {
                    var firstCandidate: ZLTextRegion? = null
                    while (index >= 0) {
                        val candidate = myElementRegions[index]
                        if (!filter.accepts(candidate)) {
                            --index
                            continue
                        }
                        if (candidate.isExactlyOver(currentRegion)) {
                            return candidate
                        }
                        if (firstCandidate == null && candidate.isOver(currentRegion)) {
                            firstCandidate = candidate
                        }
                        --index
                    }
                    if (firstCandidate != null) {
                        return firstCandidate
                    }
                }
            }
        }
        return null
    }

    fun swapRtl(rtlMode: Boolean, first: Int, end: Int): Boolean {
        var detected = false
        var i = first
        while (i < end) {
            var e = myAreas[i]
            if (e.Element is ZLTextWord) {
                val b = ZLTextView.isRtl(rtlMode, e.Element.toString())
                if (rtlMode != b) {
                    val rangeStart = i
                    var rangeEnd = i
                    while (rangeEnd < end && b == ZLTextView.isRtl(rtlMode, myAreas[rangeEnd].Element.toString())) {
                        rangeEnd++
                    }
                    val rangeLast = rangeEnd - 1
                    e = myAreas[if (rtlMode) rangeLast else rangeStart]
                    var xStart = e.XStart
                    var prev: ZLTextElementArea? = null
                    var k = if (rtlMode) rangeStart else rangeLast
                    while (if (rtlMode) k <= rangeLast else k >= rangeStart) {
                        e = myAreas[k]
                        if (prev != null) {
                            detected = true
                            xStart += prev.XStart - e.XEnd
                        }
                        prev = e
                        val xWidth = e.XEnd - e.XStart
                        val xEnd = xStart + xWidth
                        e = ZLTextElementArea(
                            e.ParagraphIndex, e.ElementIndex, e.CharIndex, e.Length,
                            e.isLastInElement(), e.AddHyphenationSign, e.ChangeStyle,
                            e.Style, e.Element, xStart, xEnd, e.YStart, e.YEnd, e.ColumnIndex,
                        )
                        myAreas[k] = e
                        xStart = xEnd
                        if (rtlMode) k++ else k--
                    }
                    i = rangeLast
                }
            }
            i++
        }
        return detected
    }

    class RegionPair {
        @JvmField
        var Before: ZLTextRegion? = null

        @JvmField
        var After: ZLTextRegion? = null
    }
}
