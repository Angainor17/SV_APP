package org.geometerplus.zlibrary.text.view

import org.geometerplus.zlibrary.core.view.ZLViewEnums.Direction
import java.util.Collections

class ZLTextElementAreaVector {
    private val areas: MutableList<ZLTextElementArea> = Collections.synchronizedList(mutableListOf())
    private val elementRegions: MutableList<ZLTextRegion> = mutableListOf()
    private var currentElementRegion: ZLTextRegion? = null

    fun clear() {
        synchronized(areas) {
            elementRegions.clear()
            currentElementRegion = null
            areas.clear()
        }
    }

    fun size(): Int = areas.size

    fun areas(): List<ZLTextElementArea> {
        synchronized(areas) {
            return ArrayList(areas)
        }
    }

    fun getFirstArea(): ZLTextElementArea? {
        synchronized(areas) {
            return if (areas.isEmpty()) null else areas[0]
        }
    }

    fun getLastArea(): ZLTextElementArea? {
        synchronized(areas) {
            return if (areas.isEmpty()) null else areas[areas.size - 1]
        }
    }

    fun add(area: ZLTextElementArea): Boolean {
        synchronized(areas) {
            if (currentElementRegion != null && currentElementRegion!!.soul.accepts(area)) {
                currentElementRegion!!.extend()
            } else {
                var soul: ZLTextRegion.Soul? = null
                val hyperlink = area.style.hyperlink
                if (hyperlink?.id != null) {
                    soul = ZLTextHyperlinkRegionSoul(area, hyperlink)
                } else if (area.element is ZLTextImageElement) {
                    soul = ZLTextImageRegionSoul(area, area.element as ZLTextImageElement)
                } else if (area.element is ZLTextVideoElement) {
                    soul = ZLTextVideoRegionSoul(area, area.element as ZLTextVideoElement)
                } else if (area.element is ZLTextWord && !(area.element as ZLTextWord).isASpace()) {
                    soul = ZLTextWordRegionSoul(area, area.element as ZLTextWord)
                } else if (area.element is ExtensionElement) {
                    soul = ExtensionRegionSoul(area, area.element as ExtensionElement)
                }
                if (soul != null) {
                    currentElementRegion = ZLTextRegion(soul, areas, areas.size)
                    elementRegions.add(currentElementRegion!!)
                } else {
                    currentElementRegion = null
                }
            }
            return areas.add(area)
        }
    }

