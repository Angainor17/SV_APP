package org.geometerplus.fbreader.fbreader

import org.geometerplus.fbreader.bookmodel.BookModel
import org.geometerplus.fbreader.bookmodel.FBHyperlinkType
import org.geometerplus.fbreader.bookmodel.TOCTree
import org.geometerplus.fbreader.fbreader.options.ColorProfile
import org.geometerplus.fbreader.fbreader.options.ImageOptions
import org.geometerplus.fbreader.fbreader.options.MiscOptions
import org.geometerplus.fbreader.fbreader.options.PageTurningOptions
import org.geometerplus.fbreader.fbreader.options.ViewOptions
import org.geometerplus.fbreader.util.FixedTextSnippet
import org.geometerplus.fbreader.util.TextSnippet
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.filesystem.ZLResourceFile
import org.geometerplus.zlibrary.core.fonts.FontEntry
import org.geometerplus.zlibrary.core.library.ZLibrary
import org.geometerplus.zlibrary.core.util.ZLColor
import org.geometerplus.zlibrary.core.view.ZLPaintContext
import org.geometerplus.zlibrary.core.view.ZLViewEnums.Animation
import org.geometerplus.zlibrary.core.view.ZLViewEnums.Direction
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
import java.util.Collections
import java.util.TreeSet

open class FBView(reader: FBReaderApp) : ZLTextView(reader) {
    private val reader: FBReaderApp = reader
    private val viewOptions: ViewOptions = reader.viewOptions
    private val bookElementManager: BookElementManager = BookElementManager(this)
    @JvmField
    var footer: Footer? = null
    private var startY: Int = 0
    private var isBrightnessAdjustmentInProgress: Boolean = false
    private var startBrightness: Int = 0
    private var zoneMap: TapZoneMap? = null

    override fun setModel(model: ZLTextModel?) {
        super.setModel(model)
        footer?.resetTOCMarks()
    }

    private fun getZoneMap(): TapZoneMap {
        val prefs = reader.pageTurningOptions
        var id = prefs.tapZoneMap.value
        if ("" == id) {
            id = if (prefs.horizontal.value) "right_to_left" else "up"
        }
        if (zoneMap == null || id != zoneMap!!.name) {
            zoneMap = TapZoneMap.zoneMap(id)
        }
        return zoneMap!!
    }

    open fun onFingerSingleTapLastResort(x: Int, y: Int) {
        reader.runAction(
            getZoneMap().getActionByCoordinates(
                x, y, contextWidth, contextHeight,
                if (isDoubleTapSupported) TapZoneMap.Tap.singleNotDoubleTap else TapZoneMap.Tap.singleTap
            ), x, y
        )
    }

    override fun onFingerSingleTap(x: Int, y: Int) {
        val hyperlinkRegion = findRegion(x, y, maxSelectionDistance(), ZLTextRegion.HyperlinkFilter)
        if (hyperlinkRegion != null) {
            outlineRegion(hyperlinkRegion)
            reader.viewWidget.reset()
            reader.viewWidget.repaint()
            reader.runAction(ActionCode.PROCESS_HYPERLINK)
            return
        }

        val bookRegion = findRegion(x, y, 0, ZLTextRegion.ExtensionFilter)
        if (bookRegion != null) {
            reader.runAction(ActionCode.DISPLAY_BOOK_POPUP, bookRegion)
            return
        }

        val videoRegion = findRegion(x, y, 0, ZLTextRegion.VideoFilter)
        if (videoRegion != null) {
            outlineRegion(videoRegion)
            reader.viewWidget.reset()
            reader.viewWidget.repaint()
            reader.runAction(ActionCode.OPEN_VIDEO, videoRegion.soul as ZLTextVideoRegionSoul)
            return
        }

        val highlighting = findHighlighting(x, y, maxSelectionDistance())
        if (highlighting is BookmarkHighlighting) {
            reader.runAction(
                ActionCode.SELECTION_BOOKMARK,
                highlighting.bookmark
            )
            return
        }

        if (reader.isActionEnabled(ActionCode.HIDE_TOAST)) {
            reader.runAction(ActionCode.HIDE_TOAST)
            return
        }

        onFingerSingleTapLastResort(x, y)
    }

