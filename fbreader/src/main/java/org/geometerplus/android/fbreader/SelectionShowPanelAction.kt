package org.geometerplus.android.fbreader

import org.geometerplus.fbreader.fbreader.FBReaderApp

internal class SelectionShowPanelAction(baseActivity: FBReader, fbreader: FBReaderApp) : FBAndroidAction(baseActivity, fbreader) {
    override fun isEnabled(): Boolean = !Reader.getTextView().isSelectionEmpty

    override fun run(vararg params: Any?) {
        BaseActivity.showSelectionPanel()
    }
}
