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

abstract class ZLTextTraverser(view: ZLTextView) {
    private val myView: ZLTextView = view

    protected abstract fun processWord(word: ZLTextWord)

    protected abstract fun processControlElement(control: ZLTextControlElement)

    protected abstract fun processSpace()

    protected abstract fun processNbSpace()

    protected abstract fun processEndOfParagraph()

    fun traverse(from: ZLTextPosition, to: ZLTextPosition) {
        val fromParagraph = from.paragraphIndex
        val toParagraph = to.paragraphIndex
        var cursor = myView.cursor(fromParagraph)
        for (i in fromParagraph..toParagraph) {
            val fromElement = if (i == fromParagraph) from.elementIndex else 0
            val toElement = if (i == toParagraph) to.elementIndex else cursor.getParagraphLength() - 1

            for (j in fromElement..toElement) {
                val element = cursor.getElement(j)
                if (element === ZLTextElement.HSpace) {
                    processSpace()
                } else if (element === ZLTextElement.NBSpace) {
                    processNbSpace()
                } else if (element is ZLTextWord) {
                    processWord(element)
                }
            }
            if (i < toParagraph) {
                processEndOfParagraph()
                cursor = cursor.next()
            }
        }
    }
}
