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

package org.geometerplus.fbreader.util

import org.geometerplus.fbreader.bookmodel.FBTextKind
import org.geometerplus.zlibrary.text.view.ZLTextControlElement
import org.geometerplus.zlibrary.text.view.ZLTextElement
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition
import org.geometerplus.zlibrary.text.view.ZLTextPosition
import org.geometerplus.zlibrary.text.view.ZLTextWord
import org.geometerplus.zlibrary.text.view.ZLTextWordCursor

class AutoTextSnippet(start: ZLTextWordCursor, maxChars: Int) : TextSnippet {
    @JvmField
    val IsEndOfText: Boolean

    private val myStart: ZLTextPosition
    private val myEnd: ZLTextPosition
    private val myText: String

    init {
        val cursor = ZLTextWordCursor(start)

        val buffer = Buffer(cursor)
        val sentenceBuffer = Buffer(cursor)
        val phraseBuffer = Buffer(cursor)

        var wordCounter = 0
        var sentenceCounter = 0
        var storedWordCounter = 0
        var lineIsNonEmpty = false
        var appendLineBreak = false

        mainLoop@
        while (buffer.Builder.length + sentenceBuffer.Builder.length + phraseBuffer.Builder.length < maxChars &&
            sentenceCounter < maxChars / 20
        ) {
            while (cursor.isEndOfParagraph) {
                if (!cursor.nextParagraph()) {
                    break@mainLoop
                }
                if (!buffer.isEmpty() && cursor.getParagraphCursor()!!.isLikeEndOfSection()) {
                    break@mainLoop
                }
                if (!phraseBuffer.isEmpty()) {
                    sentenceBuffer.append(phraseBuffer)
                }
                if (!sentenceBuffer.isEmpty()) {
                    if (appendLineBreak) {
                        buffer.append("\n")
                    }
                    buffer.append(sentenceBuffer)
                    ++sentenceCounter
                    storedWordCounter = wordCounter
                }
                lineIsNonEmpty = false
                if (!buffer.isEmpty()) {
                    appendLineBreak = true
                }
            }

            val element = cursor.element
            when {
                element === ZLTextElement.HSpace -> {
                    if (lineIsNonEmpty) {
                        phraseBuffer.append(" ")
                    }
                }
                element === ZLTextElement.NBSpace -> {
                    if (lineIsNonEmpty) {
                        phraseBuffer.append(" ")
                    }
                }
                element is ZLTextWord -> {
                    phraseBuffer.Builder.append(element.Data, element.Offset, element.Length)
                    phraseBuffer.Cursor.setCursor(cursor)
                    phraseBuffer.Cursor.setCharIndex(element.Length)
                    ++wordCounter
                    lineIsNonEmpty = true
                    when (element.Data[element.Offset + element.Length - 1]) {
                        ',', ':', ';', ')' -> sentenceBuffer.append(phraseBuffer)
                        '.', '!', '?' -> {
                            ++sentenceCounter
                            if (appendLineBreak) {
                                buffer.append("\n")
                                appendLineBreak = false
                            }
                            sentenceBuffer.append(phraseBuffer)
                            buffer.append(sentenceBuffer)
                            storedWordCounter = wordCounter
                        }
                    }
                }
                element is ZLTextControlElement -> {
                    if (element.isStart) {
                        when (element.kind) {
                            FBTextKind.H1, FBTextKind.H2 -> {
                                if (!buffer.isEmpty()) {
                                    break@mainLoop
                                }
                            }
                        }
                    }
                }
            }
            cursor.nextWord()
        }

        IsEndOfText =
            cursor.isEndOfText() || cursor.getParagraphCursor()!!.isLikeEndOfSection()

        if (IsEndOfText) {
            sentenceBuffer.append(phraseBuffer)
            if (appendLineBreak) {
                buffer.append("\n")
            }
            buffer.append(sentenceBuffer)
        } else if (storedWordCounter < 4 || sentenceCounter < maxChars / 30) {
            if (sentenceBuffer.isEmpty()) {
                sentenceBuffer.append(phraseBuffer)
            }
            if (appendLineBreak) {
                buffer.append("\n")
            }
            buffer.append(sentenceBuffer)
        }

        myStart = ZLTextFixedPosition(start)
        myEnd = buffer.Cursor
        myText = buffer.Builder.toString()
    }

    override fun getStart(): ZLTextPosition = myStart

    override fun getEnd(): ZLTextPosition = myEnd

    override fun getText(): String = myText

    private class Buffer(cursor: ZLTextWordCursor) {
        val Builder = StringBuilder()
        val Cursor = ZLTextWordCursor(cursor)

        fun isEmpty(): Boolean = Builder.length == 0

        fun append(buffer: Buffer) {
            Builder.append(buffer.Builder)
            Cursor.setCursor(buffer.Cursor)
            buffer.Builder.delete(0, buffer.Builder.length)
        }

        fun append(data: CharSequence) {
            Builder.append(data)
        }
    }
}
