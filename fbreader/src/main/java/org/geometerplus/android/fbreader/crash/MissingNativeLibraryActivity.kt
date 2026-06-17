package org.geometerplus.android.fbreader.crash

import android.os.Bundle
import org.geometerplus.android.fbreader.util.SimpleDialogActivity
import org.geometerplus.zlibrary.core.resources.ZLResource

class MissingNativeLibraryActivity : SimpleDialogActivity() {
    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)

        val resource = ZLResource.resource("crash").getResource("missingNativeLibrary")

        setTitle(resource.getResource("title").value)
        textView().text = resource.getResource("text").value
        okButton().setOnClickListener(finishListener())
        setButtonTexts("ok", null)
    }
}
