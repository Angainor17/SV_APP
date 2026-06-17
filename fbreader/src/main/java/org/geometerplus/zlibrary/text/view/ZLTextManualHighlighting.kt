package org.geometerplus.zlibrary.text.view

import org.geometerplus.zlibrary.core.util.ZLColor

internal class ZLTextManualHighlighting(
    view: ZLTextView,
    start: ZLTextPosition,
    end: ZLTextPosition
) : ZLTextSimpleHighlighting(view, start, end) {

    override fun getBackgroundColor(): ZLColor = view.highlightingBackgroundColor

    override fun getForegroundColor(): ZLColor = view.highlightingForegroundColor

    override fun getOutlineColor(): ZLColor? = null
}
