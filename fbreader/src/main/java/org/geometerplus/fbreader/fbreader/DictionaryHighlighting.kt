package org.geometerplus.fbreader.fbreader

import org.geometerplus.zlibrary.core.util.ZLColor
import org.geometerplus.zlibrary.text.view.ZLTextPosition
import org.geometerplus.zlibrary.text.view.ZLTextSimpleHighlighting
import org.geometerplus.zlibrary.text.view.ZLTextView

class DictionaryHighlighting private constructor(
    view: ZLTextView,
    start: ZLTextPosition,
    end: ZLTextPosition
) : ZLTextSimpleHighlighting(view, start, end) {

    companion object {
        @JvmStatic
        fun get(view: ZLTextView): DictionaryHighlighting? {
            val hilite = view.selection
            if (hilite.isEmpty()) {
                return null
            }

            val start = hilite.getStartPosition()
            val end = hilite.getEndPosition()

            return DictionaryHighlighting(view, start, end)
        }
    }

    override fun getBackgroundColor(): ZLColor? = view.selectionBackgroundColor

    override fun getForegroundColor(): ZLColor? = null

    override fun getOutlineColor(): ZLColor? = null
}
