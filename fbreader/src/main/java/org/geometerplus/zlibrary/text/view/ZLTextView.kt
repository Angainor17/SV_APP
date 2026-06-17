package org.geometerplus.zlibrary.text.view

import org.geometerplus.zlibrary.core.application.ZLApplication
import org.geometerplus.zlibrary.core.util.RationalNumber
import org.geometerplus.zlibrary.core.util.ZLColor
import org.geometerplus.zlibrary.core.view.Hull
import org.geometerplus.zlibrary.core.view.SelectionCursor
import org.geometerplus.zlibrary.core.view.ZLPaintContext
import org.geometerplus.zlibrary.core.view.ZLViewEnums.Direction
import org.geometerplus.zlibrary.core.view.ZLViewEnums.PageIndex
import org.geometerplus.zlibrary.text.hyphenation.ZLTextHyphenationInfo
import org.geometerplus.zlibrary.text.hyphenation.ZLTextHyphenator
import org.geometerplus.zlibrary.text.model.ZLTextAlignmentType
import org.geometerplus.zlibrary.text.model.ZLTextMark
import org.geometerplus.zlibrary.text.model.ZLTextModel
import org.geometerplus.zlibrary.text.model.ZLTextParagraph

abstract class ZLTextView(application: ZLApplication) : ZLTextViewBase(application) {

    companion object {
        const val SCROLLBAR_HIDE = 0
        const val SCROLLBAR_SHOW = 1
        const val SCROLLBAR_SHOW_AS_PROGRESS = 2

        private val DEFAULT_LETTERS = "System developers have used modeling languages for decades to specify, visualize, construct, and document systems. The Unified Modeling Language (UML) is one of those languages. UML makes it possible for team members to collaborate by providing a common language that applies to a multitude of different systems. Essentially, it enables you to communicate solutions in a consistent, tool-supported language.".toCharArray()
        private val SPACE = charArrayOf(' ')

        @JvmStatic
        fun isRtl(rtlMode: Boolean, s: String): Boolean {
            if (isRtl(s)) return true
            if (rtlMode && isLtr(s)) return false
            return rtlMode
        }

        @JvmStatic
        fun isRtl(s: String): Boolean {
            for (c in s.toCharArray()) {
                when (Character.getDirectionality(c)) {
                    Character.DIRECTIONALITY_RIGHT_TO_LEFT,
                    Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC,
                    Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING,
                    Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE -> return true
                }
            }
            return false
        }

        @JvmStatic
        fun isLtr(s: String): Boolean {
            for (c in s.toCharArray()) {
                when (Character.getDirectionality(c)) {
                    Character.DIRECTIONALITY_LEFT_TO_RIGHT,
                    Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING,
                    Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE -> return true
                }
            }
            return false
        }
    }

    @JvmField val selection = ZLTextSelection(this)
    private val lineInfoCache = HashMap<ZLTextLineInfo, ZLTextLineInfo>()
    private val highlightings = java.util.Collections.synchronizedSet(java.util.TreeSet<ZLTextHighlighting>())
    private val lettersBuffer = CharArray(512)

    @JvmField var currentPage = ZLTextPage()
    var rtlDetected = false
    var rtlMode = false

    private var model: ZLTextModel? = null
    private var scrollingMode = 0
    private var overlappingValue = 0
    private var previousPage = ZLTextPage()
    private var nextPage = ZLTextPage()
    private var outlinedRegionSoul: ZLTextRegion.Soul? = null
    private var showOutline = true
    private var cursorManager: CursorManager? = null
    private var lettersBufferLength = 0
    private var lettersModel: ZLTextModel? = null
    private var charWidth = -1f
    @Volatile private var cachedWord: ZLTextWord? = null
    @Volatile private var cachedInfo: ZLTextHyphenationInfo? = null

    val modelValue: ZLTextModel?
        get() = model

    fun getModel(): ZLTextModel? = model

    @Synchronized
    open fun setModel(model: ZLTextModel?) {
        cursorManager = model?.let { CursorManager(it, extensionManager!!) }

        selection.clear()
        highlightings.clear()

        this.model = model
        currentPage.reset()
        previousPage.reset()
        nextPage.reset()
        if (model != null) {
            val paragraphsNumber = model.paragraphsNumber
            if (paragraphsNumber > 0) {
                currentPage.moveStartCursor(cursorManager!!.get(0)!!)
            }
            model.language?.let { ln ->
                rtlMode = ln.startsWith("ar") || ln.startsWith("ps") ||
                          ln.startsWith("fa") || ln.startsWith("iw") || ln.startsWith("he")
            }
            rtlDetected = false
        }
        Application.viewWidget.reset()
    }

    fun getStartCursor(): ZLTextWordCursor {
        if (currentPage.startCursor.isNull) {
            preparePaintInfo(currentPage)
        }
        return currentPage.startCursor
    }

    fun getEndCursor(): ZLTextWordCursor {
        if (currentPage.endCursor.isNull) {
            preparePaintInfo(currentPage)
        }
        return currentPage.endCursor
    }

    @Synchronized
    private fun gotoMark(mark: ZLTextMark?) {
        if (mark == null) return

        previousPage.reset()
        nextPage.reset()
        var doRepaint = false
        if (currentPage.startCursor.isNull) {
            doRepaint = true
            preparePaintInfo(currentPage)
        }
        if (currentPage.startCursor.isNull) return
        if (currentPage.startCursor.paragraphIndex != mark.paragraphIndex ||
            currentPage.startCursor.getMark()?.compareTo(mark) ?: 0 > 0
        ) {
            doRepaint = true
            gotoPosition(mark.paragraphIndex, 0, 0)
            preparePaintInfo(currentPage)
        }
        if (currentPage.endCursor.isNull) {
            preparePaintInfo(currentPage)
        }
        while (mark.compareTo(currentPage.endCursor.getMark()!!) > 0) {
            doRepaint = true
            turnPage(true, ScrollingMode.NO_OVERLAPPING, 0)
            preparePaintInfo(currentPage)
        }
        if (doRepaint) {
            if (currentPage.startCursor.isNull) {
                preparePaintInfo(currentPage)
            }
            Application.viewWidget.reset()
            Application.viewWidget.repaint()
        }
    }

    @Synchronized
    fun gotoHighlighting(highlighting: ZLTextHighlighting) {
        previousPage.reset()
        nextPage.reset()
        var doRepaint = false
        if (currentPage.startCursor.isNull) {
            doRepaint = true
            preparePaintInfo(currentPage)
        }
        if (currentPage.startCursor.isNull) return
        if (!highlighting.intersects(currentPage)) {
            gotoPosition(highlighting.getStartPosition().paragraphIndex, 0, 0)
            preparePaintInfo(currentPage)
        }
        if (currentPage.endCursor.isNull) {
            preparePaintInfo(currentPage)
        }
        while (!highlighting.intersects(currentPage)) {
            doRepaint = true
            turnPage(true, ScrollingMode.NO_OVERLAPPING, 0)
            preparePaintInfo(currentPage)
        }
        if (doRepaint) {
            if (currentPage.startCursor.isNull) {
                preparePaintInfo(currentPage)
            }
            Application.viewWidget.reset()
            Application.viewWidget.repaint()
        }
    }

    @Synchronized
    fun search(text: String, ignoreCase: Boolean, wholeText: Boolean, backward: Boolean, thisSectionOnly: Boolean): Int {
        if (model == null || text.isEmpty()) return 0
        val count = model!!.search(text, 0, model!!.paragraphsNumber, ignoreCase)
        previousPage.reset()
        nextPage.reset()
        if (!currentPage.startCursor.isNull) {
            rebuildPaintInfo()
            if (count > 0) {
                val mark = currentPage.startCursor.getMark()
                gotoMark(
                    if (wholeText) {
                        if (backward) model!!.lastMark else model!!.firstMark
                    } else {
                        if (backward) model!!.getPreviousMark(mark) else model!!.getNextMark(mark)
                    }
                )
            }
            Application.viewWidget.reset()
            Application.viewWidget.repaint()
        }
        return count
    }

