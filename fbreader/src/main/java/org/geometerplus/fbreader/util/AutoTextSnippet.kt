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

    @JvmField val isEndOfText: Boolean
    private val start: ZLTextPosition
    private val end: ZLTextPosition
    private val text: String

    init {
        println("AutoTextSnippet $maxChars")
        val cursor = ZLTextWordCursor(start)

        val buffer = Buffer(cursor)
        val sentenceBuffer = Buffer(cursor)
        val phraseBuffer = Buffer(cursor)

        var wordCounter = 0
        var sentenceCounter = 0
        var storedWordCounter = 0
        var lineIsNonEmpty = false
        var appendLineBreak = false

        mainLoop@ while (buffer.builder.length + sentenceBuffer.builder.length + phraseBuffer.builder.length < maxChars && sentenceCounter < maxChars / 20) {
            while (cursor.isEndOfParagraph) {
                if (!cursor.nextParagraph()) {
                    break@mainLoop
                }
                if (!buffer.isEmpty && cursor.paragraphCursor!!.isLikeEndOfSection) {
                    break@mainLoop
                }
                if (!phraseBuffer.isEmpty) {
                    sentenceBuffer.append(phraseBuffer)
                }
                if (!sentenceBuffer.isEmpty) {
                    if (appendLineBreak) {
                        buffer.append("\n")
                    }
                    buffer.append(sentenceBuffer)
                    ++sentenceCounter
                    storedWordCounter = wordCounter
                }
                lineIsNonEmpty = false
                if (!buffer.isEmpty) {
                    appendLineBreak = true
                }
            }
            val element = cursor.getElement()
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
                    val word = element
                    phraseBuffer.builder.append(word.data, word.offset, word.length)
                    phraseBuffer.cursor.setCursor(cursor)
                    phraseBuffer.cursor.setCharIndex(word.length)
                    ++wordCounter
                    lineIsNonEmpty = true
                    when (word.data[word.offset + word.length - 1]) {
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
                                if (!buffer.isEmpty) {
                                    break@mainLoop
                                }
                            }
                        }
                    }
                }
            }
            cursor.nextWord()
        }

        isEndOfText = cursor.isEndOfText || cursor.paragraphCursor!!.isLikeEndOfSection

        if (isEndOfText) {
            sentenceBuffer.append(phraseBuffer)
            if (appendLineBreak) {
                buffer.append("\n")
            }
            buffer.append(sentenceBuffer)
        } else if (storedWordCounter < 4 || sentenceCounter < maxChars / 30) {
            if (sentenceBuffer.isEmpty) {
                sentenceBuffer.append(phraseBuffer)
            }
            if (appendLineBreak) {
                buffer.append("\n")
            }
            buffer.append(sentenceBuffer)
        }

        this.start = ZLTextFixedPosition(start)
        this.end = buffer.cursor
        this.text = buffer.builder.toString()
    }

    override fun getStart(): ZLTextPosition = start

    override fun getEnd(): ZLTextPosition = end

    override fun getText(): String = text

    private class Buffer(cursor: ZLTextWordCursor) {
        val builder = StringBuilder()
        val cursor = ZLTextWordCursor(cursor)

        val isEmpty: Boolean
            get() = builder.isEmpty()

        fun append(buffer: Buffer) {
            builder.append(buffer.builder)
            cursor.setCursor(buffer.cursor)
            buffer.builder.clear()
        }

        fun append(data: CharSequence) {
            builder.append(data)
        }
    }
}
