package org.geometerplus.android.util

import android.app.Activity
import android.content.Intent

object OrientationUtil {
    private const val KEY = "fbreader.orientation"

    @JvmStatic
    fun startActivity(current: Activity, intent: Intent) {
        current.startActivity(intent.putExtra(KEY, current.requestedOrientation))
    }

    @JvmStatic
    fun startActivityForResult(current: Activity, intent: Intent, requestCode: Int) {
        current.startActivityForResult(intent.putExtra(KEY, current.requestedOrientation), requestCode)
    }

    @JvmStatic
    fun setOrientation(activity: Activity, intent: Intent?) {
        if (intent == null) {
            return
        }
        val orientation = intent.getIntExtra(KEY, Int.MIN_VALUE)
        if (orientation != Int.MIN_VALUE) {
            activity.requestedOrientation = orientation
        }
    }
}
