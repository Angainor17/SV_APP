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
    private var myTextStyle: ZLTextStyle? = null
    private var myWordHeight: Int = -1
    private var myMetrics: ZLTextMetrics? = null
    private var myMaxSelectionDistance: Int = 0
    private var myWordPartArray: CharArray = CharArray(20)

    fun maxSelectionDistance(): Int {
        if (myMaxSelectionDistance == 0) {
            myMaxSelectionDistance = ZLibrary.Instance().getDisplayDPI() / 20
        }
        return myMaxSelectionDistance
    }

    protected open fun resetMetrics() {
        myMetrics = null
    }

    protected open fun metrics(): ZLTextMetrics {
        // this local variable is used to guarantee null will not
        // be returned from this method even in multi-thread environment
        var m = myMetrics
        if (m == null) {
            m = ZLTextMetrics(
                ZLibrary.Instance().getDisplayDPI(),
                // TODO: screen area width
                100,
                // TODO: screen area height
                100,
                getTextStyleCollection().baseStyle.getFontSize()
            )
            myMetrics = m
        }
        return m
    }

    fun getWordHeight(): Int {
        if (myWordHeight == -1) {
            val textStyle = myTextStyle!!
            myWordHeight = getContext().getStringHeight() * textStyle.getLineSpacePercent() / 100 + textStyle.getVerticalAlign(metrics())
        }
        return myWordHeight
    }

    abstract fun getTextStyleCollection(): ZLTextStyleCollection

    abstract fun getImageFitting(): ImageFitting

    abstract fun getLeftMargin(): Int

    abstract fun getRightMargin(): Int

    abstract fun getTopMargin(): Int

    abstract fun getBottomMargin(): Int

    abstract fun getSpaceBetweenColumns(): Int

    abstract fun twoColumnView(): Boolean

    abstract fun getWallpaperFile(): ZLFile?

    abstract fun getFillMode(): ZLPaintContext.FillMode

    abstract fun getBackgroundColor(): ZLColor?

    abstract fun getSelectionBackgroundColor(): ZLColor?

    abstract fun getSelectionForegroundColor(): ZLColor?

    abstract fun getHighlightingBackgroundColor(): ZLColor?

    abstract fun getHighlightingForegroundColor(): ZLColor?

    abstract fun getTextColor(hyperlink: ZLTextHyperlink): ZLColor?

    open fun getTextAreaSize(): ZLPaintContext.Size =
        ZLPaintContext.Size(getTextColumnWidth(), getTextAreaHeight())

    open fun getTextAreaHeight(): Int =
        getContextHeight() - getTopMargin() - getBottomMargin()

    protected open fun getColumnIndex(x: Int): Int {
        if (!twoColumnView()) {
            return -1
        }
        return if (2 * x <= getContextWidth() + getLeftMargin() - getRightMargin()) 0 else 1
    }

    open fun getTextColumnWidth(): Int =
        if (twoColumnView()) {
            (getContextWidth() - getLeftMargin() - getSpaceBetweenColumns() - getRightMargin()) / 2
        } else {
            getContextWidth() - getLeftMargin() - getRightMargin()
        }

    fun getTextStyle(): ZLTextStyle {
        if (myTextStyle == null) {
            resetTextStyle()
        }
        return myTextStyle!!
    }

    fun setTextStyle(style: ZLTextStyle) {
        if (myTextStyle !== style) {
            myTextStyle = style
            myWordHeight = -1
        }
        getContext().setFont(
            style.getFontEntries(),
            style.getFontSize(metrics()),
            style.isBold(),
            style.isItalic(),
            style.isUnderline(),
            style.isStrikeThrough()
        )
    }

    fun resetTextStyle() {
        setTextStyle(getTextStyleCollection().baseStyle)
    }

    open fun isStyleChangeElement(element: ZLTextElement): Boolean =
        element === ZLTextElement.StyleClose ||
            element is ZLTextStyleElement ||
            element is ZLTextControlElement

    open fun applyStyleChangeElement(element: ZLTextElement) {
        when {
            element === ZLTextElement.StyleClose -> applyStyleClose()
            element is ZLTextStyleElement -> applyStyle(element)
            element is ZLTextControlElement -> applyControl(element)
        }
    }

    open fun applyStyleChanges(cursor: ZLTextParagraphCursor, index: Int, end: Int) {
        var i = index
        while (i != end) {
            applyStyleChangeElement(cursor.getElement(i)!!)
            ++i
        }
    }

    private fun applyControl(control: ZLTextControlElement) {
        if (control.isStart) {
            val hyperlink = if (control is ZLTextHyperlinkControlElement) control.Hyperlink else null
            val description = getTextStyleCollection().getDescription(control.kind)
            if (description != null) {
                setTextStyle(ZLTextNGStyle(myTextStyle!!, description, hyperlink))
            }
        } else {
            setTextStyle(myTextStyle!!.Parent)
        }
    }

    private fun applyStyle(element: ZLTextStyleElement) {
        setTextStyle(ZLTextExplicitlyDecoratedStyle(myTextStyle!!, element.Entry))
    }

    private fun applyStyleClose() {
        setTextStyle(myTextStyle!!.Parent)
    }

    protected open fun getScalingType(imageElement: ZLTextImageElement): ZLPaintContext.ScalingType =
        when (getImageFitting()) {
            ImageFitting.none -> ZLPaintContext.ScalingType.IntegerCoefficient
            ImageFitting.covers -> if (imageElement.IsCover) {
                ZLPaintContext.ScalingType.FitMaximum
            } else {
                ZLPaintContext.ScalingType.IntegerCoefficient
            }
            ImageFitting.all -> ZLPaintContext.ScalingType.FitMaximum
        }

    fun getElementWidth(element: ZLTextElement, charIndex: Int): Int {
        if (element is ZLTextWord) {
            return getWordWidth(element, charIndex)
        } else if (element is ZLTextImageElement) {
            val imageElement = element
            val size = getContext().imageSize(
                imageElement.ImageData!!,
                getTextAreaSize(),
                getScalingType(imageElement)
            )
            return if (size != null) size.Width else 0
        } else if (element is ZLTextVideoElement) {
            return minOf(300, getTextColumnWidth())
        } else if (element is ExtensionElement) {
            return element.getWidth()
        } else if (element === ZLTextElement.NBSpace) {
            return getContext().getSpaceWidth()
        } else if (element === ZLTextElement.Indent) {
            return myTextStyle!!.getFirstLineIndent(metrics())
        } else if (element is ZLTextFixedHSpaceElement) {
            return getContext().getSpaceWidth() * element.length.toInt()
        }
        return 0
    }

    fun getElementHeight(element: ZLTextElement): Int {
        if (element === ZLTextElement.NBSpace ||
            element is ZLTextWord ||
            element is ZLTextFixedHSpaceElement
        ) {
            return getWordHeight()
        } else if (element is ZLTextImageElement) {
            val imageElement = element
            val size = getContext().imageSize(
                imageElement.ImageData!!,
                getTextAreaSize(),
                getScalingType(imageElement)
            )
            return (if (size != null) size.Height else 0) +
                maxOf(getContext().getStringHeight() * (myTextStyle!!.getLineSpacePercent() - 100) / 100, 3)
        } else if (element is ZLTextVideoElement) {
            return minOf(minOf(200, getTextAreaHeight()), getTextColumnWidth() * 2 / 3)
        } else if (element is ExtensionElement) {
            return element.getHeight()
        }
        return 0
    }

    fun getElementDescent(element: ZLTextElement): Int =
        if (element is ZLTextWord) getContext().getDescent() else 0

    fun getWordWidth(word: ZLTextWord, start: Int): Int =
        if (start == 0) {
            word.getWidth(getContext())
        } else {
            getContext().getStringWidth(word.Data, word.Offset + start, word.Length - start)
        }

    fun getWordWidth(word: ZLTextWord, start: Int, length: Int): Int =
        getContext().getStringWidth(word.Data, word.Offset + start, length)

    fun getWordWidth(word: ZLTextWord, start: Int, length: Int, addHyphenationSign: Boolean): Int {
        var len = length
        if (len == -1) {
            if (start == 0) {
                return word.getWidth(getContext())
            }
            len = word.Length - start
        }
        if (!addHyphenationSign) {
            return getContext().getStringWidth(word.Data, word.Offset + start, len)
        }
        var part = myWordPartArray
        if (len + 1 > part.size) {
            part = CharArray(len + 1)
            myWordPartArray = part
        }
        System.arraycopy(word.Data, word.Offset + start, part, 0, len)
        part[len] = '-'
        return getContext().getStringWidth(part, 0, len + 1)
    }

    open fun getAreaLength(paragraph: ZLTextParagraphCursor, area: ZLTextElementArea, toCharIndex: Int): Int {
        setTextStyle(area.Style)
        val word = paragraph.getElement(area.ElementIndex) as ZLTextWord
        var length = toCharIndex - area.CharIndex
        var selectHyphenationSign = false
        if (length >= area.Length) {
            selectHyphenationSign = area.AddHyphenationSign
            length = area.Length
        }
        return if (length > 0) {
            getWordWidth(word, area.CharIndex, length, selectHyphenationSign)
        } else {
            0
        }
    }

    fun drawWord(
        x: Int,
        y: Int,
        word: ZLTextWord,
        start: Int,
        length: Int,
        addHyphenationSign: Boolean,
        color: ZLColor
    ) {
        val context = getContext()
        if (start == 0 && length == -1) {
            drawString(context, x, y, word.Data, word.Offset, word.Length, word.getMark(), color, 0)
        } else {
            var len = length
            if (len == -1) {
                len = word.Length - start
            }
            if (!addHyphenationSign) {
                drawString(context, x, y, word.Data, word.Offset + start, len, word.getMark(), color, start)
            } else {
                var part = myWordPartArray
                if (len + 1 > part.size) {
                    part = CharArray(len + 1)
                    myWordPartArray = part
                }
                System.arraycopy(word.Data, word.Offset + start, part, 0, len)
                part[len] = '-'
                drawString(context, x, y, part, 0, len + 1, word.getMark(), color, start)
            }
        }
    }

    private fun drawString(
        context: ZLPaintContext,
        x0: Int,
        y: Int,
        str: CharArray,
        offset: Int,
        length: Int,
        mark: ZLTextWord.Mark?,
        color: ZLColor,
        shift: Int
    ) {
        var x = x0
        if (mark == null) {
            context.setTextColor(color)
            context.drawString(x, y, str, offset, length)
        } else {
            var pos = 0
            var currentMark: ZLTextWord.Mark? = mark
            while (currentMark != null && pos < length) {
                var markStart = currentMark.Start - shift
                var markLen = currentMark.Length

                if (markStart < pos) {
                    markLen += markStart - pos
                    markStart = pos
                }

                if (markLen <= 0) {
                    currentMark = currentMark.getNext()
                    continue
                }

                if (markStart > pos) {
                    val endPos = minOf(markStart, length)
                    context.setTextColor(color)
                    context.drawString(x, y, str, offset + pos, endPos - pos)
                    x += context.getStringWidth(str, offset + pos, endPos - pos)
                }

                if (markStart < length) {
                    context.setFillColor(getHighlightingBackgroundColor()!!)
                    val endPos = minOf(markStart + markLen, length)
                    val endX = x + context.getStringWidth(str, offset + markStart, endPos - markStart)
                    context.fillRectangle(x, y - context.getStringHeight(), endX - 1, y + context.getDescent())
                    context.setTextColor(getHighlightingForegroundColor()!!)
                    context.drawString(x, y, str, offset + markStart, endPos - markStart)
                    x = endX
                }
                pos = markStart + markLen
                currentMark = currentMark.getNext()
            }

            if (pos < length) {
                context.setTextColor(color)
                context.drawString(x, y, str, offset + pos, length - pos)
            }
        }
    }

    @Suppress("EnumEntryName")
    enum class ImageFitting {
        none, covers, all
    }
}
