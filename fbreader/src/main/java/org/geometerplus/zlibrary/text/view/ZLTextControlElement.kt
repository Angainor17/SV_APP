package org.geometerplus.zlibrary.text.view

open class ZLTextControlElement protected constructor(
    @JvmField val kind: Byte,
    @JvmField val isStart: Boolean
) : ZLTextElement() {

    companion object {
        private val startElements = arrayOfNulls<ZLTextControlElement>(256)
        private val endElements = arrayOfNulls<ZLTextControlElement>(256)

        @JvmStatic
        operator fun get(kind: Byte, isStart: Boolean): ZLTextControlElement {
            val elements = if (isStart) startElements else endElements
            var element = elements[kind.toInt() and 0xFF]
            if (element == null) {
                element = ZLTextControlElement(kind, isStart)
                elements[kind.toInt() and 0xFF] = element
            }
            return element
        }
    }
}
