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

import org.geometerplus.zlibrary.core.application.ZLApplication
import org.geometerplus.zlibrary.core.util.RationalNumber
import org.geometerplus.zlibrary.core.util.RationalNumber.Companion.create
import org.geometerplus.zlibrary.core.util.ZLColor
import org.geometerplus.zlibrary.core.view.Hull
import org.geometerplus.zlibrary.core.view.SelectionCursor
import org.geometerplus.zlibrary.core.view.ZLPaintContext
import org.geometerplus.zlibrary.core.view.ZLPaintContext.ColorAdjustingMode
import org.geometerplus.zlibrary.core.view.ZLViewEnums
import org.geometerplus.zlibrary.core.view.ZLViewEnums.PageIndex
import org.geometerplus.zlibrary.text.hyphenation.ZLTextHyphenationInfo
import org.geometerplus.zlibrary.text.hyphenation.ZLTextHyphenator.Companion.Instance
import org.geometerplus.zlibrary.text.model.ZLTextAlignmentType
import org.geometerplus.zlibrary.text.model.ZLTextMark
import org.geometerplus.zlibrary.text.model.ZLTextModel
import org.geometerplus.zlibrary.text.model.ZLTextParagraph
import org.geometerplus.zlibrary.text.view.ZLTextElementAreaVector.RegionPair
import org.geometerplus.zlibrary.text.view.ZLTextRegion.Soul
import java.util.Collections
import java.util.LinkedList
import java.util.TreeSet
import kotlin.concurrent.Volatile
import kotlin.math.max
import kotlin.math.min

abstract class ZLTextView(application: ZLApplication) : ZLTextViewBase(application) {
    val mySelection: ZLTextSelection = ZLTextSelection(this)
    private val myLineInfoCache = HashMap<ZLTextLineInfo?, ZLTextLineInfo?>()
    private val myHighlightings: MutableSet<ZLTextHighlighting> =
        Collections.synchronizedSet(
            TreeSet<ZLTextHighlighting>()
        )
    private val myLettersBuffer = CharArray(512)
    var myCurrentPage: ZLTextPage = ZLTextPage()
    var rtlDetected: Boolean = false
    var rtlMode: Boolean = false
    private var myModel: ZLTextModel? = null
    private var myScrollingMode = 0
    private var myOverlappingValue = 0
    private var myPreviousPage = ZLTextPage()
    private var myNextPage = ZLTextPage()
    private var myOutlinedRegionSoul: Soul? = null
    private var myShowOutline = true
    private var myCursorManager: CursorManager? = null
    private var myLettersBufferLength = 0
    private var myLettersModel: ZLTextModel? = null
    private var myCharWidth = -1f

    @Volatile
    private var myCachedWord: ZLTextWord? = null

    @Volatile
    private var myCachedInfo: ZLTextHyphenationInfo? = null

    /**
     * Индекс марки, к которой курсор перешёл при последнем search()/findNext()/findPrevious(),
     * среди всех найденных marks (0-based), либо -1 если ещё не определён.
     */
    var searchMarkIndex: Int = -1
        private set

    @set:Synchronized
    open var model: ZLTextModel?
        get() = myModel
        set(model) {
            myCursorManager =
                if (model != null) CursorManager(model, this.extensionManager) else null

            mySelection.clear()
            myHighlightings.clear()

            myModel = model
            myCurrentPage.reset()
            myPreviousPage.reset()
            myNextPage.reset()
            if (myModel != null) {
                val paragraphsNumber = myModel!!.getParagraphsNumber()
                if (paragraphsNumber > 0) {
                    myCurrentPage.moveStartCursor(myCursorManager!!.get(0)!!)
                }
                val ln = myModel!!.getLanguage()
                if (ln != null) rtlMode =
                    ln.startsWith("ar") || ln.startsWith("ps") || ln.startsWith("fa") || ln.startsWith(
                        "iw"
                    ) || ln.startsWith("he")
                rtlDetected = false
            }
            Application.getViewWidget()!!.reset()
        }

    val startCursor: ZLTextWordCursor
        get() {
            if (myCurrentPage.StartCursor.isNull()) {
                preparePaintInfo(myCurrentPage)
            }
            return myCurrentPage.StartCursor
        }

    val endCursor: ZLTextWordCursor
        get() {
            if (myCurrentPage.EndCursor.isNull()) {
                preparePaintInfo(myCurrentPage)
            }
            return myCurrentPage.EndCursor
        }

    @Synchronized
    private fun gotoMark(mark: ZLTextMark?) {
        if (mark == null) {
            return
        }

        myPreviousPage.reset()
        myNextPage.reset()
        var doRepaint = false
        if (myCurrentPage.StartCursor.isNull()) {
            doRepaint = true
            preparePaintInfo(myCurrentPage)
        }
        if (myCurrentPage.StartCursor.isNull()) {
            return
        }
        if (myCurrentPage.StartCursor.paragraphIndex != mark.paragraphIndex ||
            myCurrentPage.StartCursor.getMark()!!.compareTo(mark) > 0
        ) {
            doRepaint = true
            gotoPosition(mark.paragraphIndex, 0, 0)
            preparePaintInfo(myCurrentPage)
        }
        if (myCurrentPage.EndCursor.isNull()) {
            preparePaintInfo(myCurrentPage)
        }
        while (mark.compareTo(myCurrentPage.EndCursor.getMark()!!) > 0) {
            doRepaint = true
            turnPage(true, ScrollingMode.NO_OVERLAPPING, 0)
            preparePaintInfo(myCurrentPage)
        }
        if (doRepaint) {
            if (myCurrentPage.StartCursor.isNull()) {
                preparePaintInfo(myCurrentPage)
            }
            Application.getViewWidget()!!.reset()
            Application.getViewWidget()!!.repaint()
        }
    }

    @Synchronized
    fun gotoHighlighting(highlighting: ZLTextHighlighting) {
        myPreviousPage.reset()
        myNextPage.reset()
        var doRepaint = false
        if (myCurrentPage.StartCursor.isNull()) {
            doRepaint = true
            preparePaintInfo(myCurrentPage)
        }
        if (myCurrentPage.StartCursor.isNull()) {
            return
        }
        if (!highlighting.intersects(myCurrentPage)) {
            gotoPosition(highlighting.getStartPosition()!!.paragraphIndex, 0, 0)
            preparePaintInfo(myCurrentPage)
        }
        if (myCurrentPage.EndCursor.isNull()) {
            preparePaintInfo(myCurrentPage)
        }
        while (!highlighting.intersects(myCurrentPage)) {
            doRepaint = true
            turnPage(true, ScrollingMode.NO_OVERLAPPING, 0)
            preparePaintInfo(myCurrentPage)
        }
        if (doRepaint) {
            if (myCurrentPage.StartCursor.isNull()) {
                preparePaintInfo(myCurrentPage)
            }
            Application.getViewWidget()!!.reset()
            Application.getViewWidget()!!.repaint()
        }
    }

    @Synchronized
    fun search(
        text: String,
        ignoreCase: Boolean,
        wholeText: Boolean,
        backward: Boolean,
        thisSectionOnly: Boolean
    ): Int {
        if (myModel == null || text.length == 0) {
            return 0
        }
        val startIndex = 0
        val endIndex = myModel!!.getParagraphsNumber()
        if (thisSectionOnly) {
            // TODO: implement
        }
        val count = myModel!!.search(text, startIndex, endIndex, ignoreCase)
        myPreviousPage.reset()
        myNextPage.reset()
        this.searchMarkIndex = -1
        if (!myCurrentPage.StartCursor.isNull()) {
            rebuildPaintInfo()
            if (count > 0) {
                val mark = myCurrentPage.StartCursor.getMark()
                val target =
                    if (wholeText) (if (backward) myModel!!.getLastMark() else myModel!!.getFirstMark()) else (if (backward) myModel!!.getPreviousMark(
                        mark!!
                    ) else myModel!!.getNextMark(mark!!))
                gotoMark(target)
                if (target != null) {
                    this.searchMarkIndex = myModel!!.getMarks().indexOf(target)
                }
            }
            Application.getViewWidget()!!.reset()
            Application.getViewWidget()!!.repaint()
        }
        return count
    }

    fun canFindNext(): Boolean {
        val end = myCurrentPage.EndCursor
        return !end.isNull() && (myModel != null) && (myModel!!.getNextMark(end.getMark()!!) != null)
    }

    @Synchronized
    fun findNext() {
        val end = myCurrentPage.EndCursor
        if (!end.isNull()) {
            val target = myModel!!.getNextMark(end.getMark()!!)
            gotoMark(target)
            if (target != null) {
                this.searchMarkIndex = myModel!!.getMarks().indexOf(target)
            }
        }
    }

    fun canFindPrevious(): Boolean {
        val start = myCurrentPage.StartCursor
        return !start.isNull() && (myModel != null) && (myModel!!.getPreviousMark(start.getMark()!!) != null)
    }

