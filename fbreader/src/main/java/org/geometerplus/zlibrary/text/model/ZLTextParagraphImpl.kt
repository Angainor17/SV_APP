package org.geometerplus.zlibrary.text.model

internal open class ZLTextParagraphImpl(
    private val myModel: ZLTextPlainModel,
    private val myIndex: Int
) : ZLTextParagraph {
    override fun iterator(): ZLTextParagraph.EntryIterator = myModel.EntryIteratorImpl(myIndex)

    override fun getKind(): Byte = ZLTextParagraph.Kind.TEXT_PARAGRAPH
}
