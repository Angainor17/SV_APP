package org.geometerplus.android.fbreader

import org.geometerplus.fbreader.fbreader.FBReaderApp

internal class SelectionHidePanelAction(
    baseActivity: FBReader,
    fbreader: FBReaderApp
) : FBAndroidAction(baseActivity, fbreader) {
    override fun run(vararg params: Any?) {
        BaseActivity.hideSelectionPanel()
    }
}
