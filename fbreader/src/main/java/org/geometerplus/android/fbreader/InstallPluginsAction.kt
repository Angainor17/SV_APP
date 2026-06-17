package org.geometerplus.android.fbreader

import android.content.Intent
import org.geometerplus.fbreader.fbreader.FBReaderApp

internal class InstallPluginsAction(
    baseActivity: FBReader,
    fbreader: FBReaderApp
) : FBAndroidAction(baseActivity, fbreader) {
    override fun run(vararg params: Any?) {
        BaseActivity.startActivity(Intent(BaseActivity, PluginListActivity::class.java))
    }
}
