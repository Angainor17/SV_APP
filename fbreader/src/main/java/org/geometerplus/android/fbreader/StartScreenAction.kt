package org.geometerplus.android.fbreader

import org.geometerplus.fbreader.fbreader.FBReaderApp

internal class StartScreenAction(
    baseActivity: FBReader,
    fbreader: FBReaderApp
) : FBAndroidAction(baseActivity, fbreader) {
    override fun run(vararg params: Any?) {
        Reader.openHelpBook()
    }
}
