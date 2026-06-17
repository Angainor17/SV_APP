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

package org.geometerplus.android.fbreader

import android.app.Activity
import android.view.ViewGroup
import android.widget.RelativeLayout
import org.geometerplus.fbreader.fbreader.FBReaderApp
import org.geometerplus.zlibrary.core.application.ZLApplication
import org.geometerplus.zlibrary.text.view.ZLTextWordCursor

abstract class PopupPanel(fbReader: FBReaderApp) : ZLApplication.PopupPanel(fbReader) {
    @JvmField var startPosition: ZLTextWordCursor? = null

    @Volatile
    @JvmField protected var myWindow: SimplePopupWindow? = null
    @Volatile
    private var myActivity: Activity? = null
    @Volatile
    private var myRoot: RelativeLayout? = null

    protected val reader: FBReaderApp
        get() = Application as FBReaderApp

    override fun show_() {
        if (myActivity != null) {
            createControlPanel(myActivity!!, myRoot!!)
        }
        myWindow?.show()
    }

    override fun hide_() {
        myWindow?.hide()
    }

    private fun removeWindow(activity: Activity) {
        if (myWindow != null && activity == myWindow!!.context) {
            val root = myWindow!!.parent as ViewGroup
            myWindow!!.hide()
            root.removeView(myWindow)
            myWindow = null
        }
    }

    fun initPosition() {
        if (startPosition == null) {
            startPosition = ZLTextWordCursor(reader.getTextView().getStartCursor())
        }
    }

    fun storePosition() {
        if (startPosition == null) {
            return
        }

        val reader = reader
        if (startPosition != reader.getTextView().getStartCursor()) {
            reader.addInvisibleBookmark(startPosition!!)
            reader.storePosition()
        }
    }

    fun setPanelInfo(activity: Activity, root: RelativeLayout) {
        myActivity = activity
        myRoot = root
    }

    abstract fun createControlPanel(activity: Activity, root: RelativeLayout)

    fun showPopup() {
        show_()
    }

    fun hidePopup() {
        hide_()
    }

    companion object {
        @JvmStatic
        fun removeAllWindows(application: ZLApplication, activity: Activity) {
            for (popup in application.popupPanels()) {
                when (popup) {
                    is PopupPanel -> popup.removeWindow(activity)
                    is NavigationPopup -> popup.removeWindow(activity)
                }
            }
        }

        fun restoreVisibilities(application: ZLApplication) {
            val popup = application.activePopup
            when (popup) {
                is PopupPanel -> popup.showPopup()
                is NavigationPopup -> popup.showPopup()
            }
        }
    }
}