    @Synchronized
    fun findPrevious() {
        val start = myCurrentPage.StartCursor
        if (!start.isNull()) {
            val target = myModel!!.getPreviousMark(start.getMark()!!)
            gotoMark(target)
            if (target != null) {
                this.searchMarkIndex = myModel!!.getMarks().indexOf(target)
            }
        }
    }

    fun clearFindResults() {
        if (!findResultsAreEmpty()) {
            myModel!!.removeAllMarks()
            rebuildPaintInfo()
            Application.getViewWidget()!!.reset()
            Application.getViewWidget()!!.repaint()
        }
    }

    fun findResultsAreEmpty(): Boolean {
        return myModel == null || myModel!!.getMarks().isEmpty()
    }

    @Synchronized
    public override fun onScrollingFinished(pageIndex: PageIndex) {
        when (pageIndex) {
            PageIndex.current -> {}
            PageIndex.previous -> {
                val swap = myNextPage
                myNextPage = myCurrentPage
                myCurrentPage = myPreviousPage
                myPreviousPage = swap
                myPreviousPage.reset()
                if (myCurrentPage.PaintState == PaintStateEnum.NOTHING_TO_PAINT) {
                    preparePaintInfo(myNextPage)
                    myCurrentPage.EndCursor.setCursor(myNextPage.StartCursor)
                    myCurrentPage.PaintState = PaintStateEnum.END_IS_KNOWN
                } else if (!myCurrentPage.EndCursor.isNull() && !myNextPage.StartCursor.isNull() && !myCurrentPage.EndCursor.samePositionAs(
                        myNextPage.StartCursor
                    )
                ) {
                    myNextPage.reset()
                    myNextPage.StartCursor.setCursor(myCurrentPage.EndCursor)
                    myNextPage.PaintState = PaintStateEnum.START_IS_KNOWN
                    Application.getViewWidget()!!.reset()
                }
            }

            PageIndex.next -> {
                val swap = myPreviousPage
                myPreviousPage = myCurrentPage
                myCurrentPage = myNextPage
                myNextPage = swap
                myNextPage.reset()
                when (myCurrentPage.PaintState) {
                    PaintStateEnum.NOTHING_TO_PAINT -> {
                        preparePaintInfo(myPreviousPage)
                        myCurrentPage.StartCursor.setCursor(myPreviousPage.EndCursor)
                        myCurrentPage.PaintState = PaintStateEnum.START_IS_KNOWN
                    }

                    PaintStateEnum.READY -> {
                        myNextPage.StartCursor.setCursor(myCurrentPage.EndCursor)
                        myNextPage.PaintState = PaintStateEnum.START_IS_KNOWN
                    }
                }
            }
        }
    }

    fun removeHighlightings(type: Class<out ZLTextHighlighting?>): Boolean {
        var result = false
        synchronized(myHighlightings) {
            val it = myHighlightings.iterator()
            while (it.hasNext()) {
                val h = it.next()
                if (type.isInstance(h)) {
                    it.remove()
                    result = true
                }
            }
        }
        return result
    }

    fun highlight(start: ZLTextPosition, end: ZLTextPosition) {
        removeHighlightings(ZLTextManualHighlighting::class.java)
        addHighlighting(ZLTextManualHighlighting(this, start, end))
    }

    fun addHighlighting(h: ZLTextHighlighting?) {
        myHighlightings.add(h!!)
        Application.getViewWidget()!!.reset()
        Application.getViewWidget()!!.repaint()
    }

    fun addHighlightings(hilites: MutableCollection<ZLTextHighlighting>) {
        myHighlightings.addAll(hilites)
        Application.getViewWidget()!!.reset()
        Application.getViewWidget()!!.repaint()
    }

    fun clearHighlighting() {
        if (removeHighlightings(ZLTextManualHighlighting::class.java)) {
            Application.getViewWidget()!!.reset()
            Application.getViewWidget()!!.repaint()
        }
    }

    protected fun moveSelectionCursorTo(which: SelectionCursor.Which, x: Int, y: Int) {
        var y = y
        y -= getTextStyleCollection().baseStyle.getFontSize() / 2
        mySelection.setCursorInMovement(which, x, y)
        mySelection.expandTo(myCurrentPage, x, y)
        Application.getViewWidget()!!.reset()
        Application.getViewWidget()!!.repaint()
    }

    protected open fun releaseSelectionCursor() {
        mySelection.stop()
        Application.getViewWidget()!!.reset()
        Application.getViewWidget()!!.repaint()
    }

    protected val selectionCursorInMovement: SelectionCursor.Which?
        get() = mySelection.getCursorInMovement()

    private fun getSelectionCursorPoint(
        page: ZLTextPage,
        which: SelectionCursor.Which?
    ): ZLTextSelection.Point? {
        if (which == null) {
            return null
        }

        if (which == mySelection.getCursorInMovement()) {
            return mySelection.getCursorInMovementPoint()
        }

        if (which == SelectionCursor.Which.Left) {
            if (mySelection.hasPartBeforePage(page)) {
                return null
            }
            val area = mySelection.getStartArea(page)
            if (area != null) {
                return ZLTextSelection.Point(
                    if (rtlMode) area.XEnd else area.XStart,
                    (area.YStart + area.YEnd) / 2
                )
            }
        } else {
            if (mySelection.hasPartAfterPage(page)) {
                return null
            }
            val area = mySelection.getEndArea(page)
            if (area != null) {
                return ZLTextSelection.Point(
                    if (rtlMode) area.XStart else area.XEnd,
                    (area.YStart + area.YEnd) / 2
                )
            }
        }
        return null
    }

    private fun distance2ToCursor(x: Int, y: Int, which: SelectionCursor.Which?): Float {
        val point = getSelectionCursorPoint(myCurrentPage, which)
        if (point == null) {
            return Float.MAX_VALUE
        }
        val dX = (x - point.X).toFloat()
        val dY = (y - point.Y).toFloat()
        return dX * dX + dY * dY
    }

    protected fun findSelectionCursor(x: Int, y: Int): SelectionCursor.Which? {
        return findSelectionCursor(x, y, Float.MAX_VALUE)
    }

    protected fun findSelectionCursor(x: Int, y: Int, maxDistance2: Float): SelectionCursor.Which? {
        if (mySelection.isEmpty()) {
            return null
        }

        val leftDistance2 = distance2ToCursor(x, y, SelectionCursor.Which.Left)
        val rightDistance2 = distance2ToCursor(x, y, SelectionCursor.Which.Right)

        if (rightDistance2 < leftDistance2) {
            return if (rightDistance2 <= maxDistance2) SelectionCursor.Which.Right else null
        } else {
            return if (leftDistance2 <= maxDistance2) SelectionCursor.Which.Left else null
        }
    }

    fun drawSelectionCursor(
        context: ZLPaintContext,
        page: ZLTextPage,
        which: SelectionCursor.Which?
    ) {
        val pt = getSelectionCursorPoint(page, which)
        if (pt != null) {
            SelectionCursor.draw(context, which!!, pt.X, pt.Y, getSelectionBackgroundColor()!!)
        }
    }

    @Synchronized
    public override fun preparePage(context: ZLPaintContext, pageIndex: PageIndex) {
        setContext(context)
        preparePaintInfo(getPage(pageIndex))
    }

