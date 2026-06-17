package org.geometerplus.zlibrary.text.view

import org.geometerplus.zlibrary.core.image.ZLImageManager
import org.geometerplus.zlibrary.core.resources.ZLResource
import org.geometerplus.zlibrary.text.model.ZLTextMark
import org.geometerplus.zlibrary.text.model.ZLTextModel
import org.geometerplus.zlibrary.text.model.ZLTextOtherStyleEntry
import org.geometerplus.zlibrary.text.model.ZLTextParagraph
import org.geometerplus.zlibrary.text.model.ZLTextStyleEntry
import org.vimgadgets.linebreak.LineBreaker

class ZLTextParagraphCursor {
    val index: Int
    val model: ZLTextModel
    internal val cursorManager: CursorManager
    private val elements = mutableListOf<ZLTextElement>()

    constructor(model: ZLTextModel, index: Int) : this(CursorManager(model, null), model, index)

    internal constructor(cManager: CursorManager, model: ZLTextModel, index: Int) {
        cursorManager = cManager
        this.model = model
        this.index = minOf(index, model.paragraphsNumber - 1)
        fill()
    }

    internal fun fill() {
        val paragraph = model.getParagraph(index)
        when (paragraph.kind) {
            ZLTextParagraph.Kind.TEXT_PARAGRAPH -> {
                Processor(
                    paragraph,
                    cursorManager.ExtensionManager,
                    LineBreaker(model.language),
                    model.marks,
                    index,
                    elements
                ).fill()
            }
            ZLTextParagraph.Kind.EMPTY_LINE_PARAGRAPH -> {
                elements.add(ZLTextWord(SPACE_ARRAY, 0, 1, 0))
            }
            ZLTextParagraph.Kind.ENCRYPTED_SECTION_PARAGRAPH -> {
                val entry = ZLTextOtherStyleEntry()
                entry.setFontModifier(ZLTextStyleEntry.FontModifier.FONT_MODIFIER_BOLD, true)
                elements.add(ZLTextStyleElement(entry))
                elements.add(
                    ZLTextWord(
                        ZLResource.resource("drm").getResource("encryptedSection").value,
                        0
                    )
                )
            }
        }
    }

    internal fun clear() {
        elements.clear()
    }

    val isFirst: Boolean
        get() = index == 0

    val isLast: Boolean
        get() = index + 1 >= model.paragraphsNumber

    val isLikeEndOfSection: Boolean
        get() = when (model.getParagraph(index).kind) {
            ZLTextParagraph.Kind.END_OF_SECTION_PARAGRAPH,
            ZLTextParagraph.Kind.PSEUDO_END_OF_SECTION_PARAGRAPH -> true
            else -> false
        }

    val isEndOfSection: Boolean
        get() = model.getParagraph(index).kind == ZLTextParagraph.Kind.END_OF_SECTION_PARAGRAPH

    val paragraphLength: Int
        get() = elements.size

    fun previous(): ZLTextParagraphCursor? = if (isFirst) null else cursorManager[index - 1]

    fun next(): ZLTextParagraphCursor? = if (isLast) null else cursorManager[index + 1]

    fun getElement(index: Int): ZLTextElement? {
        return try {
            elements[index]
        } catch (e: IndexOutOfBoundsException) {
            null
        }
    }

    fun getParagraph(): ZLTextParagraph = model.getParagraph(index)

    override fun toString(): String = "ZLTextParagraphCursor [$index (0..${elements.size})]"

