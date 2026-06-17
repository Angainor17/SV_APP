package org.geometerplus.android.fbreader

import org.geometerplus.android.fbreader.dict.DictionaryUtil
import org.geometerplus.fbreader.fbreader.DictionaryHighlighting
import org.geometerplus.fbreader.fbreader.FBReaderApp
import org.geometerplus.fbreader.util.TextSnippet

internal class SelectionTranslateAction(baseActivity: FBReader, fbreader: FBReaderApp) : FBAndroidAction(baseActivity, fbreader) {

    override fun run(vararg params: Any?) {
        val fbview = Reader.getTextView()
        val dictionaryHilite = DictionaryHighlighting.get(fbview)
        val snippet: TextSnippet? = fbview.selectedSnippet

        if (dictionaryHilite == null || snippet == null) {
            return
        }

        DictionaryUtil.openTextInDictionary(
            BaseActivity,
            snippet.getText(),
            fbview.countOfSelectedWords == 1,
            fbview.selectionStartY,
            fbview.selectionEndY
        ) {
            fbview.addHighlighting(dictionaryHilite)
            Reader.viewWidget.repaint()
        }
        fbview.clearSelection()
    }
}