    @Synchronized
    public override fun paint(context: ZLPaintContext, pageIndex: PageIndex) {
        setContext(context)
        val wallpaper = getWallpaperFile()
        if (wallpaper != null) {
            context.clear(wallpaper, getFillMode())
        } else {
            context.clear(getBackgroundColor()!!)
        }

        if (myModel == null || myModel!!.getParagraphsNumber() == 0) {
            return
        }

        val page: ZLTextPage
        when (pageIndex) {
            PageIndex.current -> page = myCurrentPage
            PageIndex.previous -> {
                page = myPreviousPage
                if (myPreviousPage.PaintState == PaintStateEnum.NOTHING_TO_PAINT) {
                    preparePaintInfo(myCurrentPage)
                    myPreviousPage.EndCursor.setCursor(myCurrentPage.StartCursor)
                    myPreviousPage.PaintState = PaintStateEnum.END_IS_KNOWN
                }
            }

            PageIndex.next -> {
                page = myNextPage
                if (myNextPage.PaintState == PaintStateEnum.NOTHING_TO_PAINT) {
                    preparePaintInfo(myCurrentPage)
                    myNextPage.StartCursor.setCursor(myCurrentPage.EndCursor)
                    myNextPage.PaintState = PaintStateEnum.START_IS_KNOWN
                }
            }

            else -> page = myCurrentPage
        }

        page.TextElementMap.clear()

        preparePaintInfo(page)

        if (page.StartCursor.isNull() || page.EndCursor.isNull()) {
            return
        }

        val lineInfos = page.LineInfos
        val labels = IntArray(lineInfos.size + 1)
        var x =
            (if (rtlMode) ((if (page.twoColumnView()) page.getTextWidth() * 2 + getSpaceBetweenColumns() else page.getTextWidth()) + getLeftMargin()) else getLeftMargin())
        var y = getTopMargin() + page.topMargin
        var index = 0
        var columnIndex = 0
        var previousInfo: ZLTextLineInfo? = null
        for (info in lineInfos) {
            info.adjust(previousInfo)
            val first = page.TextElementMap.size()
            prepareTextLine(page, info, x, y, columnIndex)
            rtlDetected = rtlDetected or page.TextElementMap.swapRtl(
                rtlMode,
                first,
                page.TextElementMap.size()
            )
            y += info.Height + info.Descent + info.VSpaceAfter
            labels[++index] = page.TextElementMap.size()
            if (index == page.Column0Height) {
                y = getTopMargin() + page.topMargin
                x += (page.getTextWidth() + getSpaceBetweenColumns()) * (if (rtlMode) -1 else 1)
                columnIndex = 1
            }
            previousInfo = info
        }

        val hilites = findHilites(page)

        x =
            if (rtlMode) ((if (page.twoColumnView()) page.getTextWidth() * 2 + getSpaceBetweenColumns() else page.getTextWidth()) + getLeftMargin()) else getLeftMargin()
        y = getTopMargin() + page.topMargin
        index = 0
        for (info in lineInfos) {
            drawTextLine(page, hilites, info, labels[index], labels[index + 1])
            y += info.Height + info.Descent + info.VSpaceAfter
            ++index
            if (index == page.Column0Height) {
                y = getTopMargin() + page.topMargin
                x += (page.getTextWidth() + getSpaceBetweenColumns()) * (if (rtlMode) -1 else 1)
            }
        }

        for (h in hilites) {
            var mode = Hull.DrawMode.None

            val bgColor = h.getBackgroundColor()
            if (bgColor != null) {
                context.setFillColor(bgColor, 128)
                mode = mode or Hull.DrawMode.Fill
            }

            val outlineColor = h.getOutlineColor()
            if (outlineColor != null) {
                context.setLineColor(outlineColor)
                mode = mode or Hull.DrawMode.Outline
            }

            if (mode != Hull.DrawMode.None) {
                h.hull(page).draw(getContext(), mode)
            }
        }

        val outlinedElementRegion = getOutlinedRegion(page)
        if (outlinedElementRegion != null && myShowOutline) {
            context.setLineColor(getSelectionBackgroundColor()!!)
            outlinedElementRegion.hull().draw(context, Hull.DrawMode.Outline)
        }

        drawSelectionCursor(context, page, SelectionCursor.Which.Left)
        drawSelectionCursor(context, page, SelectionCursor.Which.Right)
    }

    private fun getPage(pageIndex: PageIndex): ZLTextPage {
        when (pageIndex) {
            PageIndex.current -> return myCurrentPage
            PageIndex.previous -> return myPreviousPage
            PageIndex.next -> return myNextPage
            else -> return myCurrentPage
        }
    }

    abstract fun scrollbarType(): Int

    public override fun isScrollbarShown(): Boolean {
        return scrollbarType() == SCROLLBAR_SHOW || scrollbarType() == SCROLLBAR_SHOW_AS_PROGRESS
    }

    @Synchronized
    protected fun sizeOfTextBeforeParagraph(paragraphIndex: Int): Int {
        return if (myModel != null) myModel!!.getTextLength(paragraphIndex - 1) else 0
    }

    @Synchronized
    protected fun sizeOfFullText(): Int {
        if (myModel == null || myModel!!.getParagraphsNumber() == 0) {
            return 1
        }
        return myModel!!.getTextLength(myModel!!.getParagraphsNumber() - 1)
    }

    @Synchronized
    private fun getCurrentCharNumber(pageIndex: PageIndex, startNotEndOfPage: Boolean): Int {
        if (myModel == null || myModel!!.getParagraphsNumber() == 0) {
            return 0
        }
        val page = getPage(pageIndex)
        preparePaintInfo(page)
        if (startNotEndOfPage) {
            return max(0, sizeOfTextBeforeCursor(page.StartCursor))
        } else {
            var end = sizeOfTextBeforeCursor(page.EndCursor)
            if (end == -1) {
                end = myModel!!.getTextLength(myModel!!.getParagraphsNumber() - 1) - 1
            }
            return max(1, end)
        }
    }

    @Synchronized
    public override fun getScrollbarFullSize(): Int {
        return sizeOfFullText()
    }

    @Synchronized
    public override fun getScrollbarThumbPosition(pageIndex: PageIndex): Int {
        return if (scrollbarType() == SCROLLBAR_SHOW_AS_PROGRESS) 0 else getCurrentCharNumber(
            pageIndex,
            true
        )
    }

    @Synchronized
    public override fun getScrollbarThumbLength(pageIndex: PageIndex): Int {
        val start = if (scrollbarType() == SCROLLBAR_SHOW_AS_PROGRESS)
            0
        else
            getCurrentCharNumber(pageIndex, true)
        val end = getCurrentCharNumber(pageIndex, false)
        return max(1, end - start)
    }

    private fun sizeOfTextBeforeCursor(wordCursor: ZLTextWordCursor): Int {
        val paragraphCursor = wordCursor.getParagraphCursor()
        if (paragraphCursor == null) {
            return -1
        }
        val paragraphIndex = paragraphCursor.Index
        var sizeOfText = myModel!!.getTextLength(paragraphIndex - 1)
        val paragraphLength = paragraphCursor.getParagraphLength()
        if (paragraphLength > 0) {
            sizeOfText +=
                ((myModel!!.getTextLength(paragraphIndex) - sizeOfText)
                        * wordCursor.elementIndex
                        / paragraphLength)
        }
        return sizeOfText
    }

    // Can be called only when (myModel.getParagraphsNumber() != 0)
    @Synchronized
    private fun computeCharsPerPage(): Float {
        setTextStyle(getTextStyleCollection().baseStyle)

        val textWidth = getTextColumnWidth()
        val textHeight = getTextAreaHeight()

        val num = myModel!!.getParagraphsNumber()
        val totalTextSize = myModel!!.getTextLength(num - 1)
        val charsPerParagraph = (totalTextSize.toFloat()) / num

        val charWidth = computeCharWidth()

        val indentWidth = getElementWidth(ZLTextElement.Indent, 0)
        val effectiveWidth = textWidth - (indentWidth + 0.5f * textWidth) / charsPerParagraph
        val charsPerLine = min(
            effectiveWidth / charWidth,
            charsPerParagraph * 1.2f
        )

        val strHeight = getWordHeight() + getContext().getDescent()
        val effectiveHeight = (textHeight -
                (getTextStyle().getSpaceBefore(metrics())
                        + getTextStyle().getSpaceAfter(metrics()) / 2) / charsPerParagraph).toInt()
        val linesPerPage = effectiveHeight / strHeight

        return charsPerLine * linesPerPage
    }

    @Synchronized
    private fun computeTextPageNumber(textSize: Int): Int {
        if (myModel == null || myModel!!.getParagraphsNumber() == 0) {
            return 1
        }

        val factor = 1.0f / computeCharsPerPage()
        val pages = textSize * factor
        return max((pages + 1.0f - 0.5f * factor).toInt(), 1)
    }

    private fun computeCharWidth(): Float {
        if (myLettersModel !== myModel) {
            myLettersModel = myModel
            myLettersBufferLength = 0
            myCharWidth = -1f

            var paragraph = 0
            val textSize = myModel!!.getTextLength(myModel!!.getParagraphsNumber() - 1)
            if (textSize > myLettersBuffer.size) {
                paragraph =
                    myModel!!.findParagraphByTextLength((textSize - myLettersBuffer.size) / 2)
            }
            while (paragraph < myModel!!.getParagraphsNumber()
                && myLettersBufferLength < myLettersBuffer.size
            ) {
                val it = myModel!!.getParagraph(paragraph++)!!.iterator()
                while (myLettersBufferLength < myLettersBuffer.size && it!!.next()) {
                    if (it.getType() == ZLTextParagraph.Entry.TEXT) {
                        val len = min(
                            it.getTextLength(),
                            myLettersBuffer.size - myLettersBufferLength
                        )
                        System.arraycopy(
                            it.getTextData(), it.getTextOffset(),
                            myLettersBuffer, myLettersBufferLength, len
                        )
                        myLettersBufferLength += len
                    }
                }
            }

            if (myLettersBufferLength == 0) {
                myLettersBufferLength = min(myLettersBuffer.size, ourDefaultLetters.size)
                System.arraycopy(ourDefaultLetters, 0, myLettersBuffer, 0, myLettersBufferLength)
            }
        }

        if (myCharWidth < 0f) {
            myCharWidth = computeCharWidth(myLettersBuffer, myLettersBufferLength)
        }
        return myCharWidth
    }

    private fun computeCharWidth(pattern: CharArray, length: Int): Float {
        return getContext().getStringWidth(pattern, 0, length) / (length.toFloat())
    }

