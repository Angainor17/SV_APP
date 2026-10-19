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
package org.geometerplus.fbreader.fbreader

import org.geometerplus.fbreader.bookmodel.BookModel
import org.geometerplus.fbreader.bookmodel.FBHyperlinkType
import org.geometerplus.fbreader.bookmodel.TOCTree
import org.geometerplus.fbreader.fbreader.TapZoneMap.Companion.zoneMap
import org.geometerplus.fbreader.fbreader.options.ColorProfile
import org.geometerplus.fbreader.fbreader.options.ImageOptions
import org.geometerplus.fbreader.fbreader.options.MiscOptions.WordTappingActionEnum
import org.geometerplus.fbreader.fbreader.options.PageTurningOptions.FingerScrollingType
import org.geometerplus.fbreader.fbreader.options.ViewOptions
import org.geometerplus.fbreader.util.FixedTextSnippet
import org.geometerplus.fbreader.util.TextSnippet
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.filesystem.ZLFile.Companion.createFileByPath
import org.geometerplus.zlibrary.core.filesystem.ZLResourceFile
import org.geometerplus.zlibrary.core.fonts.FontEntry
import org.geometerplus.zlibrary.core.fonts.FontEntry.Companion.systemEntry
import org.geometerplus.zlibrary.core.library.ZLibrary.Companion.Instance
import org.geometerplus.zlibrary.core.util.ZLColor
import org.geometerplus.zlibrary.core.view.ZLPaintContext
import org.geometerplus.zlibrary.core.view.ZLPaintContext.ColorAdjustingMode
import org.geometerplus.zlibrary.core.view.ZLPaintContext.FillMode
import org.geometerplus.zlibrary.core.view.ZLViewEnums
import org.geometerplus.zlibrary.core.view.ZLViewEnums.PageIndex
import org.geometerplus.zlibrary.text.model.ZLTextModel
import org.geometerplus.zlibrary.text.view.ExtensionElementManager
import org.geometerplus.zlibrary.text.view.ZLTextHyperlink
import org.geometerplus.zlibrary.text.view.ZLTextHyperlinkRegionSoul
import org.geometerplus.zlibrary.text.view.ZLTextImageRegionSoul
import org.geometerplus.zlibrary.text.view.ZLTextRegion
import org.geometerplus.zlibrary.text.view.ZLTextVideoRegionSoul
import org.geometerplus.zlibrary.text.view.ZLTextView
import org.geometerplus.zlibrary.text.view.ZLTextWordRegionSoul
import org.geometerplus.zlibrary.text.view.style.ZLTextStyleCollection
import java.util.TreeSet

