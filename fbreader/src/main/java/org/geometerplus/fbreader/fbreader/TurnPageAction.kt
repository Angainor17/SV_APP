package org.geometerplus.fbreader.fbreader

import org.geometerplus.fbreader.fbreader.options.PageTurningOptions
import org.geometerplus.zlibrary.core.view.ZLViewEnums.Direction
import org.geometerplus.zlibrary.core.view.ZLViewEnums.PageIndex

internal class TurnPageAction(
    fbreader: FBReaderApp,
    private val forward: Boolean
) : FBAction(fbreader) {

    override fun isEnabled(): Boolean {
        val fingerScrolling = Reader.pageTurningOptions.fingerScrolling.value
        return fingerScrolling == PageTurningOptions.FingerScrollingType.byTap ||
                fingerScrolling == PageTurningOptions.FingerScrollingType.byTapAndFlick
    }

    override fun run(vararg params: Any?) {
        val preferences = Reader.pageTurningOptions
        if (params.size == 2 && params[0] is Int && params[1] is Int) {
            val x = params[0] as Int
            val y = params[1] as Int
            Reader.viewWidget.startAnimatedScrolling(
                if (forward) PageIndex.next else PageIndex.previous,
                x, y,
                if (preferences.horizontal.value) Direction.rightToLeft else Direction.up,
                preferences.animationSpeed.value
            )
        } else {
            Reader.viewWidget.startAnimatedScrolling(
                if (forward) PageIndex.next else PageIndex.previous,
                if (preferences.horizontal.value) Direction.rightToLeft else Direction.up,
                preferences.animationSpeed.value
            )
        }
    }
}