    override fun isDoubleTapSupported(): Boolean =
        reader.miscOptions.enableDoubleTap.value

    override fun onFingerDoubleTap(x: Int, y: Int) {
        reader.runAction(ActionCode.HIDE_TOAST)

        reader.runAction(
            getZoneMap().getActionByCoordinates(
                x, y, contextWidth, contextHeight, TapZoneMap.Tap.doubleTap
            ), x, y
        )
    }

    override fun onFingerPress(x: Int, y: Int) {
        reader.runAction(ActionCode.HIDE_TOAST)

        val maxDist = ZLibrary.Instance().displayDPI / 4f
        val cursor = findSelectionCursor(x, y, maxDist * maxDist)
        if (cursor != null) {
            reader.runAction(ActionCode.SELECTION_HIDE_PANEL)
            moveSelectionCursorTo(cursor, x, y)
            return
        }

        if (reader.miscOptions.allowScreenBrightnessAdjustment.value && x < contextWidth / 10) {
            isBrightnessAdjustmentInProgress = true
            startY = y
            startBrightness = reader.viewWidget.getScreenBrightness()
            return
        }

        startManualScrolling(x, y)
    }

    private fun isFlickScrollingEnabled(): Boolean {
        val fingerScrolling = reader.pageTurningOptions.fingerScrolling.value
        return fingerScrolling == PageTurningOptions.FingerScrollingType.byFlick ||
                fingerScrolling == PageTurningOptions.FingerScrollingType.byTapAndFlick
    }

    private fun startManualScrolling(x: Int, y: Int) {
        if (!isFlickScrollingEnabled()) {
            return
        }

        val horizontal = reader.pageTurningOptions.horizontal.value
        val direction = if (horizontal) Direction.rightToLeft else Direction.up
        reader.viewWidget.startManualScrolling(x, y, direction)
    }

    override fun onFingerMove(x: Int, y: Int) {
        val cursor = getSelectionCursorInMovement()
        if (cursor != null) {
            moveSelectionCursorTo(cursor, x, y)
            return
        }

        synchronized(this) {
            if (isBrightnessAdjustmentInProgress) {
                if (x >= contextWidth / 5) {
                    isBrightnessAdjustmentInProgress = false
                    startManualScrolling(x, y)
                } else {
                    val delta = (startBrightness + 30) * (startY - y) / contextHeight
                    reader.viewWidget.setScreenBrightness(startBrightness + delta)
                    return
                }
            }

            if (isFlickScrollingEnabled()) {
                reader.viewWidget.scrollManuallyTo(x, y)
            }
        }
    }

    override fun onFingerRelease(x: Int, y: Int) {
        val cursor = getSelectionCursorInMovement()
        if (cursor != null) {
            releaseSelectionCursor()
        } else if (isBrightnessAdjustmentInProgress) {
            isBrightnessAdjustmentInProgress = false
        } else if (isFlickScrollingEnabled()) {
            reader.viewWidget.startAnimatedScrolling(
                x, y, reader.pageTurningOptions.animationSpeed.value
            )
        }
    }

    override fun onFingerLongPress(x: Int, y: Int): Boolean {
        reader.runAction(ActionCode.HIDE_TOAST)

        val region = findRegion(x, y, maxSelectionDistance(), ZLTextRegion.AnyRegionFilter)
        if (region != null) {
            val soul = region.soul
            var doSelectRegion = false
            if (soul is ZLTextWordRegionSoul) {
                when (reader.miscOptions.wordTappingAction.value) {
                    MiscOptions.WordTappingActionEnum.startSelecting -> {
                        reader.runAction(ActionCode.SELECTION_HIDE_PANEL)
                        initSelection(x, y)
                        val cursor = findSelectionCursor(x, y)
                        if (cursor != null) {
                            moveSelectionCursorTo(cursor, x, y)
                        }
                        return true
                    }
                    MiscOptions.WordTappingActionEnum.selectSingleWord,
                    MiscOptions.WordTappingActionEnum.openDictionary -> {
                        doSelectRegion = true
                    }
                    else -> {}
                }
            } else if (soul is ZLTextImageRegionSoul) {
                doSelectRegion = reader.imageOptions.tapAction.value != ImageOptions.TapActionEnum.doNothing
            } else if (soul is ZLTextHyperlinkRegionSoul) {
                doSelectRegion = true
            }

            if (doSelectRegion) {
                outlineRegion(region)
                reader.viewWidget.reset()
                reader.viewWidget.repaint()
                return true
            }
        }
        return false
    }

