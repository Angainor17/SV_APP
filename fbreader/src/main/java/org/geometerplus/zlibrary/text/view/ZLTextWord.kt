package org.geometerplus.zlibrary.text.view

import org.geometerplus.zlibrary.core.view.ZLPaintContext

class ZLTextWord : ZLTextElement {
    @JvmField val Data: CharArray
    @JvmField val Offset: Int
    @JvmField val Length: Int
    private var width: Int = -1
    private var mark: Mark? = null
    private var paragraphOffset: Int

    val data: CharArray get() = Data
    val offset: Int get() = Offset
    val length: Int get() = Length

    constructor(word: String, paragraphOffset: Int) {
        this.Data = word.toCharArray()
        this.Offset = 0
        this.Length = word.length
        this.paragraphOffset = paragraphOffset
    }

    constructor(data: CharArray, offset: Int, length: Int, paragraphOffset: Int) {
        this.Data = data
        this.Offset = offset
        this.Length = length
        this.paragraphOffset = paragraphOffset
    }

    fun isASpace(): Boolean {
        for (i in offset until offset + length) {
            if (!Character.isWhitespace(data[i])) {
                return false
            }
        }
        return true
    }

    fun getMark(): Mark? = mark

    fun getParagraphOffset(): Int = paragraphOffset

    fun addMark(start: Int, length: Int) {
        val existingMark = mark
        val newMark = Mark(start, length)
        if (existingMark == null || existingMark.start > start) {
            newMark.next = existingMark
            mark = newMark
        } else {
            var current: Mark = existingMark
            while (true) {
                val nextMark: Mark? = current.next
                if (nextMark == null) {
                    break
                }
                if (nextMark.start >= start) {
                    break
                }
                current = nextMark
            }
            newMark.next = current.next
            current.next = newMark
        }
    }

    fun getWidth(context: ZLPaintContext): Int {
        var w = width
        if (w <= 1) {
            w = context.getStringWidth(data, offset, length)
            width = w
        }
        return w
    }

    override fun toString(): String = getString()

    fun getString(): String = String(data, offset, length)

    inner class Mark(val start: Int, val length: Int) {
        internal var next: Mark? = null
    }
}
