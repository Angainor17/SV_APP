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

import org.geometerplus.zlibrary.core.view.ZLPaintContext

class ZLTextWord : ZLTextElement {
    @JvmField val Data: CharArray
    @JvmField val Offset: Int
    @JvmField val Length: Int

    private var myWidth: Int = -1
    private var myMark: Mark? = null
    private var myParagraphOffset: Int

    constructor(word: String, paragraphOffset: Int) :
            this(word.toCharArray(), 0, word.length, paragraphOffset)

    constructor(data: CharArray, offset: Int, length: Int, paragraphOffset: Int) {
        Data = data
        Offset = offset
        Length = length
        myParagraphOffset = paragraphOffset
    }

    fun isASpace(): Boolean {
        for (i in Offset until Offset + Length) {
            if (!Character.isWhitespace(Data[i])) {
                return false
            }
        }
        return true
    }

    fun getMark(): Mark? = myMark

    fun getParagraphOffset(): Int = myParagraphOffset

    fun addMark(start: Int, length: Int) {
        val mark = Mark(start, length)
        val existingMark = myMark
        if (existingMark == null || existingMark.Start > start) {
            mark.setNext(existingMark)
            myMark = mark
        } else {
            var current = existingMark!!
            while (current.getNext() != null && current.getNext()!!.Start < start) {
                current = current.getNext()!!
            }
            mark.setNext(current.getNext())
            current.setNext(mark)
        }
    }

    fun getWidth(context: ZLPaintContext): Int {
        var width = myWidth
        if (width <= 1) {
            width = context.getStringWidth(Data, Offset, Length)
            myWidth = width
        }
        return width
    }

    override fun toString(): String = getString()

    fun getString(): String = String(Data, Offset, Length)

    inner class Mark(
        @JvmField val Start: Int,
        @JvmField val Length: Int,
    ) {
        private var myNext: Mark? = null

        fun getNext(): Mark? = myNext

        internal fun setNext(mark: Mark?) {
            myNext = mark
        }
    }
}
