package org.geometerplus.zlibrary.text.view

import org.geometerplus.zlibrary.core.view.ZLPaintContext

abstract class ExtensionElement : ZLTextElement() {
    abstract fun getWidth(): Int
    abstract fun getHeight(): Int
    abstract fun draw(context: ZLPaintContext, area: ZLTextElementArea)
}
