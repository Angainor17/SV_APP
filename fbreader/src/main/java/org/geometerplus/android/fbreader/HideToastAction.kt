package org.geometerplus.android.fbreader

import org.geometerplus.fbreader.fbreader.FBReaderApp

internal class HideToastAction(baseActivity: FBReader, fbreader: FBReaderApp) : FBAndroidAction(baseActivity, fbreader) {
    override fun isEnabled(): Boolean = BaseActivity.isToastShown()

    override fun run(vararg params: Any?) {
        BaseActivity.hideToast()
    }
}
