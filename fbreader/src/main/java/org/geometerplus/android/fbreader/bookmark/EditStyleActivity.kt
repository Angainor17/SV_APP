/*
 * Copyright (C) 2010-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.android.fbreader.bookmark

import android.os.Bundle
import android.preference.PreferenceActivity
import android.view.Window
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow
import org.geometerplus.android.fbreader.preferences.ColorPreference
import org.geometerplus.android.fbreader.preferences.ZLCheckBoxPreference
import org.geometerplus.android.fbreader.preferences.ZLStringPreference
import org.geometerplus.fbreader.book.BookmarkUtil
import org.geometerplus.fbreader.book.HighlightingStyle
import org.geometerplus.zlibrary.core.resources.ZLResource
import org.geometerplus.zlibrary.core.util.ZLColor
import org.geometerplus.zlibrary.ui.android.library.UncaughtExceptionHandler

class EditStyleActivity : PreferenceActivity() {

    private val myRootResource = ZLResource.resource("editStyle")
    private val myCollection = BookCollectionShadow()
    private var myStyle: HighlightingStyle? = null
    private var myBgColorPreference: BgColorPreference? = null

    override fun onCreate(bundle: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        super.onCreate(bundle)
        Thread.setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler(this))

        val screen = preferenceManager.createPreferenceScreen(this)
        preferenceScreen = screen

        myCollection.bindToService(this) {
            myStyle = myCollection.getHighlightingStyle(intent.getIntExtra(STYLE_ID_KEY, -1))
            if (myStyle == null) {
                finish()
                return@bindToService
            }
            screen.addPreference(NamePreference())
            screen.addPreference(InvisiblePreference())
            myBgColorPreference = BgColorPreference()
            screen.addPreference(myBgColorPreference)
        }
    }

    override fun onDestroy() {
        myCollection.unbind()
        super.onDestroy()
    }

    private inner class NamePreference : ZLStringPreference(this@EditStyleActivity, myRootResource, "name") {
        init {
            value = BookmarkUtil.getStyleName(myStyle!!)
        }

        override fun setValue(value: String) {
            super.setValue(value)
            BookmarkUtil.setStyleName(myStyle!!, value)
            myCollection.saveHighlightingStyle(myStyle!!)
        }
    }

    private inner class InvisiblePreference : ZLCheckBoxPreference(this@EditStyleActivity, myRootResource.getResource("invisible")) {
        private var mySavedBgColor: ZLColor? = null

        init {
            setChecked(myStyle!!.backgroundColor == null)
        }

        override fun onClick() {
            super.onClick()
            if (isChecked) {
                mySavedBgColor = myStyle!!.backgroundColor
                myStyle!!.backgroundColor = null
                myBgColorPreference!!.isEnabled = false
            } else {
                myStyle!!.backgroundColor = mySavedBgColor ?: ZLColor(127, 127, 127)
                myBgColorPreference!!.isEnabled = true
            }
            myCollection.saveHighlightingStyle(myStyle!!)
        }
    }

    private inner class BgColorPreference : ColorPreference(this@EditStyleActivity) {
        init {
            isEnabled = savedColor != null
        }

        override fun getTitle(): String = myRootResource.getResource("bgColor").value

        override fun getSavedColor(): ZLColor? = myStyle!!.backgroundColor

        override fun saveColor(color: ZLColor) {
            myStyle!!.backgroundColor = color
            myCollection.saveHighlightingStyle(myStyle!!)
        }
    }

    companion object {
        const val STYLE_ID_KEY = "style.id"
    }
}