    override fun onFingerMoveAfterLongPress(x: Int, y: Int) {
        val cursor = getSelectionCursorInMovement()
        if (cursor != null) {
            moveSelectionCursorTo(cursor, x, y)
            return
        }

        var region = getOutlinedRegion()
        if (region != null) {
            var soul = region.soul
            if (soul is ZLTextHyperlinkRegionSoul || soul is ZLTextWordRegionSoul) {
                if (reader.miscOptions.wordTappingAction.value != MiscOptions.WordTappingActionEnum.doNothing) {
                    region = findRegion(x, y, maxSelectionDistance(), ZLTextRegion.AnyRegionFilter)
                    if (region != null) {
                        soul = region.soul
                        if (soul is ZLTextHyperlinkRegionSoul || soul is ZLTextWordRegionSoul) {
                            outlineRegion(region)
                            reader.viewWidget.reset()
                            reader.viewWidget.repaint()
                        }
                    }
                }
            }
        }
    }

    override fun onFingerReleaseAfterLongPress(x: Int, y: Int) {
        val cursor = getSelectionCursorInMovement()
        if (cursor != null) {
            releaseSelectionCursor()
            return
        }

        val region = getOutlinedRegion()
        if (region != null) {
            val soul = region.soul

            var doRunAction = false
            if (soul is ZLTextWordRegionSoul) {
                doRunAction = reader.miscOptions.wordTappingAction.value == MiscOptions.WordTappingActionEnum.openDictionary
            } else if (soul is ZLTextImageRegionSoul) {
                doRunAction = reader.imageOptions.tapAction.value == ImageOptions.TapActionEnum.openImageView
            }

            if (doRunAction) {
                reader.runAction(ActionCode.PROCESS_HYPERLINK)
            }
        }
    }

    override fun onFingerEventCancelled() {
        val cursor = getSelectionCursorInMovement()
        if (cursor != null) {
            releaseSelectionCursor()
        }
    }

    override fun onTrackballRotated(diffX: Int, diffY: Int): Boolean {
        if (diffX == 0 && diffY == 0) {
            return true
        }

        val direction = if (diffY != 0) {
            if (diffY > 0) Direction.down else Direction.up
        } else {
            if (diffX > 0) Direction.leftToRight else Direction.rightToLeft
        }

        MoveCursorAction(reader, direction).checkAndRun()
        return true
    }

    override val textStyleCollection: ZLTextStyleCollection
        get() = viewOptions.getTextStyleCollection()

    override val imageFitting: ImageFitting
        get() = reader.imageOptions.fitToScreen.value ?: ImageFitting.none

    override val leftMargin: Int
        get() = viewOptions.leftMargin.value

    override val rightMargin: Int
        get() = viewOptions.rightMargin.value

    override val topMargin: Int
        get() = viewOptions.topMargin.value

    override val bottomMargin: Int
        get() = viewOptions.bottomMargin.value

    override val spaceBetweenColumns: Int
        get() = viewOptions.spaceBetweenColumns.value

    override fun twoColumnView(): Boolean {
        return contextHeight <= contextWidth && viewOptions.twoColumnView.value
    }

    override val wallpaperFile: ZLFile?
        get() {
            val filePath = viewOptions.getColorProfile().wallpaperOption.value
            if ("" == filePath) {
                return null
            }

            val file = ZLFile.createFileByPath(filePath)
            if (file == null || !file.exists()) {
                return null
            }
            return file
        }

