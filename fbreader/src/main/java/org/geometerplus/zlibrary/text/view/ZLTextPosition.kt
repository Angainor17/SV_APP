package org.geometerplus.zlibrary.text.view

abstract class ZLTextPosition : Comparable<ZLTextPosition> {
    abstract val paragraphIndex: Int
    abstract val elementIndex: Int
    abstract val charIndex: Int

    fun samePositionAs(position: ZLTextPosition): Boolean =
        paragraphIndex == position.paragraphIndex &&
        elementIndex == position.elementIndex &&
        charIndex == position.charIndex

    override fun compareTo(position: ZLTextPosition): Int {
        val p0 = paragraphIndex
        val p1 = position.paragraphIndex
        if (p0 != p1) {
            return if (p0 < p1) -1 else 1
        }

        val e0 = elementIndex
        val e1 = position.elementIndex
        if (e0 != e1) {
            return if (e0 < e1) -1 else 1
        }

        return charIndex - position.charIndex
    }

    fun compareToIgnoreChar(position: ZLTextPosition): Int {
        val p0 = paragraphIndex
        val p1 = position.paragraphIndex
        if (p0 != p1) {
            return if (p0 < p1) -1 else 1
        }

        return elementIndex - position.elementIndex
    }

    override fun hashCode(): Int =
        (paragraphIndex shl 16) + (elementIndex shl 8) + charIndex

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other !is ZLTextPosition) {
            return false
        }
        return paragraphIndex == other.paragraphIndex &&
               elementIndex == other.elementIndex &&
               charIndex == other.charIndex
    }

    override fun toString(): String =
        "${javaClass.simpleName} [$paragraphIndex,$elementIndex,$charIndex]"
}
