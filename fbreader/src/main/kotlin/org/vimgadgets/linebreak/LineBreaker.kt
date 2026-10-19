package org.vimgadgets.linebreak

class LineBreaker(private val myLanguage: String) {
    companion object {
        const val NOBREAK: Char = 2.toChar()

        init {
            System.loadLibrary("LineBreak-v2")
            init()
        }

        @JvmStatic
        private external fun init()

        @JvmStatic
        private external fun setLineBreaksForCharArray(
            data: CharArray,
            offset: Int,
            length: Int,
            lang: String,
            breaks: ByteArray
        )
    }

    fun setLineBreaks(data: CharArray, offset: Int, length: Int, breaks: ByteArray) {
        setLineBreaksForCharArray(data, offset, length, myLanguage, breaks)
    }
}
