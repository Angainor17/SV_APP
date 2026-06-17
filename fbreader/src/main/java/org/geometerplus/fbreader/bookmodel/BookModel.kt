package org.geometerplus.fbreader.bookmodel

import org.geometerplus.fbreader.book.Book
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

class BookModel protected constructor(val book: Book) {

    val tocTree: TOCTree = TOCTree.createRoot()
    val fontManager: FontManager = FontManager()

    protected val imageMap: MutableMap<String, ZLImage> = HashMap()
    protected val footnotes: MutableMap<String, ZLTextModel> = HashMap()
    protected var internalHyperlinks: CachedCharStorage? = null
    protected var myBookTextModel: ZLTextModel? = null

    private var resolver: LabelResolver? = null
    private var currentTree: TOCTree = tocTree

    companion object {
        @Throws(BookReadingException::class)
        @JvmStatic
        fun createModel(book: Book, plugin: FormatPlugin): BookModel {
            if (plugin is BuiltinFormatPlugin) {
                val model = BookModel(book)
                plugin.readModel(model)
                return model
            }

            throw BookReadingException(
                "unknownPluginType", null, listOf(plugin.toString())
            )
        }
    }

    fun setLabelResolver(resolver: LabelResolver?) {
        this.resolver = resolver
    }

    fun getLabel(id: String): Label? {
        var label = getLabelInternal(id)
        if (label == null && resolver != null) {
            for (candidate in resolver!!.getCandidates(id)) {
                label = getLabelInternal(candidate)
                if (label != null) {
                    break
                }
            }
        }
        return label
    }

    fun registerFontFamilyList(families: Array<String>) {
        fontManager.index(families.toList())
    }

    fun registerFontEntry(family: String, entry: FontEntry) {
        fontManager.Entries[family] = entry
    }

    fun registerFontEntry(family: String, normal: FileInfo?, bold: FileInfo?, italic: FileInfo?, boldItalic: FileInfo?) {
        registerFontEntry(family, FontEntry(family, normal, bold, italic, boldItalic))
    }

    fun createTextModel(
        id: String, language: String?, paragraphsNumber: Int,
        entryIndices: IntArray, entryOffsets: IntArray,
        paragraphLenghts: IntArray, textSizes: IntArray, paragraphKinds: ByteArray,
        directoryName: String, fileExtension: String, blocksNumber: Int
    ): ZLTextModel {
        return ZLTextPlainModel(
            id, language, paragraphsNumber,
            entryIndices, entryOffsets,
            paragraphLenghts, textSizes, paragraphKinds,
            directoryName, fileExtension, blocksNumber, imageMap, fontManager
        )
    }

    fun setBookTextModel(model: ZLTextModel) {
        myBookTextModel = model
    }

    fun setFootnoteModel(model: ZLTextModel) {
        footnotes[model.id] = model
    }

    fun getTextModel(): ZLTextModel? = myBookTextModel

    fun getFootnoteModel(id: String): ZLTextModel? = footnotes[id]

    fun addImage(id: String, image: ZLImage) {
        imageMap[id] = image
    }

    fun initInternalHyperlinks(directoryName: String, fileExtension: String, blocksNumber: Int) {
        internalHyperlinks = CachedCharStorage(directoryName, fileExtension, blocksNumber)
    }

    fun addTOCItem(text: String, reference: Int) {
        currentTree = TOCTree(currentTree).apply {
            setText(text)
            setReference(myBookTextModel!!, reference)
        }
    }

    fun leaveTOCItem() {
        currentTree = currentTree.parent ?: tocTree
    }

    private fun getLabelInternal(id: String): Label? {
        val len = id.length
        val internalHyperlinks = this.internalHyperlinks ?: return null
        val size = internalHyperlinks.size()

        for (i in 0 until size) {
            val block = internalHyperlinks.block(i)
            var offset = 0
            while (offset < block.size) {
                val labelLength = block[offset++].code
                if (labelLength == 0) {
                    break
                }
                val idLength = block[offset + labelLength].code
                if (labelLength != len || id != String(block, offset, labelLength)) {
                    offset += labelLength + idLength + 3
                    continue
                }
                offset += labelLength + 1
                val modelId = if (idLength > 0) String(block, offset, idLength) else null
                offset += idLength
                val paragraphNumber = block[offset].code + (block[offset + 1].code shl 16)
                return Label(modelId, paragraphNumber)
            }
        }
        return null
    }

    interface LabelResolver {
        fun getCandidates(id: String): List<String>
    }

    class Label(@JvmField val modelId: String?, @JvmField val paragraphIndex: Int)
}
