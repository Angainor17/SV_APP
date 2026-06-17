package org.geometerplus.zlibrary.text.view

class ZLTextHyperlinkControlElement(
    kind: Byte,
    type: Byte,
    id: String
) : ZLTextControlElement(kind, true) {
    @JvmField val Hyperlink = ZLTextHyperlink(type, id)
}
