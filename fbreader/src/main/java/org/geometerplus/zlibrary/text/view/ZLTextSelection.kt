package org.geometerplus.zlibrary.text.view

import org.geometerplus.zlibrary.core.util.ZLColor
import org.geometerplus.zlibrary.core.view.SelectionCursor

class ZLTextSelection(private val view: ZLTextView) : ZLTextHighlighting() {
    private val cursorInMovementPoint = Point(-1, -1)
    private var leftMostRegionSoul: ZLTextRegion.Soul? = null
    private var rightMostRegionSoul: ZLTextRegion.Soul? = null

    private var cursorInMovement: SelectionCursor.Which? = null
    private var scroller: Scroller? = null

    override fun isEmpty(): Boolean = leftMostRegionSoul == null

    fun clear(): Boolean {
        if (isEmpty()) {
            return false
        }

        stop()
        leftMostRegionSoul = null
        rightMostRegionSoul = null
        cursorInMovement = null
        return true
    }

    fun setCursorInMovement(which: SelectionCursor.Which, x: Int, y: Int) {
        cursorInMovement = which
        cursorInMovementPoint.x = x
        cursorInMovementPoint.y = y
    }

    fun getCursorInMovement(): SelectionCursor.Which? = cursorInMovement

    fun getCursorInMovementPoint(): Point = cursorInMovementPoint

    fun start(x: Int, y: Int): Boolean {
        clear()

        val region = view.findRegion(
            x, y, view.maxSelectionDistance(), ZLTextRegion.AnyRegionFilter
        ) ?: return false

        rightMostRegionSoul = region.soul
        leftMostRegionSoul = region.soul
        return true
    }

    fun stop() {
        cursorInMovement = null
        scroller?.stop()
        scroller = null
    }

    fun expandTo(page: ZLTextPage, x: Int, y: Int) {
        if (isEmpty()) {
            return
        }

        val vector = page.textElementMap
        val firstArea = vector.getFirstArea()
        val lastArea = vector.getLastArea()
        if (firstArea != null && y < firstArea.yStart) {
            if (scroller != null && scroller!!.scrollsForward()) {
                scroller!!.stop()
                scroller = null
            }
            if (scroller == null) {
                scroller = Scroller(page, false, x, y)
                return
            }
        } else if (lastArea != null && y > lastArea.yEnd) {
            if (scroller != null && !scroller!!.scrollsForward()) {
                scroller!!.stop()
                scroller = null
            }
            if (scroller == null) {
                scroller = Scroller(page, true, x, y)
                return
            }
        } else {
            scroller?.stop()
            scroller = null
        }

        scroller?.setXY(x, y)

        var region = view.findRegion(x, y, view.maxSelectionDistance(), ZLTextRegion.AnyRegionFilter)
        if (region == null) {
            val pair = view.findRegionsPair(x, y, ZLTextRegion.AnyRegionFilter)
            if (pair.before != null || pair.after != null) {
                val base = when (cursorInMovement) {
                    SelectionCursor.Which.Right -> leftMostRegionSoul
                    else -> rightMostRegionSoul
                }
                if (pair.before != null) {
                    val beforeRegion = pair.before!!
                    region = if (base!!.compareTo(beforeRegion.soul) <= 0) {
                        pair.before
                    } else {
                        pair.after
                    }
                } else {
                    region = if (base!!.compareTo(pair.after!!.soul) >= 0) {
                        pair.after
                    } else {
                        pair.before
                    }
                }
            }
        }
        if (region == null) {
            return
        }

        val soul = region.soul
        if (cursorInMovement == SelectionCursor.Which.Right) {
            if (leftMostRegionSoul!!.compareTo(soul) <= 0) {
                rightMostRegionSoul = soul
            } else {
                rightMostRegionSoul = leftMostRegionSoul
                leftMostRegionSoul = soul
                cursorInMovement = SelectionCursor.Which.Left
            }
        } else {
            if (rightMostRegionSoul!!.compareTo(soul) >= 0) {
                leftMostRegionSoul = soul
            } else {
                leftMostRegionSoul = rightMostRegionSoul
                rightMostRegionSoul = soul
                cursorInMovement = SelectionCursor.Which.Right
            }
        }

        if (cursorInMovement == SelectionCursor.Which.Right) {
            if (hasPartAfterPage(page)) {
                view.turnPage(true, ZLTextView.ScrollingMode.SCROLL_LINES, 1)
                view.Application.viewWidget.reset()
                view.preparePaintInfo()
            }
        } else {
            if (hasPartBeforePage(page)) {
                view.turnPage(false, ZLTextView.ScrollingMode.SCROLL_LINES, 1)
                view.Application.viewWidget.reset()
                view.preparePaintInfo()
            }
        }
    }

