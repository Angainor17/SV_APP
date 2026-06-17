package org.geometerplus.fbreader.fbreader

internal class FindNextAction(fbreader: FBReaderApp) : FBAction(fbreader) {
    override fun isEnabled(): Boolean {
        val view = Reader.getTextView()
        return view != null && view.canFindNext()
    }

    override fun run(vararg params: Any?) {
        Reader.getTextView().findNext()
    }
}