    override val fillMode: ZLPaintContext.FillMode
        get() = if (wallpaperFile is ZLResourceFile)
            ZLPaintContext.FillMode.tileMirror
        else
            viewOptions.getColorProfile().fillModeOption.value ?: ZLPaintContext.FillMode.tileMirror

    override val backgroundColor: ZLColor
        get() = viewOptions.getColorProfile().backgroundOption.value ?: ZLColor(0, 0, 0)

    override val selectionBackgroundColor: ZLColor
        get() = viewOptions.getColorProfile().selectionBackgroundOption.value ?: ZLColor(0, 0, 0)

    override val selectionForegroundColor: ZLColor
        get() = viewOptions.getColorProfile().selectionForegroundOption.value ?: ZLColor(0, 0, 0)

    override fun getTextColor(hyperlink: ZLTextHyperlink?): ZLColor {
        if (hyperlink == null) {
            return viewOptions.getColorProfile().regularTextOption.value ?: ZLColor(0, 0, 0)
        }
        val profile = viewOptions.getColorProfile()
        return when (hyperlink.type) {
            FBHyperlinkType.INTERNAL, FBHyperlinkType.FOOTNOTE -> {
                val book = reader.currentBook
                val id = hyperlink.id
                if (book != null && id != null && reader.collection.isHyperlinkVisited(book, id))
                    profile.visitedHyperlinkTextOption.value ?: ZLColor(0, 0, 0)
                else
                    profile.hyperlinkTextOption.value ?: ZLColor(0, 0, 0)
            }
            FBHyperlinkType.EXTERNAL -> profile.hyperlinkTextOption.value ?: ZLColor(0, 0, 0)
            else -> profile.regularTextOption.value ?: ZLColor(0, 0, 0)
        }
    }

    override val highlightingBackgroundColor: ZLColor
        get() = viewOptions.getColorProfile().highlightingBackgroundOption.value ?: ZLColor(0, 0, 0)

    override val highlightingForegroundColor: ZLColor
        get() = viewOptions.getColorProfile().highlightingForegroundOption.value ?: ZLColor(0, 0, 0)

    override fun getFooterArea(): FooterArea? {
        when (viewOptions.scrollbarType.value) {
            SCROLLBAR_SHOW_AS_FOOTER -> {
                if (footer !is FooterNewStyle) {
                    footer?.let { reader.removeTimerTask(it.updateTask) }
                    footer = FooterNewStyle()
                    reader.addTimerTask(footer!!.updateTask, 15000)
                }
            }
            SCROLLBAR_SHOW_AS_FOOTER_OLD_STYLE -> {
                if (footer !is FooterOldStyle) {
                    footer?.let { reader.removeTimerTask(it.updateTask) }
                    footer = FooterOldStyle()
                    reader.addTimerTask(footer!!.updateTask, 15000)
                }
            }
            else -> {
                footer?.let { reader.removeTimerTask(it.updateTask) }
                footer = null
            }
        }
        return footer
    }

