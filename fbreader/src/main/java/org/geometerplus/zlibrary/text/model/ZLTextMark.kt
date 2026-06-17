package org.geometerplus.zlibrary.text.model

class ZLTextMark : Comparable<ZLTextMark> {

    @JvmField
    val paragraphIndex: Int

    @JvmField
    val offset: Int

    @JvmField
    val length: Int

    constructor(paragraphIndex: Int, offset: Int, length: Int) {
        this.paragraphIndex = paragraphIndex
        this.offset = offset
        this.length = length
    }

    constructor(mark: ZLTextMark) {
        paragraphIndex = mark.paragraphIndex
        offset = mark.offset
        length = mark.length
    }

    override fun compareTo(other: ZLTextMark): Int {
        val diff = paragraphIndex - other.paragraphIndex
        return if (diff != 0) diff else offset - other.offset
    }

    override fun toString(): String = "$paragraphIndex $offset $length"
}