    @Synchronized
    open fun pagePosition(): PagePosition {
        var current = computeTextPageNumber(getCurrentCharNumber(PageIndex.current, false))
        var total = computeTextPageNumber(sizeOfFullText())

        if (total > 3) {
            return PagePosition(current, total)
        }

        preparePaintInfo(myCurrentPage)
        var cursor = myCurrentPage.StartCursor
        if (cursor == null || cursor.isNull()) {
            return PagePosition(current, total)
        }

        if (cursor.isStartOfText()) {
            current = 1
        } else {
            var prevCursor = myPreviousPage.StartCursor
            if (prevCursor == null || prevCursor.isNull()) {
                preparePaintInfo(myPreviousPage)
                prevCursor = myPreviousPage.StartCursor
            }
            if (prevCursor != null && !prevCursor.isNull()) {
                current = if (prevCursor.isStartOfText()) 2 else 3
            }
        }

        total = current
        cursor = myCurrentPage.EndCursor
        if (cursor == null || cursor.isNull()) {
            return PagePosition(current, total)
        }
        if (!cursor.isEndOfText()) {
            var nextCursor = myNextPage.EndCursor
            if (nextCursor == null || nextCursor.isNull()) {
                preparePaintInfo(myNextPage)
                nextCursor = myNextPage.EndCursor
            }
            if (nextCursor != null) {
                total += if (nextCursor.isEndOfText()) 1 else 2
            }
        }

        return PagePosition(current, total)
    }

    val progress: RationalNumber?
        get() {
            val position = pagePosition()
            return create(position.Current.toLong(), position.Total.toLong())
        }

    @Synchronized
    open fun gotoPage(page: Int) {
        if (myModel == null || myModel!!.getParagraphsNumber() == 0) {
            return
        }

        val factor = computeCharsPerPage()
        val textSize = page * factor

        var intTextSize = textSize.toInt()
        var paragraphIndex = myModel!!.findParagraphByTextLength(intTextSize)

        if (paragraphIndex > 0 && myModel!!.getTextLength(paragraphIndex) > intTextSize) {
            --paragraphIndex
        }
        intTextSize = myModel!!.getTextLength(paragraphIndex)

        var sizeOfTextBefore = myModel!!.getTextLength(paragraphIndex - 1)
        while (paragraphIndex > 0 && intTextSize == sizeOfTextBefore) {
            --paragraphIndex
            intTextSize = sizeOfTextBefore
            sizeOfTextBefore = myModel!!.getTextLength(paragraphIndex - 1)
        }

        val paragraphLength = intTextSize - sizeOfTextBefore

        val wordIndex: Int
        if (paragraphLength == 0) {
            wordIndex = 0
        } else {
            preparePaintInfo(myCurrentPage)
            val cursor = ZLTextWordCursor(myCurrentPage.EndCursor)
            cursor.moveToParagraph(paragraphIndex)
            wordIndex = cursor.getParagraphCursor()!!.getParagraphLength()
        }

        gotoPositionByEnd(paragraphIndex, wordIndex, 0)
    }

    open fun gotoHome() {
        val cursor = this.startCursor
        if (!cursor.isNull() && cursor.isStartOfParagraph() && cursor.paragraphIndex == 0) {
            return
        }
        gotoPosition(0, 0, 0)
        preparePaintInfo()
    }

    private fun findHilites(page: ZLTextPage): MutableList<ZLTextHighlighting> {
        val hilites = LinkedList<ZLTextHighlighting>()
        if (mySelection.intersects(page)) {
            hilites.add(mySelection)
        }
        synchronized(myHighlightings) {
            for (h in myHighlightings) {
                if (h.intersects(page)) {
                    hilites.add(h)
                }
            }
        }
        return hilites
    }

    protected abstract val adjustingModeForImages: ColorAdjustingMode?

    private fun drawTextLine(
        page: ZLTextPage,
        hilites: MutableList<ZLTextHighlighting>,
        info: ZLTextLineInfo,
        from: Int,
        to: Int
    ) {
        val context = getContext()
        val paragraph = info.ParagraphCursor
        var index = from
        val endElementIndex = info.EndElementIndex
        var charIndex = info.RealStartCharIndex
        val pageAreas: List<ZLTextElementArea> = page.TextElementMap.areas()
        if (to > pageAreas.size) {
            return
        }
        var wordIndex = info.RealStartElementIndex
        while (wordIndex < endElementIndex && index < to) {
            val element = paragraph.getElement(wordIndex)!!
            val area = pageAreas.get(index)
            if (element === area.Element) {
                ++index
                if (area.ChangeStyle) {
                    setTextStyle(area.Style)
                }
                val areaX = area.XStart
                val areaY =
                    area.YEnd - getElementDescent(element) - getTextStyle().getVerticalAlign(metrics())
                if (element is ZLTextWord) {
                    val pos: ZLTextPosition =
                        ZLTextFixedPosition(info.ParagraphCursor.Index, wordIndex, 0)
                    val hl = getWordHilite(pos, hilites)
                    val hlColor = if (hl != null) hl.getForegroundColor() else null
                    drawWord(
                        areaX, areaY, element, charIndex, -1, false,
                        if (hlColor != null) hlColor else getTextColor(getTextStyle().Hyperlink!!)!!
                    )
                } else if (element is ZLTextImageElement) {
                    val imageElement = element
                    context.drawImage(
                        areaX, areaY,
                        imageElement.ImageData!!,
                        getTextAreaSize(),
                        getScalingType(imageElement),
                        this.adjustingModeForImages!!
                    )
                } else if (element is ZLTextVideoElement) {
                    // TODO: draw
                    context.setLineColor(getTextColor(ZLTextHyperlink.NO_LINK)!!)
                    context.setFillColor(ZLColor(127, 127, 127))
                    val xStart = area.XStart + 10
                    val xEnd = area.XEnd - 10
                    val yStart = area.YStart + 10
                    val yEnd = area.YEnd - 10
                    context.fillRectangle(xStart, yStart, xEnd, yEnd)
                    context.drawLine(xStart, yStart, xStart, yEnd)
                    context.drawLine(xStart, yEnd, xEnd, yEnd)
                    context.drawLine(xEnd, yEnd, xEnd, yStart)
                    context.drawLine(xEnd, yStart, xStart, yStart)
                    val l = xStart + (xEnd - xStart) * 7 / 16
                    val r = xStart + (xEnd - xStart) * 10 / 16
                    val t = yStart + (yEnd - yStart) * 2 / 6
                    val b = yStart + (yEnd - yStart) * 4 / 6
                    val c = yStart + (yEnd - yStart) / 2
                    context.setFillColor(ZLColor(196, 196, 196))
                    context.fillPolygon(intArrayOf(l, l, r), intArrayOf(t, b, c))
                } else if (element is ExtensionElement) {
                    element.draw(context, area)
                } else if (element === ZLTextElement.HSpace || element === ZLTextElement.NBSpace) {
                    val cw = context.getSpaceWidth()
                    var len = 0
                    while (len < area.XEnd - area.XStart) {
                        context.drawString(areaX + len, areaY, SPACE, 0, 1)
                        len += cw
                    }
                }
            }
            ++wordIndex
            charIndex = 0
        }
        if (index != to) {
            val area = pageAreas.get(index++)
            if (area.ChangeStyle) {
                setTextStyle(area.Style)
            }
            val start = if (info.StartElementIndex == info.EndElementIndex)
                info.StartCharIndex
            else
                0
            val len = info.EndCharIndex - start
            val word = paragraph.getElement(info.EndElementIndex) as ZLTextWord
            val pos: ZLTextPosition =
                ZLTextFixedPosition(info.ParagraphCursor.Index, info.EndElementIndex, 0)
            val hl = getWordHilite(pos, hilites)
            val hlColor = if (hl != null) hl.getForegroundColor() else null
            drawWord(
                area.XStart,
                area.YEnd - context.getDescent() - getTextStyle().getVerticalAlign(metrics()),
                word,
                start,
                len,
                area.AddHyphenationSign,
                if (hlColor != null) hlColor else getTextColor(getTextStyle().Hyperlink!!)!!
            )
        }
    }

    private fun getWordHilite(
        pos: ZLTextPosition,
        hilites: MutableList<ZLTextHighlighting>
    ): ZLTextHighlighting? {
        for (h in hilites) {
            if (h.getStartPosition()!!.compareToIgnoreChar(pos) <= 0
                && pos.compareToIgnoreChar(h.getEndPosition()!!) <= 0
            ) {
                return h
            }
        }
        return null
    }

