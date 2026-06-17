package org.geometerplus.zlibrary.text.view

import org.geometerplus.zlibrary.core.util.ZLColor
import org.geometerplus.zlibrary.core.view.Hull

abstract class ZLTextHighlighting : Comparable<ZLTextHighlighting> {

    abstract fun isEmpty(): Boolean
    abstract fun getStartPosition(): ZLTextPosition
    abstract fun getEndPosition(): ZLTextPosition
    abstract fun getStartArea(page: ZLTextPage): ZLTextElementArea?
    abstract fun getEndArea(page: ZLTextPage): ZLTextElementArea?
    abstract fun getForegroundColor(): ZLColor?
    abstract fun getBackgroundColor(): ZLColor?
    abstract fun getOutlineColor(): ZLColor?

    fun intersects(page: ZLTextPage): Boolean {
        return !isEmpty() &&
                !page.startCursor.isNull && !page.endCursor.isNull &&
                page.startCursor.compareTo(getEndPosition()) < 0 &&
                page.endCursor.compareTo(getStartPosition()) > 0
    }

    fun intersects(region: ZLTextRegion): Boolean {
        val soul = region.soul
        return !isEmpty() &&
                soul.compareTo(getStartPosition()) >= 0 &&
                soul.compareTo(getEndPosition()) <= 0
    }

    internal fun hull(page: ZLTextPage): Hull {
        val startPosition = getStartPosition()
        val endPosition = getEndPosition()
        val areas = page.textElementMap.areas()
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
