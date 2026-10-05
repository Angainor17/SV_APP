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

import org.geometerplus.zlibrary.text.model.ZLTextMark

class ZLTextWordCursor : ZLTextPosition {
    private var myParagraphCursor: ZLTextParagraphCursor? = null
    private var myElementIndex = 0
    private var myCharIndex = 0

    constructor()

    constructor(cursor: ZLTextWordCursor) {
        setCursor(cursor)
    }

    constructor(paragraphCursor: ZLTextParagraphCursor) {
        setCursor(paragraphCursor)
    }

    fun setCursor(cursor: ZLTextWordCursor) {
        myParagraphCursor = cursor.myParagraphCursor
        myElementIndex = cursor.myElementIndex
        myCharIndex = cursor.myCharIndex
    }

    fun setCursor(paragraphCursor: ZLTextParagraphCursor) {
        myParagraphCursor = paragraphCursor
        myElementIndex = 0
        myCharIndex = 0
    }

    fun isNull(): Boolean = myParagraphCursor == null

    fun isStartOfParagraph(): Boolean = myElementIndex == 0 && myCharIndex == 0

    fun isStartOfText(): Boolean = isStartOfParagraph() && myParagraphCursor!!.isFirst()

    val isEndOfParagraph: Boolean
        get() {
            val cursor = myParagraphCursor ?: return false
            return myElementIndex == cursor.getParagraphLength()
        }

    fun isEndOfText(): Boolean = isEndOfParagraph && myParagraphCursor!!.isLast()

    override val paragraphIndex: Int
        get() = myParagraphCursor?.Index ?: 0

    override val elementIndex: Int
        get() = myElementIndex

    override val charIndex: Int
        get() = myCharIndex

    fun setCharIndex(charIndex: Int) {
        val ci = maxOf(0, charIndex)
        myCharIndex = 0
        if (ci > 0) {
            val element = myParagraphCursor!!.getElement(myElementIndex)
            if (element is ZLTextWord) {
                if (ci <= element.Length) {
                    myCharIndex = ci
                }
            }
        }
    }

    val element: ZLTextElement
        get() = myParagraphCursor!!.getElement(myElementIndex)

    fun getParagraphCursor(): ZLTextParagraphCursor? = myParagraphCursor

    fun getMark(): ZLTextMark? {
        val paragraph = myParagraphCursor ?: return null
        val paragraphLength = paragraph.getParagraphLength()
        var wordIndex = myElementIndex
        while (wordIndex < paragraphLength && paragraph.getElement(wordIndex) !is ZLTextWord) {
            wordIndex++
        }
        if (wordIndex < paragraphLength) {
            return ZLTextMark(
                paragraph.Index,
                (paragraph.getElement(wordIndex) as ZLTextWord).getParagraphOffset(),
                0,
            )
        }
        return ZLTextMark(paragraph.Index + 1, 0, 0)
    }

    fun nextWord() {
        myElementIndex++
        myCharIndex = 0
    }

    fun previousWord() {
        myElementIndex--
        myCharIndex = 0
    }

    fun nextParagraph(): Boolean {
        val cursor = myParagraphCursor
        if (cursor != null) {
            if (!cursor.isLast()) {
                myParagraphCursor = cursor.next()
                moveToParagraphStart()
                return true
            }
        }
        return false
    }

    fun previousParagraph(): Boolean {
        val cursor = myParagraphCursor
        if (cursor != null) {
            if (!cursor.isFirst()) {
                myParagraphCursor = cursor.previous()
                moveToParagraphStart()
                return true
            }
        }
        return false
    }

    fun moveToParagraphStart() {
        if (myParagraphCursor != null) {
            myElementIndex = 0
            myCharIndex = 0
        }
    }

    fun moveToParagraphEnd() {
        val cursor = myParagraphCursor
        if (cursor != null) {
            myElementIndex = cursor.getParagraphLength()
            myCharIndex = 0
        }
    }

    fun moveToParagraph(paragraphIndex: Int) {
        val cursor = myParagraphCursor
        if (cursor != null && paragraphIndex != cursor.Index) {
            val model = cursor.Model
            val clamped = maxOf(0, minOf(paragraphIndex, model.getParagraphsNumber() - 1))
            myParagraphCursor = cursor.CursorManager.get(clamped)
            moveToParagraphStart()
        }
    }

    fun moveTo(position: ZLTextPosition) {
        moveToParagraph(position.paragraphIndex)
        moveTo(position.elementIndex, position.charIndex)
    }

    fun moveTo(wordIndex: Int, charIndex: Int) {
        val cursor = myParagraphCursor
        if (cursor != null) {
            if (wordIndex == 0 && charIndex == 0) {
                myElementIndex = 0
                myCharIndex = 0
            } else {
                val wi = maxOf(0, wordIndex)
                val size = cursor.getParagraphLength()
                if (wi > size) {
                    myElementIndex = size
                    myCharIndex = 0
                } else {
                    myElementIndex = wi
                    setCharIndex(charIndex)
                }
            }
        }
    }

    fun reset() {
        myParagraphCursor = null
        myElementIndex = 0
        myCharIndex = 0
    }

    fun rebuild() {
        val cursor = myParagraphCursor
        if (cursor != null) {
            cursor.clear()
            cursor.fill()
            moveTo(myElementIndex, myCharIndex)
        }
    }

    override fun toString(): String =
        super.toString() + " (" + myParagraphCursor + "," + myElementIndex + "," + myCharIndex + ")"
}
