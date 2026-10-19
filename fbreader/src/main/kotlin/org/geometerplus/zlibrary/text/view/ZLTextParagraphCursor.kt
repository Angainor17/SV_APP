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

import org.geometerplus.zlibrary.core.image.ZLImageManager
import org.geometerplus.zlibrary.core.resources.ZLResource
import org.geometerplus.zlibrary.text.model.ZLTextMark
import org.geometerplus.zlibrary.text.model.ZLTextModel
import org.geometerplus.zlibrary.text.model.ZLTextOtherStyleEntry
import org.geometerplus.zlibrary.text.model.ZLTextParagraph
import org.geometerplus.zlibrary.text.model.ZLTextStyleEntry
import org.vimgadgets.linebreak.LineBreaker

class ZLTextParagraphCursor internal constructor(
    cManager: CursorManager,
    @JvmField val Model: ZLTextModel,
    index: Int,
) {
    internal val CursorManager: CursorManager = cManager

    @JvmField
    val Index: Int = minOf(index, Model.getParagraphsNumber() - 1)

    private val myElements = ArrayList<ZLTextElement>()

    init {
        fill()
    }

    constructor(model: ZLTextModel, index: Int) : this(CursorManager(model, null), model, index)

    fun fill() {
        val paragraph = Model.getParagraph(Index)!!
        when (paragraph.getKind()) {
            ZLTextParagraph.Kind.TEXT_PARAGRAPH ->
                Processor(
                    paragraph,
                    CursorManager.ExtensionManager,
                    LineBreaker(Model.getLanguage()!!),
                    Model.getMarks(),
                    Index,
                    myElements,
                ).fill()

            ZLTextParagraph.Kind.EMPTY_LINE_PARAGRAPH ->
                myElements.add(ZLTextWord(SPACE_ARRAY, 0, 1, 0))

            ZLTextParagraph.Kind.ENCRYPTED_SECTION_PARAGRAPH -> {
                val entry = ZLTextOtherStyleEntry()
                entry.setFontModifier(ZLTextStyleEntry.FontModifier.FONT_MODIFIER_BOLD, true)
                myElements.add(ZLTextStyleElement(entry))
                myElements.add(ZLTextWord(ZLResource.resource("drm").getResource("encryptedSection").getValue(), 0))
            }

            else -> {}
        }
    }

    fun clear() {
        myElements.clear()
    }

    fun isFirst(): Boolean = Index == 0

    fun isLast(): Boolean = Index + 1 >= Model.getParagraphsNumber()

    fun isLikeEndOfSection(): Boolean =
        when (Model.getParagraph(Index)!!.getKind()) {
            ZLTextParagraph.Kind.END_OF_SECTION_PARAGRAPH,
            ZLTextParagraph.Kind.PSEUDO_END_OF_SECTION_PARAGRAPH,
            -> true

            else -> false
        }

    fun isEndOfSection(): Boolean =
        Model.getParagraph(Index)!!.getKind() == ZLTextParagraph.Kind.END_OF_SECTION_PARAGRAPH

    fun getParagraphLength(): Int = myElements.size

    fun previous(): ZLTextParagraphCursor? =
        if (isFirst()) null else CursorManager.get(Index - 1)

    fun next(): ZLTextParagraphCursor? =
        if (isLast()) null else CursorManager.get(Index + 1)

    fun getElement(index: Int): ZLTextElement? =
        try {
            myElements[index]
        } catch (e: IndexOutOfBoundsException) {
            null
        }

    fun getParagraph(): ZLTextParagraph? = Model.getParagraph(Index)

    override fun toString(): String =
        "ZLTextParagraphCursor [$Index (0..${myElements.size})]"

    private companion object {
        private val SPACE_ARRAY = charArrayOf(' ')
    }

    private class Processor(
        private val myParagraph: ZLTextParagraph,
        private val myExtManager: ExtensionElementManager?,
        private val myLineBreaker: LineBreaker,
        private val myMarks: List<ZLTextMark>,
        paragraphIndex: Int,
        private val myElements: ArrayList<ZLTextElement>,
    ) {
        private var myOffset = 0
        private var myFirstMark = 0
        private var myLastMark = 0

        init {
            val mark = ZLTextMark(paragraphIndex, 0, 0)
            var i = 0
            while (i < myMarks.size) {
                if (myMarks[i].compareTo(mark) >= 0) {
                    break
                }
                i++
            }
            myFirstMark = i
            myLastMark = myFirstMark
            while (myLastMark != myMarks.size && myMarks[myLastMark].paragraphIndex == paragraphIndex) {
                myLastMark++
            }
        }

        fun fill() {
            var hyperlinkDepth = 0
            var hyperlink: ZLTextHyperlink? = null

            val elements = myElements
            val it = myParagraph.iterator()!!
            while (it.next()) {
                when (it.getType()) {
                    ZLTextParagraph.Entry.TEXT ->
                        processTextEntry(it.getTextData(), it.getTextOffset(), it.getTextLength(), hyperlink)

                    ZLTextParagraph.Entry.CONTROL -> {
                        if (hyperlink != null) {
                            hyperlinkDepth += if (it.getControlIsStart()) 1 else -1
                            if (hyperlinkDepth == 0) {
                                hyperlink = null
                            }
                        }
                        elements.add(ZLTextControlElement.get(it.getControlKind(), it.getControlIsStart()))
                    }

                    ZLTextParagraph.Entry.HYPERLINK_CONTROL -> {
                        val hyperlinkType = it.getHyperlinkType()
                        if (hyperlinkType != 0.toByte()) {
                            val control = ZLTextHyperlinkControlElement(
                                it.getControlKind(), hyperlinkType, it.getHyperlinkId(),
                            )
                            elements.add(control)
                            hyperlink = control.Hyperlink
                            hyperlinkDepth = 1
                        }
                    }

                    ZLTextParagraph.Entry.IMAGE -> {
                        val imageEntry = it.getImageEntry()
                        val image = imageEntry.image
                        if (image != null) {
                            val data = ZLImageManager.Instance().getImageData(image)
                            if (data != null) {
                                if (hyperlink != null) {
                                    hyperlink.addElementIndex(elements.size)
                                }
                                elements.add(
                                    ZLTextImageElement(imageEntry.id, data, image.getURI(), imageEntry.isCover),
                                )
                            }
                        }
                    }

                    ZLTextParagraph.Entry.AUDIO -> {}

                    ZLTextParagraph.Entry.VIDEO ->
                        elements.add(ZLTextVideoElement(it.getVideoEntry().sources()))

                    ZLTextParagraph.Entry.EXTENSION -> {
                        val extManager = myExtManager
                        if (extManager != null) {
                            elements.addAll(extManager.getElements(it.getExtensionEntry()))
                        }
                    }

                    ZLTextParagraph.Entry.STYLE_CSS,
                    ZLTextParagraph.Entry.STYLE_OTHER,
                    ->
                        elements.add(ZLTextStyleElement(it.getStyleEntry()))

                    ZLTextParagraph.Entry.STYLE_CLOSE ->
                        elements.add(ZLTextElement.StyleClose)

                    ZLTextParagraph.Entry.FIXED_HSPACE ->
                        elements.add(ZLTextFixedHSpaceElement.getElement(it.getFixedHSpaceLength()))
                }
            }
        }

        private fun processTextEntry(
            data: CharArray,
            offset: Int,
            length: Int,
            hyperlink: ZLTextHyperlink?,
        ) {
            if (length != 0) {
                if (ourBreaks.size < length) {
                    ourBreaks = ByteArray(length)
                }
                val breaks = ourBreaks
                myLineBreaker.setLineBreaks(data, offset, length, breaks)

                val hSpace = ZLTextElement.HSpace
                val nbSpace = ZLTextElement.NBSpace
                val elements = myElements
                var ch = ' '
                var previousChar = ' '
                var spaceState = NO_SPACE
                var wordStart = 0
                var index = 0
                while (index < length) {
                    previousChar = ch
                    ch = data[offset + index]
                    if (Character.isWhitespace(ch)) {
                        if (index > 0 && spaceState == NO_SPACE) {
                            addWord(data, offset + wordStart, index - wordStart, myOffset + wordStart, hyperlink)
                        }
                        spaceState = SPACE
                    } else if (Character.isSpaceChar(ch)) {
                        if (index > 0 && spaceState == NO_SPACE) {
                            addWord(data, offset + wordStart, index - wordStart, myOffset + wordStart, hyperlink)
                        }
                        elements.add(nbSpace)
                        if (spaceState != SPACE) {
                            spaceState = NON_BREAKABLE_SPACE
                        }
                    } else {
                        when (spaceState) {
                            SPACE -> {
                                elements.add(hSpace)
                                wordStart = index
                            }

                            NON_BREAKABLE_SPACE ->
                                wordStart = index

                            NO_SPACE -> {
                                if (index > 0 &&
                                    breaks[index - 1].toInt() != LineBreaker.NOBREAK.toInt() &&
                                    previousChar != '-' &&
                                    index != wordStart
                                ) {
                                    addWord(data, offset + wordStart, index - wordStart, myOffset + wordStart, hyperlink)
                                    wordStart = index
                                }
                            }
                        }
                        spaceState = NO_SPACE
                    }
                    ++index
                }
                when (spaceState) {
                    SPACE -> elements.add(hSpace)
                    NON_BREAKABLE_SPACE -> elements.add(nbSpace)
                    NO_SPACE ->
                        addWord(data, offset + wordStart, length - wordStart, myOffset + wordStart, hyperlink)
                }
                myOffset += length
            }
        }

        private fun addWord(
            data: CharArray,
            offset: Int,
            len: Int,
            paragraphOffset: Int,
            hyperlink: ZLTextHyperlink?,
        ) {
            val word = ZLTextWord(data, offset, len, paragraphOffset)
            var i = myFirstMark
            while (i < myLastMark) {
                val mark = myMarks[i]
                if (mark.offset < paragraphOffset + len && mark.offset + mark.length > paragraphOffset) {
                    word.addMark(mark.offset - paragraphOffset, mark.length)
                }
                i++
            }
            if (hyperlink != null) {
                hyperlink.addElementIndex(myElements.size)
            }
            myElements.add(word)
        }

        private companion object {
            private const val NO_SPACE = 0
            private const val SPACE = 1
            private const val NON_BREAKABLE_SPACE = 2
            private var ourBreaks = ByteArray(1024)
        }
    }
}