open class FBView(private val myReader: FBReaderApp) : ZLTextView(
    myReader
) {
    private val myViewOptions: ViewOptions
    var myFooter: Footer? = null
    private var myStartY = 0
    private var myIsBrightnessAdjustmentInProgress = false
    private var myStartBrightness = 0
    private var myZoneMap: TapZoneMap? = null

    init {
        myViewOptions = myReader.ViewOptions
    }

    public override var model: ZLTextModel?
        get() = super.model
        set(model) {
            super.model = model
            if (myFooter != null) {
                myFooter!!.resetTOCMarks()
            }
        }

    private val zoneMap: TapZoneMap
        get() {
            val prefs = myReader.PageTurningOptions
            var id = prefs.tapZoneMap.getValue()
            if ("" == id) {
                id = if (prefs.horizontal.getValue()) "right_to_left" else "up"
            }
            if (myZoneMap == null || id != myZoneMap!!.Name) {
                myZoneMap = zoneMap(id)
            }
            return myZoneMap!!
        }

    open fun onFingerSingleTapLastResort(x: Int, y: Int) {
        myReader.runAction(
            this.zoneMap.getActionByCoordinates(
                x, y, getContextWidth(), getContextHeight(),
                if (isDoubleTapSupported()) TapZoneMap.Tap.singleNotDoubleTap else TapZoneMap.Tap.singleTap
            ), x, y
        )
    }

    public override fun onFingerSingleTap(x: Int, y: Int) {
        val hyperlinkRegion = findRegion(x, y, maxSelectionDistance(), ZLTextRegion.HyperlinkFilter)
        if (hyperlinkRegion != null) {
            outlineRegion(hyperlinkRegion)
            myReader.getViewWidget()!!.reset()
            myReader.getViewWidget()!!.repaint()
            myReader.runAction(ActionCode.PROCESS_HYPERLINK)
            return
        }

        val bookRegion = findRegion(x, y, 0, ZLTextRegion.ExtensionFilter)
        if (bookRegion != null) {
            myReader.runAction(ActionCode.DISPLAY_BOOK_POPUP, bookRegion)
            return
        }

        val videoRegion = findRegion(x, y, 0, ZLTextRegion.VideoFilter)
        if (videoRegion != null) {
            outlineRegion(videoRegion)
            myReader.getViewWidget()!!.reset()
            myReader.getViewWidget()!!.repaint()
            myReader.runAction(ActionCode.OPEN_VIDEO, videoRegion.soul as ZLTextVideoRegionSoul)
            return
        }

        val highlighting = findHighlighting(x, y, maxSelectionDistance())
        if (highlighting is BookmarkHighlighting) {
            myReader.runAction(
                ActionCode.SELECTION_BOOKMARK,
                highlighting.Bookmark
            )
            return
        }

        if (myReader.isActionEnabled(ActionCode.HIDE_TOAST)) {
            myReader.runAction(ActionCode.HIDE_TOAST)
            return
        }

        onFingerSingleTapLastResort(x, y)
    }

    public override fun isDoubleTapSupported(): Boolean {
        return myReader.MiscOptions.EnableDoubleTap.getValue()
    }

    public override fun onFingerDoubleTap(x: Int, y: Int) {
        myReader.runAction(ActionCode.HIDE_TOAST)

        myReader.runAction(
            this.zoneMap.getActionByCoordinates(
                x, y, getContextWidth(), getContextHeight(), TapZoneMap.Tap.doubleTap
            ), x, y
        )
    }

    public override fun onFingerPress(x: Int, y: Int) {
        myReader.runAction(ActionCode.HIDE_TOAST)

        val maxDist = (Instance().getDisplayDPI() / 4).toFloat()
        val cursor = findSelectionCursor(x, y, maxDist * maxDist)
        if (cursor != null) {
            myReader.runAction(ActionCode.SELECTION_HIDE_PANEL)
            moveSelectionCursorTo(cursor, x, y)
            return
        }

        if (myReader.MiscOptions.AllowScreenBrightnessAdjustment.getValue() && x < getContextWidth() / 10) {
            myIsBrightnessAdjustmentInProgress = true
            myStartY = y
            myStartBrightness = myReader.getViewWidget()!!.getScreenBrightness()
            return
        }

        startManualScrolling(x, y)
    }

    private val isFlickScrollingEnabled: Boolean
        get() {
            val fingerScrolling =
                myReader.PageTurningOptions.fingerScrolling.getValue()
            return fingerScrolling == FingerScrollingType.byFlick ||
                    fingerScrolling == FingerScrollingType.byTapAndFlick
        }

    private fun startManualScrolling(x: Int, y: Int) {
        if (!this.isFlickScrollingEnabled) {
            return
        }

        val horizontal = myReader.PageTurningOptions.horizontal.getValue()
        val direction =
            if (horizontal) ZLViewEnums.Direction.rightToLeft else ZLViewEnums.Direction.up
        myReader.getViewWidget()!!.startManualScrolling(x, y, direction)
    }

    public override fun onFingerMove(x: Int, y: Int) {
        val cursor = selectionCursorInMovement
        if (cursor != null) {
            moveSelectionCursorTo(cursor, x, y)
            return
        }

        synchronized(this) {
            if (myIsBrightnessAdjustmentInProgress) {
                if (x >= getContextWidth() / 5) {
                    myIsBrightnessAdjustmentInProgress = false
                    startManualScrolling(x, y)
                } else {
                    val delta = (myStartBrightness + 30) * (myStartY - y) / getContextHeight()
                    myReader.getViewWidget()!!.setScreenBrightness(myStartBrightness + delta)
                    return
                }
            }
            if (this.isFlickScrollingEnabled) {
                myReader.getViewWidget()!!.scrollManuallyTo(x, y)
            }
        }
    }

    public override fun onFingerRelease(x: Int, y: Int) {
        val cursor = selectionCursorInMovement
        if (cursor != null) {
            releaseSelectionCursor()
        } else if (myIsBrightnessAdjustmentInProgress) {
            myIsBrightnessAdjustmentInProgress = false
        } else if (this.isFlickScrollingEnabled) {
            myReader.getViewWidget()!!.startAnimatedScrolling(
                x, y, myReader.PageTurningOptions.animationSpeed.getValue()
            )
        }
    }

    public override fun onFingerLongPress(x: Int, y: Int): Boolean {
        myReader.runAction(ActionCode.HIDE_TOAST)

        val region = findRegion(x, y, maxSelectionDistance(), ZLTextRegion.AnyRegionFilter)
        if (region != null) {
            val soul = region.soul
            var doSelectRegion = false
            if (soul is ZLTextWordRegionSoul) {
                when (myReader.MiscOptions.WordTappingAction.getValue()) {
                    WordTappingActionEnum.startSelecting -> {
                        // SELECTION_HIDE_PANEL не вызываем - панель должна оставаться видимой
                        // При нажатии на маркер для движения, SELECTION_HIDE_PANEL вызывается в onFingerPress
                        initSelection(x, y)
                        val cursor = findSelectionCursor(x, y)
                        if (cursor != null) {
                            moveSelectionCursorTo(cursor, x, y)
                        }
                        return true
                    }

                    WordTappingActionEnum.selectSingleWord, WordTappingActionEnum.openDictionary -> doSelectRegion =
                        true

                    WordTappingActionEnum.doNothing -> {}
                }
            } else if (soul is ZLTextImageRegionSoul) {
                doSelectRegion =
                    myReader.ImageOptions.TapAction.getValue() !=
                            ImageOptions.TapActionEnum.doNothing
            } else if (soul is ZLTextHyperlinkRegionSoul) {
                doSelectRegion = true
            }

            if (doSelectRegion) {
                outlineRegion(region)
                myReader.getViewWidget()!!.reset()
                myReader.getViewWidget()!!.repaint()
                return true
            }
        }
        return false
    }

    public override fun onFingerMoveAfterLongPress(x: Int, y: Int) {
        val cursor = selectionCursorInMovement
        if (cursor != null) {
            moveSelectionCursorTo(cursor, x, y)
            return
        }

        var region = outlinedRegion
        if (region != null) {
            var soul = region.soul
            if (soul is ZLTextHyperlinkRegionSoul ||
                soul is ZLTextWordRegionSoul
            ) {
                if (myReader.MiscOptions.WordTappingAction.getValue() !=
                    WordTappingActionEnum.doNothing
                ) {
                    region = findRegion(x, y, maxSelectionDistance(), ZLTextRegion.AnyRegionFilter)
                    if (region != null) {
                        soul = region.soul
                        if (soul is ZLTextHyperlinkRegionSoul
                            || soul is ZLTextWordRegionSoul
                        ) {
                            outlineRegion(region)
                            myReader.getViewWidget()!!.reset()
                            myReader.getViewWidget()!!.repaint()
                        }
                    }
                }
            }
        }
    }

    public override fun onFingerReleaseAfterLongPress(x: Int, y: Int) {
        val cursor = selectionCursorInMovement
        if (cursor != null) {
            releaseSelectionCursor()
            return
        }

        val region = outlinedRegion
        if (region != null) {
            val soul = region.soul

            var doRunAction = false
            if (soul is ZLTextWordRegionSoul) {
                doRunAction =
                    myReader.MiscOptions.WordTappingAction.getValue() ==
                            WordTappingActionEnum.openDictionary
            } else if (soul is ZLTextImageRegionSoul) {
                doRunAction =
                    myReader.ImageOptions.TapAction.getValue() ==
                            ImageOptions.TapActionEnum.openImageView
            }

            if (doRunAction) {
                myReader.runAction(ActionCode.PROCESS_HYPERLINK)
            }
        }
    }

    public override fun onFingerEventCancelled() {
        val cursor = selectionCursorInMovement
        if (cursor != null) {
            releaseSelectionCursor()
        }
    }

    public override fun onTrackballRotated(diffX: Int, diffY: Int): Boolean {
        if (diffX == 0 && diffY == 0) {
            return true
        }

        val direction =
            if (diffY != 0) (if (diffY > 0) ZLViewEnums.Direction.down else ZLViewEnums.Direction.up) else (if (diffX > 0) ZLViewEnums.Direction.leftToRight else ZLViewEnums.Direction.rightToLeft)

        MoveCursorAction(myReader, direction).run()
        return true
    }

    override fun getTextStyleCollection(): ZLTextStyleCollection {
        return myViewOptions.textStyleCollection
    }

    override fun getImageFitting(): ImageFitting {
        return myReader.ImageOptions.FitToScreen.getValue()
    }

    override fun getLeftMargin(): Int {
        return myViewOptions.LeftMargin.getValue()
    }

    override fun getRightMargin(): Int {
        return myViewOptions.RightMargin.getValue()
    }

    override fun getTopMargin(): Int {
        return myViewOptions.TopMargin.getValue()
    }

    override fun getBottomMargin(): Int {
        return myViewOptions.BottomMargin.getValue()
    }

    override fun getSpaceBetweenColumns(): Int {
        return myViewOptions.SpaceBetweenColumns.getValue()
    }

    override fun twoColumnView(): Boolean {
        return getContextHeight() <= getContextWidth() && myViewOptions.TwoColumnView.getValue()
    }

    override fun getWallpaperFile(): ZLFile? {
        val filePath = myViewOptions.colorProfile.WallpaperOption.getValue()
        if ("" == filePath) {
            return null
        }

        val file = createFileByPath(filePath)
        if (file == null || !file.exists()) {
            return null
        }
        return file
    }

    override fun getFillMode(): FillMode {
        return if (getWallpaperFile() is ZLResourceFile)
            FillMode.tileMirror
        else
            myViewOptions.colorProfile.FillModeOption.getValue()
    }

    override fun getBackgroundColor(): ZLColor? {
        return myViewOptions.colorProfile.BackgroundOption.getValue()
    }

    override fun getSelectionBackgroundColor(): ZLColor? {
        return myViewOptions.colorProfile.SelectionBackgroundOption.getValue()
    }

    override fun getSelectionForegroundColor(): ZLColor? {
        return myViewOptions.colorProfile.SelectionForegroundOption.getValue()
    }

    override fun getTextColor(hyperlink: ZLTextHyperlink): ZLColor? {
        val profile = myViewOptions.colorProfile
        when (hyperlink.Type) {
            FBHyperlinkType.NONE -> return profile.RegularTextOption.getValue()
            FBHyperlinkType.INTERNAL, FBHyperlinkType.FOOTNOTE -> return if (myReader.Collection.isHyperlinkVisited(
                    myReader.currentBook!!,
                    hyperlink.Id!!
                )
            )
                profile.VisitedHyperlinkTextOption.getValue()
            else
                profile.HyperlinkTextOption.getValue()

            FBHyperlinkType.EXTERNAL -> return profile.HyperlinkTextOption.getValue()
            else -> return profile.RegularTextOption.getValue()
        }
    }

    override fun getHighlightingBackgroundColor(): ZLColor? {
        return myViewOptions.colorProfile.HighlightingBackgroundOption.getValue()
    }

    override fun getHighlightingForegroundColor(): ZLColor? {
        return myViewOptions.colorProfile.HighlightingForegroundOption.getValue()
    }

    override val footerArea: Footer
        get() {
            when (myViewOptions.ScrollbarType.getValue()) {
                SCROLLBAR_SHOW_AS_FOOTER -> {
                    val footer = myFooter
                    if (footer !is FooterNewStyle) {
                        if (footer != null) {
                            myReader.removeTimerTask(footer.UpdateTask)
                        }
                        myFooter = FooterNewStyle()
                        myReader.addTimerTask(myFooter!!.UpdateTask, 15000)
                    }
                }

                SCROLLBAR_SHOW_AS_FOOTER_OLD_STYLE -> {
                    val footer = myFooter
                    if (footer !is FooterOldStyle) {
                        if (footer != null) {
                            myReader.removeTimerTask(footer.UpdateTask)
                        }
                        myFooter = FooterOldStyle()
                        myReader.addTimerTask(myFooter!!.UpdateTask, 15000)
                    }
                }

                else -> {
                    val footer = myFooter
                    if (footer != null) {
                        myReader.removeTimerTask(footer.UpdateTask)
                        myFooter = null
                    }
                }
            }
            return myFooter!!
        }

    override fun releaseSelectionCursor() {
        super.releaseSelectionCursor()
        if (this.countOfSelectedWords > 0) {
            myReader.runAction(ActionCode.SELECTION_SHOW_PANEL)
        }
    }

    val selectedSnippet: TextSnippet?
        get() {
            val start = selectionStartPosition
            val end = selectionEndPosition
            if (start == null || end == null) {
                return null
            }
            val traverser = TextBuildTraverser(this)
            traverser.traverse(start, end)
            return FixedTextSnippet(start, end, traverser.text)
        }

    val countOfSelectedWords: Int
        get() {
            val traverser = WordCountTraverser(this)
            if (!isSelectionEmpty) {
                traverser.traverse(selectionStartPosition!!, selectionEndPosition!!)
            }
            return traverser.getCount()
        }

    public override fun scrollbarType(): Int {
        return myViewOptions.ScrollbarType.getValue()
    }

    public override fun getAnimationType(): ZLViewEnums.Animation {
        return myReader.PageTurningOptions.animation.getValue()
    }

    override val adjustingModeForImages: ColorAdjustingMode?
        get() {
            if (myReader.ImageOptions.MatchBackground.getValue()) {
                if (ColorProfile.DAY == myViewOptions.colorProfile.Name) {
                    return ColorAdjustingMode.DARKEN_TO_BACKGROUND
                } else {
                    return ColorAdjustingMode.LIGHTEN_TO_BACKGROUND
                }
            } else {
                return ColorAdjustingMode.NONE
            }
        }

    @Synchronized
    override fun onScrollingFinished(pageIndex: PageIndex) {
        super.onScrollingFinished(pageIndex)
        myReader.storePosition()
    }

    override val extensionManager: ExtensionElementManager?
        get() = null

    abstract inner class Footer : FooterArea {
        protected var myTOCMarks: ArrayList<TOCTree>? = null
        internal val UpdateTask: Runnable = object : Runnable {
            override fun run() {
                myReader.getViewWidget()!!.repaint()
            }
        }
        private var myMaxTOCMarksNumber = -1
        private var myFontEntry: MutableList<FontEntry>? = null
        private val myHeightMap: MutableMap<String?, Int?> = HashMap<String?, Int?>()
        private val myCharHeightMap: MutableMap<String?, Int?> = HashMap<String?, Int?>()

        override val height: Int
            get() = myViewOptions.FooterHeight.getValue()

        @Synchronized
        fun resetTOCMarks() {
            myTOCMarks = null
        }

        @Synchronized
        protected fun updateTOCMarks(model: BookModel, maxNumber: Int) {
            if (myTOCMarks != null && myMaxTOCMarksNumber == maxNumber) {
                return
            }

            myTOCMarks = ArrayList<TOCTree>()
            myMaxTOCMarksNumber = maxNumber

            val toc = model.TOCTree
            if (toc == null) {
                return
            }
            var maxLevel = Int.MAX_VALUE
            if (toc.getSize() >= maxNumber) {
                val sizes = IntArray(10)
                for (tocItem in toc) {
                    if (tocItem.Level < 10) {
                        ++sizes[tocItem.Level]
                    }
                }
                for (i in 1..<sizes.size) {
                    sizes[i] += sizes[i - 1]
                }
                maxLevel = sizes.size - 1
                while (maxLevel >= 0) {
                    if (sizes[maxLevel] < maxNumber) {
                        break
                    }
                    --maxLevel
                }
            }
            for (tocItem in toc.allSubtrees(maxLevel)) {
                myTOCMarks!!.add(tocItem)
            }
        }

        protected open fun buildInfoString(pagePosition: PagePosition, separator: String?): String {
            val info = StringBuilder()
            val footerOptions = myViewOptions.footerOptions

            if (footerOptions.showProgressAsPages()) {
                maybeAddSeparator(info, separator)
                info.append(pagePosition.Current)
                info.append("/")
                info.append(pagePosition.Total)
            }
            if (footerOptions.showProgressAsPercentage() && pagePosition.Total != 0) {
                maybeAddSeparator(info, separator)
                info.append((100 * pagePosition.Current / pagePosition.Total).toString())
                info.append("%")
            }

            if (footerOptions.ShowClock.getValue()) {
                maybeAddSeparator(info, separator)
                info.append(Instance().getCurrentTimeString())
            }
            if (footerOptions.ShowBattery.getValue()) {
                maybeAddSeparator(info, separator)
                info.append(myReader.getBatteryLevel())
                info.append("%")
            }
            return info.toString()
        }

        private fun maybeAddSeparator(info: StringBuilder, separator: String?) {
            if (info.length > 0) {
                info.append(separator)
            }
        }

        @Synchronized
        protected fun setFont(context: ZLPaintContext, height: Int, bold: Boolean): Int {
            val family = myViewOptions.footerOptions.Font.getValue()
            if (myFontEntry == null || family != myFontEntry!!.get(0).Family) {
                myFontEntry = mutableListOf(systemEntry(family))
            }
            val key = family + (if (bold) "N" else "B") + height
            val cached = myHeightMap.get(key)
            if (cached != null) {
                context.setFont(myFontEntry!!, cached, bold, false, false, false)
                val charHeight = myCharHeightMap.get(key)
                return if (charHeight != null) charHeight else height
            } else {
                var h = height + 2
                var charHeight = height
                val max = if (height < 9) height - 1 else height - 2
                while (h > 5) {
                    context.setFont(myFontEntry!!, h, bold, false, false, false)
                    charHeight = context.getCharHeight('H')
                    if (charHeight <= max) {
                        break
                    }
                    --h
                }
                myHeightMap.put(key, h)
                myCharHeightMap.put(key, charHeight)
                return charHeight
            }
        }
    }

    open inner class FooterOldStyle : Footer() {
        @Synchronized
        override fun paint(context: ZLPaintContext) {
            val wallpaper = getWallpaperFile()
            if (wallpaper != null) {
                context.clear(wallpaper, getFillMode())
            } else {
                context.clear(getBackgroundColor()!!)
            }

            val model = myReader.Model
            if (model == null) {
                return
            }

            //final ZLColor bgColor = getBackgroundColor();
            // TODO: separate color option for footer color
            val fgColor = getTextColor(ZLTextHyperlink.NO_LINK)
            val fillColor = myViewOptions.colorProfile.FooterFillOption.getValue()

            val left = getLeftMargin()
            val right = context.getWidth() - getRightMargin()
            val height = height
            val lineWidth = if (height <= 10) 1 else 2
            val delta = if (height <= 10) 0 else 1
            setFont(context, height, height > 10)

            val pagePosition = this@FBView.pagePosition()

            // draw info text
            val infoString = buildInfoString(pagePosition, " ")
            val infoWidth = context.getStringWidth(infoString)
            context.setTextColor(fgColor!!)
            context.drawString(right - infoWidth, height - delta, infoString)

            // draw gauge
            val gaugeRight = right - (if (infoWidth == 0) 0 else infoWidth + 10)
            val gaugeWidth = gaugeRight - left - 2 * lineWidth

            context.setLineColor(fgColor)
            context.setLineWidth(lineWidth)
            context.drawLine(left, lineWidth, left, height - lineWidth)
            context.drawLine(left, height - lineWidth, gaugeRight, height - lineWidth)
            context.drawLine(gaugeRight, height - lineWidth, gaugeRight, lineWidth)
            context.drawLine(gaugeRight, lineWidth, left, lineWidth)

            val gaugeInternalRight =
                left + lineWidth + (1.0 * gaugeWidth * pagePosition.Current / pagePosition.Total).toInt()

            context.setFillColor(fillColor!!)
            context.fillRectangle(
                left + 1,
                height - 2 * lineWidth,
                gaugeInternalRight,
                lineWidth + 1
            )

            val footerOptions = myViewOptions.footerOptions
            if (footerOptions.ShowTOCMarks.getValue()) {
                updateTOCMarks(model, footerOptions.MaxTOCMarks.getValue())
                val fullLength = sizeOfFullText()
                for (tocItem in myTOCMarks!!) {
                    val reference = tocItem.reference
                    if (reference != null) {
                        val refCoord = sizeOfTextBeforeParagraph(reference.ParagraphIndex)
                        val xCoord =
                            left + 2 * lineWidth + (1.0 * gaugeWidth * refCoord / fullLength).toInt()
                        context.drawLine(xCoord, height - lineWidth, xCoord, lineWidth)
                    }
                }
            }
        }
    }

    open inner class FooterNewStyle : Footer() {
        @Synchronized
        override fun paint(context: ZLPaintContext) {
            val cProfile = myViewOptions.colorProfile
            context.clear(cProfile.FooterNGBackgroundOption.getValue()!!)

            val model = myReader.Model
            if (model == null) {
                return
            }

            val textColor = cProfile.FooterNGForegroundOption.getValue()
            val readColor = cProfile.FooterNGForegroundOption.getValue()
            val unreadColor = cProfile.FooterNGForegroundUnreadOption.getValue()

            val left = getLeftMargin()
            val right = context.getWidth() - getRightMargin()
            val height = height
            val lineWidth = if (height <= 12) 1 else 2
            val charHeight = setFont(context, height, height > 12)

            val pagePosition = this@FBView.pagePosition()

            // draw info text
            val infoString = buildInfoString(pagePosition, "  ")
            val infoWidth = context.getStringWidth(infoString)
            context.setTextColor(textColor!!)
            context.drawString(right - infoWidth, (height + charHeight + 1) / 2, infoString)

            // draw gauge
            val gaugeRight = right - (if (infoWidth == 0) 0 else infoWidth + 10)
            val gaugeInternalRight =
                left + (1.0 * (gaugeRight - left) * pagePosition.Current / pagePosition.Total + 0.5).toInt()
            val v = height / 2

            context.setLineWidth(lineWidth)
            context.setLineColor(readColor!!)
            context.drawLine(left, v, gaugeInternalRight, v)
            if (gaugeInternalRight < gaugeRight) {
                context.setLineColor(unreadColor!!)
                context.drawLine(gaugeInternalRight + 1, v, gaugeRight, v)
            }

            // draw labels
            val footerOptions = myViewOptions.footerOptions
            if (footerOptions.ShowTOCMarks.getValue()) {
                val labels = TreeSet<Int?>()
                labels.add(left)
                labels.add(gaugeRight)
                updateTOCMarks(model, footerOptions.MaxTOCMarks.getValue())
                val fullLength = sizeOfFullText()
                for (tocItem in myTOCMarks!!) {
                    val reference = tocItem.reference
                    if (reference != null) {
                        val refCoord = sizeOfTextBeforeParagraph(reference.ParagraphIndex)
                        labels.add(left + (1.0 * (gaugeRight - left) * refCoord / fullLength + 0.5).toInt())
                    }
                }
                for (l in labels) {
                    context.setLineColor((if (l!! <= gaugeInternalRight) readColor else unreadColor)!!)
                    context.drawLine(l, v + 3, l, v - lineWidth - 2)
                }
            }
        }
    }

    companion object {
        const val SCROLLBAR_SHOW_AS_FOOTER: Int = 3
        const val SCROLLBAR_SHOW_AS_FOOTER_OLD_STYLE: Int = 4
    }
}
