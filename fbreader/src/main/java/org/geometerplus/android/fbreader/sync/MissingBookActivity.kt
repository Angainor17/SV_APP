package org.geometerplus.android.fbreader.sync

import android.os.Bundle
import org.geometerplus.android.fbreader.network.BookDownloaderService
import org.geometerplus.android.fbreader.util.SimpleDialogActivity
import org.geometerplus.zlibrary.core.resources.ZLResource

class MissingBookActivity : SimpleDialogActivity() {

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)

        val intent = intent
        val title = intent.getStringExtra(BookDownloaderService.Key.BOOK_TITLE)
        setTitle(errorTitle())
        textView().setText(errorMessage(title))
        intent.setClass(this, BookDownloaderService::class.java)

        okButton().setOnClickListener {
            startService(intent)
            finish()
        }
        setButtonTexts("download", null)
    }

    companion object {
        fun errorTitle(): String {
            return ZLResource.resource("errorMessage").getResource("bookIsMissingTitle").value
        }

        fun errorMessage(title: String?): String {
            return ZLResource.resource("errorMessage").getResource("bookIsMissing").value
                .replace("%s", title ?: "")
        }
    }
}
