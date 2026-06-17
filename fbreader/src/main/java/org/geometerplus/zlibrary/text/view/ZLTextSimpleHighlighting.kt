package org.geometerplus.zlibrary.text.view

abstract class ZLTextSimpleHighlighting(
    protected val view: ZLTextView,
    start: ZLTextPosition,
    end: ZLTextPosition
) : ZLTextHighlighting() {

    private val startPosition: ZLTextPosition = ZLTextFixedPosition(start)
    private val endPosition: ZLTextPosition = ZLTextFixedPosition(end)

    override fun isEmpty(): Boolean = false

    override fun getStartPosition(): ZLTextPosition = startPosition

    override fun getEndPosition(): ZLTextPosition = endPosition

    override fun getStartArea(page: ZLTextPage): ZLTextElementArea? =
        page.textElementMap.getFirstAfter(startPosition)

    override fun getEndArea(page: ZLTextPage): ZLTextElementArea? =
        page.textElementMap.getLastBefore(endPosition)
}