    private fun buildInfos(page: ZLTextPage, start: ZLTextWordCursor, result: ZLTextWordCursor) {
        val end =
            if (result.isNull() || result.samePositionAs(start)) null else ZLTextFixedPosition(
                result
            )
        result.setCursor(start)
        var textAreaHeight = page.getTextHeight()
        page.LineInfos.clear()
        page.Column0Height = 0
        var nextParagraph: Boolean
        var info: ZLTextLineInfo? = null
        do {
            val previousInfo = info
            resetTextStyle()
            val paragraphCursor = result.getParagraphCursor()
            val wordIndex = result.elementIndex
            applyStyleChanges(paragraphCursor!!, 0, wordIndex)
            info = ZLTextLineInfo(paragraphCursor!!, wordIndex, result.charIndex, getTextStyle())
            val endIndex = info.ParagraphCursorLength
            while (info!!.EndElementIndex != endIndex) {
                info = processTextLine(
                    page,
                    paragraphCursor,
                    info.EndElementIndex,
                    info.EndCharIndex,
                    endIndex,
                    previousInfo
                )
                textAreaHeight -= info.Height + info.Descent
                if (textAreaHeight < 0 && page.LineInfos.size > page.Column0Height) {
                    if (page.Column0Height == 0 && page.twoColumnView()) {
                        textAreaHeight = page.getTextHeight()
                        textAreaHeight -= info.Height + info.Descent
                        page.Column0Height = page.LineInfos.size
                    } else {
                        break
                    }
                }
                textAreaHeight -= info.VSpaceAfter
                if (end != null && result.compareTo(end) >= 0) {
                    break
                }
                result.moveTo(info.EndElementIndex, info.EndCharIndex)
                page.LineInfos.add(info)
                if (textAreaHeight < 0) {
                    if (page.Column0Height == 0 && page.twoColumnView()) {
                        textAreaHeight = page.getTextHeight()
                        page.Column0Height = page.LineInfos.size
                    } else {
                        break
                    }
                }
            }
            nextParagraph = result.isEndOfParagraph && result.nextParagraph()
            if (nextParagraph && result.getParagraphCursor()!!.isEndOfSection()) {
                if (page.Column0Height == 0 && page.twoColumnView() && !page.LineInfos.isEmpty()) {
                    textAreaHeight = page.getTextHeight()
                    page.Column0Height = page.LineInfos.size
                }
            }
        } while (nextParagraph && textAreaHeight >= 0 &&
            (!result.getParagraphCursor()!!.isEndOfSection() ||
                    page.LineInfos.size == page.Column0Height)
        )
        if (end != null && result.compareTo(end) >= 0) {
            if ((info.EndElementIndex != info.ParagraphCursorLength || nextParagraph) && textAreaHeight >= 0 && (!result.getParagraphCursor()!!
                    .isEndOfSection() || page.LineInfos.size == page.Column0Height)
            ) page.topMargin = textAreaHeight
            else page.topMargin = 0
            val last = page.LineInfos.size - 1
            if (last >= 0) {
                info = page.LineInfos.get(last)
                if (end.compareTo(
                        ZLTextFixedPosition(
                            info.ParagraphCursor.Index,
                            info.EndElementIndex,
                            info.EndCharIndex
                        )
                    ) < 0
                ) {
                    info.EndElementIndex = end.ElementIndex
                    info.EndCharIndex = end.CharIndex
                }
            }
            result.moveTo(end)
        } else {
            page.topMargin = 0
        }
        resetTextStyle()
    }

    private val isHyphenationPossible: Boolean
        get() = getTextStyleCollection().baseStyle.AutoHyphenationOption.getValue()
                && getTextStyle().allowHyphenations()

    @Synchronized
    private fun getHyphenationInfo(word: ZLTextWord): ZLTextHyphenationInfo {
        if (myCachedWord != word) {
            myCachedWord = word
            myCachedInfo = Instance().getInfo(word)
        }
        return myCachedInfo!!
    }

    private fun processTextLine(
        page: ZLTextPage,
        paragraphCursor: ZLTextParagraphCursor,
        startIndex: Int,
        startCharIndex: Int,
        endIndex: Int,
        previousInfo: ZLTextLineInfo?
    ): ZLTextLineInfo {
        val info = processTextLineInternal(
            page, paragraphCursor, startIndex, startCharIndex, endIndex, previousInfo
        )
        if (info.EndElementIndex == startIndex && info.EndCharIndex == startCharIndex) {
            info.EndElementIndex = paragraphCursor.getParagraphLength()
            info.EndCharIndex = 0
            // TODO: add error element
        }
        return info
    }

    private fun processTextLineInternal(
        page: ZLTextPage,
        paragraphCursor: ZLTextParagraphCursor,
        startIndex: Int,
        startCharIndex: Int,
        endIndex: Int,
        previousInfo: ZLTextLineInfo?
    ): ZLTextLineInfo {
        val context = getContext()
        val info = ZLTextLineInfo(paragraphCursor, startIndex, startCharIndex, getTextStyle())
        val cachedInfo = myLineInfoCache.get(info)
        if (cachedInfo != null) {
            cachedInfo.adjust(previousInfo)
            applyStyleChanges(paragraphCursor, startIndex, cachedInfo.EndElementIndex)
            return cachedInfo
        }

        var currentElementIndex = startIndex
        var currentCharIndex = startCharIndex
        val isFirstLine = startIndex == 0 && startCharIndex == 0

        if (isFirstLine) {
            var element = paragraphCursor.getElement(currentElementIndex)!!
            while (isStyleChangeElement(element)) {
                applyStyleChangeElement(element)
                ++currentElementIndex
                currentCharIndex = 0
                if (currentElementIndex == endIndex) {
                    break
                }
                element = paragraphCursor.getElement(currentElementIndex)!!
            }
            info.StartStyle = getTextStyle()
            info.RealStartElementIndex = currentElementIndex
            info.RealStartCharIndex = currentCharIndex
        }

        var storedStyle = getTextStyle()

        val maxWidth = page.getTextWidth() - storedStyle.getRightIndent(metrics())
        info.LeftIndent = storedStyle.getLeftIndent(metrics())
        if (isFirstLine && storedStyle.getAlignment() != ZLTextAlignmentType.ALIGN_CENTER) {
            info.LeftIndent += storedStyle.getFirstLineIndent(metrics())
        }
        if (info.LeftIndent > maxWidth - 20) {
            info.LeftIndent = maxWidth * 3 / 4
        }

        info.Width = info.LeftIndent

        if (info.RealStartElementIndex == endIndex) {
            info.EndElementIndex = info.RealStartElementIndex
            info.EndCharIndex = info.RealStartCharIndex
            return info
        }

        var newWidth = info.Width
        var newHeight = info.Height
        var newDescent = info.Descent
        var wordOccurred = false
        var isVisible = false
        var lastSpaceWidth = 0
        var internalSpaceCounter = 0
        var removeLastSpace = false

        do {
            var element = paragraphCursor.getElement(currentElementIndex)!!
            newWidth += getElementWidth(element, currentCharIndex)
            newHeight = max(newHeight, getElementHeight(element))
            newDescent = max(newDescent, getElementDescent(element))
            if (element === ZLTextElement.HSpace) {
                if (wordOccurred) {
                    wordOccurred = false
                    internalSpaceCounter++
                    lastSpaceWidth = context.getSpaceWidth()
                    newWidth += lastSpaceWidth
                }
            } else if (element === ZLTextElement.NBSpace) {
                wordOccurred = true
            } else if (element is ZLTextWord) {
                wordOccurred = true
                isVisible = true
            } else if (element is ZLTextImageElement) {
                wordOccurred = true
                isVisible = true
            } else if (element is ZLTextVideoElement) {
                wordOccurred = true
                isVisible = true
            } else if (element is ExtensionElement) {
                wordOccurred = true
                isVisible = true
            } else if (isStyleChangeElement(element)) {
                applyStyleChangeElement(element)
            }
            if (newWidth > maxWidth) {
                if (info.EndElementIndex != startIndex || element is ZLTextWord) {
                    break
                }
            }
            val previousElement = element
            ++currentElementIndex
            currentCharIndex = 0
            var allowBreak = currentElementIndex == endIndex
            if (!allowBreak) {
                element = paragraphCursor.getElement(currentElementIndex)!!
                allowBreak =
                    previousElement !== ZLTextElement.NBSpace && element !== ZLTextElement.NBSpace &&
                            (element !is ZLTextWord || previousElement is ZLTextWord) && (element !is ZLTextImageElement) && (element !is ZLTextControlElement)
            }
            if (allowBreak) {
                info.IsVisible = isVisible
                info.Width = newWidth
                if (info.Height < newHeight) {
                    info.Height = newHeight
                }
                if (info.Descent < newDescent) {
                    info.Descent = newDescent
                }
                info.EndElementIndex = currentElementIndex
                info.EndCharIndex = currentCharIndex
                info.SpaceCounter = internalSpaceCounter
                storedStyle = getTextStyle()
                removeLastSpace = !wordOccurred && (internalSpaceCounter > 0)
            }
        } while (currentElementIndex != endIndex)

        if (currentElementIndex != endIndex &&
            (this.isHyphenationPossible || info.EndElementIndex == startIndex)
        ) {
            val element = paragraphCursor.getElement(currentElementIndex)
            if (element is ZLTextWord) {
                val word = element
                newWidth -= getWordWidth(word, currentCharIndex)
                val spaceLeft = maxWidth - newWidth
                if ((word.Length > 3 && spaceLeft > 2 * context.getSpaceWidth())
                    || info.EndElementIndex == startIndex
                ) {
                    val hyphenationInfo = getHyphenationInfo(word)
                    var hyphenationPosition = currentCharIndex
                    var subwordWidth = 0
                    var right = word.Length - 1
                    var left = currentCharIndex
                    while (right > left) {
                        val mid = (right + left + 1) / 2
                        var m1 = mid
                        while (m1 > left && !hyphenationInfo.isHyphenationPossible(m1)) {
                            --m1
                        }
                        if (m1 > left) {
                            val w = getWordWidth(
                                word,
                                currentCharIndex,
                                m1 - currentCharIndex,
                                word.Data[word.Offset + m1 - 1] != '-'
                            )
                            if (w < spaceLeft) {
                                left = mid
                                hyphenationPosition = m1
                                subwordWidth = w
                            } else {
                                right = mid - 1
                            }
                        } else {
                            left = mid
                        }
                    }
                    if (hyphenationPosition == currentCharIndex && info.EndElementIndex == startIndex) {
                        subwordWidth = getWordWidth(word, currentCharIndex, 1, false)
                        var right =
                            if (word.Length == currentCharIndex + 1) word.Length else word.Length - 1
                        var left = currentCharIndex + 1
                        while (right > left) {
                            val mid = (right + left + 1) / 2
                            val w = getWordWidth(
                                word,
                                currentCharIndex,
                                mid - currentCharIndex,
                                word.Data[word.Offset + mid - 1] != '-'
                            )
                            if (w <= spaceLeft) {
                                left = mid
                                subwordWidth = w
                            } else {
                                right = mid - 1
                            }
                        }
                        hyphenationPosition = right
                    }
                    if (hyphenationPosition > currentCharIndex) {
                        info.IsVisible = true
                        info.Width = newWidth + subwordWidth
                        if (info.Height < newHeight) {
                            info.Height = newHeight
                        }
                        if (info.Descent < newDescent) {
                            info.Descent = newDescent
                        }
                        info.EndElementIndex = currentElementIndex
                        info.EndCharIndex = hyphenationPosition
                        info.SpaceCounter = internalSpaceCounter
                        storedStyle = getTextStyle()
                        removeLastSpace = false
                    }
                }
            }
        }

        if (removeLastSpace) {
            info.Width -= lastSpaceWidth
            info.SpaceCounter--
        }

        setTextStyle(storedStyle)

        if (isFirstLine) {
            info.VSpaceBefore = info.StartStyle.getSpaceBefore(metrics())
            if (previousInfo != null) {
                info.PreviousInfoUsed = true
                info.Height += max(0, info.VSpaceBefore - previousInfo.VSpaceAfter)
            } else {
                info.PreviousInfoUsed = false
                info.Height += info.VSpaceBefore
            }
        }
        if (info.isEndOfParagraph()) {
            info.VSpaceAfter = getTextStyle().getSpaceAfter(metrics())
        }

        if (info.EndElementIndex != endIndex || endIndex == info.ParagraphCursorLength) {
            myLineInfoCache.put(info, info)
        }

        return info
    }

