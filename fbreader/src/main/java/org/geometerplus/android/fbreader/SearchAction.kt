package org.geometerplus.android.fbreader

import org.geometerplus.fbreader.fbreader.FBReaderApp

internal class SearchAction(baseActivity: FBReader, fbreader: FBReaderApp) : FBAndroidAction(baseActivity, fbreader) {
    override fun isVisible(): Boolean = Reader.model != null

    override fun run(vararg params: Any?) {
        BaseActivity.onSearchRequested()
    }
}
