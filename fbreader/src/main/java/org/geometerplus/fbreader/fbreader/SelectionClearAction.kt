package org.geometerplus.fbreader.fbreader

internal class SelectionClearAction(fbreader: FBReaderApp) : FBAction(fbreader) {
    override fun run(vararg params: Any?) {
        Reader.getTextView().clearSelection()
    }
}
