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

import org.geometerplus.zlibrary.core.util.ZLColor
import org.geometerplus.zlibrary.core.view.SelectionCursor

class ZLTextSelection(private val myView: ZLTextView) : ZLTextHighlighting() {
    private val myCursorInMovementPoint = Point(-1, -1)
    private var myLeftMostRegionSoul: ZLTextRegion.Soul? = null
    private var myRightMostRegionSoul: ZLTextRegion.Soul? = null

    private var myCursorInMovement: SelectionCursor.Which? = null
    private var myScroller: Scroller? = null

    override fun isEmpty(): Boolean = myLeftMostRegionSoul == null

    fun clear(): Boolean {
        if (isEmpty()) {
            return false
        }

        stop()
        myLeftMostRegionSoul = null
        myRightMostRegionSoul = null
        myCursorInMovement = null
        return true
    }

    fun setCursorInMovement(which: SelectionCursor.Which, x: Int, y: Int) {
        myCursorInMovement = which
        myCursorInMovementPoint.X = x
        myCursorInMovementPoint.Y = y
    }

    fun getCursorInMovement(): SelectionCursor.Which? = myCursorInMovement

    fun getCursorInMovementPoint(): Point = myCursorInMovementPoint

    fun start(x: Int, y: Int): Boolean {
        clear()

        val region = myView.findRegion(
            x, y, myView.maxSelectionDistance(), ZLTextRegion.AnyRegionFilter,
        )
        if (region == null) {
            return false
        }

        myRightMostRegionSoul = region.soul
        myLeftMostRegionSoul = region.soul
        return true
    }

    fun stop() {
        myCursorInMovement = null
        myScroller?.stop()
        myScroller = null
    }

    fun expandTo(page: ZLTextPage, x: Int, y: Int) {
        if (isEmpty()) {
            return
        }

        val vector = page.TextElementMap
        val firstArea = vector.getFirstArea()
        val lastArea = vector.getLastArea()
        val scroller = myScroller
        if (firstArea != null && y < firstArea.YStart) {
            if (scroller != null && scroller.scrollsForward()) {
                scroller.stop()
                myScroller = null
            }
            if (myScroller == null) {
                myScroller = Scroller(page, false, x, y)
                return
            }
        } else if (lastArea != null && y > lastArea.YEnd) {
            if (scroller != null && !scroller.scrollsForward()) {
                scroller.stop()
                myScroller = null
            }
            if (myScroller == null) {
                myScroller = Scroller(page, true, x, y)
                return
            }
        } else {
            if (scroller != null) {
                scroller.stop()
                myScroller = null
            }
        }

        myScroller?.setXY(x, y)

        var region = myView.findRegion(x, y, myView.maxSelectionDistance(), ZLTextRegion.AnyRegionFilter)
        if (region == null) {
            val pair = myView.findRegionsPair(x, y, ZLTextRegion.AnyRegionFilter)
            val before = pair.Before
            val after = pair.After
            if (before != null || after != null) {
                val base =
                    if (myCursorInMovement == SelectionCursor.Which.Right) myLeftMostRegionSoul else myRightMostRegionSoul
                if (before != null) {
                    region = if (base!!.compareTo(before.soul) <= 0) before else after
                } else {
                    region = if (base!!.compareTo(after!!.soul) >= 0) after else before
                }
            }
        }
        if (region == null) {
            return
        }

        val soul = region.soul
        if (myCursorInMovement == SelectionCursor.Which.Right) {
            if (myLeftMostRegionSoul!!.compareTo(soul) <= 0) {
                myRightMostRegionSoul = soul
            } else {
                myRightMostRegionSoul = myLeftMostRegionSoul
                myLeftMostRegionSoul = soul
                myCursorInMovement = SelectionCursor.Which.Left
            }
        } else {
            if (myRightMostRegionSoul!!.compareTo(soul) >= 0) {
                myLeftMostRegionSoul = soul
            } else {
                myLeftMostRegionSoul = myRightMostRegionSoul
                myRightMostRegionSoul = soul
                myCursorInMovement = SelectionCursor.Which.Right
            }
        }

        if (myCursorInMovement == SelectionCursor.Which.Right) {
            if (hasPartAfterPage(page)) {
                myView.turnPage(true, ZLTextView.ScrollingMode.SCROLL_LINES, 1)
                myView.Application.getViewWidget()!!.reset()
                myView.preparePaintInfo()
            }
        } else {
            if (hasPartBeforePage(page)) {
                myView.turnPage(false, ZLTextView.ScrollingMode.SCROLL_LINES, 1)
                myView.Application.getViewWidget()!!.reset()
                myView.preparePaintInfo()
            }
        }
    }

