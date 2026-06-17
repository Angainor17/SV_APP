package org.geometerplus.zlibrary.text.hyphenation

class ZLTextHyphenationInfo(length: Int) {
    @JvmField
    internal val Mask: BooleanArray = BooleanArray(length - 1)

    fun isHyphenationPossible(position: Int): Boolean {
        return position < Mask.size && Mask[position]
    }
}
