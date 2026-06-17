package org.geometerplus.zlibrary.text.view

abstract class ZLTextTraverser(private val view: ZLTextView) {

    protected abstract fun processWord(word: ZLTextWord)

    protected abstract fun processControlElement(control: ZLTextControlElement)

    protected abstract fun processSpace()

    protected abstract fun processNbSpace()

    protected abstract fun processEndOfParagraph()

    fun traverse(from: ZLTextPosition, to: ZLTextPosition) {
        val fromParagraph = from.paragraphIndex
        val toParagraph = to.paragraphIndex
        var cursor = view.cursor(fromParagraph)
        for (i in fromParagraph..toParagraph) {
            val fromElement = if (i == fromParagraph) from.elementIndex else 0
            val toElement = if (i == toParagraph) to.elementIndex else cursor.paragraphLength - 1

            for (j in fromElement..toElement) {
                val element = cursor.getElement(j)
                when (element) {
                    ZLTextElement.HSpace -> processSpace()
                    ZLTextElement.NBSpace -> processNbSpace()
                    is ZLTextWord -> processWord(element)
                }
            }
            if (i < toParagraph) {
                processEndOfParagraph()
                cursor = cursor.next()!!
            }
        }
    }
}
