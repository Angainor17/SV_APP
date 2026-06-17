package org.geometerplus.android.fbreader

import android.content.Intent
import org.geometerplus.android.fbreader.preferences.PreferenceActivity
import org.geometerplus.android.util.OrientationUtil
import org.geometerplus.fbreader.fbreader.FBReaderApp

internal class ShowPreferencesAction(
    baseActivity: FBReader,
    fbreader: FBReaderApp
) : FBAndroidAction(baseActivity, fbreader) {

    override fun run(vararg params: Any) {
        val intent = Intent(BaseActivity.applicationContext, PreferenceActivity::class.java)
        if (params.size == 1 && params[0] is String) {
            intent.putExtra(PreferenceActivity.SCREEN_KEY, params[0] as String)
        }
        OrientationUtil.startActivityForResult(BaseActivity, intent, FBReaderMainActivity.REQUEST_PREFERENCES)
    }
}
