package org.geometerplus.zlibrary.text.view

import org.geometerplus.zlibrary.core.application.ZLApplication
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.library.ZLibrary
import org.geometerplus.zlibrary.core.util.ZLColor
import org.geometerplus.zlibrary.core.view.ZLPaintContext
import org.geometerplus.zlibrary.core.view.ZLView
import org.geometerplus.zlibrary.text.model.ZLTextMetrics
import org.geometerplus.zlibrary.text.view.style.ZLTextExplicitlyDecoratedStyle
import org.geometerplus.zlibrary.text.view.style.ZLTextNGStyle
import org.geometerplus.zlibrary.text.view.style.ZLTextStyleCollection

abstract class ZLTextViewBase(application: ZLApplication) : ZLView(application) {
    private var textStyle: ZLTextStyle? = null
    private var wordHeight: Int = -1
    private var metrics: ZLTextMetrics? = null
    private var maxSelectionDistanceValue: Int = 0
    private var wordPartArray = CharArray(20)

    fun maxSelectionDistance(): Int {
        if (maxSelectionDistanceValue == 0) {
            maxSelectionDistanceValue = ZLibrary.Instance().displayDPI / 20
        }
        return maxSelectionDistanceValue
    }

    protected fun resetMetrics() {
        metrics = null
    }

    protected fun metrics(): ZLTextMetrics {
        var m = metrics
        if (m == null) {
            m = ZLTextMetrics(
                ZLibrary.Instance().displayDPI,
                100,
                100,
                textStyleCollection.getBaseStyle().fontSize
            )
            metrics = m
        }
        return m
    }

    internal fun getWordHeight(): Int {
        if (wordHeight == -1) {
            val style = textStyle!!
            wordHeight = context.stringHeight * style.getLineSpacePercent() / 100 + style.getVerticalAlign(metrics())
        }
        return wordHeight
    }

    abstract val textStyleCollection: ZLTextStyleCollection

    abstract val imageFitting: ImageFitting

    abstract val leftMargin: Int

    abstract val rightMargin: Int

    abstract val topMargin: Int

    abstract val bottomMargin: Int

    abstract val spaceBetweenColumns: Int

    abstract fun twoColumnView(): Boolean

    abstract val wallpaperFile: ZLFile?

    abstract val fillMode: ZLPaintContext.FillMode

    abstract val backgroundColor: ZLColor

    abstract val selectionBackgroundColor: ZLColor

    abstract val selectionForegroundColor: ZLColor

    abstract val highlightingBackgroundColor: ZLColor

    abstract val highlightingForegroundColor: ZLColor

    abstract fun getTextColor(hyperlink: ZLTextHyperlink?): ZLColor

    internal fun getTextAreaSize(): ZLPaintContext.Size =
        ZLPaintContext.Size(textColumnWidth, textAreaHeight)

    internal val textAreaHeight: Int
        get() = contextHeight - topMargin - bottomMargin

    protected fun getColumnIndex(x: Int): Int {
        if (!twoColumnView()) {
            return -1
        }
        return if (2 * x <= contextWidth + leftMargin - rightMargin) 0 else 1
    }

    open val textColumnWidth: Int
        get() = if (twoColumnView()) {
            (contextWidth - leftMargin - spaceBetweenColumns - rightMargin) / 2
        } else {
            contextWidth - leftMargin - rightMargin
        }

    internal fun getTextStyle(): ZLTextStyle {
        if (textStyle == null) {
            resetTextStyle()
        }
        return textStyle!!
    }

    internal fun setTextStyle(style: ZLTextStyle) {
        if (textStyle != style) {
            textStyle = style
            wordHeight = -1
        }
        context.setFont(
            style.getFontEntries(),
            style.getFontSize(metrics()),
            style.isBold(),
            style.isItalic(),
            style.isUnderline(),
            style.isStrikeThrough()
        )
    }

    internal fun resetTextStyle() {
        setTextStyle(textStyleCollection.getBaseStyle())
    }

