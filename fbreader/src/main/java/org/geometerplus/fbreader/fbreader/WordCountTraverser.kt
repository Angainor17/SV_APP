package org.geometerplus.fbreader.fbreader

import org.geometerplus.zlibrary.text.view.ZLTextControlElement
import org.geometerplus.zlibrary.text.view.ZLTextTraverser
import org.geometerplus.zlibrary.text.view.ZLTextView
import org.geometerplus.zlibrary.text.view.ZLTextWord

internal class WordCountTraverser(view: ZLTextView) : ZLTextTraverser(view) {

    var count = 0
        protected set

    override fun processWord(word: ZLTextWord) {
        ++count
    }

    override fun processControlElement(control: ZLTextControlElement) {
        // does nothing
    }

    override fun processSpace() {
        // does nothing
    }

    override fun processNbSpace() {
        // does nothing
    }

    override fun processEndOfParagraph() {
        // does nothing
    }
}
