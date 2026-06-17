package org.geometerplus.android.fbreader

import android.content.ActivityNotFoundException
import android.content.Intent
import org.geometerplus.android.fbreader.api.FBReaderIntents
import org.geometerplus.android.fbreader.bookmark.BookmarksActivity
import org.geometerplus.android.util.OrientationUtil
import org.geometerplus.android.util.PackageUtil
import org.geometerplus.fbreader.fbreader.FBReaderApp

internal class ShowBookmarksAction(baseActivity: FBReader, fbreader: FBReaderApp) : FBAndroidAction(baseActivity, fbreader) {

    override fun isVisible(): Boolean = Reader.model != null

    override fun run(vararg params: Any?) {
        val externalIntent = Intent(FBReaderIntents.Action.EXTERNAL_BOOKMARKS)
        val internalIntent = Intent(BaseActivity.applicationContext, BookmarksActivity::class.java)
        if (PackageUtil.canBeStarted(BaseActivity, externalIntent, true)) {
            try {
                startBookmarksActivity(externalIntent)
            } catch (e: ActivityNotFoundException) {
                startBookmarksActivity(internalIntent)
            }
        } else {
            startBookmarksActivity(internalIntent)
        }
    }

    private fun startBookmarksActivity(intent: Intent) {
        Reader.currentBook?.let { FBReaderIntents.putBookExtra(intent, it) }
        Reader.createBookmark(80, true)?.let { FBReaderIntents.putBookmarkExtra(intent, it) }
        OrientationUtil.startActivity(BaseActivity, intent)
    }
}
