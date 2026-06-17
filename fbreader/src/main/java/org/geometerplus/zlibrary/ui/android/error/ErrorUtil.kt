package org.geometerplus.zlibrary.ui.android.error

import android.content.Context

class ErrorUtil(private val myContext: Context) {
    fun getVersionName(): String {
        return try {
            val info = myContext.packageManager.getPackageInfo(myContext.packageName, 0)
            "${info.versionName} (${info.versionCode})"
        } catch (e: Exception) {
            ""
        }
    }
}