    internal fun isStyleChangeElement(element: ZLTextElement?): Boolean =
        element == ZLTextElement.StyleClose ||
        element is ZLTextStyleElement ||
        element is ZLTextControlElement

    internal fun applyStyleChangeElement(element: ZLTextElement?) {
        when (element) {
            ZLTextElement.StyleClose -> applyStyleClose()
            is ZLTextStyleElement -> applyStyle(element)
            is ZLTextControlElement -> applyControl(element)
        }
    }

    internal fun applyStyleChanges(cursor: ZLTextParagraphCursor, index: Int, end: Int) {
        for (i in index until end) {
            applyStyleChangeElement(cursor.getElement(i))
        }
    }

    private fun applyControl(control: ZLTextControlElement) {
        if (control.isStart) {
            val hyperlink = if (control is ZLTextHyperlinkControlElement) control.Hyperlink else null
            val description = textStyleCollection.getDescription(control.kind)
            if (description != null) {
                setTextStyle(ZLTextNGStyle(textStyle!!, description, hyperlink))
            }
        } else {
            setTextStyle(textStyle!!.parent!!)
        }
    }

    private fun applyStyle(element: ZLTextStyleElement) {
        setTextStyle(ZLTextExplicitlyDecoratedStyle(textStyle!!, element.Entry))
    }

    private fun applyStyleClose() {
        setTextStyle(textStyle!!.parent!!)
    }

    protected open fun getScalingType(imageElement: ZLTextImageElement): ZLPaintContext.ScalingType =
        when (imageFitting) {
            ImageFitting.none -> ZLPaintContext.ScalingType.IntegerCoefficient
            ImageFitting.covers -> if (imageElement.IsCover) {
                ZLPaintContext.ScalingType.FitMaximum
            } else {
                ZLPaintContext.ScalingType.IntegerCoefficient
            }
            ImageFitting.all -> ZLPaintContext.ScalingType.FitMaximum
        }

    internal fun getElementWidth(element: ZLTextElement?, charIndex: Int): Int {
        return when (element) {
            is ZLTextWord -> getWordWidth(element, charIndex)
            is ZLTextImageElement -> {
                val size = context.imageSize(
                    element.ImageData,
                    getTextAreaSize(),
                    getScalingType(element)
                )
                size?.Width ?: 0
            }
            is ZLTextVideoElement -> minOf(300, textColumnWidth)
            is ExtensionElement -> element.getWidth()
            ZLTextElement.NBSpace -> context.spaceWidth
            ZLTextElement.Indent -> textStyle!!.getFirstLineIndent(metrics())
            is ZLTextFixedHSpaceElement -> context.spaceWidth * element.length
            else -> 0
        }
    }

    internal fun getElementHeight(element: ZLTextElement?): Int {
        return when {
            element == ZLTextElement.NBSpace ||
            element is ZLTextWord ||
            element is ZLTextFixedHSpaceElement -> getWordHeight()

            element is ZLTextImageElement -> {
                val size = context.imageSize(
                    element.ImageData,
                    getTextAreaSize(),
                    getScalingType(element)
                )
                (size?.Height ?: 0) +
                    maxOf(context.stringHeight * (textStyle!!.getLineSpacePercent() - 100) / 100, 3)
            }

            element is ZLTextVideoElement -> minOf(
                minOf(200, textAreaHeight),
                textColumnWidth * 2 / 3
            )

            element is ExtensionElement -> element.getHeight()
            else -> 0
        }
    }

    internal fun getElementDescent(element: ZLTextElement?): Int =
        if (element is ZLTextWord) context.descent else 0

    internal fun getWordWidth(word: ZLTextWord, start: Int): Int =
        if (start == 0) {
            word.getWidth(context)
        } else {
            context.getStringWidth(word.data, word.offset + start, word.length - start)
        }

    internal fun getWordWidth(word: ZLTextWord, start: Int, length: Int): Int =
        context.getStringWidth(word.data, word.offset + start, length)