    fun getFirstAfter(position: ZLTextPosition?): ZLTextElementArea? {
        if (position == null) {
            return null
        }
        synchronized(areas) {
            for (area in areas) {
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
        synchronized(areas) {
            for (i in areas.size - 1 downTo 0) {
                val area = areas[i]
                if (position.compareTo(area) > 0) {
                    return area
                }
            }
        }
        return null
    }

    fun binarySearch(x: Int, y: Int): ZLTextElementArea? {
        synchronized(areas) {
            var left = 0
            var right = areas.size
            while (left < right) {
                val middle = (left + right) / 2
                val candidate = areas[middle]
                when {
                    candidate.yStart > y -> right = middle
                    candidate.yEnd < y -> left = middle + 1
                    candidate.xStart > x -> right = middle
                    candidate.xEnd < x -> left = middle + 1
                    else -> return candidate
                }
            }
            return null
        }
    }

    fun getRegion(soul: ZLTextRegion.Soul?): ZLTextRegion? {
        if (soul == null) {
            return null
        }
        synchronized(areas) {
            for (region in elementRegions) {
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
        synchronized(areas) {
            for (region in elementRegions) {
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

    fun findRegionsPair(x: Int, y: Int, columnIndex: Int, filter: ZLTextRegion.Filter): RegionPair {
        val pair = RegionPair()
        synchronized(areas) {
            for (region in elementRegions) {
                if (filter.accepts(region)) {
                    if (region.isBefore(x, y, columnIndex)) {
                        pair.before = region
                    } else {
                        pair.after = region
                        break
                    }
                }
            }
        }
        return pair
    }

    fun nextRegion(currentRegion: ZLTextRegion?, direction: Direction, filter: ZLTextRegion.Filter): ZLTextRegion? {
        synchronized(areas) {
            if (elementRegions.isEmpty()) {
                return null
            }

            var index = currentRegion?.let { elementRegions.indexOf(it) } ?: -1

            when (direction) {
                Direction.rightToLeft, Direction.up -> {
                    if (index == -1) {
                        index = elementRegions.size - 1
                    } else if (index == 0) {
                        return null
                    } else {
                        --index
                    }
                }
                Direction.leftToRight, Direction.down -> {
                    if (index == elementRegions.size - 1) {
                        return null
                    } else {
                        ++index
                    }
                }
            }

            when (direction) {
                Direction.rightToLeft -> {
                    for (i in index downTo 0) {
                        val candidate = elementRegions[i]
                        if (filter.accepts(candidate) && candidate.isAtLeftOf(currentRegion)) {
                            return candidate
                        }
                    }
                }
                Direction.leftToRight -> {
                    for (i in index until elementRegions.size) {
                        val candidate = elementRegions[i]
                        if (filter.accepts(candidate) && candidate.isAtRightOf(currentRegion)) {
                            return candidate
                        }
                    }
                }
                Direction.down -> {
                    var firstCandidate: ZLTextRegion? = null
                    for (i in index until elementRegions.size) {
                        val candidate = elementRegions[i]
                        if (!filter.accepts(candidate)) {
                            continue
                        }
                        if (candidate.isExactlyUnder(currentRegion)) {
                            return candidate
                        }
                        if (firstCandidate == null && candidate.isUnder(currentRegion)) {
                            firstCandidate = candidate
                        }
                    }
                    if (firstCandidate != null) {
                        return firstCandidate
                    }
                }
                Direction.up -> {
                    var firstCandidate: ZLTextRegion? = null
                    for (i in index downTo 0) {
                        val candidate = elementRegions[i]
                        if (!filter.accepts(candidate)) {
                            continue
                        }
                        if (candidate.isExactlyOver(currentRegion)) {
                            return candidate
                        }
                        if (firstCandidate == null && candidate.isOver(currentRegion)) {
                            firstCandidate = candidate
                        }
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
            var e = areas[i]
            if (e.element is ZLTextWord) {
                val b = ZLTextView.isRtl(rtlMode, e.element.toString())
                if (rtlMode != b) {
                    var rangeStart = i
                    var rangeEnd = i
                    while (rangeEnd < end && b == ZLTextView.isRtl(rtlMode, areas[rangeEnd].element.toString())) {
                        rangeEnd++
                    }
                    val rangeLast = rangeEnd - 1
                    e = areas[if (rtlMode) rangeLast else rangeStart]
                    var xStart = e.xStart
                    var prev: ZLTextElementArea? = null
                    val step = if (rtlMode) 1 else -1
                    var k = if (rtlMode) rangeStart else rangeLast
                    while (if (rtlMode) k <= rangeLast else k >= rangeStart) {
                        e = areas[k]
                        if (prev != null) {
                            detected = true
                            xStart += prev.xStart - e.xEnd
                        }
                        prev = e
                        val xWidth = e.xEnd - e.xStart
                        val xEnd = xStart + xWidth
                        e = ZLTextElementArea(
                            e.paragraphIndex, e.elementIndex, e.charIndex,
                            e.length, e.isLastInElement(), e.addHyphenationSign,
                            e.changeStyle, e.style, e.element,
                            xStart, xEnd, e.yStart, e.yEnd, e.columnIndex
                        )
                        areas[k] = e
                        xStart = xEnd
                        k += step
                    }
                    i = rangeLast
                }
            }
            i++
        }
        return detected
    }

    class RegionPair {
        var before: ZLTextRegion? = null
        var after: ZLTextRegion? = null
    }
}
