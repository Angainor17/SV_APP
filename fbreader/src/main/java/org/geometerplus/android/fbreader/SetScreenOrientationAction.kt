package org.geometerplus.android.fbreader

import android.app.Activity
import android.content.pm.ActivityInfo
import org.fbreader.util.Boolean3
import org.geometerplus.fbreader.fbreader.FBReaderApp
import org.geometerplus.zlibrary.core.library.ZLibrary

internal class SetScreenOrientationAction(
    baseActivity: FBReader,
    fbreader: FBReaderApp,
    private val myOptionValue: String
) : FBAndroidAction(baseActivity, fbreader) {

    override fun isChecked(): Boolean3 {
        return if (myOptionValue == ZLibrary.Instance().orientationOption.value)
            Boolean3.TRUE else Boolean3.FALSE
    }

    override fun run(vararg params: Any) {
        setOrientation(BaseActivity, myOptionValue)
        ZLibrary.Instance().orientationOption.value = myOptionValue
        Reader.onRepaintFinished()
    }

    companion object {
        fun setOrientation(activity: Activity, optionValue: String) {
            var orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            when (optionValue) {
                ZLibrary.SCREEN_ORIENTATION_SENSOR -> orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                ZLibrary.SCREEN_ORIENTATION_PORTRAIT -> orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                ZLibrary.SCREEN_ORIENTATION_LANDSCAPE -> orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                ZLibrary.SCREEN_ORIENTATION_REVERSE_PORTRAIT -> orientation = 9 // ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                ZLibrary.SCREEN_ORIENTATION_REVERSE_LANDSCAPE -> orientation = 8 // ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            }
            activity.requestedOrientation = orientation
        }
    }
}
