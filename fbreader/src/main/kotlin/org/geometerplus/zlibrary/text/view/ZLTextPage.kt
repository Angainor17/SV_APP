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

import java.util.ArrayList

class ZLTextPage {
    @JvmField
    val StartCursor = ZLTextWordCursor()

    @JvmField
    val EndCursor = ZLTextWordCursor()

    @JvmField
    val LineInfos = ArrayList<ZLTextLineInfo>()

    @JvmField
    var TextElementMap = ZLTextElementAreaVector()

    @JvmField
    var topMargin: Int = 0

    @JvmField
    var Column0Height: Int = 0

    @JvmField
    var PaintState: Int = PaintStateEnum.NOTHING_TO_PAINT

    private var myColumnWidth: Int = 0
    private var myHeight: Int = 0
    private var myTwoColumnView: Boolean = false

    fun setSize(columnWidth: Int, height: Int, twoColumnView: Boolean, keepEndNotStart: Boolean) {
        if (myColumnWidth == columnWidth && myHeight == height && myColumnWidth == columnWidth) {
            return
        }
        myColumnWidth = columnWidth
        myHeight = height
        myTwoColumnView = twoColumnView

        if (PaintState != PaintStateEnum.NOTHING_TO_PAINT) {
            LineInfos.clear()
            if (keepEndNotStart) {
                if (!EndCursor.isNull()) {
                    StartCursor.reset()
                    PaintState = PaintStateEnum.END_IS_KNOWN
                } else if (!StartCursor.isNull()) {
                    EndCursor.reset()
                    PaintState = PaintStateEnum.START_IS_KNOWN
                }
            } else {
                if (!StartCursor.isNull()) {
                    EndCursor.reset()
                    PaintState = PaintStateEnum.START_IS_KNOWN
                } else if (!EndCursor.isNull()) {
                    StartCursor.reset()
                    PaintState = PaintStateEnum.END_IS_KNOWN
                }
            }
        }
    }

    fun reset() {
        StartCursor.reset()
        EndCursor.reset()
        LineInfos.clear()
        PaintState = PaintStateEnum.NOTHING_TO_PAINT
    }

    fun moveStartCursor(cursor: ZLTextParagraphCursor) {
        StartCursor.setCursor(cursor)
        EndCursor.reset()
        LineInfos.clear()
        PaintState = PaintStateEnum.START_IS_KNOWN
    }

    fun moveStartCursor(paragraphIndex: Int, wordIndex: Int, charIndex: Int) {
        if (StartCursor.isNull()) {
            StartCursor.setCursor(EndCursor)
        }
        StartCursor.moveToParagraph(paragraphIndex)
        StartCursor.moveTo(wordIndex, charIndex)
        EndCursor.reset()
        LineInfos.clear()
        PaintState = PaintStateEnum.START_IS_KNOWN
    }

    fun moveEndCursor(paragraphIndex: Int, wordIndex: Int, charIndex: Int) {
        if (EndCursor.isNull()) {
            EndCursor.setCursor(StartCursor)
        }
        EndCursor.moveToParagraph(paragraphIndex)
        if (paragraphIndex > 0 && wordIndex == 0 && charIndex == 0) {
            EndCursor.previousParagraph()
            EndCursor.moveToParagraphEnd()
        } else {
            EndCursor.moveTo(wordIndex, charIndex)
        }
        StartCursor.reset()
        LineInfos.clear()
        PaintState = PaintStateEnum.END_IS_KNOWN
    }

    fun getTextWidth(): Int = myColumnWidth

    fun getTextHeight(): Int = myHeight

    fun twoColumnView(): Boolean = myTwoColumnView

    fun isEmptyPage(): Boolean {
        for (info in LineInfos) {
            if (info.IsVisible) {
                return false
            }
        }
        return true
    }

    fun findLineFromStart(cursor: ZLTextWordCursor, overlappingValue: Int) {
        if (LineInfos.isEmpty() || overlappingValue == 0) {
            cursor.reset()
            return
        }
        var info: ZLTextLineInfo? = null
        var value = overlappingValue
        for (i in LineInfos) {
            info = i
            if (info.IsVisible) {
                --value
                if (value == 0) {
                    break
                }
            }
        }
        cursor.setCursor(info!!.ParagraphCursor)
        cursor.moveTo(info!!.EndElementIndex, info!!.EndCharIndex)
    }

    fun findLineFromEnd(cursor: ZLTextWordCursor, overlappingValue: Int) {
        if (LineInfos.isEmpty() || overlappingValue == 0) {
            cursor.reset()
            return
        }
        val infos = LineInfos
        val size = infos.size
        var info: ZLTextLineInfo? = null
        var value = overlappingValue
        for (i in size - 1 downTo 0) {
            info = infos[i]
            if (info.IsVisible) {
                --value
                if (value == 0) {
                    break
                }
            }
        }
        cursor.setCursor(info!!.ParagraphCursor)
        cursor.moveTo(info!!.StartElementIndex, info!!.StartCharIndex)
    }

    fun findPercentFromStart(cursor: ZLTextWordCursor, percent: Int) {
        if (LineInfos.isEmpty()) {
            cursor.reset()
            return
        }
        var height = myHeight * percent / 100
        var visibleLineOccured = false
        var info: ZLTextLineInfo? = null
        for (i in LineInfos) {
            info = i
            if (info.IsVisible) {
                visibleLineOccured = true
            }
            height -= info.Height + info.Descent + info.VSpaceAfter
            if (visibleLineOccured && height <= 0) {
                break
            }
        }
        cursor.setCursor(info!!.ParagraphCursor)
        cursor.moveTo(info!!.EndElementIndex, info!!.EndCharIndex)
    }
}
