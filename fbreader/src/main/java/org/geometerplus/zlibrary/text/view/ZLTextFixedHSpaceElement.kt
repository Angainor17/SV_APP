package org.geometerplus.zlibrary.text.view

class ZLTextFixedHSpaceElement private constructor(
    @JvmField val length: Short
) : ZLTextElement() {

    companion object {
        private val collection = arrayOfNulls<ZLTextElement>(20)

        @JvmStatic
        operator fun get(length: Short): ZLTextElement {
            return if (length < 20) {
                var cached = collection[length.toInt()]
                if (cached == null) {
                    cached = ZLTextFixedHSpaceElement(length)
                    collection[length.toInt()] = cached
                }
                cached
            } else {
                ZLTextFixedHSpaceElement(length)
            }
        }
    }
}