    private fun prepareTextLine(
        page: ZLTextPage,
        info: ZLTextLineInfo,
        x: Int,
        y: Int,
        columnIndex: Int
    ) {
        var x = x
        var y = y
        y = min(y + info.Height, getTopMargin() + page.getTextHeight() + page.topMargin - 1)

        val context = getContext()
        val paragraphCursor = info.ParagraphCursor

        setTextStyle(info.StartStyle)
        var spaceCounter = info.SpaceCounter
        var fullCorrection = 0
        val endOfParagraph = info.isEndOfParagraph()
        var wordOccurred = false
        var changeStyle = true
        x += info.LeftIndent * (if (rtlMode) -1 else 1)

        val maxWidth = page.getTextWidth()
        when (getTextStyle().getAlignment()) {
            ZLTextAlignmentType.ALIGN_RIGHT -> x += (maxWidth - getTextStyle().getRightIndent(
                metrics()
            ) - info.Width) * (if (rtlMode) 0 else 1)

            ZLTextAlignmentType.ALIGN_CENTER -> x += ((maxWidth - getTextStyle().getRightIndent(
                metrics()
            ) - info.Width) / 2) * (if (rtlMode) -1 else 1)

            ZLTextAlignmentType.ALIGN_JUSTIFY -> if (!endOfParagraph && (paragraphCursor.getElement(
                    info.EndElementIndex
                ) !== ZLTextElement.AfterParagraph)
            ) {
                fullCorrection = maxWidth - getTextStyle().getRightIndent(metrics()) - info.Width
            }

            ZLTextAlignmentType.ALIGN_LEFT -> x -= (maxWidth - getTextStyle().getLeftIndent(metrics()) - info.Width) * (if (rtlMode) 1 else 0)
            ZLTextAlignmentType.ALIGN_UNDEFINED -> {}
        }

        val paragraph = info.ParagraphCursor
        val paragraphIndex = paragraph.Index
        val endElementIndex = info.EndElementIndex
        var charIndex = info.RealStartCharIndex
        var spaceElement: ZLTextElementArea? = null
        var wordIndex = info.RealStartElementIndex
        while (wordIndex < endElementIndex) {
            val element = paragraph.getElement(wordIndex)!!
            val width = getElementWidth(element, charIndex)
            if (element === ZLTextElement.HSpace) {
                if (wordOccurred && spaceCounter > 0) {
                    val correction = fullCorrection / spaceCounter
                    val spaceLength = context.getSpaceWidth() + correction
                    if (getTextStyle().isUnderline()) {
                        spaceElement = ZLTextElementArea(
                            paragraphIndex,
                            wordIndex,
                            0,
                            0,  // length
                            true,  // is last in element
                            false,  // add hyphenation sign
                            false,  // changed style
                            getTextStyle(),
                            element,
                            (if (rtlMode) x - spaceLength else x),
                            (if (rtlMode) x else x + spaceLength),
                            y,
                            y,
                            columnIndex
                        )
                    } else {
                        spaceElement = null
                    }
                    x += spaceLength * (if (rtlMode) -1 else 1)
                    fullCorrection -= correction
                    wordOccurred = false
                    --spaceCounter
                }
            } else if (element is ZLTextWord || element is ZLTextImageElement || element is ZLTextVideoElement || element is ExtensionElement) {
                val height = getElementHeight(element)
                val descent = getElementDescent(element)
                val length = if (element is ZLTextWord) element.Length else 0
                if (spaceElement != null) {
                    page.TextElementMap.add(spaceElement)
                    spaceElement = null
                }
                page.TextElementMap.add(
                    ZLTextElementArea(
                        paragraphIndex,
                        wordIndex,
                        charIndex,
                        length - charIndex,
                        true,  // is last in element
                        false,  // add hyphenation sign
                        changeStyle,
                        getTextStyle(),
                        element,
                        (if (rtlMode) x - width else x),
                        (if (rtlMode) x else x + width) - 1,
                        y - height + 1,
                        y + descent,
                        columnIndex
                    )
                )
                changeStyle = false
                wordOccurred = true
            } else if (isStyleChangeElement(element)) {
                applyStyleChangeElement(element)
                changeStyle = true
            }
            x += width * (if (rtlMode) -1 else 1)
            ++wordIndex
            charIndex = 0
        }
        if (!endOfParagraph) {
            val len = info.EndCharIndex
            if (len > 0) {
                val wordIndex = info.EndElementIndex
                val word = paragraph.getElement(wordIndex) as ZLTextWord
                val addHyphenationSign = word.Data[word.Offset + len - 1] != '-'
                val width = getWordWidth(word, 0, len, addHyphenationSign)
                val height = getElementHeight(word)
                val descent = context.getDescent()
                page.TextElementMap.add(
                    ZLTextElementArea(
                        paragraphIndex,
                        wordIndex,
                        0,
                        len,
                        false,  // is last in element
                        addHyphenationSign,
                        changeStyle,
                        getTextStyle(),
                        word,
                        (if (rtlMode) x - width else x),
                        (if (rtlMode) (x + 1) else (x + width - 1)),
                        y - height + 1,
                        y + descent,
                        columnIndex
                    )
                )
            }
        }
    }

    @Synchronized
    fun turnPage(forward: Boolean, scrollingMode: Int, value: Int) {
        preparePaintInfo(myCurrentPage)
        myPreviousPage.reset()
        myNextPage.reset()
        if (myCurrentPage.PaintState == PaintStateEnum.READY) {
            myCurrentPage.PaintState =
                if (forward) PaintStateEnum.TO_SCROLL_FORWARD else PaintStateEnum.TO_SCROLL_BACKWARD
            myScrollingMode = scrollingMode
            myOverlappingValue = value
        }
    }

    @Synchronized
    fun gotoPosition(position: ZLTextPosition?) {
        if (position != null) {
            gotoPosition(position.paragraphIndex, position.elementIndex, position.charIndex)
        }
    }

    @Synchronized
    fun gotoPosition(paragraphIndex: Int, wordIndex: Int, charIndex: Int) {
        if (myModel != null && myModel!!.getParagraphsNumber() > 0) {
            Application.getViewWidget()!!.reset()
            myCurrentPage.moveStartCursor(paragraphIndex, wordIndex, charIndex)
            myPreviousPage.reset()
            myNextPage.reset()
            preparePaintInfo(myCurrentPage)
            if (myCurrentPage.isEmptyPage()) {
                turnPage(true, ScrollingMode.NO_OVERLAPPING, 0)
            }
        }
    }

