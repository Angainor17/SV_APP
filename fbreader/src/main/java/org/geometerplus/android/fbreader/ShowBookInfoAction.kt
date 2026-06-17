package org.geometerplus.android.fbreader

import android.content.Intent
import org.geometerplus.android.fbreader.api.FBReaderIntents
import org.geometerplus.android.fbreader.library.BookInfoActivity
import org.geometerplus.android.util.OrientationUtil
import org.geometerplus.fbreader.fbreader.FBReaderApp

internal class ShowBookInfoAction(baseActivity: FBReader, fbreader: FBReaderApp) : FBAndroidAction(baseActivity, fbreader) {

    override fun isVisible(): Boolean = Reader.model != null

    override fun run(vararg params: Any?) {
        val intent =
            Intent(BaseActivity.applicationContext, BookInfoActivity::class.java)
                .putExtra(BookInfoActivity.FROM_READING_MODE_KEY, true)
        Reader.currentBook?.let { FBReaderIntents.putBookExtra(intent, it) }
        OrientationUtil.startActivity(BaseActivity, intent)
    }
}
