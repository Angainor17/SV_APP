package org.geometerplus.fbreader.fbreader

import org.geometerplus.zlibrary.text.view.ZLTextView

internal class FindPreviousAction(fbreader: FBReaderApp) : FBAction(fbreader) {
    override fun isEnabled(): Boolean {
        val view: ZLTextView? = Reader.getTextView()
        return view != null && view.canFindPrevious()
    }

    override fun run(vararg params: Any?) {
        Reader.getTextView().findPrevious()
    }
}
