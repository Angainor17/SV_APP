package org.geometerplus.zlibrary.text.view

class ZLTextElementArea(
    paragraphIndex: Int,
    elementIndex: Int,
    charIndex: Int,
    @JvmField val Length: Int,
    private val isLastInElement: Boolean,
    val addHyphenationSign: Boolean,
    val changeStyle: Boolean,
    val style: ZLTextStyle,
    val element: ZLTextElement,
    @JvmField val XStart: Int,
    @JvmField val XEnd: Int,
    @JvmField val YStart: Int,
    @JvmField val YEnd: Int,
    val columnIndex: Int
) : ZLTextFixedPosition(paragraphIndex, elementIndex, charIndex) {

    val length: Int get() = Length
    val xStart: Int get() = XStart
    val xEnd: Int get() = XEnd
    val yStart: Int get() = YStart
    val yEnd: Int get() = YEnd

    fun contains(x: Int, y: Int): Boolean =
        y in YStart..YEnd && x in XStart..XEnd

    fun isFirstInElement(): Boolean = charIndex == 0

    fun isLastInElement(): Boolean = isLastInElement
}