    @Synchronized
    private fun gotoPositionByEnd(paragraphIndex: Int, wordIndex: Int, charIndex: Int) {
        if (myModel != null && myModel!!.getParagraphsNumber() > 0) {
            myCurrentPage.moveEndCursor(paragraphIndex, wordIndex, charIndex)
            myPreviousPage.reset()
            myNextPage.reset()
            preparePaintInfo(myCurrentPage)
            if (myCurrentPage.isEmptyPage()) {
                turnPage(false, ScrollingMode.NO_OVERLAPPING, 0)
            }
        }
    }

    @Synchronized
    fun gotoPosition(start: ZLTextPosition, end: ZLTextPosition?) {
        Application.getViewWidget()!!.reset()
        myCurrentPage.StartCursor.moveTo(start)
        if (end == null) myCurrentPage.EndCursor.reset()
        else myCurrentPage.EndCursor.moveTo(end)
        myCurrentPage.PaintState = PaintStateEnum.START_IS_KNOWN
        myPreviousPage.reset()
        myNextPage.reset()
        preparePaintInfo(myCurrentPage)
    }

    @Synchronized
    fun preparePaintInfo() {
        myPreviousPage.reset()
        myNextPage.reset()
        preparePaintInfo(myCurrentPage)
    }

    @Synchronized
    private fun preparePaintInfo(page: ZLTextPage) {
        page.setSize(
            getTextColumnWidth(),
            getTextAreaHeight(),
            twoColumnView(),
            page == myPreviousPage
        )

        if (page.PaintState == PaintStateEnum.NOTHING_TO_PAINT || page.PaintState == PaintStateEnum.READY) {
            return
        }
        val oldState = page.PaintState

        val cache = myLineInfoCache
        for (info in page.LineInfos) {
            cache.put(info, info)
        }

        when (page.PaintState) {
            PaintStateEnum.TO_SCROLL_FORWARD -> if (!page.EndCursor.isEndOfText()) {
                val startCursor = ZLTextWordCursor()
                when (myScrollingMode) {
                    ScrollingMode.NO_OVERLAPPING -> {}
                    ScrollingMode.KEEP_LINES -> page.findLineFromEnd(
                        startCursor,
                        myOverlappingValue
                    )

                    ScrollingMode.SCROLL_LINES -> {
                        page.findLineFromStart(startCursor, myOverlappingValue)
                        if (startCursor.isEndOfParagraph) {
                            startCursor.nextParagraph()
                        }
                    }

                    ScrollingMode.SCROLL_PERCENTAGE -> page.findPercentFromStart(
                        startCursor,
                        myOverlappingValue
                    )
                }

                if (!startCursor.isNull() && startCursor.samePositionAs(page.StartCursor)) {
                    page.findLineFromStart(startCursor, 1)
                }

                var handled = false
                if (!startCursor.isNull()) {
                    val endCursor = ZLTextWordCursor()
                    buildInfos(page, startCursor, endCursor)
                    if (!page.isEmptyPage() && (myScrollingMode != ScrollingMode.KEEP_LINES || !endCursor.samePositionAs(
                            page.EndCursor
                        ))
                    ) {
                        page.StartCursor.setCursor(startCursor)
                        page.EndCursor.setCursor(endCursor)
                        handled = true
                    }
                }

                if (!handled) {
                    page.StartCursor.setCursor(page.EndCursor)
                    buildInfos(page, page.StartCursor, page.EndCursor)
                }
            }

            PaintStateEnum.TO_SCROLL_BACKWARD -> if (!page.StartCursor.isStartOfText()) {
                when (myScrollingMode) {
                    ScrollingMode.NO_OVERLAPPING -> page.StartCursor.setCursor(
                        findStartOfPrevousPage(page, page.StartCursor)
                    )

                    ScrollingMode.KEEP_LINES -> {
                        val endCursor = ZLTextWordCursor()
                        page.findLineFromStart(endCursor, myOverlappingValue)
                        if (!endCursor.isNull() && endCursor.samePositionAs(page.EndCursor)) {
                            page.findLineFromEnd(endCursor, 1)
                        }
                        if (!endCursor.isNull()) {
                            val startCursor = findStartOfPrevousPage(page, endCursor)
                            if (startCursor.samePositionAs(page.StartCursor)) {
                                page.StartCursor.setCursor(
                                    findStartOfPrevousPage(
                                        page,
                                        page.StartCursor
                                    )
                                )
                            } else {
                                page.StartCursor.setCursor(startCursor)
                            }
                        } else {
                            page.StartCursor.setCursor(
                                findStartOfPrevousPage(
                                    page,
                                    page.StartCursor
                                )
                            )
                        }
                    }

                    ScrollingMode.SCROLL_LINES -> page.StartCursor.setCursor(
                        findStart(
                            page,
                            page.StartCursor,
                            SizeUnit.Companion.LINE_UNIT,
                            myOverlappingValue
                        )
                    )

                    ScrollingMode.SCROLL_PERCENTAGE -> page.StartCursor.setCursor(
                        findStart(
                            page,
                            page.StartCursor,
                            SizeUnit.Companion.PIXEL_UNIT,
                            page.getTextHeight() * myOverlappingValue / 100
                        )
                    )
                }
                buildInfos(page, page.StartCursor, page.EndCursor)
                if (page.isEmptyPage()) {
                    page.StartCursor.setCursor(
                        findStart(
                            page,
                            page.StartCursor,
                            SizeUnit.Companion.LINE_UNIT,
                            1
                        )
                    )
                    buildInfos(page, page.StartCursor, page.EndCursor)
                }
            }

            PaintStateEnum.START_IS_KNOWN -> if (!page.StartCursor.isNull()) {
                buildInfos(page, page.StartCursor, page.EndCursor)
            }

            PaintStateEnum.END_IS_KNOWN -> if (!page.EndCursor.isNull()) {
                page.StartCursor.setCursor(findStartOfPrevousPage(page, page.EndCursor))
                buildInfos(page, page.StartCursor, page.EndCursor)
            }

            else -> {}
        }
        page.PaintState = PaintStateEnum.READY
        // TODO: cache?
        myLineInfoCache.clear()

        if (page == myCurrentPage) {
            if (oldState != PaintStateEnum.START_IS_KNOWN) {
                myPreviousPage.reset()
            }
            if (oldState != PaintStateEnum.END_IS_KNOWN) {
                myNextPage.reset()
            }
        }
    }

    fun clearCaches() {
        resetMetrics()
        rebuildPaintInfo()
        Application.getViewWidget()!!.reset()
        myCharWidth = -1f
    }

    @Synchronized
    protected fun rebuildPaintInfo() {
        myPreviousPage.reset()
        myNextPage.reset()
        if (myCursorManager != null) {
            myCursorManager!!.evictAll()
        }

        if (myCurrentPage.PaintState != PaintStateEnum.NOTHING_TO_PAINT) {
            myCurrentPage.LineInfos.clear()
            if (!myCurrentPage.StartCursor.isNull()) {
                myCurrentPage.StartCursor.rebuild()
                myCurrentPage.EndCursor.reset()
                myCurrentPage.PaintState = PaintStateEnum.START_IS_KNOWN
            } else if (!myCurrentPage.EndCursor.isNull()) {
                myCurrentPage.EndCursor.rebuild()
                myCurrentPage.StartCursor.reset()
                myCurrentPage.PaintState = PaintStateEnum.END_IS_KNOWN
            }
        }

        myLineInfoCache.clear()
    }

    private fun infoSize(info: ZLTextLineInfo, unit: Int): Int {
        return if (unit == SizeUnit.Companion.PIXEL_UNIT) (info.Height + info.Descent + info.VSpaceAfter) else (if (info.IsVisible) 1 else 0)
    }

    private fun paragraphSize(
        page: ZLTextPage,
        cursor: ZLTextWordCursor,
        beforeCurrentPosition: Boolean,
        unit: Int
    ): ParagraphSize {
        val size = ParagraphSize()

        val paragraphCursor = cursor.getParagraphCursor()
        if (paragraphCursor == null) {
            return size
        }
        val endElementIndex =
            if (beforeCurrentPosition) cursor.elementIndex else paragraphCursor.getParagraphLength()

        resetTextStyle()

        var wordIndex = 0
        var charIndex = 0
        var info: ZLTextLineInfo? = null
        while (wordIndex != endElementIndex) {
            val prev = info
            info =
                processTextLine(page, paragraphCursor, wordIndex, charIndex, endElementIndex, prev)
            wordIndex = info.EndElementIndex
            charIndex = info.EndCharIndex
            size.Height += infoSize(info, unit)
            if (prev == null) {
                size.TopMargin = info.VSpaceBefore
            }
            size.BottomMargin = info.VSpaceAfter
        }

        return size
    }

    private fun skip(page: ZLTextPage, cursor: ZLTextWordCursor, unit: Int, size: Int) {
        var size = size
        val paragraphCursor = cursor.getParagraphCursor()
        if (paragraphCursor == null) {
            return
        }
        val endElementIndex = paragraphCursor.getParagraphLength()

        resetTextStyle()
        applyStyleChanges(paragraphCursor, 0, cursor.elementIndex)

        var info: ZLTextLineInfo? = null
        while (!cursor.isEndOfParagraph && size > 0) {
            info = processTextLine(
                page,
                paragraphCursor,
                cursor.elementIndex,
                cursor.charIndex,
                endElementIndex,
                info
            )
            cursor.moveTo(info.EndElementIndex, info.EndCharIndex)
            size -= infoSize(info, unit)
        }
    }