    fun canFindNext(): Boolean {
        val end = currentPage.endCursor
        return !end.isNull && model != null && model!!.getNextMark(end.getMark()) != null
    }

    @Synchronized
    fun findNext() {
        val end = currentPage.endCursor
        if (!end.isNull) {
            gotoMark(model!!.getNextMark(end.getMark()))
        }
    }

    fun canFindPrevious(): Boolean {
        val start = currentPage.startCursor
        return !start.isNull && model != null && model!!.getPreviousMark(start.getMark()) != null
    }

    @Synchronized
    fun findPrevious() {
        val start = currentPage.startCursor
        if (!start.isNull) {
            gotoMark(model!!.getPreviousMark(start.getMark()))
        }
    }

    fun clearFindResults() {
        if (!findResultsAreEmpty()) {
            model!!.removeAllMarks()
            rebuildPaintInfo()
            Application.viewWidget.reset()
            Application.viewWidget.repaint()
        }
    }

    fun findResultsAreEmpty(): Boolean = model == null || model!!.marks.isEmpty()

    override fun onScrollingFinished(pageIndex: PageIndex) {
        when (pageIndex) {
            PageIndex.current -> {}
            PageIndex.previous -> {
                val swap = nextPage
                nextPage = currentPage
                currentPage = previousPage
                previousPage = swap
                previousPage.reset()
                if (currentPage.paintState == PaintStateEnum.NOTHING_TO_PAINT) {
                    preparePaintInfo(nextPage)
                    currentPage.endCursor.setCursor(nextPage.startCursor)
                    currentPage.paintState = PaintStateEnum.END_IS_KNOWN
                } else if (!currentPage.endCursor.isNull &&
                    !nextPage.startCursor.isNull &&
                    !currentPage.endCursor.samePositionAs(nextPage.startCursor)
                ) {
                    nextPage.reset()
                    nextPage.startCursor.setCursor(currentPage.endCursor)
                    nextPage.paintState = PaintStateEnum.START_IS_KNOWN
                    Application.viewWidget.reset()
                }
            }
            PageIndex.next -> {
                val swap = previousPage
                previousPage = currentPage
                currentPage = nextPage
                nextPage = swap
                nextPage.reset()
                when (currentPage.paintState) {
                    PaintStateEnum.NOTHING_TO_PAINT -> {
                        preparePaintInfo(previousPage)
                        currentPage.startCursor.setCursor(previousPage.endCursor)
                        currentPage.paintState = PaintStateEnum.START_IS_KNOWN
                    }
                    PaintStateEnum.READY -> {
                        nextPage.startCursor.setCursor(currentPage.endCursor)
                        nextPage.paintState = PaintStateEnum.START_IS_KNOWN
                    }
                }
            }
        }
    }