    internal fun getWordWidth(word: ZLTextWord, start: Int, length: Int, addHyphenationSign: Boolean): Int {
        var len = length
        if (len == -1) {
            if (start == 0) {
                return word.getWidth(context)
            }
            len = word.length - start
        }
        if (!addHyphenationSign) {
            return context.getStringWidth(word.data, word.offset + start, len)
        }
        var part = wordPartArray
        if (len + 1 > part.size) {
            part = CharArray(len + 1)
            wordPartArray = part
        }
        System.arraycopy(word.data, word.offset + start, part, 0, len)
        part[len] = '-'
        return context.getStringWidth(part, 0, len + 1)
    }

    open fun getAreaLength(paragraph: ZLTextParagraphCursor, area: ZLTextElementArea, toCharIndex: Int): Int {
        setTextStyle(area.style)
        val word = paragraph.getElement(area.elementIndex) as ZLTextWord
        var length = toCharIndex - area.charIndex
        var selectHyphenationSign = false
        if (length >= area.length) {
            selectHyphenationSign = area.addHyphenationSign
            length = area.length
        }
        return if (length > 0) {
            getWordWidth(word, area.charIndex, length, selectHyphenationSign)
        } else {
            0
        }
    }

    internal fun drawWord(
        x: Int,
        y: Int,
        word: ZLTextWord,
        start: Int,
        length: Int,
        addHyphenationSign: Boolean,
        color: ZLColor
    ) {
        val ctx = context
        if (start == 0 && length == -1) {
            drawString(ctx, x, y, word.data, word.offset, word.length, word.getMark(), color, 0)
        } else {
            var len = length
            if (len == -1) {
                len = word.length - start
            }
            if (!addHyphenationSign) {
                drawString(ctx, x, y, word.data, word.offset + start, len, word.getMark(), color, start)
            } else {
                var part = wordPartArray
                if (len + 1 > part.size) {
                    part = CharArray(len + 1)
                    wordPartArray = part
                }
                System.arraycopy(word.data, word.offset + start, part, 0, len)
                part[len] = '-'
                drawString(ctx, x, y, part, 0, len + 1, word.getMark(), color, start)
            }
        }
    }

    private fun drawString(
        context: ZLPaintContext,
        x: Int,
        y: Int,
        str: CharArray,
        offset: Int,
        length: Int,
        mark: ZLTextWord.Mark?,
        color: ZLColor,
        shift: Int
    ) {
        var currentX = x
        var currentMark = mark
        if (currentMark == null) {
            context.setTextColor(color)
            context.drawString(currentX, y, str, offset, length)
        } else {
            var pos = 0
            while (currentMark != null && pos < length) {
                var markStart = currentMark.start - shift
                var markLen = currentMark.length

                if (markStart < pos) {
                    markLen += markStart - pos
                    markStart = pos
                }

                if (markLen <= 0) {
                    currentMark = currentMark.next
                    continue
                }

                if (markStart > pos) {
                    val endPos = minOf(markStart, length)
                    context.setTextColor(color)
                    context.drawString(currentX, y, str, offset + pos, endPos - pos)
                    currentX += context.getStringWidth(str, offset + pos, endPos - pos)
                }

                if (markStart < length) {
                    context.setFillColor(highlightingBackgroundColor)
                    val endPos = minOf(markStart + markLen, length)
                    val endX = currentX + context.getStringWidth(str, offset + markStart, endPos - markStart)
                    context.fillRectangle(
                        currentX, y - context.stringHeight,
                        endX - 1, y + context.descent
                    )
                    context.setTextColor(highlightingForegroundColor)
                    context.drawString(currentX, y, str, offset + markStart, endPos - markStart)
                    currentX = endX
                }
                pos = markStart + markLen
                currentMark = currentMark.next
            }

            if (pos < length) {
                context.setTextColor(color)
                context.drawString(currentX, y, str, offset + pos, length - pos)
            }
        }
    }

    enum class ImageFitting {
        none, covers, all
    }
}
