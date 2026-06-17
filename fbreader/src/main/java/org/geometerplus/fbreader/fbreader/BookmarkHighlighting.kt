package org.geometerplus.fbreader.fbreader

import org.geometerplus.fbreader.book.Bookmark
import org.geometerplus.fbreader.book.IBookCollection
import org.geometerplus.zlibrary.core.util.ZLColor
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition
import org.geometerplus.zlibrary.text.view.ZLTextPosition
import org.geometerplus.zlibrary.text.view.ZLTextSimpleHighlighting
import org.geometerplus.zlibrary.text.view.ZLTextView

class BookmarkHighlighting(
    view: ZLTextView,
    val collection: IBookCollection<*>,
    val bookmark: Bookmark
) : ZLTextSimpleHighlighting(view, startPosition(bookmark), endPosition(bookmark)) {

    companion object {
        private fun startPosition(bookmark: Bookmark): ZLTextPosition =
            ZLTextFixedPosition(bookmark.paragraphIndex, bookmark.elementIndex, 0)

        private fun endPosition(bookmark: Bookmark): ZLTextPosition {
            val end = bookmark.end
            if (end != null) {
                return end
            }
            // TODO: compute end and save bookmark
            return bookmark
        }
    }

    override fun getBackgroundColor(): ZLColor? {
        val bmStyle = collection.getHighlightingStyle(bookmark.styleId)
        return bmStyle?.backgroundColor
    }

    override fun getForegroundColor(): ZLColor? {
        val bmStyle = collection.getHighlightingStyle(bookmark.styleId)
        return bmStyle?.foregroundColor
    }

    override fun getOutlineColor(): ZLColor? = null
}
