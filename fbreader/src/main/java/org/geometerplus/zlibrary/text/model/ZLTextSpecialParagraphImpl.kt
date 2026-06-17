package org.geometerplus.zlibrary.text.model

internal class ZLTextSpecialParagraphImpl(
    private val myKind: Byte,
    model: ZLTextPlainModel,
    offset: Int
) : ZLTextParagraphImpl(model, offset) {
    override fun getKind(): Byte = myKind
}
