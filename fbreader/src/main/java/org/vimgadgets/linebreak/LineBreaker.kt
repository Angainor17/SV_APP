package org.vimgadgets.linebreak

class LineBreaker(private val myLanguage: String) {
    companion object {
        const val MUSTBREAK: Char = 0.toChar()
        const val ALLOWBREAK: Char = 1.toChar()
        const val NOBREAK: Char = 2.toChar()
        const val INSIDEACHAR: Char = 3.toChar()

        init {
            System.loadLibrary("LineBreak-v2")
            init()
        }

        @JvmStatic
        private external fun init()

        @JvmStatic
        private external fun setLineBreaksForCharArray(data: CharArray, offset: Int, length: Int, lang: String, breaks: ByteArray)

        @JvmStatic
        private external fun setLineBreaksForString(data: String, lang: String, breaks: ByteArray)
    }

    fun setLineBreaks(data: CharArray, offset: Int, length: Int, breaks: ByteArray) {
        setLineBreaksForCharArray(data, offset, length, myLanguage, breaks)
    }

    fun setLineBreaks(data: String, breaks: ByteArray) {
        setLineBreaksForString(data, myLanguage, breaks)
    }
}
