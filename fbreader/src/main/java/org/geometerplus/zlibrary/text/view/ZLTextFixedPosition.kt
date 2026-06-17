package org.geometerplus.zlibrary.text.view

open class ZLTextFixedPosition(
    @JvmField val ParagraphIndex: Int,
    @JvmField val ElementIndex: Int,
    @JvmField val CharIndex: Int
) : ZLTextPosition() {

    override val paragraphIndex: Int get() = ParagraphIndex
    override val elementIndex: Int get() = ElementIndex
    override val charIndex: Int get() = CharIndex

    constructor(position: ZLTextPosition) : this(
        position.paragraphIndex,
        position.elementIndex,
        position.charIndex
    )

    class WithTimestamp(
        paragraphIndex: Int,
        elementIndex: Int,
        charIndex: Int,
        stamp: Long?
    ) : ZLTextFixedPosition(paragraphIndex, elementIndex, charIndex) {
        @JvmField val Timestamp: Long = stamp ?: -1
        val timestamp: Long get() = Timestamp

        override fun toString(): String = "${super.toString()}; timestamp = $timestamp"
    }
}
