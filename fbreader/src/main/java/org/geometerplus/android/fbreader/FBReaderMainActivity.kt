/*
 * Copyright (C) 2009-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.github.johnpersano.supertoasts.SuperActivityToast
import org.geometerplus.android.fbreader.dict.DictionaryUtil
import org.geometerplus.zlibrary.core.options.ZLIntegerOption
import org.geometerplus.zlibrary.core.options.ZLIntegerRangeOption
import org.geometerplus.zlibrary.core.options.ZLStringOption
import org.geometerplus.zlibrary.ui.android.library.UncaughtExceptionHandler
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidLibrary
import org.geometerplus.zlibrary.ui.android.view.AndroidFontUtil

abstract class FBReaderMainActivity : Activity() {

    @Volatile
    private var myToast: SuperActivityToast? = null

    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        Thread.setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler(this))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_DICTIONARY -> DictionaryUtil.onActivityResult(this, resultCode, data)
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun getZLibrary(): ZLAndroidLibrary {
        return (application as ZLAndroidApplication).library()
    }

    /* ++++++ SCREEN BRIGHTNESS ++++++ */
    protected fun setScreenBrightnessAuto() {
        val attrs = window.attributes
        attrs.screenBrightness = -1.0f
        window.attributes = attrs
    }

    fun getScreenBrightnessSystem(): Float {
        val level = window.attributes.screenBrightness
        return if (level >= 0) level else .5f
    }

    fun setScreenBrightnessSystem(level: Float) {
        val attrs = window.attributes
        attrs.screenBrightness = level
        window.attributes = attrs
    }
    /* ------ SCREEN BRIGHTNESS ------ */

    /* ++++++ SUPER TOAST ++++++ */
    fun isToastShown(): Boolean {
        val toast = myToast
        return toast != null && toast.isShowing()
    }

    fun hideToast() {
        val toast = myToast
        if (toast != null && toast.isShowing()) {
            myToast = null
            runOnUiThread {
                toast.dismiss()
            }
        }
    }

    fun showToast(toast: SuperActivityToast) {
        hideToast()
        myToast = toast
        // TODO: avoid this hack (accessing text style via option)
        val dpi = getZLibrary().displayDPI
        val defaultFontSize = dpi * 18 / 160
        val fontSize = ZLIntegerOption("Style", "Base:fontSize", defaultFontSize).value
        val percent = ZLIntegerRangeOption("Options", "ToastFontSizePercent", 25, 100, 90).value
        val dpFontSize = fontSize * 160 * percent / dpi / 100
        toast.setTextSize(dpFontSize)
        toast.setButtonTextSize(dpFontSize * 7 / 8)

        val fontFamily = ZLStringOption("Style", "Base:fontFamily", "sans-serif").value
        toast.setTypeface(AndroidFontUtil.systemTypeface(fontFamily, false, false))

        runOnUiThread {
            toast.show()
        }
    }
    /* ------ SUPER TOAST ------ */

    abstract fun hideDictionarySelection()

    companion object {
        const val REQUEST_PREFERENCES = 1
        const val REQUEST_CANCEL_MENU = 2
        const val REQUEST_DICTIONARY = 3
    }
}