    override fun releaseSelectionCursor() {
        super.releaseSelectionCursor()
        if (countOfSelectedWords > 0) {
            reader.runAction(ActionCode.SELECTION_SHOW_PANEL)
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
            return FixedTextSnippet(start, end, traverser.getText())
        }

    val countOfSelectedWords: Int
        get() {
            val traverser = WordCountTraverser(this)
            if (!isSelectionEmpty) {
                val start = selectionStartPosition ?: return 0
                val end = selectionEndPosition ?: return 0
                traverser.traverse(start, end)
            }
            return traverser.count
        }

    override fun scrollbarType(): Int {
        return viewOptions.scrollbarType.value
    }

    override fun getAnimationType(): Animation {
        return reader.pageTurningOptions.animation.value ?: Animation.none
    }

    override fun getAdjustingModeForImages(): ZLPaintContext.ColorAdjustingMode {
        return if (reader.imageOptions.matchBackground.value) {
            if (ColorProfile.DAY == viewOptions.getColorProfile().name) {
                ZLPaintContext.ColorAdjustingMode.DARKEN_TO_BACKGROUND
            } else {
                ZLPaintContext.ColorAdjustingMode.LIGHTEN_TO_BACKGROUND
            }
        } else {
            ZLPaintContext.ColorAdjustingMode.NONE
        }
    }

    override fun onScrollingFinished(pageIndex: PageIndex) {
        super.onScrollingFinished(pageIndex)
        reader.storePosition()
    }

    override val extensionManager: ExtensionElementManager
        get() = bookElementManager

    abstract inner class Footer : FooterArea {
        protected var tocMarks: ArrayList<TOCTree>? = null
        val updateTask = Runnable {
            reader.viewWidget.repaint()
        }
        private var maxTOCMarksNumber: Int = -1
        private var fontEntry: List<FontEntry>? = null
        private val heightMap = HashMap<String, Int>()
        private val charHeightMap = HashMap<String, Int>()

        override fun getHeight(): Int {
            return viewOptions.footerHeight.value
        }

        @Synchronized
        fun resetTOCMarks() {
            tocMarks = null
        }

        @Synchronized
        protected fun updateTOCMarks(model: BookModel, maxNumber: Int) {
            if (tocMarks != null && maxTOCMarksNumber == maxNumber) {
                return
            }

            tocMarks = ArrayList<TOCTree>()
            maxTOCMarksNumber = maxNumber

            val toc = model.tocTree
            if (toc == null) {
                return
            }
            var maxLevel = Int.MAX_VALUE
            if (toc.getSize() >= maxNumber) {
                val sizes = IntArray(10)
                for (tocItem in toc) {
                    if (tocItem.level < 10) {
                        ++sizes[tocItem.level]
                    }
                }
                for (i in 1 until sizes.size) {
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
                tocMarks!!.add(tocItem)
            }
        }

        protected open fun buildInfoString(pagePosition: PagePosition, separator: String): String {
            val info = StringBuilder()
            val footerOptions = viewOptions.getFooterOptions()

            if (footerOptions.showProgressAsPages()) {
                maybeAddSeparator(info, separator)
                info.append(pagePosition.current)
                info.append("/")
                info.append(pagePosition.total)
            }
            if (footerOptions.showProgressAsPercentage() && pagePosition.total != 0) {
                maybeAddSeparator(info, separator)
                info.append(100 * pagePosition.current / pagePosition.total)
                info.append("%")
            }

            if (footerOptions.showClock.value) {
                maybeAddSeparator(info, separator)
                info.append(ZLibrary.Instance().currentTimeString)
            }
            if (footerOptions.showBattery.value) {
                maybeAddSeparator(info, separator)
                info.append(reader.batteryLevel)
                info.append("%")
            }
            return info.toString()
        }

        private fun maybeAddSeparator(info: StringBuilder, separator: String) {
            if (info.isNotEmpty()) {
                info.append(separator)
            }
        }

        @Synchronized
        protected fun setFont(context: ZLPaintContext, height: Int, bold: Boolean): Int {
            val family = viewOptions.getFooterOptions().font.value
            if (fontEntry == null || family != fontEntry!![0].Family) {
                fontEntry = Collections.singletonList(FontEntry.systemEntry(family))
            }
            val key = family + (if (bold) "N" else "B") + height
            val cached = heightMap[key]
            if (cached != null) {
                context.setFont(fontEntry, cached, bold, false, false, false)
                val charHeight = charHeightMap[key]
                return charHeight ?: height
            } else {
                var h = height + 2
                var charHeight = height
                val max = if (height < 9) height - 1 else height - 2
                while (h > 5) {
                    context.setFont(fontEntry, h, bold, false, false, false)
                    charHeight = context.getCharHeight('H')
                    if (charHeight <= max) {
                        break
                    }
                    --h
                }
                heightMap[key] = h
                charHeightMap[key] = charHeight
                return charHeight
            }
        }
    }

    open inner class FooterOldStyle : Footer() {
        @Synchronized
        override fun paint(context: ZLPaintContext) {
            val wallpaper = wallpaperFile
            if (wallpaper != null) {
                context.clear(wallpaper, fillMode)
            } else {
                context.clear(backgroundColor)
            }

            val model = reader.model
            if (model == null) {
                return
            }

            val fgColor = getTextColor(ZLTextHyperlink.NO_LINK)
            val fillColor = viewOptions.getColorProfile().footerFillOption.value

            val left = leftMargin
            val right = context.width - rightMargin
            val height = getHeight()
            val lineWidth = if (height <= 10) 1 else 2
            val delta = if (height <= 10) 0 else 1
            setFont(context, height, height > 10)

            val pagePosition = this@FBView.pagePosition()

            // draw info text
            val infoString = buildInfoString(pagePosition, " ")
            val infoWidth = context.getStringWidth(infoString)
            context.setTextColor(fgColor)
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
                left + lineWidth + (1.0 * gaugeWidth * pagePosition.current / pagePosition.total).toInt()

            context.setFillColor(fillColor)
            context.fillRectangle(left + 1, height - 2 * lineWidth, gaugeInternalRight, lineWidth + 1)

            val footerOptions = viewOptions.getFooterOptions()
            if (footerOptions.showTOCMarks.value) {
                updateTOCMarks(model, footerOptions.maxTOCMarks.value)
                val fullLength = sizeOfFullText()
                for (tocItem in tocMarks!!) {
                    val reference = tocItem.reference
                    if (reference != null) {
                        val refCoord = sizeOfTextBeforeParagraph(reference.paragraphIndex)
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
            val cProfile = viewOptions.getColorProfile()
            context.clear(cProfile.footerNGBackgroundOption.value)

            val model = reader.model
            if (model == null) {
                return
            }

            val textColor = cProfile.footerNGForegroundOption.value
            val readColor = cProfile.footerNGForegroundOption.value
            val unreadColor = cProfile.footerNGForegroundUnreadOption.value

            val left = leftMargin
            val right = context.width - rightMargin
            val height = getHeight()
            val lineWidth = if (height <= 12) 1 else 2
            val charHeight = setFont(context, height, height > 12)

            val pagePosition = this@FBView.pagePosition()

            // draw info text
            val infoString = buildInfoString(pagePosition, "  ")
            val infoWidth = context.getStringWidth(infoString)
            context.setTextColor(textColor)
            context.drawString(right - infoWidth, (height + charHeight + 1) / 2, infoString)

            // draw gauge
            val gaugeRight = right - (if (infoWidth == 0) 0 else infoWidth + 10)
            val gaugeInternalRight =
                left + (1.0 * (gaugeRight - left) * pagePosition.current / pagePosition.total + 0.5).toInt()
            val v = height / 2

            context.setLineWidth(lineWidth)
            context.setLineColor(readColor)
            context.drawLine(left, v, gaugeInternalRight, v)
            if (gaugeInternalRight < gaugeRight) {
                context.setLineColor(unreadColor)
                context.drawLine(gaugeInternalRight + 1, v, gaugeRight, v)
            }

            // draw labels
            val footerOptions = viewOptions.getFooterOptions()
            if (footerOptions.showTOCMarks.value) {
                val labels = TreeSet<Int>()
                labels.add(left)
                labels.add(gaugeRight)
                updateTOCMarks(model, footerOptions.maxTOCMarks.value)
                val fullLength = sizeOfFullText()
                for (tocItem in tocMarks!!) {
                    val reference = tocItem.reference
                    if (reference != null) {
                        val refCoord = sizeOfTextBeforeParagraph(reference.paragraphIndex)
                        labels.add(left + (1.0 * (gaugeRight - left) * refCoord / fullLength + 0.5).toInt())
                    }
                }
                for (l in labels) {
                    context.setLineColor(if (l <= gaugeInternalRight) readColor else unreadColor)
                    context.drawLine(l, v + 3, l, v - lineWidth - 2)
                }
            }
        }
    }

    companion object {
        const val SCROLLBAR_SHOW_AS_FOOTER = 3
        const val SCROLLBAR_SHOW_AS_FOOTER_OLD_STYLE = 4
    }
}
