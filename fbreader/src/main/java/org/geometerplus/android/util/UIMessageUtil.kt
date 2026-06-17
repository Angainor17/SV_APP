package org.geometerplus.android.util

import android.app.Activity
import android.widget.Toast
import org.geometerplus.zlibrary.core.resources.ZLResource

object UIMessageUtil {
    @JvmStatic
    fun showMessageText(activity: Activity, text: String) {
        activity.runOnUiThread {
            Toast.makeText(activity, text, Toast.LENGTH_LONG).show()
        }
    }

    @JvmStatic
    fun showErrorMessage(activity: Activity, resourceKey: String) {
        showMessageText(
            activity,
            ZLResource.resource("errorMessage").getResource(resourceKey).value
        )
    }

    @JvmStatic
    fun showErrorMessage(activity: Activity, resourceKey: String, parameter: String) {
        showMessageText(
            activity,
            ZLResource.resource("errorMessage").getResource(resourceKey).value.replace("%s", parameter)
        )
    }
}