    override fun getStartPosition(): ZLTextPosition {
        if (isEmpty()) {
            return ZLTextFixedPosition(0, 0, 0)
        }
        return ZLTextFixedPosition(
            leftMostRegionSoul!!.paragraphIndex,
            leftMostRegionSoul!!.startElementIndex,
            0
        )
    }

    override fun getEndPosition(): ZLTextPosition {
        if (isEmpty()) {
            return ZLTextFixedPosition(0, 0, 0)
        }
        val cursor = view.cursor(rightMostRegionSoul!!.paragraphIndex)
        val element = cursor.getElement(rightMostRegionSoul!!.endElementIndex)
        return ZLTextFixedPosition(
            rightMostRegionSoul!!.paragraphIndex,
            rightMostRegionSoul!!.endElementIndex,
            if (element is ZLTextWord) element.length else 0
        )
    }

    override fun getStartArea(page: ZLTextPage): ZLTextElementArea? {
        if (isEmpty()) {
            return null
        }
        val vector = page.textElementMap
        val region = vector.getRegion(leftMostRegionSoul)
        if (region != null) {
            return region.getFirstArea()
        }
        val firstArea = vector.getFirstArea()
        if (firstArea != null && leftMostRegionSoul!!.compareTo(firstArea) <= 0) {
            return firstArea
        }
        return null
    }

    override fun getEndArea(page: ZLTextPage): ZLTextElementArea? {
        if (isEmpty()) {
            return null
        }
        val vector = page.textElementMap
        val region = vector.getRegion(rightMostRegionSoul)
        if (region != null) {
            return region.getLastArea()
        }
        val lastArea = vector.getLastArea()
        if (lastArea != null && rightMostRegionSoul!!.compareTo(lastArea) >= 0) {
            return lastArea
        }
        return null
    }

    fun hasPartBeforePage(page: ZLTextPage): Boolean {
        if (isEmpty()) {
            return false
        }
        val firstPageArea = page.textElementMap.getFirstArea() ?: return false
        val cmp = leftMostRegionSoul!!.compareTo(firstPageArea)
        return cmp < 0 || (cmp == 0 && !firstPageArea.isFirstInElement())
    }

    fun hasPartAfterPage(page: ZLTextPage): Boolean {
        if (isEmpty()) {
            return false
        }
        val lastPageArea = page.textElementMap.getLastArea() ?: return false
        val cmp = rightMostRegionSoul!!.compareTo(lastPageArea)
        return cmp > 0 || (cmp == 0 && !lastPageArea.isLastInElement())
    }

    override fun getBackgroundColor(): ZLColor = view.selectionBackgroundColor

    override fun getForegroundColor(): ZLColor = view.selectionForegroundColor

    override fun getOutlineColor(): ZLColor? = null

    class Point(var x: Int, var y: Int)

    private inner class Scroller(
        private val page: ZLTextPage,
        private val scrollForward: Boolean,
        x: Int,
        y: Int
    ) : Runnable {
        private var x: Int = x
        private var y: Int = y

        init {
            setXY(x, y)
            view.Application.addTimerTask(this, 400)
        }

        fun scrollsForward(): Boolean = scrollForward

        fun setXY(x: Int, y: Int) {
            this.x = x
            this.y = y
        }

        override fun run() {
            view.turnPage(scrollForward, ZLTextView.ScrollingMode.SCROLL_LINES, 1)
            view.preparePaintInfo()
            expandTo(page, x, y)
            view.Application.viewWidget.reset()
            view.Application.viewWidget.repaint()
        }

        fun stop() {
            view.Application.removeTimerTask(this)
        }
    }
}
