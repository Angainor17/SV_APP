package org.geometerplus.fbreader.fbreader

import org.geometerplus.zlibrary.text.view.ZLTextControlElement
import org.geometerplus.zlibrary.text.view.ZLTextTraverser
import org.geometerplus.zlibrary.text.view.ZLTextView
import org.geometerplus.zlibrary.text.view.ZLTextWord

open class TextBuildTraverser(view: ZLTextView) : ZLTextTraverser(view) {

    protected val buffer = StringBuilder()

    override fun processWord(word: ZLTextWord) {
        buffer.append(word.data, word.offset, word.length)
    }

    override fun processControlElement(control: ZLTextControlElement) {
        // does nothing
    }

    override fun processSpace() {
        buffer.append(" ")
    }

    override fun processNbSpace() {
        buffer.append(" ")
    }

    override fun processEndOfParagraph() {
        buffer.append("\n")
    }

    fun getText(): String = buffer.toString()
}
