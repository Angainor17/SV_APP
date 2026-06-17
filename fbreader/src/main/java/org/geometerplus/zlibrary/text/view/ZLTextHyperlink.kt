package org.geometerplus.zlibrary.text.view

class ZLTextHyperlink(@JvmField val Type: Byte, @JvmField val Id: String?) {
    private var elementIndexes: MutableList<Int>? = null

    val type: Byte get() = Type
    val id: String? get() = Id

    fun addElementIndex(elementIndex: Int) {
        if (elementIndexes == null) {
            elementIndexes = mutableListOf()
        }
        elementIndexes!!.add(elementIndex)
    }

    fun elementIndexes(): List<Int> = elementIndexes?.toList() ?: emptyList()

    companion object {
        @JvmField
        val NO_LINK = ZLTextHyperlink(0.toByte(), null)
    }
}
