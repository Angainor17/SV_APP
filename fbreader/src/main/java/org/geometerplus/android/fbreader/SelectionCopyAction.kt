package org.geometerplus.android.fbreader

import android.app.Application
import android.text.ClipboardManager
import org.geometerplus.android.util.UIMessageUtil
import org.geometerplus.fbreader.fbreader.FBReaderApp
import org.geometerplus.fbreader.util.TextSnippet
import org.geometerplus.zlibrary.core.resources.ZLResource

internal class SelectionCopyAction(baseActivity: FBReader, fbreader: FBReaderApp) : FBAndroidAction(baseActivity, fbreader) {

    override fun run(vararg params: Any?) {
        val fbview = Reader.getTextView()
        val snippet: TextSnippet? = fbview.selectedSnippet
        if (snippet == null) {
            return
        }

        val text = snippet.getText()
        fbview.clearSelection()

        val clipboard =
            BaseActivity.application.getSystemService(Application.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.text = text
        UIMessageUtil.showMessageText(
            BaseActivity,
            ZLResource.resource("selection").getResource("textInBuffer").value.replace("%s", clipboard.text.toString())
        )
    }
}
