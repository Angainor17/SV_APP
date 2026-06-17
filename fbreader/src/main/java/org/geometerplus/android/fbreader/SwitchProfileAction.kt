package org.geometerplus.android.fbreader

import org.geometerplus.fbreader.fbreader.FBReaderApp

internal class SwitchProfileAction(
    baseActivity: FBReader,
    fbreader: FBReaderApp,
    private val myProfileName: String
) : FBAndroidAction(baseActivity, fbreader) {

    override fun isVisible(): Boolean = myProfileName != Reader.viewOptions.colorProfileName.value

    override fun run(vararg params: Any) {
        Reader.viewOptions.colorProfileName.value = myProfileName
        Reader.viewWidget.reset()
        Reader.viewWidget.repaint()
    }
}
