package org.geometerplus.zlibrary.text.view

internal class ZLTextLineInfo(
    @JvmField val paragraphCursor: ZLTextParagraphCursor,
    @JvmField val startElementIndex: Int,
    @JvmField val startCharIndex: Int,
    @JvmField var startStyle: ZLTextStyle
) {
    @JvmField val paragraphCursorLength: Int = paragraphCursor.paragraphLength

    @JvmField var realStartElementIndex: Int = startElementIndex
    @JvmField var realStartCharIndex: Int = startCharIndex
    @JvmField var endElementIndex: Int = startElementIndex
    @JvmField var endCharIndex: Int = startCharIndex

    @JvmField var isVisible: Boolean = false
    @JvmField var leftIndent: Int = 0
    @JvmField var width: Int = 0
    @JvmField var height: Int = 0
    @JvmField var descent: Int = 0
    @JvmField var vSpaceBefore: Int = 0
    @JvmField var vSpaceAfter: Int = 0
    @JvmField var previousInfoUsed: Boolean = false
    @JvmField var spaceCounter: Int = 0

    fun isEndOfParagraph(): Boolean = endElementIndex == paragraphCursorLength

    fun adjust(previous: ZLTextLineInfo?) {
        if (!previousInfoUsed && previous != null) {
            height -= minOf(previous.vSpaceAfter, vSpaceBefore)
            previousInfoUsed = true
        }
    }

    override fun equals(other: Any?): Boolean {
        val info = other as? ZLTextLineInfo ?: return false
        return paragraphCursor == info.paragraphCursor &&
                startElementIndex == info.startElementIndex &&
                startCharIndex == info.startCharIndex
    }

    override fun hashCode(): Int = paragraphCursor.hashCode() + startElementIndex + 239 * startCharIndex
}
