package org.geometerplus.fbreader.fbreader

internal class ClearFindResultsAction(fbreader: FBReaderApp) : FBAction(fbreader) {
    override fun run(vararg params: Any?) {
        Reader.getTextView().clearFindResults()
    }
}
