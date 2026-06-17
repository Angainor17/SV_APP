package org.geometerplus.zlibrary.core.view

import org.geometerplus.zlibrary.core.library.ZLibrary
import org.geometerplus.zlibrary.core.util.ZLColor

abstract class SelectionCursor {

    companion object {
        @JvmStatic
        fun draw(context: ZLPaintContext, which: Which, x: Int, y: Int, color: ZLColor) {
            context.setFillColor(color)
            val dpi = ZLibrary.Instance().displayDPI
            val unit = dpi / 120
            val xCenter = if (which == Which.Left) x - unit - 1 else x + unit + 1
            context.fillRectangle(xCenter - unit, y + dpi / 8, xCenter + unit, y - dpi / 8)
            if (which == Which.Left) {
                context.fillCircle(xCenter, y - dpi / 8, unit * 6)
            } else {
                context.fillCircle(xCenter, y + dpi / 8, unit * 6)
            }
        }
    }

    enum class Which {
        Left, Right
    }
}
