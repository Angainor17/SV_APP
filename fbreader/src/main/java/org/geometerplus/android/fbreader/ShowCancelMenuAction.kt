package org.geometerplus.android.fbreader

import org.geometerplus.android.fbreader.api.FBReaderIntents
import org.geometerplus.fbreader.fbreader.FBReaderApp

internal class ShowCancelMenuAction(
    baseActivity: FBReader,
    fbreader: FBReaderApp
) : FBAndroidAction(baseActivity, fbreader) {

    override fun run(vararg params: Any) {
        if (!Reader.jumpBack()) {
            if (Reader.hasCancelActions()) {
                BaseActivity.startActivityForResult(
                    FBReaderIntents.defaultInternalIntent(FBReaderIntents.Action.CANCEL_MENU),
                    FBReaderMainActivity.REQUEST_CANCEL_MENU
                )
            } else {
                Reader.closeWindow()
            }
        }
    }
}
