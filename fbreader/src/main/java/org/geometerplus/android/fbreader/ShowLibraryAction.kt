package org.geometerplus.android.fbreader

import android.content.ActivityNotFoundException
import android.content.Intent
import org.geometerplus.android.fbreader.api.FBReaderIntents
import org.geometerplus.android.fbreader.library.LibraryActivity
import org.geometerplus.android.util.OrientationUtil
import org.geometerplus.android.util.PackageUtil
import org.geometerplus.fbreader.fbreader.FBReaderApp

internal class ShowLibraryAction(baseActivity: FBReader, fbreader: FBReaderApp) : FBAndroidAction(baseActivity, fbreader) {

    override fun run(vararg params: Any?) {
        val externalIntent = Intent(FBReaderIntents.Action.EXTERNAL_LIBRARY)
        val internalIntent = Intent(BaseActivity.applicationContext, LibraryActivity::class.java)
        if (PackageUtil.canBeStarted(BaseActivity, externalIntent, true)) {
            try {
                startLibraryActivity(externalIntent)
            } catch (e: ActivityNotFoundException) {
                startLibraryActivity(internalIntent)
            }
        } else {
            startLibraryActivity(internalIntent)
        }
    }

    private fun startLibraryActivity(intent: Intent) {
        if (Reader.model != null) {
            Reader.currentBook?.let { FBReaderIntents.putBookExtra(intent, it) }
        }
        OrientationUtil.startActivity(BaseActivity, intent)
    }
}