    fun removeHighlightings(type: Class<out ZLTextHighlighting>): Boolean {
        var result = false
        synchronized(highlightings) {
            val it = highlightings.iterator()
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

    fun addHighlighting(h: ZLTextHighlighting) {
        highlightings.add(h)
        Application.viewWidget.reset()
        Application.viewWidget.repaint()
    }

    fun addHighlightings(hilites: Collection<ZLTextHighlighting>) {
        highlightings.addAll(hilites)
        Application.viewWidget.reset()
        Application.viewWidget.repaint()
    }

    fun clearHighlighting() {
        if (removeHighlightings(ZLTextManualHighlighting::class.java)) {
            Application.viewWidget.reset()
            Application.viewWidget.repaint()
        }
    }

    protected fun moveSelectionCursorTo(which: SelectionCursor.Which, x: Int, y: Int) {
        val adjustedY = y - textStyleCollection.getBaseStyle().fontSize / 2
        selection.setCursorInMovement(which, x, adjustedY)
        selection.expandTo(currentPage, x, adjustedY)
        Application.viewWidget.reset()
        Application.viewWidget.repaint()
    }

    protected open fun releaseSelectionCursor() {
        selection.stop()
        Application.viewWidget.reset()
        Application.viewWidget.repaint()
    }

    protected fun getSelectionCursorInMovement(): SelectionCursor.Which? = selection.getCursorInMovement()

    private fun getSelectionCursorPoint(page: ZLTextPage, which: SelectionCursor.Which?): ZLTextSelection.Point? {
        if (which == null) return null

        if (which == selection.getCursorInMovement()) {
            return selection.getCursorInMovementPoint()
        }

        if (which == SelectionCursor.Which.Left) {
            if (selection.hasPartBeforePage(page)) return null
            val area = selection.getStartArea(page)
            if (area != null) {
                return ZLTextSelection.Point(if (rtlMode) area.xEnd else area.xStart, (area.yStart + area.yEnd) / 2)
            }
        } else {
            if (selection.hasPartAfterPage(page)) return null
            val area = selection.getEndArea(page)
            if (area != null) {
                return ZLTextSelection.Point(if (rtlMode) area.xStart else area.xEnd, (area.yStart + area.yEnd) / 2)
            }
        }
        return null
    }

    private fun distance2ToCursor(x: Int, y: Int, which: SelectionCursor.Which): Float {
        val point = getSelectionCursorPoint(currentPage, which) ?: return Float.MAX_VALUE
        val dX = (x - point.x).toFloat()
        val dY = (y - point.y).toFloat()
        return dX * dX + dY * dY
    }

    protected fun findSelectionCursor(x: Int, y: Int): SelectionCursor.Which? =
        findSelectionCursor(x, y, Float.MAX_VALUE)

    protected fun findSelectionCursor(x: Int, y: Int, maxDistance2: Float): SelectionCursor.Which? {
        if (selection.isEmpty()) return null

        val leftDistance2 = distance2ToCursor(x, y, SelectionCursor.Which.Left)
        val rightDistance2 = distance2ToCursor(x, y, SelectionCursor.Which.Right)

        return if (rightDistance2 < leftDistance2) {
            if (rightDistance2 <= maxDistance2) SelectionCursor.Which.Right else null
        } else {
            if (leftDistance2 <= maxDistance2) SelectionCursor.Which.Left else null
        }
    }

    fun drawSelectionCursor(context: ZLPaintContext, page: ZLTextPage, which: SelectionCursor.Which) {
        val pt = getSelectionCursorPoint(page, which)
        if (pt != null) {
            SelectionCursor.draw(context, which, pt.x, pt.y, selectionBackgroundColor)
        }
    }

    @Synchronized
    override fun preparePage(context: ZLPaintContext, pageIndex: PageIndex) {
        setContext(context)
        preparePaintInfo(getPage(pageIndex))
    }

    @Synchronized
    override fun paint(context: ZLPaintContext, pageIndex: PageIndex) {
        setContext(context)
        wallpaperFile?.let { wallpaper ->
            context.clear(wallpaper, fillMode)
        } ?: context.clear(backgroundColor)

        if (model == null || model!!.paragraphsNumber == 0) return

        val page = when (pageIndex) {
            PageIndex.previous -> {
                if (previousPage.paintState == PaintStateEnum.NOTHING_TO_PAINT) {
                    preparePaintInfo(currentPage)
                    previousPage.endCursor.setCursor(currentPage.startCursor)
                    previousPage.paintState = PaintStateEnum.END_IS_KNOWN
                }
                previousPage
            }
            PageIndex.next -> {
                if (nextPage.paintState == PaintStateEnum.NOTHING_TO_PAINT) {
                    preparePaintInfo(currentPage)
                    nextPage.startCursor.setCursor(currentPage.endCursor)
                    nextPage.paintState = PaintStateEnum.START_IS_KNOWN
                }
                nextPage
            }
            else -> currentPage
        }

        page.textElementMap.clear()
        preparePaintInfo(page)

        if (page.startCursor.isNull || page.endCursor.isNull) return

        val lineInfos = page.lineInfos
        val labels = IntArray(lineInfos.size + 1)
        var x = if (rtlMode) {
            ((if (page.twoColumnView()) page.getTextWidth() * 2 + spaceBetweenColumns else page.getTextWidth()) + leftMargin)
        } else {
            leftMargin
        }
        var y = topMargin + page.topMargin
        var index = 0
        var columnIndex = 0
        var previousInfo: ZLTextLineInfo? = null
        for (info in lineInfos) {
            info.adjust(previousInfo)
            val first = page.textElementMap.size()
            prepareTextLine(page, info, x, y, columnIndex)
            rtlDetected = rtlDetected or page.textElementMap.swapRtl(rtlMode, first, page.textElementMap.size())
            y += info.height + info.descent + info.vSpaceAfter
            labels[++index] = page.textElementMap.size()
            if (index == page.column0Height) {
                y = topMargin + page.topMargin
                x += (page.getTextWidth() + spaceBetweenColumns) * if (rtlMode) -1 else 1
                columnIndex = 1
            }
            previousInfo = info
        }

        val hilites = findHilites(page)

        x = if (rtlMode) {
            ((if (page.twoColumnView()) page.getTextWidth() * 2 + spaceBetweenColumns else page.getTextWidth()) + leftMargin)
        } else {
            leftMargin
        }
        y = topMargin + page.topMargin
        index = 0
        for (info in lineInfos) {
            drawTextLine(page, hilites, info, labels[index], labels[index + 1])
            y += info.height + info.descent + info.vSpaceAfter
            ++index
            if (index == page.column0Height) {
                y = topMargin + page.topMargin
                x += (page.getTextWidth() + spaceBetweenColumns) * if (rtlMode) -1 else 1
            }
        }

        for (h in hilites) {
            var mode = Hull.DrawMode.None

            h.getBackgroundColor()?.let { bgColor ->
                context.setFillColor(bgColor, 128)
                mode = mode or Hull.DrawMode.Fill
            }

            h.getOutlineColor()?.let { outlineColor ->
                context.setLineColor(outlineColor)
                mode = mode or Hull.DrawMode.Outline
            }

            if (mode != Hull.DrawMode.None) {
                h.hull(page).draw(context, mode)
            }
        }

        getOutlinedRegion(page)?.let { outlinedElementRegion ->
            if (showOutline) {
                context.setLineColor(selectionBackgroundColor)
                outlinedElementRegion.hull().draw(context, Hull.DrawMode.Outline)
            }
        }

        drawSelectionCursor(context, page, SelectionCursor.Which.Left)
        drawSelectionCursor(context, page, SelectionCursor.Which.Right)
    }

    private fun getPage(pageIndex: PageIndex): ZLTextPage = when (pageIndex) {
        PageIndex.previous -> previousPage
        PageIndex.next -> nextPage
        else -> currentPage
    }

    abstract fun scrollbarType(): Int

    override fun isScrollbarShown(): Boolean =
        scrollbarType() == SCROLLBAR_SHOW || scrollbarType() == SCROLLBAR_SHOW_AS_PROGRESS

    @Synchronized
    protected fun sizeOfTextBeforeParagraph(paragraphIndex: Int): Int =
        model?.getTextLength(paragraphIndex - 1) ?: 0

    @Synchronized
    protected fun sizeOfFullText(): Int {
        if (model == null || model!!.paragraphsNumber == 0) return 1
        return model!!.getTextLength(model!!.paragraphsNumber - 1)
    }

    @Synchronized
    private fun getCurrentCharNumber(pageIndex: PageIndex, startNotEndOfPage: Boolean): Int {
        if (model == null || model!!.paragraphsNumber == 0) return 0
        val page = getPage(pageIndex)
        preparePaintInfo(page)
        return if (startNotEndOfPage) {
            maxOf(0, sizeOfTextBeforeCursor(page.startCursor))
        } else {
            var end = sizeOfTextBeforeCursor(page.endCursor)
            if (end == -1) {
                end = model!!.getTextLength(model!!.paragraphsNumber - 1) - 1
            }
            maxOf(1, end)
        }
    }

    @Synchronized
    override fun getScrollbarFullSize(): Int = sizeOfFullText()

    @Synchronized
    override fun getScrollbarThumbPosition(pageIndex: PageIndex): Int =
        if (scrollbarType() == SCROLLBAR_SHOW_AS_PROGRESS) 0 else getCurrentCharNumber(pageIndex, true)

    @Synchronized
    override fun getScrollbarThumbLength(pageIndex: PageIndex): Int {
        val start = if (scrollbarType() == SCROLLBAR_SHOW_AS_PROGRESS) 0 else getCurrentCharNumber(pageIndex, true)
        val end = getCurrentCharNumber(pageIndex, false)
        return maxOf(1, end - start)
    }

    private fun sizeOfTextBeforeCursor(wordCursor: ZLTextWordCursor): Int {
        val paragraphCursor = wordCursor.paragraphCursor ?: return -1
        val paragraphIndex = paragraphCursor.index
        var sizeOfText = model!!.getTextLength(paragraphIndex - 1)
        val paragraphLength = paragraphCursor.paragraphLength
        if (paragraphLength > 0) {
            sizeOfText += (model!!.getTextLength(paragraphIndex) - sizeOfText) * wordCursor.elementIndex / paragraphLength
        }
        return sizeOfText
    }

    @Synchronized
    private fun computeCharsPerPage(): Float {
        setTextStyle(textStyleCollection.getBaseStyle())

        val textWidth = textColumnWidth
        val textHeight = textAreaHeight

        val num = model!!.paragraphsNumber
        val totalTextSize = model!!.getTextLength(num - 1)
        val charsPerParagraph = totalTextSize.toFloat() / num

        val charWidth = computeCharWidth()

        val indentWidth = getElementWidth(ZLTextElement.Indent, 0)
        val effectiveWidth = textWidth - (indentWidth + 0.5f * textWidth) / charsPerParagraph
        val charsPerLine = minOf(effectiveWidth / charWidth, charsPerParagraph * 1.2f)

        val strHeight = getWordHeight() + context.descent
        val effectiveHeight = (textHeight - (getTextStyle().getSpaceBefore(metrics()) +
            getTextStyle().getSpaceAfter(metrics()) / 2) / charsPerParagraph).toInt()
        val linesPerPage = effectiveHeight / strHeight

        return charsPerLine * linesPerPage
    }

    @Synchronized
    private fun computeTextPageNumber(textSize: Int): Int {
        if (model == null || model!!.paragraphsNumber == 0) return 1

        val factor = 1.0f / computeCharsPerPage()
        val pages = textSize * factor
        return maxOf((pages + 1.0f - 0.5f * factor).toInt(), 1)
    }

    private fun computeCharWidth(): Float {
        if (lettersModel != model) {
            lettersModel = model
            lettersBufferLength = 0
            charWidth = -1f

            var paragraph = 0
            val textSize = model!!.getTextLength(model!!.paragraphsNumber - 1)
            if (textSize > lettersBuffer.size) {
                paragraph = model!!.findParagraphByTextLength((textSize - lettersBuffer.size) / 2)
            }
            while (paragraph < model!!.paragraphsNumber && lettersBufferLength < lettersBuffer.size) {
                val it = model!!.getParagraph(paragraph++).iterator()
                while (lettersBufferLength < lettersBuffer.size && it.next()) {
                    if (it.type == ZLTextParagraph.Entry.TEXT) {
                        val len = minOf(it.textLength, lettersBuffer.size - lettersBufferLength)
                        System.arraycopy(it.textData, it.textOffset, lettersBuffer, lettersBufferLength, len)
                        lettersBufferLength += len
                    }
                }
            }

            if (lettersBufferLength == 0) {
                lettersBufferLength = minOf(lettersBuffer.size, DEFAULT_LETTERS.size)
                System.arraycopy(DEFAULT_LETTERS, 0, lettersBuffer, 0, lettersBufferLength)
            }
        }

        if (charWidth < 0f) {
            charWidth = computeCharWidth(lettersBuffer, lettersBufferLength)
        }
        return charWidth
    }

    private fun computeCharWidth(pattern: CharArray, length: Int): Float =
        context.getStringWidth(pattern, 0, length) / length.toFloat()

    @Synchronized
    open fun pagePosition(): PagePosition {
        var current = computeTextPageNumber(getCurrentCharNumber(PageIndex.current, false))
        var total = computeTextPageNumber(sizeOfFullText())

        if (total > 3) {
            return PagePosition(current, total)
        }

        preparePaintInfo(currentPage)
        var cursor = currentPage.startCursor
        if (cursor == null || cursor.isNull) {
            return PagePosition(current, total)
        }

        if (cursor.isStartOfText) {
            current = 1
        } else {
            var prevCursor = previousPage.startCursor
            if (prevCursor == null || prevCursor.isNull) {
                preparePaintInfo(previousPage)
                prevCursor = previousPage.startCursor
            }
            if (prevCursor != null && !prevCursor.isNull) {
                current = if (prevCursor.isStartOfText) 2 else 3
            }
        }

        total = current
        cursor = currentPage.endCursor
        if (cursor == null || cursor.isNull) {
            return PagePosition(current, total)
        }
        if (!cursor.isEndOfText) {
            var nextCursor = nextPage.endCursor
            if (nextCursor == null || nextCursor.isNull) {
                preparePaintInfo(nextPage)
                nextCursor = nextPage.endCursor
            }
            if (nextCursor != null) {
                total += if (nextCursor.isEndOfText) 1 else 2
            }
        }

        return PagePosition(current, total)
    }

    val progress: RationalNumber
        get() = pagePosition().let { RationalNumber.create(it.current.toLong(), it.total.toLong())!! }

    @Synchronized
    open fun gotoPage(page: Int) {
        if (model == null || model!!.paragraphsNumber == 0) return

        val factor = computeCharsPerPage()
        val textSize = page * factor

        var intTextSize = textSize.toInt()
        var paragraphIndex = model!!.findParagraphByTextLength(intTextSize)

        if (paragraphIndex > 0 && model!!.getTextLength(paragraphIndex) > intTextSize) {
            --paragraphIndex
        }
        intTextSize = model!!.getTextLength(paragraphIndex)

        var sizeOfTextBefore = model!!.getTextLength(paragraphIndex - 1)
        while (paragraphIndex > 0 && intTextSize == sizeOfTextBefore) {
            --paragraphIndex
            intTextSize = sizeOfTextBefore
            sizeOfTextBefore = model!!.getTextLength(paragraphIndex - 1)
        }

        val paragraphLength = intTextSize - sizeOfTextBefore

        val wordIndex = if (paragraphLength == 0) {
            0
        } else {
            preparePaintInfo(currentPage)
            val cursor = ZLTextWordCursor(currentPage.endCursor)
            cursor.moveToParagraph(paragraphIndex)
            cursor.paragraphCursor!!.paragraphLength
        }

        gotoPositionByEnd(paragraphIndex, wordIndex, 0)
    }

    open fun gotoHome() {
        val cursor = getStartCursor()
        if (!cursor.isNull && cursor.isStartOfParagraph && cursor.paragraphIndex == 0) return
        gotoPosition(0, 0, 0)
        preparePaintInfo()
    }

    private fun findHilites(page: ZLTextPage): List<ZLTextHighlighting> {
        val hilites = mutableListOf<ZLTextHighlighting>()
        if (selection.intersects(page)) {
            hilites.add(selection)
        }
        synchronized(highlightings) {
            for (h in highlightings) {
                if (h.intersects(page)) {
                    hilites.add(h)
                }
            }
        }
        return hilites
    }

    protected abstract fun getAdjustingModeForImages(): ZLPaintContext.ColorAdjustingMode

    private fun drawTextLine(page: ZLTextPage, hilites: List<ZLTextHighlighting>, info: ZLTextLineInfo, from: Int, to: Int) {
        val ctx = context
        val paragraph = info.paragraphCursor
        var index = from
        val endElementIndex = info.endElementIndex
        var charIndex = info.realStartCharIndex
        val pageAreas = page.textElementMap.areas()
        if (to > pageAreas.size) return

        for (wordIndex in info.realStartElementIndex until endElementIndex) {
            if (index >= to) break
            val element = paragraph.getElement(wordIndex) ?: continue
            val area = pageAreas[index]
            if (element == area.element) {
                ++index
                if (area.changeStyle) {
                    setTextStyle(area.style)
                }
                val areaX = area.xStart
                val areaY = area.yEnd - getElementDescent(element) - getTextStyle().getVerticalAlign(metrics())
                when (element) {
                    is ZLTextWord -> {
                        val pos = ZLTextFixedPosition(info.paragraphCursor.index, wordIndex, 0)
                        val hl = getWordHilite(pos, hilites)
                        val hlColor = hl?.getForegroundColor()
                        drawWord(areaX, areaY, element, charIndex, -1, false, hlColor ?: getTextColor(getTextStyle().hyperlink))
                    }
                    is ZLTextImageElement -> {
                        ctx.drawImage(areaX, areaY, element.ImageData, getTextAreaSize(), getScalingType(element), getAdjustingModeForImages())
                    }
                    is ZLTextVideoElement -> {
                        ctx.setLineColor(getTextColor(ZLTextHyperlink.NO_LINK))
                        ctx.setFillColor(ZLColor(127, 127, 127))
                        val xStart = area.xStart + 10
                        val xEnd = area.xEnd - 10
                        val yStart = area.yStart + 10
                        val yEnd = area.yEnd - 10
                        ctx.fillRectangle(xStart, yStart, xEnd, yEnd)
                        ctx.drawLine(xStart, yStart, xStart, yEnd)
                        ctx.drawLine(xStart, yEnd, xEnd, yEnd)
                        ctx.drawLine(xEnd, yEnd, xEnd, yStart)
                        ctx.drawLine(xEnd, yStart, xStart, yStart)
                        val l = xStart + (xEnd - xStart) * 7 / 16
                        val r = xStart + (xEnd - xStart) * 10 / 16
                        val t = yStart + (yEnd - yStart) * 2 / 6
                        val b = yStart + (yEnd - yStart) * 4 / 6
                        val c = yStart + (yEnd - yStart) / 2
                        ctx.setFillColor(ZLColor(196, 196, 196))
                        ctx.fillPolygon(intArrayOf(l, l, r), intArrayOf(t, b, c))
                    }
                    is ExtensionElement -> element.draw(ctx, area)
                    ZLTextElement.HSpace, ZLTextElement.NBSpace -> {
                        val cw = ctx.spaceWidth
                        for (len in 0 until area.xEnd - area.xStart step cw) {
                            ctx.drawString(areaX + len, areaY, SPACE, 0, 1)
                        }
                    }
                }
            }
            charIndex = 0
        }

        if (index != to) {
            val area = pageAreas[index++]
            if (area.changeStyle) {
                setTextStyle(area.style)
            }
            val start = if (info.startElementIndex == info.endElementIndex) info.startCharIndex else 0
            val len = info.endCharIndex - start
            val word = paragraph.getElement(info.endElementIndex) as ZLTextWord
            val pos = ZLTextFixedPosition(info.paragraphCursor.index, info.endElementIndex, 0)
            val hl = getWordHilite(pos, hilites)
            val hlColor = hl?.getForegroundColor()
            drawWord(
                area.xStart, area.yEnd - ctx.descent - getTextStyle().getVerticalAlign(metrics()),
                word, start, len, area.addHyphenationSign,
                hlColor ?: getTextColor(getTextStyle().hyperlink)
            )
        }
    }

    private fun getWordHilite(pos: ZLTextPosition, hilites: List<ZLTextHighlighting>): ZLTextHighlighting? {
        for (h in hilites) {
            if (h.getStartPosition().compareToIgnoreChar(pos) <= 0 && pos.compareToIgnoreChar(h.getEndPosition()) <= 0) {
                return h
            }
        }
        return null
    }

    private fun buildInfos(page: ZLTextPage, start: ZLTextWordCursor, result: ZLTextWordCursor) {
        val end = if (result.isNull || result.samePositionAs(start)) null else ZLTextFixedPosition(result)
        result.setCursor(start)
        var textAreaHeight = page.getTextHeight()
        page.lineInfos.clear()
        page.column0Height = 0
        var nextParagraph: Boolean
        var info: ZLTextLineInfo? = null
        do {
            val previousInfo = info
            resetTextStyle()
            val paragraphCursor = result.paragraphCursor!!
            val wordIndex = result.elementIndex
            applyStyleChanges(paragraphCursor, 0, wordIndex)
            val currentInfo = ZLTextLineInfo(paragraphCursor, wordIndex, result.charIndex, getTextStyle())
            info = currentInfo
            val endIndex = currentInfo.paragraphCursorLength
            while (currentInfo.endElementIndex != endIndex) {
                info = processTextLine(page, paragraphCursor, currentInfo.endElementIndex, currentInfo.endCharIndex, endIndex, previousInfo)
                textAreaHeight -= info!!.height + info!!.descent
                if (textAreaHeight < 0 && page.lineInfos.size > page.column0Height) {
                    if (page.column0Height == 0 && page.twoColumnView()) {
                        textAreaHeight = page.getTextHeight()
                        textAreaHeight -= info!!.height + info!!.descent
                        page.column0Height = page.lineInfos.size
                    } else {
                        break
                    }
                }
                textAreaHeight -= info!!.vSpaceAfter
                if (end != null && result.compareTo(end) >= 0) break
                result.moveTo(info.endElementIndex, info.endCharIndex)
                page.lineInfos.add(info)
                if (textAreaHeight < 0) {
                    if (page.column0Height == 0 && page.twoColumnView()) {
                        textAreaHeight = page.getTextHeight()
                        page.column0Height = page.lineInfos.size
                    } else {
                        break
                    }
                }
            }
            nextParagraph = result.isEndOfParagraph && result.nextParagraph()
            if (nextParagraph && result.paragraphCursor!!.isEndOfSection) {
                if (page.column0Height == 0 && page.twoColumnView() && page.lineInfos.isNotEmpty()) {
                    textAreaHeight = page.getTextHeight()
                    page.column0Height = page.lineInfos.size
                }
            }
        } while (nextParagraph && textAreaHeight >= 0 &&
            (!result.paragraphCursor!!.isEndOfSection || page.lineInfos.size == page.column0Height))

        if (end != null && result.compareTo(end) >= 0) {
            if ((info!!.endElementIndex != info.paragraphCursorLength || nextParagraph) && textAreaHeight >= 0 &&
                (!result.paragraphCursor!!.isEndOfSection || page.lineInfos.size == page.column0Height)) {
                page.topMargin = textAreaHeight
            } else {
                page.topMargin = 0
            }
            val last = page.lineInfos.size - 1
            if (last >= 0) {
                info = page.lineInfos[last]
                if (end.compareTo(ZLTextFixedPosition(info.paragraphCursor.index, info.endElementIndex, info.endCharIndex)) < 0) {
                    info.endElementIndex = end.elementIndex
                    info.endCharIndex = end.charIndex
                }
            }
            result.moveTo(end)
        } else {
            page.topMargin = 0
        }
        resetTextStyle()
    }

    private fun isHyphenationPossible(): Boolean =
        textStyleCollection.getBaseStyle().autoHyphenationOption.value && getTextStyle().allowHyphenations()

    @Synchronized
    private fun getHyphenationInfo(word: ZLTextWord): ZLTextHyphenationInfo {
        if (cachedWord != word) {
            cachedWord = word
            cachedInfo = ZLTextHyphenator.Instance().getInfo(word)
        }
        return cachedInfo!!
    }

    private fun processTextLine(
        page: ZLTextPage,
        paragraphCursor: ZLTextParagraphCursor,
        startIndex: Int,
        startCharIndex: Int,
        endIndex: Int,
        previousInfo: ZLTextLineInfo?
    ): ZLTextLineInfo {
        val info = processTextLineInternal(page, paragraphCursor, startIndex, startCharIndex, endIndex, previousInfo)
        if (info.endElementIndex == startIndex && info.endCharIndex == startCharIndex) {
            info.endElementIndex = paragraphCursor.paragraphLength
            info.endCharIndex = 0
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
        val ctx = context
        val info = ZLTextLineInfo(paragraphCursor, startIndex, startCharIndex, getTextStyle())
        val cachedInfo = lineInfoCache[info]
        if (cachedInfo != null) {
            cachedInfo.adjust(previousInfo)
            applyStyleChanges(paragraphCursor, startIndex, cachedInfo.endElementIndex)
            return cachedInfo
        }

        var currentElementIndex = startIndex
        var currentCharIndex = startCharIndex
        val isFirstLine = startIndex == 0 && startCharIndex == 0

        if (isFirstLine) {
            var element = paragraphCursor.getElement(currentElementIndex)
            while (isStyleChangeElement(element)) {
                applyStyleChangeElement(element)
                ++currentElementIndex
                currentCharIndex = 0
                if (currentElementIndex == endIndex) break
                element = paragraphCursor.getElement(currentElementIndex)
            }
            info.startStyle = getTextStyle()
            info.realStartElementIndex = currentElementIndex
            info.realStartCharIndex = currentCharIndex
        }

        var storedStyle = getTextStyle()

        val maxWidth = page.getTextWidth() - storedStyle.getRightIndent(metrics())
        info.leftIndent = storedStyle.getLeftIndent(metrics())
        if (isFirstLine && storedStyle.getAlignment() != ZLTextAlignmentType.ALIGN_CENTER) {
            info.leftIndent += storedStyle.getFirstLineIndent(metrics())
        }
        if (info.leftIndent > maxWidth - 20) {
            info.leftIndent = maxWidth * 3 / 4
        }

        info.width = info.leftIndent

        if (info.realStartElementIndex == endIndex) {
            info.endElementIndex = info.realStartElementIndex
            info.endCharIndex = info.realStartCharIndex
            return info
        }

        var newWidth = info.width
        var newHeight = info.height
        var newDescent = info.descent
        var wordOccurred = false
        var isVisible = false
        var lastSpaceWidth = 0
        var internalSpaceCounter = 0
        var removeLastSpace = false

        do {
            val element = paragraphCursor.getElement(currentElementIndex) ?: break
            newWidth += getElementWidth(element, currentCharIndex)
            newHeight = maxOf(newHeight, getElementHeight(element))
            newDescent = maxOf(newDescent, getElementDescent(element))
            when (element) {
                ZLTextElement.HSpace -> {
                    if (wordOccurred) {
                        wordOccurred = false
                        internalSpaceCounter++
                        lastSpaceWidth = ctx.spaceWidth
                        newWidth += lastSpaceWidth
                    }
                }
                ZLTextElement.NBSpace -> wordOccurred = true
                is ZLTextWord -> { wordOccurred = true; isVisible = true }
                is ZLTextImageElement -> { wordOccurred = true; isVisible = true }
                is ZLTextVideoElement -> { wordOccurred = true; isVisible = true }
                is ExtensionElement -> { wordOccurred = true; isVisible = true }
                else -> if (isStyleChangeElement(element)) applyStyleChangeElement(element)
            }
            if (newWidth > maxWidth) {
                if (info.endElementIndex != startIndex || element is ZLTextWord) break
            }
            val previousElement = element
            ++currentElementIndex
            currentCharIndex = 0
            var allowBreak = currentElementIndex == endIndex
            if (!allowBreak) {
                val nextElement = paragraphCursor.getElement(currentElementIndex)
                allowBreak = previousElement != ZLTextElement.NBSpace &&
                    nextElement != ZLTextElement.NBSpace &&
                    (nextElement !is ZLTextWord || previousElement is ZLTextWord) &&
                    nextElement !is ZLTextImageElement &&
                    nextElement !is ZLTextControlElement
            }
            if (allowBreak) {
                info.isVisible = isVisible
                info.width = newWidth
                if (info.height < newHeight) info.height = newHeight
                if (info.descent < newDescent) info.descent = newDescent
                info.endElementIndex = currentElementIndex
                info.endCharIndex = currentCharIndex
                info.spaceCounter = internalSpaceCounter
                storedStyle = getTextStyle()
                removeLastSpace = !wordOccurred && internalSpaceCounter > 0
            }
        } while (currentElementIndex != endIndex)

        // Hyphenation handling (simplified)
        if (currentElementIndex != endIndex && (isHyphenationPossible() || info.endElementIndex == startIndex)) {
            val element = paragraphCursor.getElement(currentElementIndex)
            if (element is ZLTextWord) {
                val word = element
                newWidth -= getWordWidth(word, currentCharIndex)
                val spaceLeft = maxWidth - newWidth
                if ((word.length > 3 && spaceLeft > 2 * ctx.spaceWidth) || info.endElementIndex == startIndex) {
                    val hyphenationInfo = getHyphenationInfo(word)
                    var hyphenationPosition = currentCharIndex
                    var subwordWidth = 0
                    var right = word.length - 1
                    var left = currentCharIndex
                    while (right > left) {
                        val mid = (right + left + 1) / 2
                        var m1 = mid
                        while (m1 > left && !hyphenationInfo.isHyphenationPossible(m1)) --m1
                        if (m1 > left) {
                            val w = getWordWidth(word, currentCharIndex, m1 - currentCharIndex,
                                word.data[word.offset + m1 - 1] != '-')
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
                    if (hyphenationPosition > currentCharIndex) {
                        info.isVisible = true
                        info.width = newWidth + subwordWidth
                        if (info.height < newHeight) info.height = newHeight
                        if (info.descent < newDescent) info.descent = newDescent
                        info.endElementIndex = currentElementIndex
                        info.endCharIndex = hyphenationPosition
                        info.spaceCounter = internalSpaceCounter
                        storedStyle = getTextStyle()
                        removeLastSpace = false
                    }
                }
            }
        }

        if (removeLastSpace) {
            info.width -= lastSpaceWidth
            info.spaceCounter--
        }

        setTextStyle(storedStyle)

        if (isFirstLine) {
            info.vSpaceBefore = info.startStyle.getSpaceBefore(metrics())
            if (previousInfo != null) {
                info.previousInfoUsed = true
                info.height += maxOf(0, info.vSpaceBefore - previousInfo.vSpaceAfter)
            } else {
                info.previousInfoUsed = false
                info.height += info.vSpaceBefore
            }
        }
        if (info.isEndOfParagraph()) {
            info.vSpaceAfter = getTextStyle().getSpaceAfter(metrics())
        }

        if (info.endElementIndex != endIndex || endIndex == info.paragraphCursorLength) {
            lineInfoCache[info] = info
        }

        return info
    }

    private fun prepareTextLine(page: ZLTextPage, info: ZLTextLineInfo, x: Int, y: Int, columnIndex: Int) {
        var currentY = minOf(y + info.height, topMargin + page.getTextHeight() + page.topMargin - 1)
        var currentX = x

        val ctx = context
        val paragraphCursor = info.paragraphCursor

        setTextStyle(info.startStyle)
        var spaceCounter = info.spaceCounter
        var fullCorrection = 0
        val endOfParagraph = info.isEndOfParagraph()
        var wordOccurred = false
        var changeStyle = true
        currentX += info.leftIndent * if (rtlMode) -1 else 1

        val maxWidth = page.getTextWidth()
        when (getTextStyle().getAlignment()) {
            ZLTextAlignmentType.ALIGN_RIGHT ->
                currentX += (maxWidth - getTextStyle().getRightIndent(metrics()) - info.width) * if (rtlMode) 0 else 1
            ZLTextAlignmentType.ALIGN_CENTER ->
                currentX += ((maxWidth - getTextStyle().getRightIndent(metrics()) - info.width) / 2) * if (rtlMode) -1 else 1
            ZLTextAlignmentType.ALIGN_JUSTIFY ->
                if (!endOfParagraph && paragraphCursor.getElement(info.endElementIndex) != ZLTextElement.AfterParagraph) {
                    fullCorrection = maxWidth - getTextStyle().getRightIndent(metrics()) - info.width
                }
            ZLTextAlignmentType.ALIGN_LEFT ->
                currentX -= (maxWidth - getTextStyle().getLeftIndent(metrics()) - info.width) * if (rtlMode) 1 else 0
        }

        val paragraph = info.paragraphCursor
        val paragraphIndex = paragraph.index
        val endElementIndex = info.endElementIndex
        var charIndex = info.realStartCharIndex
        var spaceElement: ZLTextElementArea? = null

        for (wordIndex in info.realStartElementIndex until endElementIndex) {
            val element = paragraph.getElement(wordIndex) ?: continue
            val width = getElementWidth(element, charIndex)
            when (element) {
                ZLTextElement.HSpace -> {
                    if (wordOccurred && spaceCounter > 0) {
                        val correction = fullCorrection / spaceCounter
                        val spaceLength = ctx.spaceWidth + correction
                        if (getTextStyle().isUnderline()) {
                            spaceElement = ZLTextElementArea(
                                paragraphIndex, wordIndex, 0, 0, true, false, false,
                                getTextStyle(), element,
                                if (rtlMode) currentX - spaceLength else currentX,
                                if (rtlMode) currentX else currentX + spaceLength,
                                currentY, currentY, columnIndex
                            )
                        } else {
                            spaceElement = null
                        }
                        currentX += spaceLength * if (rtlMode) -1 else 1
                        fullCorrection -= correction
                        wordOccurred = false
                        --spaceCounter
                    }
                }
                is ZLTextWord, is ZLTextImageElement, is ZLTextVideoElement, is ExtensionElement -> {
                    val height = getElementHeight(element)
                    val descent = getElementDescent(element)
                    val length = if (element is ZLTextWord) element.length else 0
                    spaceElement?.let { page.textElementMap.add(it) }
                    spaceElement = null
                    page.textElementMap.add(ZLTextElementArea(
                        paragraphIndex, wordIndex, charIndex, length - charIndex, true, false,
                        changeStyle, getTextStyle(), element,
                        if (rtlMode) currentX - width else currentX,
                        if (rtlMode) currentX else currentX + width - 1,
                        currentY - height + 1, currentY + descent, columnIndex
                    ))
                    changeStyle = false
                    wordOccurred = true
                }
                else -> if (isStyleChangeElement(element)) {
                    applyStyleChangeElement(element)
                    changeStyle = true
                }
            }
            currentX += width * if (rtlMode) -1 else 1
            charIndex = 0
        }

        if (!endOfParagraph) {
            val len = info.endCharIndex
            if (len > 0) {
                val wordIndex = info.endElementIndex
                val word = paragraph.getElement(wordIndex) as ZLTextWord
                val addHyphenationSign = word.data[word.offset + len - 1] != '-'
                val width = getWordWidth(word, 0, len, addHyphenationSign)
                val height = getElementHeight(word)
                val descent = ctx.descent
                page.textElementMap.add(ZLTextElementArea(
                    paragraphIndex, wordIndex, 0, len, false, addHyphenationSign,
                    changeStyle, getTextStyle(), word,
                    if (rtlMode) currentX - width else currentX,
                    if (rtlMode) currentX + 1 else currentX + width - 1,
                    currentY - height + 1, currentY + descent, columnIndex
                ))
            }
        }
    }

    @Synchronized
    fun turnPage(forward: Boolean, scrollingMode: Int, value: Int) {
        preparePaintInfo(currentPage)
        previousPage.reset()
        nextPage.reset()
        if (currentPage.paintState == PaintStateEnum.READY) {
            currentPage.paintState = if (forward) PaintStateEnum.TO_SCROLL_FORWARD else PaintStateEnum.TO_SCROLL_BACKWARD
            this.scrollingMode = scrollingMode
            overlappingValue = value
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
        if (model != null && model!!.paragraphsNumber > 0) {
            Application.viewWidget.reset()
            currentPage.moveStartCursor(paragraphIndex, wordIndex, charIndex)
            previousPage.reset()
            nextPage.reset()
            preparePaintInfo(currentPage)
            if (currentPage.isEmptyPage()) {
                turnPage(true, ScrollingMode.NO_OVERLAPPING, 0)
            }
        }
    }

    @Synchronized
    private fun gotoPositionByEnd(paragraphIndex: Int, wordIndex: Int, charIndex: Int) {
        if (model != null && model!!.paragraphsNumber > 0) {
            currentPage.moveEndCursor(paragraphIndex, wordIndex, charIndex)
            previousPage.reset()
            nextPage.reset()
            preparePaintInfo(currentPage)
            if (currentPage.isEmptyPage()) {
                turnPage(false, ScrollingMode.NO_OVERLAPPING, 0)
            }
        }
    }

    @Synchronized
    fun gotoPosition(start: ZLTextPosition, end: ZLTextPosition?) {
        Application.viewWidget.reset()
        currentPage.startCursor.moveTo(start)
        if (end == null) currentPage.endCursor.reset() else currentPage.endCursor.moveTo(end)
        currentPage.paintState = PaintStateEnum.START_IS_KNOWN
        previousPage.reset()
        nextPage.reset()
        preparePaintInfo(currentPage)
    }

    @Synchronized
    fun preparePaintInfo() {
        previousPage.reset()
        nextPage.reset()
        preparePaintInfo(currentPage)
    }

    @Synchronized
    private fun preparePaintInfo(page: ZLTextPage) {
        page.setSize(textColumnWidth, textAreaHeight, twoColumnView(), page == previousPage)

        if (page.paintState == PaintStateEnum.NOTHING_TO_PAINT || page.paintState == PaintStateEnum.READY) return
        val oldState = page.paintState

        val cache = lineInfoCache
        for (info in page.lineInfos) {
            cache[info] = info
        }

        when (page.paintState) {
            PaintStateEnum.TO_SCROLL_FORWARD -> {
                if (!page.endCursor.isEndOfText) {
                    val startCursor = ZLTextWordCursor()
                    when (scrollingMode) {
                        ScrollingMode.NO_OVERLAPPING -> {}
                        ScrollingMode.KEEP_LINES -> page.findLineFromEnd(startCursor, overlappingValue)
                        ScrollingMode.SCROLL_LINES -> {
                            page.findLineFromStart(startCursor, overlappingValue)
                            if (startCursor.isEndOfParagraph) startCursor.nextParagraph()
                        }
                        ScrollingMode.SCROLL_PERCENTAGE -> page.findPercentFromStart(startCursor, overlappingValue)
                    }

                    if (!startCursor.isNull && startCursor.samePositionAs(page.startCursor)) {
                        page.findLineFromStart(startCursor, 1)
                    }

                    if (!startCursor.isNull) {
                        val endCursor = ZLTextWordCursor()
                        buildInfos(page, startCursor, endCursor)
                        if (!page.isEmptyPage() && (scrollingMode != ScrollingMode.KEEP_LINES || !endCursor.samePositionAs(page.endCursor))) {
                            page.startCursor.setCursor(startCursor)
                            page.endCursor.setCursor(endCursor)
                        } else {
                            page.startCursor.setCursor(page.endCursor)
                            buildInfos(page, page.startCursor, page.endCursor)
                        }
                    } else {
                        page.startCursor.setCursor(page.endCursor)
                        buildInfos(page, page.startCursor, page.endCursor)
                    }
                }
            }
            PaintStateEnum.TO_SCROLL_BACKWARD -> {
                if (!page.startCursor.isStartOfText) {
                    when (scrollingMode) {
                        ScrollingMode.NO_OVERLAPPING ->
                            page.startCursor.setCursor(findStartOfPreviousPage(page, page.startCursor))
                        ScrollingMode.KEEP_LINES -> {
                            var endCursor = ZLTextWordCursor()
                            page.findLineFromStart(endCursor, overlappingValue)
                            if (!endCursor.isNull && endCursor.samePositionAs(page.endCursor)) {
                                page.findLineFromEnd(endCursor, 1)
                            }
                            if (!endCursor.isNull) {
                                val startCursor = findStartOfPreviousPage(page, endCursor)
                                if (startCursor.samePositionAs(page.startCursor)) {
                                    page.startCursor.setCursor(findStartOfPreviousPage(page, page.startCursor))
                                } else {
                                    page.startCursor.setCursor(startCursor)
                                }
                            } else {
                                page.startCursor.setCursor(findStartOfPreviousPage(page, page.startCursor))
                            }
                        }
                        ScrollingMode.SCROLL_LINES ->
                            page.startCursor.setCursor(findStart(page, page.startCursor, SizeUnit.LINE_UNIT, overlappingValue))
                        ScrollingMode.SCROLL_PERCENTAGE ->
                            page.startCursor.setCursor(findStart(page, page.startCursor, SizeUnit.PIXEL_UNIT, page.getTextHeight() * overlappingValue / 100))
                    }
                    buildInfos(page, page.startCursor, page.endCursor)
                    if (page.isEmptyPage()) {
                        page.startCursor.setCursor(findStart(page, page.startCursor, SizeUnit.LINE_UNIT, 1))
                        buildInfos(page, page.startCursor, page.endCursor)
                    }
                }
            }
            PaintStateEnum.START_IS_KNOWN -> {
                if (!page.startCursor.isNull) {
                    buildInfos(page, page.startCursor, page.endCursor)
                }
            }
            PaintStateEnum.END_IS_KNOWN -> {
                if (!page.endCursor.isNull) {
                    page.startCursor.setCursor(findStartOfPreviousPage(page, page.endCursor))
                    buildInfos(page, page.startCursor, page.endCursor)
                }
            }
        }
        page.paintState = PaintStateEnum.READY
        lineInfoCache.clear()

        if (page == currentPage) {
            if (oldState != PaintStateEnum.START_IS_KNOWN) previousPage.reset()
            if (oldState != PaintStateEnum.END_IS_KNOWN) nextPage.reset()
        }
    }

    fun clearCaches() {
        resetMetrics()
        rebuildPaintInfo()
        Application.viewWidget.reset()
        charWidth = -1f
    }

    @Synchronized
    protected fun rebuildPaintInfo() {
        previousPage.reset()
        nextPage.reset()
        cursorManager?.evictAll()

        if (currentPage.paintState != PaintStateEnum.NOTHING_TO_PAINT) {
            currentPage.lineInfos.clear()
            if (!currentPage.startCursor.isNull) {
                currentPage.startCursor.rebuild()
                currentPage.endCursor.reset()
                currentPage.paintState = PaintStateEnum.START_IS_KNOWN
            } else if (!currentPage.endCursor.isNull) {
                currentPage.endCursor.rebuild()
                currentPage.startCursor.reset()
                currentPage.paintState = PaintStateEnum.END_IS_KNOWN
            }
        }

        lineInfoCache.clear()
    }

    private fun infoSize(info: ZLTextLineInfo, unit: Int): Int =
        if (unit == SizeUnit.PIXEL_UNIT) info.height + info.descent + info.vSpaceAfter else if (info.isVisible) 1 else 0

    private fun paragraphSize(page: ZLTextPage, cursor: ZLTextWordCursor, beforeCurrentPosition: Boolean, unit: Int): ParagraphSize {
        val size = ParagraphSize()
        val paragraphCursor = cursor.paragraphCursor ?: return size
        val endElementIndex = if (beforeCurrentPosition) cursor.elementIndex else paragraphCursor.paragraphLength

        resetTextStyle()

        var wordIndex = 0
        var charIndex = 0
        var info: ZLTextLineInfo? = null
        while (wordIndex != endElementIndex) {
            val prev = info
            info = processTextLine(page, paragraphCursor, wordIndex, charIndex, endElementIndex, prev)
            wordIndex = info.endElementIndex
            charIndex = info.endCharIndex
            size.height += infoSize(info, unit)
            if (prev == null) size.topMargin = info.vSpaceBefore
            size.bottomMargin = info.vSpaceAfter
        }

        return size
    }

    private fun skip(page: ZLTextPage, cursor: ZLTextWordCursor, unit: Int, size: Int) {
        val paragraphCursor = cursor.paragraphCursor ?: return
        val endElementIndex = paragraphCursor.paragraphLength

        resetTextStyle()
        applyStyleChanges(paragraphCursor, 0, cursor.elementIndex)

        var info: ZLTextLineInfo? = null
        var remainingSize = size
        while (!cursor.isEndOfParagraph && remainingSize > 0) {
            info = processTextLine(page, paragraphCursor, cursor.elementIndex, cursor.charIndex, endElementIndex, info)
            cursor.moveTo(info.endElementIndex, info.endCharIndex)
            remainingSize -= infoSize(info, unit)
        }
    }

    private fun findStartOfPreviousPage(page: ZLTextPage, end: ZLTextWordCursor): ZLTextWordCursor {
        var cursor = end
        if (twoColumnView()) {
            cursor = findStart(page, cursor, SizeUnit.PIXEL_UNIT, page.getTextHeight())
        }
        return findStart(page, cursor, SizeUnit.PIXEL_UNIT, page.getTextHeight())
    }

    private fun findStart(page: ZLTextPage, end: ZLTextWordCursor, unit: Int, height: Int): ZLTextWordCursor {
        val start = ZLTextWordCursor(end)
        var size = paragraphSize(page, start, true, unit)
        var remainingHeight = height - size.height
        var positionChanged = !start.isStartOfParagraph
        start.moveToParagraphStart()
        while (remainingHeight > 0) {
            val previousSize = size
            if (positionChanged && start.paragraphCursor?.isEndOfSection == true) break
            if (!start.previousParagraph()) break
            if (start.paragraphCursor?.isEndOfSection != true) positionChanged = true
            size = paragraphSize(page, start, false, unit)
            remainingHeight -= size.height
            if (previousSize.height > 0) {
                remainingHeight += minOf(size.bottomMargin, previousSize.topMargin)
            }
        }
        skip(page, start, unit, -remainingHeight)

        if (unit == SizeUnit.PIXEL_UNIT) {
            var sameStart = start.samePositionAs(end)
            if (!sameStart && start.isEndOfParagraph && end.isStartOfParagraph) {
                val startCopy = ZLTextWordCursor(start)
                startCopy.nextParagraph()
                sameStart = startCopy.samePositionAs(end)
            }
            if (sameStart) {
                start.setCursor(findStart(page, end, SizeUnit.LINE_UNIT, 1))
            }
        }

        return start
    }

    protected fun getElementByCoordinates(x: Int, y: Int): ZLTextElementArea? =
        currentPage.textElementMap.binarySearch(x, y)

    fun outlineRegion(region: ZLTextRegion?) {
        outlineRegion(region?.soul)
    }

    fun outlineRegion(soul: ZLTextRegion.Soul?) {
        showOutline = true
        outlinedRegionSoul = soul
    }

    open fun hideOutline() {
        showOutline = false
        Application.viewWidget.reset()
    }

    private fun getOutlinedRegion(page: ZLTextPage): ZLTextRegion? =
        page.textElementMap.getRegion(outlinedRegionSoul)

    fun getOutlinedRegion(): ZLTextRegion? = getOutlinedRegion(currentPage)

    protected fun findHighlighting(x: Int, y: Int, maxDistance: Int): ZLTextHighlighting? {
        val region = findRegion(x, y, maxDistance, ZLTextRegion.AnyRegionFilter) ?: return null
        synchronized(highlightings) {
            for (h in highlightings) {
                if (h.getBackgroundColor() != null && h.intersects(region)) return h
            }
        }
        return null
    }

    open fun findRegion(x: Int, y: Int, filter: ZLTextRegion.Filter): ZLTextRegion? =
        findRegion(x, y, Int.MAX_VALUE - 1, filter)

    open fun findRegion(x: Int, y: Int, maxDistance: Int, filter: ZLTextRegion.Filter): ZLTextRegion? =
        currentPage.textElementMap.findRegion(x, y, maxDistance, filter)

    fun findRegionsPair(x: Int, y: Int, filter: ZLTextRegion.Filter): ZLTextElementAreaVector.RegionPair =
        currentPage.textElementMap.findRegionsPair(x, y, getColumnIndex(x), filter)

    protected fun initSelection(x: Int, y: Int): Boolean {
        val adjustedY = y - textStyleCollection.getBaseStyle().fontSize / 2
        if (!selection.start(x, adjustedY)) return false
        Application.viewWidget.reset()
        Application.viewWidget.repaint()
        return true
    }

    fun clearSelection() {
        if (selection.clear()) {
            Application.viewWidget.reset()
            Application.viewWidget.repaint()
        }
    }

    fun getSelectionHighlighting(): ZLTextHighlighting = selection

    open val selectionStartY: Int
        get() {
            if (selection.isEmpty()) return 0
            val area = selection.getStartArea(currentPage)
            if (area != null) return area.yStart
            return if (selection.hasPartBeforePage(currentPage)) {
                currentPage.textElementMap.getFirstArea()?.yStart ?: 0
            } else {
                currentPage.textElementMap.getLastArea()?.yEnd ?: 0
            }
        }

    open val selectionEndY: Int
        get() {
            if (selection.isEmpty()) return 0
            val area = selection.getEndArea(currentPage)
            if (area != null) return area.yEnd
            return if (selection.hasPartAfterPage(currentPage)) {
                currentPage.textElementMap.getLastArea()?.yEnd ?: 0
            } else {
                currentPage.textElementMap.getFirstArea()?.yStart ?: 0
            }
        }

    val selectionStartPosition: ZLTextPosition?
        get() = selection.getStartPosition()

    val selectionEndPosition: ZLTextPosition?
        get() = selection.getStartPosition()

    val isSelectionEmpty: Boolean
        get() = selection.isEmpty()

    fun nextRegion(direction: Direction, filter: ZLTextRegion.Filter): ZLTextRegion? =
        currentPage.textElementMap.nextRegion(getOutlinedRegion(), direction, filter)

    override fun canScroll(index: PageIndex): Boolean = when (index) {
        PageIndex.next -> {
            val cursor = getEndCursor()
            cursor != null && !cursor.isNull && !cursor.isEndOfText
        }
        PageIndex.previous -> {
            val cursor = getStartCursor()
            cursor != null && !cursor.isNull && !cursor.isStartOfText
        }
        else -> true
    }

    internal fun cursor(index: Int): ZLTextParagraphCursor = cursorManager!!.get(index)!!

    protected abstract val extensionManager: ExtensionElementManager?

    object ScrollingMode {
        const val NO_OVERLAPPING = 0
        const val KEEP_LINES = 1
        const val SCROLL_LINES = 2
        const val SCROLL_PERCENTAGE = 3
    }

    private object SizeUnit {
        const val PIXEL_UNIT = 0
        const val LINE_UNIT = 1
    }

    class PagePosition(@JvmField val Current: Int, @JvmField val Total: Int) {
        val current: Int get() = Current
        val total: Int get() = Total
    }

    private class ParagraphSize {
        var height: Int = 0
        var topMargin: Int = 0
        var bottomMargin: Int = 0
    }
}
