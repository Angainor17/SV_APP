package org.geometerplus.android.fbreader

import android.content.Intent
import org.geometerplus.fbreader.fbreader.FBReaderApp
import org.geometerplus.fbreader.util.TextSnippet
import org.geometerplus.zlibrary.core.resources.ZLResource

internal class SelectionShareAction(baseActivity: FBReader, fbreader: FBReaderApp) : FBAndroidAction(baseActivity, fbreader) {

    override fun run(vararg params: Any?) {
        val fbview = Reader.getTextView()
        val snippet: TextSnippet? = fbview.selectedSnippet
        if (snippet == null) {
            return
        }

        val text = snippet.getText()
        val title = Reader.currentBook?.title ?: ""
        fbview.clearSelection()

        val intent = Intent(android.content.Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(
            android.content.Intent.EXTRA_SUBJECT,
            ZLResource.resource("selection").getResource("quoteFrom").value.replace("%s", title)
        )
        intent.putExtra(android.content.Intent.EXTRA_TEXT, text)
        BaseActivity.startActivity(Intent.createChooser(intent, null))
    }
}
