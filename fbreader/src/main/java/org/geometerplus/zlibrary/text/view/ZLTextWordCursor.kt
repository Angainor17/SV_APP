package org.geometerplus.zlibrary.text.view

import org.geometerplus.zlibrary.text.model.ZLTextMark

class ZLTextWordCursor : ZLTextPosition {
    var paragraphCursor: ZLTextParagraphCursor? = null
        private set
    private var elementIndexValue: Int = 0
    private var charIndexValue: Int = 0

    constructor()

    constructor(cursor: ZLTextWordCursor) {
        setCursor(cursor)
    }

    constructor(paragraphCursor: ZLTextParagraphCursor) {
        setCursor(paragraphCursor)
    }

    fun setCursor(cursor: ZLTextWordCursor) {
        paragraphCursor = cursor.paragraphCursor
        elementIndexValue = cursor.elementIndexValue
        charIndexValue = cursor.charIndexValue
    }

    fun setCursor(paragraphCursor: ZLTextParagraphCursor) {
        this.paragraphCursor = paragraphCursor
        elementIndexValue = 0
        charIndexValue = 0
    }

    val isNull: Boolean
        get() = paragraphCursor == null

    val isStartOfParagraph: Boolean
        get() = elementIndexValue == 0 && charIndexValue == 0

    val isStartOfText: Boolean
        get() = isStartOfParagraph && paragraphCursor!!.isFirst

    val isEndOfParagraph: Boolean
        get() = paragraphCursor != null && elementIndexValue == paragraphCursor!!.paragraphLength

    val isEndOfText: Boolean
        get() = isEndOfParagraph && paragraphCursor!!.isLast

    override val paragraphIndex: Int
        get() = paragraphCursor?.index ?: 0

    override val elementIndex: Int
        get() = elementIndexValue

    override val charIndex: Int
        get() = charIndexValue

    fun setCharIndex(charIndex: Int) {
        var idx = maxOf(0, charIndex)
        this.charIndexValue = 0
        if (idx > 0) {
            val element = paragraphCursor!!.getElement(elementIndexValue)
            if (element is ZLTextWord) {
                if (idx <= element.length) {
                    this.charIndexValue = idx
                }
            }
        }
    }

    fun getElement(): ZLTextElement? = paragraphCursor?.getElement(elementIndexValue)

    fun getMark(): ZLTextMark? {
        if (paragraphCursor == null) {
            return null
        }
        val paragraph = paragraphCursor!!
        val paragraphLength = paragraph.paragraphLength
        var wordIndex = elementIndexValue
        while (wordIndex < paragraphLength && paragraph.getElement(wordIndex) !is ZLTextWord) {
            wordIndex++
        }
        return if (wordIndex < paragraphLength) {
            ZLTextMark(paragraph.index, (paragraph.getElement(wordIndex) as ZLTextWord).getParagraphOffset(), 0)
        } else {
            ZLTextMark(paragraph.index + 1, 0, 0)
        }
    }

    fun nextWord() {
        elementIndexValue++
        charIndexValue = 0
    }

    fun previousWord() {
        elementIndexValue--
        charIndexValue = 0
    }

    fun nextParagraph(): Boolean {
        if (!isNull) {
            if (!paragraphCursor!!.isLast) {
                paragraphCursor = paragraphCursor!!.next()
                moveToParagraphStart()
                return true
            }
        }
        return false
    }

    fun previousParagraph(): Boolean {
        if (!isNull) {
            if (!paragraphCursor!!.isFirst) {
                paragraphCursor = paragraphCursor!!.previous()
                moveToParagraphStart()
                return true
            }
        }
        return false
    }

    fun moveToParagraphStart() {
        if (!isNull) {
            elementIndexValue = 0
            charIndexValue = 0
        }
    }

    fun moveToParagraphEnd() {
        if (!isNull) {
            elementIndexValue = paragraphCursor!!.paragraphLength
            charIndexValue = 0
        }
    }

    fun moveToParagraph(paragraphIndex: Int) {
        if (!isNull && paragraphIndex != paragraphCursor!!.index) {
            val model = paragraphCursor!!.model
            val idx = maxOf(0, minOf(paragraphIndex, model.paragraphsNumber - 1))
            paragraphCursor = paragraphCursor!!.cursorManager.get(idx)
            moveToParagraphStart()
        }
    }

    fun moveTo(position: ZLTextPosition) {
        moveToParagraph(position.paragraphIndex)
        moveTo(position.elementIndex, position.charIndex)
    }

    fun moveTo(wordIndex: Int, charIndex: Int) {
        if (!isNull) {
            if (wordIndex == 0 && charIndex == 0) {
                elementIndexValue = 0
                this.charIndexValue = 0
            } else {
                val idx = maxOf(0, wordIndex)
                val size = paragraphCursor!!.paragraphLength
                if (idx > size) {
                    elementIndexValue = size
                    this.charIndexValue = 0
                } else {
                    elementIndexValue = idx
                    setCharIndex(charIndex)
                }
            }
        }
    }

    fun reset() {
        paragraphCursor = null
        elementIndexValue = 0
        charIndexValue = 0
    }

    fun rebuild() {
        if (!isNull) {
            paragraphCursor!!.clear()
            paragraphCursor!!.fill()
            moveTo(elementIndexValue, charIndexValue)
        }
    }

    override fun toString(): String =
        "${super.toString()} ($paragraphCursor,$elementIndexValue,$charIndexValue)"
}
