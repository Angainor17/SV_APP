package org.geometerplus.fbreader.fbreader

import org.geometerplus.zlibrary.core.view.ZLViewEnums.Direction
import org.geometerplus.zlibrary.text.view.ZLTextRegion
import org.geometerplus.zlibrary.text.view.ZLTextView
import org.geometerplus.zlibrary.text.view.ZLTextWordRegionSoul

internal class MoveCursorAction(
    fbreader: FBReaderApp,
    private val direction: Direction
) : FBAction(fbreader) {

    override fun run(vararg params: Any?) {
        val fbView = Reader.getTextView()
        var region = fbView.getOutlinedRegion()
        val filter =
            if ((region != null && region.soul is ZLTextWordRegionSoul)
                || Reader.miscOptions.navigateAllWords.value
            ) ZLTextRegion.AnyRegionFilter
            else ZLTextRegion.ImageOrHyperlinkFilter
        region = fbView.nextRegion(direction, filter)
        if (region != null) {
            fbView.outlineRegion(region)
        } else {
            when (direction) {
                Direction.down -> fbView.turnPage(true, ZLTextView.ScrollingMode.SCROLL_LINES, 1)
                Direction.up -> fbView.turnPage(false, ZLTextView.ScrollingMode.SCROLL_LINES, 1)
                else -> {}
            }
        }

        Reader.getViewWidget().reset()
        Reader.getViewWidget().repaint()
    }
}
