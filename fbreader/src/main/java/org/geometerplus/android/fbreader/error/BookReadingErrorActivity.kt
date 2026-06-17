package org.geometerplus.android.fbreader.error

import android.content.Intent
import android.os.Bundle
import org.geometerplus.android.fbreader.util.SimpleDialogActivity
import org.geometerplus.zlibrary.core.resources.ZLResource
import org.geometerplus.zlibrary.ui.android.error.ErrorKeys
import org.geometerplus.zlibrary.ui.android.error.ErrorUtil

class BookReadingErrorActivity : SimpleDialogActivity() {

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)

        val resource = ZLResource.resource("error").getResource("bookReading")
        title = resource.getResource("title").value

        textView().text = intent.getStringExtra(ErrorKeys.MESSAGE)

        okButton().setOnClickListener {
            val sendIntent = Intent(Intent.ACTION_SEND)
            sendIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("issues@fbreader.org"))
            sendIntent.putExtra(Intent.EXTRA_TEXT, intent.getStringExtra(ErrorKeys.STACKTRACE))
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "FBReader " + ErrorUtil(this@BookReadingErrorActivity).getVersionName() + " book reading issue report")
            sendIntent.type = "message/rfc822"
            startActivity(sendIntent)
            finish()
        }
        cancelButton().setOnClickListener(finishListener())
        setButtonTexts("sendReport", "cancel")
    }
}
