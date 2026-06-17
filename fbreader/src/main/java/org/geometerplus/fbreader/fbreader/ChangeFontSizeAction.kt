package org.geometerplus.fbreader.fbreader

import org.geometerplus.zlibrary.core.options.ZLIntegerRangeOption

internal class ChangeFontSizeAction(fbreader: FBReaderApp, private val myDelta: Int) : FBAction(fbreader) {
    override fun run(vararg params: Any?) {
        val option: ZLIntegerRangeOption = Reader.viewOptions.getTextStyleCollection().getBaseStyle().fontSizeOption
        option.value = option.value + myDelta
        Reader.clearTextCaches()
        Reader.getViewWidget().repaint()
    }
}