    private class Processor(
        private val paragraph: ZLTextParagraph,
        private val extManager: ExtensionElementManager?,
        private val lineBreaker: LineBreaker,
        marks: List<ZLTextMark>,
        paragraphIndex: Int,
        private val elements: MutableList<ZLTextElement>
    ) {
        companion object {
            private const val NO_SPACE = 0
            private const val SPACE = 1
            private const val NON_BREAKABLE_SPACE = 2
            private var breaks = ByteArray(1024)
            private val SPACE_ARRAY = charArrayOf(' ')
        }

        private val marks: List<ZLTextMark> = marks
        private var offset: Int = 0
        private var firstMark: Int
        private var lastMark: Int

        init {
            val mark = ZLTextMark(paragraphIndex, 0, 0)
            var i = 0
            while (i < marks.size) {
                if (marks[i].compareTo(mark) >= 0) {
                    break
                }
                i++
            }
            firstMark = i
            lastMark = firstMark
            while (lastMark < marks.size && marks[lastMark].paragraphIndex == paragraphIndex) {
                lastMark++
            }
            offset = 0
        }

        fun fill() {
            var hyperlinkDepth = 0
            var hyperlink: ZLTextHyperlink? = null

            val elements = elements
            val it = paragraph.iterator()
            while (it.next()) {
                when (it.type) {
                    ZLTextParagraph.Entry.TEXT -> {
                        processTextEntry(it.textData, it.textOffset, it.textLength, hyperlink)
                    }
                    ZLTextParagraph.Entry.CONTROL -> {
                        if (hyperlink != null) {
                            hyperlinkDepth += if (it.controlIsStart) 1 else -1
                            if (hyperlinkDepth == 0) {
                                hyperlink = null
                            }
                        }
                        elements.add(ZLTextControlElement[it.controlKind, it.controlIsStart])
                    }
                    ZLTextParagraph.Entry.HYPERLINK_CONTROL -> {
                        val hyperlinkType = it.hyperlinkType
                        if (hyperlinkType.toInt() != 0) {
                            val control = ZLTextHyperlinkControlElement(
                                it.controlKind,
                                hyperlinkType,
                                it.hyperlinkId
                            )
                            elements.add(control)
                            hyperlink = control.Hyperlink
                            hyperlinkDepth = 1
                        }
                    }
                    ZLTextParagraph.Entry.IMAGE -> {
                        val imageEntry = it.imageEntry
                        val image = imageEntry.image
                        if (image != null) {
                            val data = ZLImageManager.Instance().getImageData(image)
                            if (data != null) {
                                hyperlink?.addElementIndex(elements.size)
                                elements.add(
                                    ZLTextImageElement(
                                        imageEntry.id,
                                        data,
                                        image.getURI(),
                                        imageEntry.isCover
                                    )
                                )
                            }
                        }
                    }
                    ZLTextParagraph.Entry.AUDIO -> {}
                    ZLTextParagraph.Entry.VIDEO -> {
                        elements.add(ZLTextVideoElement(it.videoEntry.sources()))
                    }
                    ZLTextParagraph.Entry.EXTENSION -> {
                        if (extManager != null) {
                            elements.addAll(extManager.getElements(it.extensionEntry))
                        }
                    }
                    ZLTextParagraph.Entry.STYLE_CSS,
                    ZLTextParagraph.Entry.STYLE_OTHER -> {
                        elements.add(ZLTextStyleElement(it.styleEntry))
                    }
                    ZLTextParagraph.Entry.STYLE_CLOSE -> {
                        elements.add(ZLTextElement.StyleClose)
                    }
                    ZLTextParagraph.Entry.FIXED_HSPACE -> {
                        elements.add(ZLTextFixedHSpaceElement[it.fixedHSpaceLength])
                    }
                }
            }
        }

        private fun processTextEntry(
            data: CharArray,
            offset: Int,
            length: Int,
            hyperlink: ZLTextHyperlink?
        ) {
            if (length != 0) {
                if (breaks.size < length) {
                    breaks = ByteArray(length)
                }
                val breaks = breaks
                lineBreaker.setLineBreaks(data, offset, length, breaks)

                val hSpace = ZLTextElement.HSpace
                val nbSpace = ZLTextElement.NBSpace
                val elements = elements
                var ch: Char = 0.toChar()
                var previousChar: Char = 0.toChar()
                var spaceState = NO_SPACE
                var wordStart = 0
                for (index in 0 until length) {
                    previousChar = ch
                    ch = data[offset + index]
                    if (Character.isWhitespace(ch)) {
                        if (index > 0 && spaceState == NO_SPACE) {
                            addWord(data, offset + wordStart, index - wordStart, this.offset + wordStart, hyperlink)
                        }
                        spaceState = SPACE
                    } else if (Character.isSpaceChar(ch)) {
                        if (index > 0 && spaceState == NO_SPACE) {
                            addWord(data, offset + wordStart, index - wordStart, this.offset + wordStart, hyperlink)
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
                            NON_BREAKABLE_SPACE -> {
                                wordStart = index
                            }
                            NO_SPACE -> {
                                if (index > 0 &&
                                    breaks[index - 1].toInt() != LineBreaker.NOBREAK.code &&
                                    previousChar != '-' &&
                                    index != wordStart
                                ) {
                                    addWord(data, offset + wordStart, index - wordStart, this.offset + wordStart, hyperlink)
                                    wordStart = index
                                }
                            }
                        }
                        spaceState = NO_SPACE
                    }
                }
                when (spaceState) {
                    SPACE -> elements.add(hSpace)
                    NON_BREAKABLE_SPACE -> elements.add(nbSpace)
                    NO_SPACE -> addWord(data, offset + wordStart, length - wordStart, this.offset + wordStart, hyperlink)
                }
                this.offset += length
            }
        }

        private fun addWord(
            data: CharArray,
            offset: Int,
            len: Int,
            paragraphOffset: Int,
            hyperlink: ZLTextHyperlink?
        ) {
            val word = ZLTextWord(data, offset, len, paragraphOffset)
            for (i in firstMark until lastMark) {
                val mark = marks[i]
                if (mark.offset < paragraphOffset + len && mark.offset + mark.length > paragraphOffset) {
                    word.addMark(mark.offset - paragraphOffset, mark.length)
                }
            }
            hyperlink?.addElementIndex(elements.size)
            elements.add(word)
        }
    }

    companion object {
        private val SPACE_ARRAY = charArrayOf(' ')
    }
}