    override fun getStartPosition(): ZLTextPosition? {
        if (isEmpty()) {
            return null
        }
        val soul = myLeftMostRegionSoul!!
        return ZLTextFixedPosition(soul.ParagraphIndex, soul.StartElementIndex, 0)
    }

    override fun getEndPosition(): ZLTextPosition? {
        if (isEmpty()) {
            return null
        }
        val soul = myRightMostRegionSoul!!
        val cursor = myView.cursor(soul.ParagraphIndex)
        val element = cursor!!.getElement(soul.EndElementIndex)
        return ZLTextFixedPosition(
            soul.ParagraphIndex,
            soul.EndElementIndex,
            if (element is ZLTextWord) element.Length else 0,
        )
    }

    override fun getStartArea(page: ZLTextPage): ZLTextElementArea? {
        if (isEmpty()) {
            return null
        }
        val soul = myLeftMostRegionSoul!!
        val vector = page.TextElementMap
        val region = vector.getRegion(soul)
        if (region != null) {
            return region.getFirstArea()
        }
        val firstArea = vector.getFirstArea()
        if (firstArea != null && soul.compareTo(firstArea) <= 0) {
            return firstArea
        }
        return null
    }

    override fun getEndArea(page: ZLTextPage): ZLTextElementArea? {
        if (isEmpty()) {
            return null
        }
        val soul = myRightMostRegionSoul!!
        val vector = page.TextElementMap
        val region = vector.getRegion(soul)
        if (region != null) {
            return region.getLastArea()
        }
        val lastArea = vector.getLastArea()
        if (lastArea != null && soul.compareTo(lastArea) >= 0) {
            return lastArea
        }
        return null
    }

    fun hasPartBeforePage(page: ZLTextPage): Boolean {
        if (isEmpty()) {
            return false
        }
        val soul = myLeftMostRegionSoul!!
        val firstPageArea = page.TextElementMap.getFirstArea()
        if (firstPageArea == null) {
            return false
        }
        val cmp = soul.compareTo(firstPageArea)
        return cmp < 0 || (cmp == 0 && !firstPageArea.isFirstInElement())
    }

    fun hasPartAfterPage(page: ZLTextPage): Boolean {
        if (isEmpty()) {
            return false
        }
        val soul = myRightMostRegionSoul!!
        val lastPageArea = page.TextElementMap.getLastArea()
        if (lastPageArea == null) {
            return false
        }
        val cmp = soul.compareTo(lastPageArea)
        return cmp > 0 || (cmp == 0 && !lastPageArea.isLastInElement())
    }

    override fun getBackgroundColor(): ZLColor? = myView.getSelectionBackgroundColor()

    override fun getForegroundColor(): ZLColor? = myView.getSelectionForegroundColor()

    override fun getOutlineColor(): ZLColor? = null

    class Point(x: Int, y: Int) {
        @JvmField
        var X: Int = x

        @JvmField
        var Y: Int = y
    }

    private inner class Scroller(
        private val myPage: ZLTextPage,
        private val myScrollForward: Boolean,
        x: Int,
        y: Int,
    ) : Runnable {
        private var myX = 0
        private var myY = 0

        init {
            setXY(x, y)
            myView.Application.addTimerTask(this, 400L)
        }

        fun scrollsForward(): Boolean = myScrollForward

        fun setXY(x: Int, y: Int) {
            myX = x
            myY = y
        }

        override fun run() {
            myView.turnPage(myScrollForward, ZLTextView.ScrollingMode.SCROLL_LINES, 1)
            myView.preparePaintInfo()
            expandTo(myPage, myX, myY)
            myView.Application.getViewWidget()!!.reset()
            myView.Application.getViewWidget()!!.repaint()
        }

        fun stop() {
            myView.Application.removeTimerTask(this)
        }
    }
}
