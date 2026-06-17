package org.geometerplus.android.fbreader

import org.geometerplus.fbreader.fbreader.FBReaderApp
import org.geometerplus.zlibrary.text.view.ZLTextView

internal class ShowNavigationAction(
    baseActivity: FBReader,
    fbreader: FBReaderApp
) : FBAndroidAction(baseActivity, fbreader) {

    override fun isVisible(): Boolean {
        val view = Reader.currentView as ZLTextView
        val textModel = view.getModel()
        return textModel != null && textModel.paragraphsNumber != 0
    }

    override fun run(vararg params: Any) {
        BaseActivity.navigate()
    }
}