    private fun findStartOfPrevousPage(page: ZLTextPage, end: ZLTextWordCursor): ZLTextWordCursor {
        var end = end
        if (twoColumnView()) {
            end = findStart(page, end, SizeUnit.Companion.PIXEL_UNIT, page.getTextHeight())
        }
        end = findStart(page, end, SizeUnit.Companion.PIXEL_UNIT, page.getTextHeight())
        return end
    }

    private fun findStart(
        page: ZLTextPage,
        end: ZLTextWordCursor,
        unit: Int,
        height: Int
    ): ZLTextWordCursor {
        var height = height
        val start = ZLTextWordCursor(end)
        var size = paragraphSize(page, start, true, unit)
        height -= size.Height
        var positionChanged = !start.isStartOfParagraph()
        start.moveToParagraphStart()
        while (height > 0) {
            val previousSize = size
            if (positionChanged && start.getParagraphCursor()!!.isEndOfSection()) {
                break
            }
            if (!start.previousParagraph()) {
                break
            }
            if (!start.getParagraphCursor()!!.isEndOfSection()) {
                positionChanged = true
            }
            size = paragraphSize(page, start, false, unit)
            height -= size.Height
            if (previousSize != null) {
                height += min(size.BottomMargin, previousSize.TopMargin)
            }
        }
        skip(page, start, unit, -height)

        if (unit == SizeUnit.Companion.PIXEL_UNIT) {
            var sameStart = start.samePositionAs(end)
            if (!sameStart && start.isEndOfParagraph && end.isStartOfParagraph()) {
                val startCopy = ZLTextWordCursor(start)
                startCopy.nextParagraph()
                sameStart = startCopy.samePositionAs(end)
            }
            if (sameStart) {
                start.setCursor(findStart(page, end, SizeUnit.Companion.LINE_UNIT, 1))
            }
        }

        return start
    }

    protected fun getElementByCoordinates(x: Int, y: Int): ZLTextElementArea? {
        return myCurrentPage.TextElementMap.binarySearch(x, y)
    }

    fun outlineRegion(region: ZLTextRegion?) {
        outlineRegion(if (region != null) region.soul else null)
    }

    fun outlineRegion(soul: Soul?) {
        myShowOutline = true
        myOutlinedRegionSoul = soul
    }

    open fun hideOutline() {
        myShowOutline = false
        Application.getViewWidget()!!.reset()
    }

    private fun getOutlinedRegion(page: ZLTextPage): ZLTextRegion? {
        return page.TextElementMap.getRegion(myOutlinedRegionSoul)
    }

    val outlinedRegion: ZLTextRegion?
        get() = getOutlinedRegion(myCurrentPage)

    protected fun findHighlighting(x: Int, y: Int, maxDistance: Int): ZLTextHighlighting? {
        val region = findRegion(x, y, maxDistance, ZLTextRegion.AnyRegionFilter)
        if (region == null) {
            return null
        }
        synchronized(myHighlightings) {
            for (h in myHighlightings) {
                if (h.getBackgroundColor() != null && h.intersects(region)) {
                    return h
                }
            }
        }
        return null
    }

    fun findRegion(x: Int, y: Int, filter: ZLTextRegion.Filter): ZLTextRegion? {
        return findRegion(x, y, Int.MAX_VALUE - 1, filter)
    }

    open fun findRegion(
        x: Int,
        y: Int,
        maxDistance: Int,
        filter: ZLTextRegion.Filter
    ): ZLTextRegion? {
        return myCurrentPage.TextElementMap.findRegion(x, y, maxDistance, filter)
    }

    fun findRegionsPair(x: Int, y: Int, filter: ZLTextRegion.Filter): RegionPair {
        return myCurrentPage.TextElementMap.findRegionsPair(x, y, getColumnIndex(x), filter)
    }

    /*
	public void resetRegionPointer() {
		myOutlinedRegionSoul = null;
		myShowOutline = true;
	}
*/
    protected fun initSelection(x: Int, y: Int): Boolean {
        var y = y
        y -= getTextStyleCollection().baseStyle.getFontSize() / 2
        if (!mySelection.start(x, y)) {
            return false
        }
        Application.getViewWidget()!!.reset()
        Application.getViewWidget()!!.repaint()
        return true
    }

    fun clearSelection() {
        if (mySelection.clear()) {
            Application.getViewWidget()!!.reset()
            Application.getViewWidget()!!.repaint()
        }
    }

    val selectionHighlighting: ZLTextHighlighting
        get() = mySelection

    open val selectionStartY: Int
        get() {
            if (mySelection.isEmpty()) {
                return 0
            }
            val selectionStartArea = mySelection.getStartArea(myCurrentPage)
            if (selectionStartArea != null) {
                return selectionStartArea.YStart
            }
            if (mySelection.hasPartBeforePage(myCurrentPage)) {
                val firstArea = myCurrentPage.TextElementMap.getFirstArea()
                return if (firstArea != null) firstArea.YStart else 0
            } else {
                val lastArea = myCurrentPage.TextElementMap.getLastArea()
                return if (lastArea != null) lastArea.YEnd else 0
            }
        }

    open val selectionEndY: Int
        get() {
            if (mySelection.isEmpty()) {
                return 0
            }
            val selectionEndArea = mySelection.getEndArea(myCurrentPage)
            if (selectionEndArea != null) {
                return selectionEndArea.YEnd
            }
            if (mySelection.hasPartAfterPage(myCurrentPage)) {
                val lastArea = myCurrentPage.TextElementMap.getLastArea()
                return if (lastArea != null) lastArea.YEnd else 0
            } else {
                val firstArea = myCurrentPage.TextElementMap.getFirstArea()
                return if (firstArea != null) firstArea.YStart else 0
            }
        }

    val selectionStartPosition: ZLTextPosition?
        get() = mySelection.getStartPosition()

    val selectionEndPosition: ZLTextPosition?
        get() = mySelection.getEndPosition()

    val isSelectionEmpty: Boolean
        get() = mySelection.isEmpty()

    fun nextRegion(direction: ZLViewEnums.Direction, filter: ZLTextRegion.Filter): ZLTextRegion? {
        return myCurrentPage.TextElementMap.nextRegion(this.outlinedRegion, direction, filter)
    }

    public override fun canScroll(index: PageIndex): Boolean {
        when (index) {
            PageIndex.next -> {
                val cursor = this.endCursor
                return cursor != null && !cursor.isNull() && !cursor.isEndOfText()
            }

            PageIndex.previous -> {
                val cursor = this.startCursor
                return cursor != null && !cursor.isNull() && !cursor.isStartOfText()
            }

            else -> return true
        }
    }

    fun cursor(index: Int): ZLTextParagraphCursor? {
        return myCursorManager!!.get(index)
    }

    protected abstract val extensionManager: ExtensionElementManager?

    interface ScrollingMode {
        companion object {
            const val NO_OVERLAPPING: Int = 0
            const val KEEP_LINES: Int = 1
            const val SCROLL_LINES: Int = 2
            const val SCROLL_PERCENTAGE: Int = 3
        }
    }

    private interface SizeUnit {
        companion object {
            const val PIXEL_UNIT: Int = 0
            const val LINE_UNIT: Int = 1
        }
    }

    class PagePosition(@JvmField val Current: Int, @JvmField val Total: Int)

    private class ParagraphSize {
        var Height: Int = 0
        var TopMargin: Int = 0
        var BottomMargin: Int = 0
    }

    companion object {
        const val SCROLLBAR_HIDE: Int = 0
        const val SCROLLBAR_SHOW: Int = 1
        const val SCROLLBAR_SHOW_AS_PROGRESS: Int = 2
        private val ourDefaultLetters =
            "System developers have used modeling languages for decades to specify, visualize, construct, and document systems. The Unified Modeling Language (UML) is one of those languages. UML makes it possible for team members to collaborate by providing a common language that applies to a multitude of different systems. Essentially, it enables you to communicate solutions in a consistent, tool-supported language.".toCharArray()
        private val SPACE = charArrayOf(' ')
        fun isRtl(rtlMode: Boolean, s: String): Boolean {
            if (isRtl(s)) return true
            if (rtlMode && isLtr(s)) return false
            if (!rtlMode) return false
            return true
        }

        fun isRtl(s: String): Boolean {
            for (c in s.toCharArray()) {
                when (Character.getDirectionality(c)) {
                    Character.DIRECTIONALITY_RIGHT_TO_LEFT, Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC, Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING, Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE -> return true
                }
            }
            return false
        }

        fun isLtr(s: String): Boolean {
            for (c in s.toCharArray()) {
                when (Character.getDirectionality(c)) {
                    Character.DIRECTIONALITY_LEFT_TO_RIGHT, Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING, Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE -> return true
                }
            }
            return false
        }
    }
}
