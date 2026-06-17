package org.geometerplus.fbreader.fbreader

internal class ExitAction(fbreader: FBReaderApp) : FBAction(fbreader) {
    override fun run(vararg params: Any?) {
        if (Reader.currentView !== Reader.bookTextView) {
            Reader.showBookTextView()
        } else {
            Reader.closeWindow()
        }
    }
}
