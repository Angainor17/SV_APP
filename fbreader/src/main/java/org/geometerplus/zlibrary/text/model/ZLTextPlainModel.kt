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

package org.geometerplus.zlibrary.text.model

import org.geometerplus.zlibrary.core.fonts.FontManager
import org.geometerplus.zlibrary.core.image.ZLImage
import org.geometerplus.zlibrary.core.util.ZLSearchPattern
import org.geometerplus.zlibrary.core.util.ZLSearchUtil

class ZLTextPlainModel(
    private val myId: String,
    private val myLanguage: String,
    private val myParagraphsNumber: Int,
    private val myStartEntryIndices: IntArray,
    private val myStartEntryOffsets: IntArray,
    private val myParagraphLengths: IntArray,
    private val myTextSizes: IntArray,
    private val myParagraphKinds: ByteArray,
    directoryName: String,
    fileExtension: String,
    blocksNumber: Int,
    private val myImageMap: Map<String, ZLImage>,
    private val myFontManager: FontManager,
) : ZLTextModel, ZLTextStyleEntry.Feature {

    private val myStorage = CachedCharStorage(directoryName, fileExtension, blocksNumber)
    private var myMarks: ArrayList<ZLTextMark>? = null

    override fun getId(): String? = myId

    override fun getLanguage(): String? = myLanguage

    override fun getFirstMark(): ZLTextMark? {
        val marks = myMarks
        return if (marks == null || marks.isEmpty()) null else marks[0]
    }

    override fun getLastMark(): ZLTextMark? {
        val marks = myMarks
        return if (marks == null || marks.isEmpty()) null else marks[marks.size - 1]
    }

    override fun getNextMark(position: ZLTextMark): ZLTextMark? {
        val marks = myMarks ?: return null

        var mark: ZLTextMark? = null
        for (current in marks) {
            if (current.compareTo(position) >= 0) {
                if (mark == null || mark.compareTo(current) > 0) {
                    mark = current
                }
            }
        }
        return mark
    }

    override fun getPreviousMark(position: ZLTextMark): ZLTextMark? {
        val marks = myMarks ?: return null

        var mark: ZLTextMark? = null
        for (current in marks) {
            if (current.compareTo(position) < 0) {
                if (mark == null || mark.compareTo(current) < 0) {
                    mark = current
                }
            }
        }
        return mark
    }

    override fun search(text: String, startIndex: Int, endIndex: Int, ignoreCase: Boolean): Int {
        var count = 0
        val pattern = ZLSearchPattern(text, ignoreCase)
        val marks = ArrayList<ZLTextMark>()
        myMarks = marks
        var start = startIndex
        if (start > myParagraphsNumber) {
            start = myParagraphsNumber
        }
        var end = endIndex
        if (end > myParagraphsNumber) {
            end = myParagraphsNumber
        }
        var index = start
        val it = EntryIteratorImpl(index)
        while (true) {
            var offset = 0
            while (it.next()) {
                if (it.getType() == ZLTextParagraph.Entry.TEXT) {
                    val textData = it.getTextData()
                    val textOffset = it.getTextOffset()
                    val textLength = it.getTextLength()
                    var res = ZLSearchUtil.find(textData, textOffset, textLength, pattern)
                    while (res != null) {
                        marks.add(ZLTextMark(index, offset + res.Start, res.Length))
                        ++count
                        res = ZLSearchUtil.find(textData, textOffset, textLength, pattern, res.Start + 1)
                    }
                    offset += textLength
                }
            }
            if (++index >= end) {
                break
            }
            it.reset(index)
        }
        return count
    }

    override fun getMarks(): List<ZLTextMark> = myMarks ?: emptyList()

    override fun removeAllMarks() {
        myMarks = null
    }

    override fun getParagraphsNumber(): Int = myParagraphsNumber

    override fun getParagraph(index: Int): ZLTextParagraph {
        val kind = myParagraphKinds[index]
        return if (kind == ZLTextParagraph.Kind.TEXT_PARAGRAPH) {
            ZLTextParagraphImpl(this, index)
        } else {
            ZLTextSpecialParagraphImpl(kind, this, index)
        }
    }

    override fun getTextLength(index: Int): Int {
        if (myTextSizes.isEmpty()) {
            return 0
        }
        return myTextSizes[maxOf(minOf(index, myParagraphsNumber - 1), 0)]
    }

    override fun findParagraphByTextLength(length: Int): Int {
        val index = binarySearch(myTextSizes, myParagraphsNumber, length)
        if (index >= 0) {
            return index
        }
        return minOf(-index - 1, myParagraphsNumber - 1)
    }

    internal inner class EntryIteratorImpl(index: Int) : ZLTextParagraph.EntryIterator {
        private var myDataIndex: Int = 0
        private var myDataOffset: Int = 0
        private var myCounter: Int = 0
        private var myLength: Int = 0
        private var myType: Byte = 0

        // TextEntry data
        private var myTextData: CharArray? = null
        private var myTextOffset: Int = 0
        private var myTextLength: Int = 0

        // ControlEntry data
        private var myControlKind: Byte = 0
        private var myControlIsStart: Boolean = false

        // HyperlinkControlEntry data
        private var myHyperlinkType: Byte = 0
        private var myHyperlinkId: String? = null

        // ImageEntry
        private var myImageEntry: ZLImageEntry? = null

        // VideoEntry
        private var myVideoEntry: ZLVideoEntry? = null

        // ExtensionEntry
        private var myExtensionEntry: ExtensionEntry? = null

        // StyleEntry
        private var myStyleEntry: ZLTextStyleEntry? = null

        // FixedHSpaceEntry data
        private var myFixedHSpaceLength: Short = 0

        init {
            reset(index)
        }

        fun reset(index: Int) {
            myCounter = 0
            myLength = myParagraphLengths[index]
            myDataIndex = myStartEntryIndices[index]
            myDataOffset = myStartEntryOffsets[index]
        }

        override fun getType(): Byte = myType

        override fun getTextData(): CharArray = myTextData!!

        override fun getTextOffset(): Int = myTextOffset

        override fun getTextLength(): Int = myTextLength

        override fun getControlKind(): Byte = myControlKind

        override fun getControlIsStart(): Boolean = myControlIsStart

        override fun getHyperlinkType(): Byte = myHyperlinkType

        override fun getHyperlinkId(): String = myHyperlinkId!!

        override fun getImageEntry(): ZLImageEntry = myImageEntry!!

        override fun getVideoEntry(): ZLVideoEntry = myVideoEntry!!

        override fun getExtensionEntry(): ExtensionEntry = myExtensionEntry!!

        override fun getStyleEntry(): ZLTextStyleEntry = myStyleEntry!!

        override fun getFixedHSpaceLength(): Short = myFixedHSpaceLength

        override fun next(): Boolean {
            if (myCounter >= myLength) {
                return false
            }

            var dataOffset = myDataOffset
            var data = myStorage.block(myDataIndex) ?: return false
            if (dataOffset >= data.size) {
                data = myStorage.block(++myDataIndex) ?: return false
                dataOffset = 0
            }
            var first = data[dataOffset].toShort()
            var type = first.toByte()
            if (type == 0.toByte()) {
                data = myStorage.block(++myDataIndex) ?: return false
                dataOffset = 0
                first = data[0].toShort()
                type = first.toByte()
            }
            myType = type
            ++dataOffset
            when (type) {
                ZLTextParagraph.Entry.TEXT -> {
                    var textLength = data[dataOffset++].code
                    textLength += data[dataOffset++].code shl 16
                    textLength = minOf(textLength, data.size - dataOffset)
                    myTextLength = textLength
                    myTextData = data
                    myTextOffset = dataOffset
                    dataOffset += textLength
                }

                ZLTextParagraph.Entry.CONTROL -> {
                    val kind = data[dataOffset++].toShort()
                    myControlKind = kind.toByte()
                    myControlIsStart = (kind.toInt() and 0x0100) == 0x0100
                    myHyperlinkType = 0
                }

                ZLTextParagraph.Entry.HYPERLINK_CONTROL -> {
                    val kind = data[dataOffset++].toShort()
                    myControlKind = kind.toByte()
                    myControlIsStart = true
                    myHyperlinkType = (kind.toInt() shr 8).toByte()
                    val labelLength = data[dataOffset++].toShort()
                    myHyperlinkId = String(data, dataOffset, labelLength.toInt())
                    dataOffset += labelLength.toInt()
                }

                ZLTextParagraph.Entry.IMAGE -> {
                    val vOffset = data[dataOffset++].toShort()
                    val len = data[dataOffset++].toShort()
                    val id = String(data, dataOffset, len.toInt())
                    dataOffset += len.toInt()
                    val isCover = data[dataOffset++] != ' '
                    myImageEntry = ZLImageEntry(myImageMap, id, vOffset, isCover)
                }

                ZLTextParagraph.Entry.FIXED_HSPACE -> {
                    myFixedHSpaceLength = data[dataOffset++].toShort()
                }

                ZLTextParagraph.Entry.STYLE_CSS,
                ZLTextParagraph.Entry.STYLE_OTHER,
                -> {
                    val depth = ((first.toInt() shr 8) and 0xFF).toShort()
                    val entry =
                        if (type == ZLTextParagraph.Entry.STYLE_CSS) {
                            ZLTextCSSStyleEntry(depth)
                        } else {
                            ZLTextOtherStyleEntry()
                        }

                    val mask = data[dataOffset++].toShort()
                    for (i in 0 until ZLTextStyleEntry.Feature.NUMBER_OF_LENGTHS) {
                        if (ZLTextStyleEntry.isFeatureSupported(mask, i)) {
                            val size = data[dataOffset++].toShort()
                            val unit = data[dataOffset++].toByte()
                            entry.setLength(i, size, unit)
                        }
                    }
                    if (ZLTextStyleEntry.isFeatureSupported(mask, ZLTextStyleEntry.Feature.ALIGNMENT_TYPE) ||
                        ZLTextStyleEntry.isFeatureSupported(mask, ZLTextStyleEntry.Feature.NON_LENGTH_VERTICAL_ALIGN)
                    ) {
                        val value = data[dataOffset++].toShort()
                        if (ZLTextStyleEntry.isFeatureSupported(mask, ZLTextStyleEntry.Feature.ALIGNMENT_TYPE)) {
                            entry.setAlignmentType((value.toInt() and 0xFF).toByte())
                        }
                        if (ZLTextStyleEntry.isFeatureSupported(mask, ZLTextStyleEntry.Feature.NON_LENGTH_VERTICAL_ALIGN)) {
                            entry.setVerticalAlignCode(((value.toInt() shr 8) and 0xFF).toByte())
                        }
                    }
                    if (ZLTextStyleEntry.isFeatureSupported(mask, ZLTextStyleEntry.Feature.FONT_FAMILY)) {
                        entry.setFontFamilies(myFontManager, data[dataOffset++].toShort().toInt())
                    }
                    if (ZLTextStyleEntry.isFeatureSupported(mask, ZLTextStyleEntry.Feature.FONT_STYLE_MODIFIER)) {
                        val value = data[dataOffset++].toShort()
                        entry.setFontModifiers(
                            (value.toInt() and 0xFF).toByte(),
                            ((value.toInt() shr 8) and 0xFF).toByte(),
                        )
                    }

                    myStyleEntry = entry
                }

                ZLTextParagraph.Entry.STYLE_CLOSE,
                ZLTextParagraph.Entry.RESET_BIDI,
                ZLTextParagraph.Entry.AUDIO,
                -> {
                    // No data
                }

                ZLTextParagraph.Entry.VIDEO -> {
                    myVideoEntry = ZLVideoEntry()
                    val mapSize = data[dataOffset++].toShort()
                    for (i in 0 until mapSize.toInt()) {
                        var len = data[dataOffset++].toShort()
                        val mime = String(data, dataOffset, len.toInt())
                        dataOffset += len.toInt()
                        len = data[dataOffset++].toShort()
                        val src = String(data, dataOffset, len.toInt())
                        dataOffset += len.toInt()
                        myVideoEntry!!.addSource(mime, src)
                    }
                }

                ZLTextParagraph.Entry.EXTENSION -> {
                    val kindLength = data[dataOffset++].toShort()
                    val kind = String(data, dataOffset, kindLength.toInt())
                    dataOffset += kindLength.toInt()

                    val map = HashMap<String, String>()
                    val dataSize = ((first.toInt() shr 8) and 0xFF).toShort()
                    for (i in 0 until dataSize.toInt()) {
                        val keyLength = data[dataOffset++].toShort()
                        val key = String(data, dataOffset, keyLength.toInt())
                        dataOffset += keyLength.toInt()
                        val valueLength = data[dataOffset++].toShort()
                        map[key] = String(data, dataOffset, valueLength.toInt())
                        dataOffset += valueLength.toInt()
                    }
                    myExtensionEntry = ExtensionEntry(kind, map)
                }
            }
            ++myCounter
            myDataOffset = dataOffset
            return true
        }
    }

    companion object {
        private fun binarySearch(array: IntArray, length: Int, value: Int): Int {
            var lowIndex = 0
            var highIndex = length - 1

            while (lowIndex <= highIndex) {
                val midIndex = (lowIndex + highIndex) ushr 1
                val midValue = array[midIndex]
                if (midValue > value) {
                    highIndex = midIndex - 1
                } else if (midValue < value) {
                    lowIndex = midIndex + 1
                } else {
                    return midIndex
                }
            }
            return -lowIndex - 1
        }
    }
}
