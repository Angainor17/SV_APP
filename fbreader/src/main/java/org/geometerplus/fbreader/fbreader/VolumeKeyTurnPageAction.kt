package org.geometerplus.fbreader.fbreader

import org.geometerplus.zlibrary.core.view.ZLViewEnums.Direction
import org.geometerplus.zlibrary.core.view.ZLViewEnums.PageIndex

internal class VolumeKeyTurnPageAction(
    fbreader: FBReaderApp,
    private val forward: Boolean
) : FBAction(fbreader) {

    override fun run(vararg params: Any?) {
        val preferences = Reader.pageTurningOptions
        Reader.viewWidget.startAnimatedScrolling(
            if (forward) PageIndex.next else PageIndex.previous,
            if (preferences.horizontal.value) Direction.rightToLeft else Direction.up,
            preferences.animationSpeed.value
        )
    }
}
