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

package org.geometerplus.fbreader.bookmodel

import org.geometerplus.fbreader.book.Book
import org.geometerplus.fbreader.book.BookUtil
import org.geometerplus.fbreader.formats.BookReadingException
import org.geometerplus.fbreader.formats.BuiltinFormatPlugin
import org.geometerplus.fbreader.formats.FormatPlugin
import org.geometerplus.zlibrary.core.fonts.FileInfo
import org.geometerplus.zlibrary.core.fonts.FontEntry
import org.geometerplus.zlibrary.core.fonts.FontManager
import org.geometerplus.zlibrary.core.image.ZLImage
import org.geometerplus.zlibrary.text.model.CachedCharStorage
import org.geometerplus.zlibrary.text.model.ZLTextModel
import org.geometerplus.zlibrary.text.model.ZLTextPlainModel
import java.util.Arrays
import java.util.HashMap

class BookModel private constructor(book: Book) {
    @JvmField
    val Book: Book = book

    @JvmField
    val TOCTree: TOCTree = TOCTree()

    @JvmField
    val FontManager: FontManager = FontManager()

    protected val myImageMap = HashMap<String, ZLImage>()
    protected val myFootnotes = HashMap<String, ZLTextModel>()
    protected var myInternalHyperlinks: CachedCharStorage? = null
    protected var myBookTextModel: ZLTextModel? = null
    private var myResolver: LabelResolver? = null
    private var myCurrentTree: TOCTree = TOCTree

    fun setLabelResolver(resolver: LabelResolver) {
        myResolver = resolver
    }

    fun getLabel(id: String): Label? {
        var label = getLabelInternal(id)
        val resolver = myResolver
        if (label == null && resolver != null) {
            for (candidate in resolver.getCandidates(id)) {
                label = getLabelInternal(candidate)
                if (label != null) {
                    break
                }
            }
        }
        return label
    }

    fun registerFontFamilyList(families: Array<String>) {
        FontManager.index(Arrays.asList(*families))
    }

    fun registerFontEntry(family: String, entry: FontEntry) {
        FontManager.Entries[family] = entry
    }

    fun registerFontEntry(
        family: String,
        normal: FileInfo?,
        bold: FileInfo?,
        italic: FileInfo?,
        boldItalic: FileInfo?,
    ) {
        registerFontEntry(family, FontEntry(family, normal, bold, italic, boldItalic))
    }

    fun createTextModel(
        id: String,
        language: String,
        paragraphsNumber: Int,
        entryIndices: IntArray,
        entryOffsets: IntArray,
        paragraphLenghts: IntArray,
        textSizes: IntArray,
        paragraphKinds: ByteArray,
        directoryName: String,
        fileExtension: String,
        blocksNumber: Int,
    ): ZLTextModel =
        ZLTextPlainModel(
            id, language, paragraphsNumber,
            entryIndices, entryOffsets,
            paragraphLenghts, textSizes, paragraphKinds,
            directoryName, fileExtension, blocksNumber, myImageMap, FontManager,
        )

    fun setBookTextModel(model: ZLTextModel) {
        myBookTextModel = model
    }

    fun setFootnoteModel(model: ZLTextModel) {
        myFootnotes[model.getId()!!] = model
    }

    val textModel: ZLTextModel?
        get() = myBookTextModel

    fun getFootnoteModel(id: String): ZLTextModel? = myFootnotes[id]

    fun addImage(id: String, image: ZLImage) {
        myImageMap[id] = image
    }

    fun initInternalHyperlinks(directoryName: String, fileExtension: String, blocksNumber: Int) {
        myInternalHyperlinks = CachedCharStorage(directoryName, fileExtension, blocksNumber)
    }

    fun addTOCItem(text: String, reference: Int) {
        myCurrentTree = TOCTree(myCurrentTree)
        myCurrentTree.text = text
        myCurrentTree.setReference(myBookTextModel, reference)
    }

    fun leaveTOCItem() {
        myCurrentTree = myCurrentTree.Parent ?: TOCTree
    }

    private fun getLabelInternal(id: String): Label? {
        val storage = myInternalHyperlinks!!
        val len = id.length
        val size = storage.size()
        for (i in 0 until size) {
            val block = storage.block(i)!!
            var offset = 0
            while (offset < block.size) {
                val labelLength = block[offset].toInt()
                offset++
                if (labelLength == 0) {
                    break
                }
                val idLength = block[offset + labelLength].toInt()
                if (labelLength != len || id != String(block, offset, labelLength)) {
                    offset += labelLength + idLength + 3
                    continue
                }
                offset += labelLength + 1
                val modelId: String? = if (idLength > 0) String(block, offset, idLength) else null
                offset += idLength
                val paragraphNumber = block[offset].toInt() + (block[offset + 1].toInt() shl 16)
                return Label(modelId, paragraphNumber)
            }
        }
        return null
    }

    interface LabelResolver {
        fun getCandidates(id: String): List<String>
    }

    class Label(@JvmField val ModelId: String?, @JvmField val ParagraphIndex: Int)

    companion object {
        @JvmStatic
        @Throws(BookReadingException::class)
        fun createModel(book: Book, plugin: FormatPlugin): BookModel {
            if (plugin is BuiltinFormatPlugin) {
                val model = BookModel(book)
                plugin.readModel(model)
                return model
            }
            throw BookReadingException(
                "unknownPluginType", BookUtil.fileByBook(book), arrayOf(plugin.toString()),
            )
        }
    }
}
