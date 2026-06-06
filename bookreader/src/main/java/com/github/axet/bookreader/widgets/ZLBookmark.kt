package com.github.axet.bookreader.widgets

import com.github.axet.bookreader.app.Storage
import org.geometerplus.fbreader.fbreader.FBView
import org.geometerplus.zlibrary.core.util.ZLColor
import org.geometerplus.zlibrary.text.view.ZLTextSimpleHighlighting

open class ZLBookmark(
    val view: FBView,
    val b: Storage.Bookmark
) : ZLTextSimpleHighlighting(view, b.start, b.end) {

    override fun getForegroundColor(): ZLColor? = null

    override fun getBackgroundColor(): ZLColor {
        if (b.color != 0)
            return ZLColor(b.color)
        return view.highlightingBackgroundColor
    }

    override fun getOutlineColor(): ZLColor? = null
}

open class ZLTTSMark(
    view: FBView,
    m: Storage.Bookmark
) : ZLBookmark(view, m)
